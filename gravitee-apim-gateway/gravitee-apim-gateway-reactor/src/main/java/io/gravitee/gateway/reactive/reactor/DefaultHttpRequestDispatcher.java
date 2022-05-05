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
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.http.utils.WebSocketUtils;
import io.gravitee.gateway.http.vertx.VertxHttp2ServerRequest;
import io.gravitee.gateway.http.vertx.grpc.VertxGrpcServerRequest;
import io.gravitee.gateway.http.vertx.ws.VertxWebSocketServerRequest;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest;
import io.gravitee.gateway.reactive.reactor.handler.EntrypointResolver;
import io.gravitee.gateway.reactive.reactor.processor.PlatformProcessorChainFactory;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.handler.HandlerEntrypoint;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.vertx.core.http.HttpVersion;
import io.vertx.reactivex.core.http.HttpHeaders;
import io.vertx.reactivex.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Request dispatcher responsible to dispatch any HTTP request to the appropriate {@link io.gravitee.gateway.reactor.handler.ReactorHandler}.
 * The execution mode depends on the entrypoint resolved and the associated handler:
 * <ul>
 *     <li>{@link ExecutionMode#JUPITER}: request is handled by an instance of {@link ApiReactor}</li>
 *     <li>{@link ExecutionMode#V3}: request is handled by an instance of {@link ReactorHandler}</li>
 * </ul>
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultHttpRequestDispatcher
    extends AbstractService<HttpRequestDispatcher>
    implements HttpRequestDispatcher, EventListener<ReactorEvent, Reactable> {

    public static final String ATTR_ENTRYPOINT = ExecutionContext.ATTR_PREFIX + "entrypoint";
    private final Logger log = LoggerFactory.getLogger(DefaultHttpRequestDispatcher.class);
    private final EventManager eventManager;
    private final GatewayConfiguration gatewayConfiguration;
    private final ReactorHandlerRegistry reactorHandlerRegistry;
    private final EntrypointResolver entrypointResolver;
    private final IdGenerator idGenerator;
    private final RequestProcessorChainFactory requestProcessorChainFactory;
    private final ResponseProcessorChainFactory responseProcessorChainFactory;

    public DefaultHttpRequestDispatcher(
        EventManager eventManager,
        GatewayConfiguration gatewayConfiguration,
        ReactorHandlerRegistry reactorHandlerRegistry,
        EntrypointResolver entrypointResolver,
        IdGenerator idGenerator,
        RequestProcessorChainFactory requestProcessorChainFactory,
        ResponseProcessorChainFactory responseProcessorChainFactory
    ) {
        this.eventManager = eventManager;
        this.gatewayConfiguration = gatewayConfiguration;
        this.reactorHandlerRegistry = reactorHandlerRegistry;
        this.entrypointResolver = entrypointResolver;
        this.idGenerator = idGenerator;
        this.requestProcessorChainFactory = requestProcessorChainFactory;
        this.responseProcessorChainFactory = responseProcessorChainFactory;
    }

    /**
     * {@inheritDoc}
     * Each incoming request is dispatched respecting the following step order:
     * <ul>
     *     <li>Api resolution: resolves the {@link ReactorHandler} that is able to handle the request based on the request host path.</li>
     *     <li>Api request: invokes the V3 or Jupiter {@link ReactorHandler} to handle the api request. Eventually, handle not found if no handler has been resolved.</li>
     *     <li>Platform processors: in case of V3 {@link ReactorHandler} pre and post platform processor are executed</li>
     * </ul>
     */
    @Override
    public Completable dispatch(HttpServerRequest httpServerRequest) {
        log.debug("Dispatching request on host {} and path {}", httpServerRequest.host(), httpServerRequest.path());

        final HandlerEntrypoint handlerEntrypoint = entrypointResolver.resolve(httpServerRequest.host(), httpServerRequest.path());

        Completable dispatchObs;

        if (handlerEntrypoint == null || handlerEntrypoint.target() == null) {
            // FIXME: Handle Not Found when no api has been resolved for the targeted host and path.
            dispatchObs = handleNotFound(httpServerRequest);
        } else if (handlerEntrypoint.executionMode() == ExecutionMode.JUPITER) {
            // Jupiter execution mode.
            dispatchObs = handleJupiterRequest(httpServerRequest, handlerEntrypoint);
        } else {
            // V3 execution mode.
            dispatchObs = handleV3Request(httpServerRequest, handlerEntrypoint);
        }

        return dispatchObs;
    }

    private Completable handleJupiterRequest(HttpServerRequest httpServerRequest, HandlerEntrypoint handlerEntrypoint) {
        final ApiReactor apiReactor = handlerEntrypoint.target();

        // For now, supports only sync requests.
        VertxHttpServerRequest request = new VertxHttpServerRequest(httpServerRequest, handlerEntrypoint.path(), idGenerator);

        // Set gateway tenants and zones in request metrics.
        prepareMetrics(request.metrics());

        return apiReactor.handle(request, request.response());
    }

    private Completable handleV3Request(HttpServerRequest httpServerRequest, HandlerEntrypoint handlerEntrypoint) {
        final ReactorHandler reactorHandler = handlerEntrypoint.target();

        io.gravitee.gateway.http.vertx.VertxHttpServerRequest request = createV3Request(httpServerRequest);

        // Prepare invocation execution context.
        SimpleExecutionContext simpleExecutionContext = new SimpleExecutionContext(request, request.create());

        // Required by the v3 execution mode.
        simpleExecutionContext.setAttribute(ATTR_ENTRYPOINT, handlerEntrypoint);

        // Set gateway tenants and zones in request metrics.
        prepareMetrics(request.metrics());
        // Prepare handler chain and catch the end of the v3 request handling to complete the reactive chain.
        return Completable.create(
            emitter -> {
                Handler<io.gravitee.gateway.api.ExecutionContext> endHandler = endRequestHandler(emitter, httpServerRequest);
                requestProcessorChainFactory
                    .create()
                    .handler(
                        ctx -> {
                            reactorHandler.handle(ctx, endHandler);
                        }
                    )
                    .errorHandler(result -> processResponse(simpleExecutionContext, endHandler))
                    .exitHandler(result -> processResponse(simpleExecutionContext, endHandler))
                    .handle(simpleExecutionContext);
            }
        );
    }

    private Handler<io.gravitee.gateway.api.ExecutionContext> endRequestHandler(
        final CompletableEmitter emitter,
        final HttpServerRequest httpServerRequest
    ) {
        return context -> {
            if (context.response().ended()) {
                emitter.onComplete();
            } else {
                httpServerRequest.response().rxEnd().subscribe(emitter::onComplete, emitter::onError);
            }
        };
    }

    /**
     * Prepare some global metrics for the current request (tenants, zones, ...).
     *
     * @param metrics the {@link Metrics} object to add information on.
     */
    private void prepareMetrics(Metrics metrics) {
        // Set gateway tenant
        gatewayConfiguration.tenant().ifPresent(metrics::setTenant);

        // Set gateway zone
        gatewayConfiguration.zone().ifPresent(metrics::setZone);
    }

    private Completable handleNotFound(HttpServerRequest httpServerRequest) {
        // FIXME: properly handle not found.
        httpServerRequest.response().setStatusCode(HttpStatusCode.NOT_FOUND_404);
        return httpServerRequest.response().rxEnd();
    }

    private io.gravitee.gateway.http.vertx.VertxHttpServerRequest createV3Request(HttpServerRequest httpServerRequest) {
        io.gravitee.gateway.http.vertx.VertxHttpServerRequest request;

        if (isV3WebSocket(httpServerRequest)) {
            request = new VertxWebSocketServerRequest(httpServerRequest.getDelegate(), idGenerator);
        } else {
            if (httpServerRequest.version() == HttpVersion.HTTP_2) {
                if (MediaType.APPLICATION_GRPC.equals(httpServerRequest.getHeader(HttpHeaders.CONTENT_TYPE))) {
                    request = new VertxGrpcServerRequest(httpServerRequest.getDelegate(), idGenerator);
                } else {
                    request = new VertxHttp2ServerRequest(httpServerRequest.getDelegate(), idGenerator);
                }
            } else {
                request = new io.gravitee.gateway.http.vertx.VertxHttpServerRequest(httpServerRequest.getDelegate(), idGenerator);
            }
        }

        return request;
    }

    /**
     * We are only considering HTTP_1.x requests for now.
     * There is a dedicated RFC to support WebSockets over HTTP2: https://tools.ietf.org/html/rfc8441
     *
     * @param httpServerRequest
     * @return
     */
    private boolean isV3WebSocket(HttpServerRequest httpServerRequest) {
        String connectionHeader = httpServerRequest.getHeader(HttpHeaders.CONNECTION);
        String upgradeHeader = httpServerRequest.getHeader(HttpHeaders.UPGRADE);
        return (
            (httpServerRequest.version() == HttpVersion.HTTP_1_0 || httpServerRequest.version() == HttpVersion.HTTP_1_1) &&
            WebSocketUtils.isWebSocket(httpServerRequest.method().name(), connectionHeader, upgradeHeader)
        );
    }

    private void processResponse(
        io.gravitee.gateway.api.ExecutionContext context,
        Handler<io.gravitee.gateway.api.ExecutionContext> handler
    ) {
        responseProcessorChainFactory.create().handler(handler).handle(context);
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
