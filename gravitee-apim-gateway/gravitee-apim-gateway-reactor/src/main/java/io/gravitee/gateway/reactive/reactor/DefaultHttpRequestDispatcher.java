/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.reactor;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerResponse;
import io.gravitee.gateway.reactive.reactor.handler.EntrypointResolver;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.vertx.reactivex.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultHttpRequestDispatcher
    extends AbstractService<HttpRequestDispatcher>
    implements HttpRequestDispatcher, EventListener<ReactorEvent, Reactable> {

    private final Logger log = LoggerFactory.getLogger(DefaultHttpRequestDispatcher.class);
    private final IdGenerator idGenerator;

    @Autowired
    protected EventManager eventManager;

    @Autowired
    protected GatewayConfiguration gatewayConfiguration;

    @Autowired
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Autowired
    private EntrypointResolver entrypointResolver;

    public DefaultHttpRequestDispatcher(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public Completable dispatch(HttpServerRequest httpServerRequest) {
        log.debug("Dispatching request on host {} and path {}", httpServerRequest.host(), httpServerRequest.path());
        return Maybe
            .fromCallable(() -> entrypointResolver.resolve(httpServerRequest.host(), httpServerRequest.path()))
            .flatMapCompletable(
                handlerEntrypoint -> {
                    final ReactorHandler target = handlerEntrypoint.target();

                    // TODO: NotFoundHandler to make it work like an api and avoid creating a NotFoundProcessor;

                    if (target instanceof ApiReactor) {
                        // For now, supports only sync requests.
                        VertxHttpServerRequest request = new VertxHttpServerRequest(
                            httpServerRequest,
                            handlerEntrypoint.path(),
                            idGenerator
                        );
                        VertxHttpServerResponse response = new VertxHttpServerResponse(request);

                        // Set gateway tenant
                        gatewayConfiguration.tenant().ifPresent(tenant -> request.metrics().setTenant(tenant));

                        // Set gateway zone
                        gatewayConfiguration.zone().ifPresent(zone -> request.metrics().setZone(zone));

                        return ((ApiReactor) target).handle(request, response);
                    } else {
                        // TODO: Handle v3 compatibility.
                        return httpServerRequest.response().rxEnd();
                    }
                }
            );
    }

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> event) {
        switch (event.type()) {
            case DEPLOY:
                reactorHandlerRegistry.create(event.content());
                break;
            case UPDATE:
                reactorHandlerRegistry.update(event.content());
                break;
            case UNDEPLOY:
                reactorHandlerRegistry.remove(event.content());
                break;
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        eventManager.subscribeForEvents(this, ReactorEvent.class);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        reactorHandlerRegistry.clear();
    }
}
