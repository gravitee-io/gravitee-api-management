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
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SERVER_ID;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_TRACING_ERROR;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_TRACING_ROOT_SPAN;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.http.RequestPathNormalizer;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.RequestClientAuthConfiguration;
import io.gravitee.gateway.env.RequestPathConfiguration;
import io.gravitee.gateway.env.RequestPathHandling;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.http.utils.RequestUtils;
import io.gravitee.gateway.http.vertx.VertxHttp2ServerRequest;
import io.gravitee.gateway.http.vertx.VertxHttpServerRequestOptions;
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
import io.gravitee.gateway.reactive.http.vertx.ClientCloseClassifier;
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
import io.gravitee.node.opentelemetry.tracer.vertx.VertxContext;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.net.HostAndPort;
import io.vertx.rxjava3.core.http.HttpHeaders;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import java.util.List;
import lombok.CustomLog;

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
@CustomLog
public class DefaultHttpRequestDispatcher implements HttpRequestDispatcher {

    private static final String ATTR_INTERNAL_VERTX_TIMER_ID = ContextAttributes.ATTR_PREFIX + "vertx-timer-id";
    public static final String ATTR_ENTRYPOINT = ContextAttributes.ATTR_PREFIX + "entrypoint";
    // Key checked by gravitee-node's RouteGetter (OTel span naming) before falling back to the raw request URI.
    private static final String TRACING_ROUTE_CONTEXT_KEY = "VertxRoute";

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
    private final RequestPathConfiguration requestPathConfiguration;
    private final Vertx vertx;
    private final ComponentProvider globalComponentProvider;
    private final TracingHook tracingHook;
    private final boolean warningsEnabled;

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
        RequestPathConfiguration requestPathConfiguration,
        Vertx vertx,
        boolean warningsEnabled
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
        this.requestPathConfiguration = requestPathConfiguration;
        this.vertx = vertx;
        this.tracingHook = new TracingHook("Processor chain");
        this.warningsEnabled = warningsEnabled;
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
        // Before anything else, so that whatever the dispatcher does with this request — the scan,
        // the resolution, the acceptor lookup — falls inside the window every latency metric covers.
        // Stamping it in the wrapper's constructor would start the clock once the work is done.
        final long receivedAt = System.currentTimeMillis();

        //Keep same behavior as in Vertx4 when host was also returning the port.
        //The authority is null when the request has no Host header (nor :authority pseudo-header), as Vertx4 host() was.
        final HostAndPort authority = httpServerRequest.authority();
        final String host;
        if (authority == null) {
            host = null;
        } else {
            host = authority.port() > 0 ? authority.host() + ":" + authority.port() : authority.host();
        }

        final String rawPath = httpServerRequest.path();
        log.debug("Dispatching request on host {} and path {}", host, rawPath);

        // One scan answers what both active modes ask. REJECT needs nothing more: a path that is
        // not in the form it claims to be is refused without ever being rewritten. NORMALIZE only
        // pays for the resolution when there is something to resolve.
        final boolean needsNormalization = requestPathConfiguration.isEnabled() && RequestPathNormalizer.needsNormalization(rawPath);

        final String normalizedPath;
        if (needsNormalization) {
            if (requestPathConfiguration.getHandling() == RequestPathHandling.REJECT) {
                return handleRejectedPath(httpServerRequest, serverId, receivedAt);
            }
            normalizedPath = RequestPathNormalizer.normalize(rawPath);
            // No normalized form at all: the path carries a malformed percent sequence. Inside this
            // branch on purpose. Under RAW nothing is inspected, and a null path — which the Vert.x
            // API declares and an HTTP/2 request without a :path pseudo-header actually produces —
            // must keep reaching the acceptor exactly as it did before this setting existed.
            if (normalizedPath == null) {
                return handleRejectedPath(httpServerRequest, serverId, receivedAt);
            }
            // Both forms, deliberately: the point of this line is to answer "what did the client
            // actually send" once the gateway has started deciding on something else.
            log.debug("Path normalized from [{}] to [{}]", rawPath, normalizedPath);
        } else {
            normalizedPath = rawPath;
        }

        // Exactly needsNormalization: the normalizer answers the same instance if and only if there
        // was nothing to resolve, and the property test holds the two methods to that. Reusing the
        // flag rather than comparing references makes that coupling explicit at the call site.
        final boolean pathWasNormalized = needsNormalization;

        final HttpAcceptor httpAcceptor = httpAcceptorResolver.resolve(host, normalizedPath, serverId);
        Context vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        if (httpAcceptor == null || httpAcceptor.reactor() == null) {
            log.debug("No acceptor found for host {} and path {}, handling as not found", host, normalizedPath);
            // The resolved path, like the API branch below. The lookup that just failed was made on
            // it, so an operator investigating a 404 has to see it — reporting the received path
            // would send them looking for a context path the gateway never tried. Nothing is lost:
            // uri() still carries the bytes the client sent.
            MutableExecutionContext mutableCtx = prepareExecutionContext(
                httpServerRequest,
                serverId,
                pathWasNormalized ? normalizedPath : null,
                receivedAt
            );
            mutableCtx.tracer(
                new io.gravitee.gateway.reactive.api.tracing.Tracer(vertxContext, gatewayTracingContext.opentelemetryTracer())
            );
            // No route matched: bucket all of it under "/" rather than the unbounded raw request URI
            // (scanner/probe traffic on internet-facing gateways is otherwise the largest source of cardinality).
            markTracingRoute(vertxContext, "/");
            ProcessorChain preProcessorChain = platformProcessorChainFactory.preProcessorChain();
            List<ProcessorHook> processHooks = gatewayTracingContext.isVerbose() ? List.of(tracingHook) : List.of();
            Completable handleNotFoundCompletable = HookHelper.hook(
                () -> preProcessorChain.execute(mutableCtx, ExecutionPhase.REQUEST),
                preProcessorChain.getId(),
                processHooks,
                mutableCtx,
                ExecutionPhase.REQUEST
            ).andThen(handleNotFound(mutableCtx, processHooks));
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
            log.debug("Request routed to API reactor on path [{}]", httpAcceptor.path());
            // The path is seeded at construction so that pathInfo, the flow selectors and the
            // upstream URI all derive from the value the acceptor actually matched. uri() and
            // parameters() keep reading the untouched native request, and MetricsProcessor reports
            // both forms from those two — received and routed — with no custom metric needed.
            MutableExecutionContext mutableCtx = prepareExecutionContext(
                httpServerRequest,
                serverId,
                pathWasNormalized ? normalizedPath : null,
                receivedAt
            );
            mutableCtx.request().contextPath(httpAcceptor.path());
            markTracingRoute(vertxContext, httpAcceptor.path());
            TracingContext tracingContext = apiReactor.tracingContext();
            mutableCtx.tracer(new io.gravitee.gateway.reactive.api.tracing.Tracer(vertxContext, tracingContext.opentelemetryTracer()));
            mutableCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, apiReactor.api());
            registerClientCloseClassifier(httpServerRequest, mutableCtx);
            ProcessorChain preProcessorChain = platformProcessorChainFactory.preProcessorChain();
            List<ProcessorHook> processHooks = List.of();
            Completable handleCompletable = HookHelper.hook(
                () -> preProcessorChain.execute(mutableCtx, ExecutionPhase.REQUEST),
                preProcessorChain.getId(),
                processHooks,
                mutableCtx,
                ExecutionPhase.REQUEST
            ).andThen(Completable.defer(() -> apiReactor.handle(mutableCtx)));

            if (tracingContext.isEnabled()) {
                handleCompletable = handleCompletable
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
                Completable postProcessCompletable = HookHelper.hook(
                    () -> postProcessorChain.execute(mutableCtx, ExecutionPhase.RESPONSE),
                    postProcessorChain.getId(),
                    processHooks,
                    mutableCtx,
                    ExecutionPhase.RESPONSE
                ).subscribeOn(Schedulers.computation());

                if (tracingContext.isEnabled()) {
                    postProcessCompletable = postProcessCompletable
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
                postProcessCompletable
                    .doOnError(error -> log.warn("Unexpected error while executing post-processor chain", error))
                    .onErrorComplete()
                    .subscribe();
            });
        }
        // V3 execution mode.
        log.debug("Request routed to V3 handler on path [{}]", httpAcceptor.path());
        return handleV3Request(httpServerRequest, httpAcceptor, vertxContext, pathWasNormalized ? normalizedPath : null, receivedAt);
    }

    /**
     * Surface a client→gateway connection close (TCP reset, broken pipe, plain channel close) onto <b>this</b>
     * request's metrics by hooking the per-request/stream response exception handler. The in-flight execution context
     * is captured in the closure, so the close is correlated to the exact request that owns the stream — without
     * stashing any request state on the connection, which is reused across keep-alive requests and multiplexes
     * concurrent HTTP/2 streams and therefore cannot be correlated back to a single request.
     * <p>
     * This complements {@code VertxHttpServerResponse}'s response write-failure path: that path catches a close that
     * happens while the gateway is actively streaming the response body, whereas this handler catches a reset that
     * arrives while the gateway is otherwise busy (reading the request, waiting on the backend). Both go through the
     * same {@code ClientCloseClassifier.decorate}, whose guards make a double notification idempotent.
     * <p>
     * Only confirmed client closes are decorated (same {@code isClientConnectionClose} pre-filter as the write-failure
     * path): the response exception handler can also receive genuine gateway-side faults (a protocol/stream error, a
     * TLS write fault), and classifying those as a 499 client abort would both mislabel the request and — via
     * {@code decorate}'s first-tagger-wins guard — block the real failure from being recorded. Non-close throwables are
     * left for the normal error path; we only log them.
     */
    private void registerClientCloseClassifier(final HttpServerRequest httpServerRequest, final MutableExecutionContext ctx) {
        httpServerRequest.response().exceptionHandler(throwable -> classifyResponseStreamError(ctx, throwable));
    }

    /**
     * Decorate the in-flight request only when the response-stream error is a confirmed client close; otherwise leave
     * it for the normal error path (just log). Package-private and static for direct testing of the gate.
     */
    static void classifyResponseStreamError(final MutableExecutionContext ctx, final Throwable throwable) {
        if (ClientCloseClassifier.isClientConnectionClose(throwable)) {
            ClientCloseClassifier.decorate(ctx, throwable);
        } else {
            ctx.withLogger(log).debug("Ignoring non-client-close error on the response stream", throwable);
        }
    }

    private void markTracingRoute(final Context vertxContext, final String route) {
        // Vert.x 5 keeps Object-keyed getLocal/putLocal behind ContextInternal (public Context only exposes the
        // ContextLocal<T>-typed overloads) - matches how gravitee-node's own RouteGetter reads this same key.
        ((ContextInternal) vertxContext).putLocal(TRACING_ROUTE_CONTEXT_KEY, withoutTrailingSlash(route));
    }

    // httpAcceptor.path() always has a trailing slash; strip it so http.route matches url.path (both for
    // correlation in APM tooling and so pre-existing transaction.name-based searches keep matching).
    private static String withoutTrailingSlash(final String path) {
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    /**
     * Visible to subclasses so that a dispatcher serving a single kind of traffic can build the same
     * context this one builds, rather than a lookalike. The debug dispatcher needs it to answer a
     * rejected path without losing the debug session — see {@code DebugHttpRequestDispatcher}.
     */
    protected MutableExecutionContext prepareExecutionContext(
        final HttpServerRequest httpServerRequest,
        String serverId,
        final String path,
        final long receivedAt
    ) {
        VertxHttpServerRequest request = new VertxHttpServerRequest(
            httpServerRequest,
            idGenerator,
            VertxHttpServerRequest.VertxHttpServerRequestOptions.builder()
                .clientAuthHeaderName(requestClientAuthConfiguration.getHeaderName())
                .path(path)
                .timestamp(receivedAt)
                .build()
        );

        MutableExecutionContext ctx = createExecutionContext(request);
        ctx.componentProvider(globalComponentProvider);
        ctx.setInternalAttribute(ATTR_INTERNAL_LISTENER_TYPE, ListenerType.HTTP);
        ctx.setInternalAttribute(ATTR_INTERNAL_SERVER_ID, serverId);

        return ctx;
    }

    protected DefaultExecutionContext createExecutionContext(VertxHttpServerRequest request) {
        DefaultExecutionContext context = new DefaultExecutionContext(request, request.response());
        context.setWarningsEnabled(warningsEnabled);
        return context;
    }

    /**
     * Refuses the request before any API is selected, through a processor chain rather than on the
     * raw response, so that it is measured and reported like any other request the gateway answers
     * on its own.
     *
     * <p>The platform pre-processor chain runs first, exactly as it does for a not-found. It is not
     * decoration: {@code XForwardProcessor} is what rewrites the remote address from
     * {@code X-Forwarded-For}, and without it a gateway behind an ingress reports every rejection
     * with the load balancer's address instead of the prober's — which would defeat the reason
     * {@code handlers.rejected.analytics.enabled} defaults to on. The same chain carries the
     * connection drain and the W3C trace context.
     *
     * <p>A subclass needing to do something around a rejection hooks into
     * {@link #afterRejectedPath(Completable, MutableExecutionContext)} rather than restating this
     * flow. That is not a preference: while the debug dispatcher carried its own copy, this method
     * gained the pre-processor chain and the whole tracing block in a single review round, and the
     * copy silently kept neither.
     */
    private Completable handleRejectedPath(final HttpServerRequest httpServerRequest, final String serverId, final long receivedAt) {
        // Nothing is rewritten here, so the path stays the one received — which is what the report
        // has to carry for an operator to see what was actually sent.
        final MutableExecutionContext ctx = prepareExecutionContext(httpServerRequest, serverId, null, receivedAt);
        ctx.request().contextPath("/");

        final Context vertxContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        ctx.tracer(new io.gravitee.gateway.reactive.api.tracing.Tracer(vertxContext, gatewayTracingContext.opentelemetryTracer()));
        // No route was resolved and none will be, so the span is bucketed like a not-found rather
        // than under the unbounded raw request URI a prober chose.
        markTracingRoute(vertxContext, "/");

        final List<ProcessorHook> processHooks = gatewayTracingContext.isVerbose() ? List.of(tracingHook) : List.of();
        final ProcessorChain preProcessorChain = platformProcessorChainFactory.preProcessorChain();
        final ProcessorChain rejectedChain = notFoundProcessorChainFactory.rejectedPathProcessorChain();

        final Completable rejected = HookHelper.hook(
            () -> preProcessorChain.execute(ctx, ExecutionPhase.REQUEST),
            preProcessorChain.getId(),
            processHooks,
            ctx,
            ExecutionPhase.REQUEST
        ).andThen(
            HookHelper.hook(
                () -> rejectedChain.execute(ctx, ExecutionPhase.RESPONSE),
                rejectedChain.getId(),
                processHooks,
                ctx,
                ExecutionPhase.RESPONSE
            )
        );

        if (!gatewayTracingContext.isEnabled()) {
            return afterRejectedPath(rejected, ctx);
        }
        return afterRejectedPath(
            rejected
                .doOnSubscribe(disposable -> {
                    final Span rootSpan = ctx
                        .getTracer()
                        .startRootSpanFrom(new ObservableHttpServerRequest(httpServerRequest.getDelegate()));
                    ctx.putInternalAttribute(ATTR_INTERNAL_TRACING_ROOT_SPAN, rootSpan);
                })
                .doOnError(throwable -> ctx.putInternalAttribute(ATTR_INTERNAL_TRACING_ERROR, throwable))
                .doFinally(() -> {
                    final Span rootSpan = ctx.getInternalAttribute(ATTR_INTERNAL_TRACING_ROOT_SPAN);
                    final Throwable throwable = ctx.getInternalAttribute(ATTR_INTERNAL_TRACING_ERROR);
                    ctx
                        .getTracer()
                        .endWithResponseAndError(
                            rootSpan,
                            new ObservableHttpServerResponse(httpServerRequest.getDelegate().response()),
                            throwable
                        );
                }),
            ctx
        );
    }

    /**
     * The extension point for a dispatcher that must do something once a rejection has been handled.
     *
     * <p>It receives the rejection and the context it ran on, so a subclass can wrap the former
     * without rebuilding the latter — which is the whole point, since the context is precisely what
     * a second copy of this flow had to recreate, and what made that copy drift.
     *
     * <p>Answers the rejection untouched by default.
     */
    protected Completable afterRejectedPath(final Completable rejection, final MutableExecutionContext ctx) {
        return rejection;
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
        final Context vertxContext,
        final String normalizedPath,
        final long receivedAt
    ) {
        final ReactorHandler reactorHandler = handlerEntrypoint.reactor();
        markTracingRoute(vertxContext, handlerEntrypoint.path());

        // Both are handed over at construction: pathInfo, and therefore the upstream URI, must
        // derive from the path the acceptor matched rather than from the one received; and the
        // clock must have started before the dispatcher did any of that work.
        io.gravitee.gateway.http.vertx.VertxHttpServerRequest request = createV3Request(
            httpServerRequest,
            idGenerator,
            VertxHttpServerRequestOptions.builder().path(normalizedPath).timestamp(receivedAt).build()
        );

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
        return Completable.create(emitter -> {
            Handler<io.gravitee.gateway.api.ExecutionContext> endHandler = endRequestHandler(emitter, httpServerRequest);
            requestProcessorChainFactory
                .create()
                .handler(ctx -> {
                    reactorHandler.handle(ctx, executionContext ->
                        executionContext
                            .response()
                            .endHandler(aVoid -> processResponse(executionContext, endHandler))
                            .end()
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

    /**
     * @deprecated kept so that anything overriding it keeps compiling — but it is no longer called,
     *     so an override here decorates nothing. Move to
     *     {@link #createV3Request(HttpServerRequest, IdGenerator, VertxHttpServerRequestOptions)},
     *     which is the signature {@link #handleV3Request} invokes and which absorbs new fields
     *     without ever moving again.
     */
    @Deprecated
    protected io.gravitee.gateway.http.vertx.VertxHttpServerRequest createV3Request(
        HttpServerRequest httpServerRequest,
        IdGenerator idGenerator
    ) {
        return createV3Request(httpServerRequest, idGenerator, VertxHttpServerRequestOptions.builder().build());
    }

    /**
     * The extension point for building the v3 request: this is the signature
     * {@link #handleV3Request} calls, and the only one worth overriding to decorate it.
     *
     * <p>It takes a single options parameter on purpose, so that a field added later moves nothing
     * and leaves every subclass and plugin overriding it untouched.
     */
    protected io.gravitee.gateway.http.vertx.VertxHttpServerRequest createV3Request(
        HttpServerRequest httpServerRequest,
        IdGenerator idGenerator,
        VertxHttpServerRequestOptions options
    ) {
        io.gravitee.gateway.http.vertx.VertxHttpServerRequest request;

        if (isV3WebSocket(httpServerRequest)) {
            request = new VertxWebSocketServerRequest(httpServerRequest.getDelegate(), idGenerator, options);
        } else {
            if (httpServerRequest.version() == HttpVersion.HTTP_2) {
                if (MediaType.APPLICATION_GRPC.equals(httpServerRequest.getHeader(HttpHeaders.CONTENT_TYPE))) {
                    request = new VertxGrpcServerRequest(httpServerRequest.getDelegate(), idGenerator, options);
                } else {
                    request = new VertxHttp2ServerRequest(httpServerRequest.getDelegate(), idGenerator, options);
                }
            } else {
                request = new io.gravitee.gateway.http.vertx.VertxHttpServerRequest(httpServerRequest.getDelegate(), idGenerator, options);
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
            final long vertxTimerId = vertx.setTimer(requestTimeoutConfiguration.getRequestTimeout(), event -> {
                if (!httpServerRequest.response().ended()) {
                    final Handler<Long> handler = request.timeoutHandler();
                    handler.handle(event);
                }
            });
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
