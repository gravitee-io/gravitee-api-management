package io.gravitee.gateway.reactive.reactor;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.http.vertx.VertxSyncHttpServerRequest;
import io.gravitee.gateway.reactive.http.vertx.VertxSyncHttpServerResponse;
import io.gravitee.gateway.reactive.reactor.handler.EntrypointResolver;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.handler.HandlerEntrypoint;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.reactivex.Completable;
import io.vertx.reactivex.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultHttpRequestDispatcher extends AbstractService<HttpRequestDispatcher> implements HttpRequestDispatcher, EventListener<ReactorEvent, Reactable> {

    private final Logger log = LoggerFactory.getLogger(DefaultHttpRequestDispatcher.class);

    @Autowired
    protected EventManager eventManager;

    @Autowired
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Autowired
    private EntrypointResolver entrypointResolver;

    @Autowired
    protected GatewayConfiguration gatewayConfiguration;

    private final IdGenerator idGenerator;

    public DefaultHttpRequestDispatcher(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public Completable dispatch(HttpServerRequest httpServerRequest) {
        final HandlerEntrypoint entrypoint = entrypointResolver.resolve(httpServerRequest.host(), httpServerRequest.path());
        final ReactorHandler target = entrypoint.target();

        // TODO: NotFoundHandler to make it work like an api and avoid creating a NotFoundProcessor;
        Request<?> request;
        Response<?> response;

        if (target instanceof ApiReactor) {
            // For now, supports only sync requests.
            request = new VertxSyncHttpServerRequest(httpServerRequest, entrypoint.path(), idGenerator);
            response = new VertxSyncHttpServerResponse((VertxSyncHttpServerRequest) request);

            // Set gateway tenant
            gatewayConfiguration.tenant().ifPresent(tenant -> request.metrics().setTenant(tenant));

            // Set gateway zone
            gatewayConfiguration.zone().ifPresent(zone -> request.metrics().setZone(zone));

            return ((ApiReactor<VertxSyncHttpServerRequest, VertxSyncHttpServerResponse>) target).handle(
                    (VertxSyncHttpServerRequest) request,
                    (VertxSyncHttpServerResponse) response
                );
        } else {
            // TODO: Handle v3 compatibility.
            return httpServerRequest.response().rxEnd();
        }
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
