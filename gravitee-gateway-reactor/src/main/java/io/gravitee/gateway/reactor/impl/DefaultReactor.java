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
package io.gravitee.gateway.reactor.impl;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.handler.ReactorHandlerResolver;
import io.gravitee.gateway.reactor.handler.ResponseTimeHandler;
import io.gravitee.gateway.reactor.handler.reporter.ReporterHandler;
import io.gravitee.gateway.report.ReporterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.CompletableFuture;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class DefaultReactor extends AbstractService implements
        Reactor, EventListener<ReactorEvent, Reactable> {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultReactor.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    @Qualifier("notFoundHandler")
    private ReactorHandler notFoundHandler;

    @Autowired
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Autowired
    private ReactorHandlerResolver reactorHandlerResolver;

    @Autowired
    private ReporterService reporterService;

    @Override
    public CompletableFuture<Response> process(final Request serverRequest, final Response serverResponse) {
        LOGGER.debug("Receiving a request {} for path {}", serverRequest.id(), serverRequest.path());

        ReactorHandler handler = getHandler(serverRequest);

        // Prepare the handler chain
        return handler.handle(serverRequest, serverResponse).whenCompleteAsync(
                new ResponseTimeHandler(serverRequest).andThen(new ReporterHandler(reporterService, serverRequest)));
    }

    private ReactorHandler getHandler(final Request serverRequest) {
            LOGGER.debug("Receiving a request {} for path {}", serverRequest.id(), serverRequest.path());

            ReactorHandler reactorHandler = reactorHandlerResolver.resolve(serverRequest);
            reactorHandler = (reactorHandler != null) ? reactorHandler : notFoundHandler;

            // Prepare the handler chain
            return reactorHandler;
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
