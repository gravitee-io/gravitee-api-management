/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.reactor;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_LISTENER_TYPE;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_TRACING_ERROR;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_TRACING_ROOT_SPAN;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.RequestClientAuthConfiguration;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.http.utils.RequestUtils;
import io.gravitee.gateway.http.vertx.VertxHttp2ServerRequest;
import io.gravitee.gateway.http.vertx.grpc.VertxGrpcServerRequest;
import io.gravitee.gateway.http.vertx.ws.VertxWebSocketServerRequest;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.hook.ProcessorHook;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.hook.HookHelper;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest;
import io.gravitee.gateway.reactive.reactor.handler.HttpAcceptorResolver;
import io.gravitee.gateway.reactive.reactor.processor.DefaultPlatformProcessorChainFactory;
import io.gravitee.gateway.reactive.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpServerRequest;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpServerResponse;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpVersion;
import io.vertx.rxjava3.core.http.HttpHeaders;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Request dispatcher responsible to dispatch any HTTP request to the appropriate {@link io.gravitee.gateway.reactor.handler.ReactorHandler}.
 * The execution mode depends on the reactable resolved and the associated handler:
 * <ul>
 *     <li>{@link ExecutionMode#V4_EMULATION_ENGINE}: request is handled by an instance of {@link ApiReactor}</li>
 *     <li>{@link ExecutionMode#V3}: request is handled by an instance of {@link ReactorHandler}</li>
 * </ul>
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DefaultHttpRequestDispatcher implements HttpRequestDispatcher {

    private static final String ATTR_INTERNAL_VERTX_TIMER_ID = ContextAttributes.ATTR_PREFIX + "vertx-timer-id";
    public static final String ATTR_ENTRYPOINT = ContextAttributes.ATTR_PREFIX + "entrypoint";
    private final GatewayConfiguration gatewayConfiguration;
    private final HttpAcceptorResolver httpAcceptorResolver;
    private final IdGenerator idGenerator;
    private final RequestProcessorChainFactory requestProcessorChainFactory;
    private final ResponseProcessorChainFactory responseProcessorChainFactory;
    private final DefaultPlatformProcessorChainFactory platformProcessorChainFactory;
    private final NotFoundProcessorChainFactory notFoundProcessorChainFactory;
    private final TracingContext gatewayTracingContext;
    private final RequestTimeoutConfiguration requestTimeoutConfiguration;
    private final RequestClientAuthConfiguration requestClientAuthConfiguration;
    private final Vertx vertx;
    private final ComponentProvider globalComponentProvider;
    private final TracingHook tracingHook;

    public DefaultHttpRequestDispatcher(
        GatewayConfiguration gatewayConfiguration,
        HttpAcceptorResolver httpAcceptorResolver,
        IdGenerator idGenerator,
        ComponentProvider globalComponentProvider,
        RequestProcessorChainFactory requestProcessorChainFactory,
        ResponseProcessorChainFactory responseProcessorChainFactory,
        DefaultPlatformProcessorChainFactory platformProcessorChainFactory,
        NotFoundProcessorChainFactory notFoundProcessorChainFactory,
        TracingContext gatewayTracingContext,
        RequestTimeoutConfiguration requestTimeoutConfiguration,
        RequestClientAuthConfiguration requestClientAuthConfiguration,
        Vertx vertx
    ) {
        this.gatewayConfiguration = gatewayConfiguration;
        this.httpAcceptorResolver = httpAcceptorResolver;
        this.idGenerator = idGenerator;
        this.globalComponentProvider = globalComponentProvider;
        this.requestProcessorChainFactory = requestProcessorChainFactory;
        this.responseProcessorChainFactory = responseProcessorChainFactory;
        this.platformProcessorChainFactory = platformProcessorChainFactory;
        this.notFoundProcessorChainFactory = notFoundProcessorChainFactory;
        this.gatewayTracingContext = gatewayTracingContext;
        this.requestTimeoutConfiguration = requestTimeoutConfiguration;
        this.requestClientAuthConfiguration = requestClientAuthConfiguration;
        this.vertx = vertx;
        this.tracingHook = new TracingHook("Processor chain");
    }

    /**
     * {@inheritDoc}
     * Each incoming request is dispatched respecting the following step order:
     * <ul>
     *     <li>Api resolution: resolves the {@link ReactorHandler} that is able to handle the request based on the request host path.</li>
     *     <li>Api request: invokes the V3 or V4 emulation engine {@link ReactorHandler} to handle the api request. Eventually, handle not found if no handler has been resolved.</li>
     *     <li>Platform processors: in case of V3 {@link ReactorHandler} pre and post platform processor are executed</li>
     * </ul>
     */
    @Override
    public Completable dispatch(HttpServerRequest httpServerRequest, String serverId) {
        log.debug("Dispatching request on host {} and path {}", httpServerRequest.host(), httpServerRequest.path());

        final HttpAcceptor httpAcceptor = httpAcceptorResolver.resolve(httpServerRequest.host(), httpServerRequest.path(), serverId);
        Context vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        if (httpAcceptor == null || httpAcceptor.reactor() == null) {
            MutableExecutionContext mutableCtx = prepareExecutionContext(httpServerRequest);
            mutableCtx.tracer(
                new io.gravitee.gateway.reactive.api.tracing.Tracer(vertxContext, gatewayTracingContext.opentelemetryTracer())
            );
            ProcessorChain preProcessorChain = platformProcessorChainFactory.preProcessorChain();
            List<ProcessorHook> processHooks = gatewayTracingContext.isVerbose() ? List.of(tracingHook) : List.of();
            Completable handleNotFoundCompletable = HookHelper
                .hook(
                    () -> preProcessorChain.execute(mutableCtx, ExecutionPhase.REQUEST),
                    preProcessorChain.getId(),
                    processHooks,
                    mutableCtx,
                    ExecutionPhase.REQUEST
                )
                .andThen(handleNotFound(mutableCtx, processHooks));
            if (gatewayTracingContext.isEnabled()) {
                return handleNotFoundCompletable
                    .doOnSubscribe(disposable -> {
                        Span rootSpan = mutableCtx
                            .getTracer()
                            .startRootSpanFrom(new ObservableHttpServerRequest(httpServerRequest.getDelegate()));
                        mutableCtx.putInternalAttribute(ATTR_INTERNAL_TRACING_ROOT_SPAN, rootSpan);
                    })
                    .doOnError(throwable -> mutableCtx.putInternalAttribute(ATTR_INTERNAL_TRACING_ERROR, throwable))
                    .doFinally(() -> {
                        Span rootSpan = mutableCtx.getInternalAttribute(ATTR_INTERNAL_TRACING_ROOT_SPAN);
                        Throwable throwable = mutableCtx.getInternalAttribute(ATTR_INTERNAL_TRACING_ERROR);
                        mutableCtx
                            .getTracer()
                            .endWithResponseAndError(
                                rootSpan,
                                new ObservableHttpServerResponse(httpServerRequest.getDelegate().response()),
                                throwable
                            );
                    });
            } else {
                return handleNotFoundCompletable;
            }
        } else if (httpAcceptor.reactor() instanceof ApiReactor<?> apiReactor) {
            MutableExecutionContext mutableCtx = prepareExecutionContext(httpServerRequest);
            mutableCtx.request().contextPath(httpAcceptor.path());
            TracingContext tracingContext = apiReactor.tracingContext();
            mutableCtx.tracer(new io.gravitee.gateway.reactive.api.tracing.Tracer(vertxContext, tracingContext.opentelemetryTracer()));
            mutableCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, apiReactor.api());
            ProcessorChain preProcessorChain = platformProcessorChainFactory.preProcessorChain();
            List<ProcessorHook> processHooks = gatewayTracingContext.isVerbose() ? List.of(tracingHook) : List.of();
            Completable handleCompletable = HookHelper
                .hook(
                    () -> preProcessorChain.execute(mutableCtx, ExecutionPhase.REQUEST),
                    preProcessorChain.getId(),
                    processHooks,
                    mutableCtx,
                    ExecutionPhase.REQUEST
                )
                .andThen(Completable.defer(() -> apiReactor.handle(mutableCtx)));

            if (tracingContext.isEnabled()) {
                handleCompletable =
                    handleCompletable
                        .doOnSubscribe(disposable -> {
                            Span rootSpan = mutableCtx
                                .getTracer()
                                .startRootSpanFrom(new ObservableHttpServerRequest(httpServerRequest.getDelegate()));
                            mutableCtx.putInternalAttribute(ATTR_INTERNAL_TRACING_ROOT_SPAN, rootSpan);
                        })
                        .doOnError(throwable -> mutableCtx.putInternalAttribute(ATTR_INTERNAL_TRACING_ERROR, throwable));
            }
            return handleCompletable.doFinally(() -> {
                // Post action are dissociated from the main execution once the request has been handled and cover all the cases (error, success, cancel).
                ProcessorChain postProcessorChain = platformProcessorChainFactory.postProcessorChain();
                Completable postProcessCompletable = HookHelper
                    .hook(
                        () -> postProcessorChain.execute(mutableCtx, ExecutionPhase.RESPONSE),
                        postProcessorChain.getId(),
                        processHooks,
                        mutableCtx,
                        ExecutionPhase.RESPONSE
                    )
                    .subscribeOn(Schedulers.computation());

                if (tracingContext.isEnabled()) {
                    postProcessCompletable =
                        postProcessCompletable
                            .doOnError(throwable -> mutableCtx.putInternalAttribute(ATTR_INTERNAL_TRACING_ERROR, throwable))
                            .doFinally(() -> {
                                Span rootSpan = mutableCtx.getInternalAttribute(ATTR_INTERNAL_TRACING_ROOT_SPAN);
                                Throwable throwable = mutableCtx.getInternalAttribute(ATTR_INTERNAL_TRACING_ERROR);
                                mutableCtx
                                    .getTracer()
                                    .endWithResponseAndError(
                                        rootSpan,
                                        new ObservableHttpServerResponse(httpServerRequest.getDelegate().response()),
                                        throwable
                                    );
                            });
                }
                postProcessCompletable.onErrorComplete().subscribe();
            });
        }
        // V3 execution mode.
        return handleV3Request(httpServerRequest, httpAcceptor, vertxContext);
    }

    private MutableExecutionContext prepareExecutionContext(final HttpServerRequest httpServerRequest) {
        VertxHttpServerRequest request = new VertxHttpServerRequest(
            httpServerRequest,
            idGenerator,
            new VertxHttpServerRequest.VertxHttpServerRequestOptions(requestClientAuthConfiguration.getHeaderName())
        );

        MutableExecutionContext ctx = createExecutionContext(request);
        ctx.componentProvider(globalComponentProvider);
        ctx.setInternalAttribute(ATTR_INTERNAL_LISTENER_TYPE, ListenerType.HTTP);

        return ctx;
    }

    protected DefaultExecutionContext createExecutionContext(VertxHttpServerRequest request) {
        return new DefaultExecutionContext(request, request.response());
    }

    private Completable handleNotFound(final MutableExecutionContext ctx, final List<ProcessorHook> notFoundProcessorHook) {
        ctx.request().contextPath("/");
        ProcessorChain processorChain = notFoundProcessorChainFactory.processorChain();
        return HookHelper.hook(
            () -> processorChain.execute(ctx, ExecutionPhase.RESPONSE),
            processorChain.getId(),
            notFoundProcessorHook,
            ctx,
            ExecutionPhase.RESPONSE
        );
    }

    private Completable handleV3Request(
        final HttpServerRequest httpServerRequest,
        final HttpAcceptor handlerEntrypoint,
        final Context vertxContext
    ) {
        final ReactorHandler reactorHandler = handlerEntrypoint.reactor();

        io.gravitee.gateway.http.vertx.VertxHttpServerRequest request = createV3Request(httpServerRequest, idGenerator);

        // Prepare invocation execution context.
        SimpleExecutionContext simpleExecutionContext = createV3ExecutionContext(httpServerRequest, request);
        simpleExecutionContext.tracer(
            new io.gravitee.gateway.reactive.api.tracing.Tracer(vertxContext, reactorHandler.tracingContext().opentelemetryTracer())
        );

        // Required by the v3 execution mode.
        simpleExecutionContext.setAttribute(ATTR_ENTRYPOINT, handlerEntrypoint);

        // Set gateway tenants and zones in request metrics.
        prepareV3Metrics(request.metrics());

        // Prepare handler chain and catch the end of the v3 request handling to complete the reactive chain.
        return Completable
            .create(emitter -> {
                Handler<io.gravitee.gateway.api.ExecutionContext> endHandler = endRequestHandler(emitter, httpServerRequest);
                requestProcessorChainFactory
                    .create()
                    .handler(ctx -> {
                        reactorHandler.handle(
                            ctx,
                            executionContext ->
                                executionContext.response().endHandler(aVoid -> processResponse(executionContext, endHandler)).end()
                        );
                    })
                    .errorHandler(result -> processResponse(simpleExecutionContext, endHandler))
                    .exitHandler(result -> processResponse(simpleExecutionContext, endHandler))
                    .handle(simpleExecutionContext);
            })
            .doOnSubscribe(disposable -> {
                Span rootSpan = simpleExecutionContext
                    .getTracer()
                    .startRootSpanFrom(new ObservableHttpServerRequest(httpServerRequest.getDelegate()));
                simpleExecutionContext.getAttributes().put(ATTR_INTERNAL_TRACING_ROOT_SPAN, rootSpan);
            })
            .doOnComplete(() -> {
                Span rootSpan = (Span) simpleExecutionContext.getAttribute(ATTR_INTERNAL_TRACING_ROOT_SPAN);
                simpleExecutionContext
                    .getTracer()
                    .endWithResponse(rootSpan, new ObservableHttpServerResponse(httpServerRequest.getDelegate().response()));
            })
            .doOnError(throwable -> {
                Span rootSpan = (Span) simpleExecutionContext.getAttribute(ATTR_INTERNAL_TRACING_ROOT_SPAN);
                simpleExecutionContext
                    .getTracer()
                    .endWithResponseAndError(
                        rootSpan,
                        new ObservableHttpServerResponse(httpServerRequest.getDelegate().response()),
                        throwable
                    );
            });
    }

    private Handler<io.gravitee.gateway.api.ExecutionContext> endRequestHandler(
        final CompletableEmitter emitter,
        final HttpServerRequest httpServerRequest
    ) {
        return context -> {
            Long vertxTimerId = (Long) context.getAttribute(ATTR_INTERNAL_VERTX_TIMER_ID);
            if (vertxTimerId != null) {
                vertx.cancelTimer(vertxTimerId);
                context.removeAttribute(ATTR_INTERNAL_VERTX_TIMER_ID);
            }
            if (context.response().ended()) {
                emitter.onComplete();
            } else {
                httpServerRequest.response().rxEnd().subscribe(emitter::onComplete, emitter::tryOnError);
            }
        };
    }

    /**
     * Prepare some global metrics for the current request (tenants, zones, ...).
     *
     * @param metrics the {@link Metrics} object to add information on.
     */
    private void prepareV3Metrics(io.gravitee.reporter.api.http.Metrics metrics) {
        // Set gateway tenant
        gatewayConfiguration.tenant().ifPresent(metrics::setTenant);

        // Set gateway zone
        gatewayConfiguration.zone().ifPresent(metrics::setZone);
    }

    protected io.gravitee.gateway.http.vertx.VertxHttpServerRequest createV3Request(
        HttpServerRequest httpServerRequest,
        IdGenerator idGenerator
    ) {
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

    private SimpleExecutionContext createV3ExecutionContext(
        HttpServerRequest httpServerRequest,
        io.gravitee.gateway.http.vertx.VertxHttpServerRequest request
    ) {
        SimpleExecutionContext simpleExecutionContext = new SimpleExecutionContext(request, request.createResponse());

        if (requestTimeoutConfiguration.getRequestTimeout() > 0 && !isV3WebSocket(httpServerRequest)) {
            final long vertxTimerId = vertx.setTimer(
                requestTimeoutConfiguration.getRequestTimeout(),
                event -> {
                    if (!httpServerRequest.response().ended()) {
                        final Handler<Long> handler = request.timeoutHandler();
                        handler.handle(event);
                    }
                }
            );
            simpleExecutionContext.setAttribute(ATTR_INTERNAL_VERTX_TIMER_ID, vertxTimerId);
        }

        return simpleExecutionContext;
    }

    /**
     * We are only considering HTTP_1.x requests for now.
     * There is a dedicated RFC to support WebSockets over HTTP2: https://tools.ietf.org/html/rfc8441
     *
     * @param httpServerRequest
     * @return <code>true</code> if given request is websocket, <code>false</code> otherwise
     */
    private boolean isV3WebSocket(HttpServerRequest httpServerRequest) {
        return RequestUtils.isWebSocket(httpServerRequest);
    }

    private void processResponse(
        io.gravitee.gateway.api.ExecutionContext context,
        Handler<io.gravitee.gateway.api.ExecutionContext> handler
    ) {
        responseProcessorChainFactory.create().handler(handler).handle(context);
    }
}
