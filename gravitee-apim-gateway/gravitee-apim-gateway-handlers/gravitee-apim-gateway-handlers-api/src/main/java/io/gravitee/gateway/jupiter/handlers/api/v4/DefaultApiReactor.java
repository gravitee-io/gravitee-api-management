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
package io.gravitee.gateway.jupiter.handlers.api.v4;

import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.REPORTERS_LOGGING_EXCLUDED_RESPONSE_TYPES_PROPERTY;
import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.REPORTERS_LOGGING_MAX_SIZE_PROPERTY;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.MESSAGE_REQUEST;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.MESSAGE_RESPONSE;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.REQUEST;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.RESPONSE;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_INVOKER;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_INVOKER_SKIP;
import static io.reactivex.rxjava3.core.Completable.defer;
import static io.reactivex.rxjava3.core.Observable.interval;
import static java.lang.Boolean.TRUE;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.context.*;
import io.gravitee.gateway.jupiter.api.hook.ChainHook;
import io.gravitee.gateway.jupiter.api.hook.InvokerHook;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionHelper;
import io.gravitee.gateway.jupiter.core.hook.HookHelper;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.core.tracing.TracingHook;
import io.gravitee.gateway.jupiter.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.jupiter.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.jupiter.core.v4.entrypoint.DefaultEntrypointConnectorResolver;
import io.gravitee.gateway.jupiter.core.v4.invoker.EndpointInvoker;
import io.gravitee.gateway.jupiter.handlers.api.adapter.invoker.InvokerAdapter;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.AnalyticsMessageHook;
import io.gravitee.gateway.jupiter.handlers.api.v4.analytics.logging.LoggingHook;
import io.gravitee.gateway.jupiter.handlers.api.v4.flow.FlowChain;
import io.gravitee.gateway.jupiter.handlers.api.v4.flow.FlowChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.v4.security.SecurityChain;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.gravitee.gateway.jupiter.reactor.v4.subscription.DefaultSubscriptionAcceptor;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.DefaultHttpAcceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultApiReactor extends AbstractLifecycleComponent<ReactorHandler> implements ApiReactor<Api> {

    public static final String PENDING_REQUESTS_TIMEOUT_PROPERTY = "api.pending_requests_timeout";
    protected static final String REQUEST_TIMEOUT_KEY = "REQUEST_TIMEOUT";
    protected static final String SERVICES_TRACING_ENABLED_PROPERTY = "services.tracing.enabled";
    static final int STOP_UNTIL_INTERVAL_PERIOD_MS = 100;
    private static final Logger log = LoggerFactory.getLogger(DefaultApiReactor.class);
    protected final List<ChainHook> processorChainHooks;
    protected final List<InvokerHook> invokerHooks;
    private final Api api;

    @Getter
    private final ComponentProvider componentProvider;

    @Getter
    private final List<TemplateVariableProvider> ctxTemplateVariableProviders;

    private final PolicyManager policyManager;
    private final DefaultEntrypointConnectorResolver entrypointConnectorResolver;
    private final EndpointManager endpointManager;
    private final ReporterService reporterService;
    private final EndpointInvoker defaultInvoker;
    private final ResourceLifecycleManager resourceLifecycleManager;
    private final ProcessorChain beforeHandleProcessors;
    private final ProcessorChain afterHandleProcessors;
    private final ProcessorChain beforeApiExecutionProcessors;
    private final ProcessorChain afterApiExecutionProcessors;
    private final ProcessorChain onErrorProcessors;
    private final ProcessorChain afterEntrypointRequestProcessors;
    private final ProcessorChain beforeEntrypointResponseProcessors;
    private final ProcessorChain afterApiExecutionMessageProcessors;
    private final io.gravitee.gateway.jupiter.handlers.api.flow.FlowChain platformFlowChain;
    private final FlowChain apiPlanFlowChain;
    private final FlowChain apiFlowChain;
    private final Node node;
    private final boolean tracingEnabled;
    private final String loggingExcludedResponseType;
    private final String loggingMaxSize;
    private final RequestTimeoutConfiguration requestTimeoutConfiguration;
    private final long pendingRequestsTimeout;
    private final AtomicLong pendingRequests = new AtomicLong(0);
    private final boolean isEventNative;
    private SecurityChain securityChain;
    private Lifecycle.State lifecycleState;
    private AnalyticsContext analyticsContext;

    public DefaultApiReactor(
        final Api api,
        final DeploymentContext deploymentContext,
        final ComponentProvider componentProvider,
        final List<TemplateVariableProvider> ctxTemplateVariableProviders,
        final PolicyManager policyManager,
        final EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        final EndpointManager endpointManager,
        final ResourceLifecycleManager resourceLifecycleManager,
        final ApiProcessorChainFactory apiProcessorChainFactory,
        final io.gravitee.gateway.jupiter.handlers.api.flow.FlowChainFactory flowChainFactory,
        final FlowChainFactory v4FlowChainFactory,
        final Configuration configuration,
        final Node node,
        final RequestTimeoutConfiguration requestTimeoutConfiguration,
        final ReporterService reporterService
    ) {
        this.api = api;
        this.componentProvider = componentProvider;
        this.ctxTemplateVariableProviders = ctxTemplateVariableProviders;
        this.policyManager = policyManager;
        this.endpointManager = endpointManager;
        this.reporterService = reporterService;
        this.entrypointConnectorResolver =
            new DefaultEntrypointConnectorResolver(api.getDefinition(), deploymentContext, entrypointConnectorPluginManager);
        this.defaultInvoker = new EndpointInvoker(endpointManager);

        this.resourceLifecycleManager = resourceLifecycleManager;

        this.beforeHandleProcessors = apiProcessorChainFactory.beforeHandle(api);
        this.afterHandleProcessors = apiProcessorChainFactory.afterHandle(api);
        this.beforeApiExecutionProcessors = apiProcessorChainFactory.beforeApiExecution(api);
        this.afterApiExecutionProcessors = apiProcessorChainFactory.afterApiExecution(api);
        this.afterApiExecutionMessageProcessors = apiProcessorChainFactory.afterApiExecutionMessage(api);
        this.afterEntrypointRequestProcessors = apiProcessorChainFactory.afterEntrypointRequest(api);
        this.beforeEntrypointResponseProcessors = apiProcessorChainFactory.beforeEntrypointResponse(api);
        this.onErrorProcessors = apiProcessorChainFactory.onError(api);

        this.platformFlowChain = flowChainFactory.createPlatformFlow(api);
        this.apiPlanFlowChain = v4FlowChainFactory.createPlanFlow(api);
        this.apiFlowChain = v4FlowChainFactory.createApiFlow(api);

        this.node = node;
        this.lifecycleState = Lifecycle.State.INITIALIZED;
        this.tracingEnabled = configuration.getProperty(SERVICES_TRACING_ENABLED_PROPERTY, Boolean.class, false);
        this.pendingRequestsTimeout = configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L);
        this.loggingExcludedResponseType =
            configuration.getProperty(REPORTERS_LOGGING_EXCLUDED_RESPONSE_TYPES_PROPERTY, String.class, null);
        this.loggingMaxSize = configuration.getProperty(REPORTERS_LOGGING_MAX_SIZE_PROPERTY, String.class, null);
        this.requestTimeoutConfiguration = requestTimeoutConfiguration;

        this.processorChainHooks = new ArrayList<>();
        this.invokerHooks = new ArrayList<>();
        this.isEventNative = this.api.getDefinition().getType() == ApiType.ASYNC;
    }

    @Override
    public Api api() {
        return api;
    }

    @Override
    public Completable handle(final MutableExecutionContext ctx) {
        ctx.componentProvider(componentProvider);
        if (ctxTemplateVariableProviders != null) {
            ctx.templateVariableProviders(new HashSet<>(ctxTemplateVariableProviders));
        }

        // Prepare attributes and metrics before handling the request.
        prepareContextAttributes(ctx);
        prepareMetrics(ctx);

        return handleRequest(ctx);
    }

    private void prepareContextAttributes(MutableExecutionContext ctx) {
        ctx.setAttribute(ContextAttributes.ATTR_CONTEXT_PATH, ctx.request().contextPath());
        ctx.setAttribute(ContextAttributes.ATTR_API, api.getId());
        ctx.setAttribute(ContextAttributes.ATTR_API_DEPLOYED_AT, api.getDeployedAt().getTime());
        ctx.setAttribute(ContextAttributes.ATTR_ORGANIZATION, api.getOrganizationId());
        ctx.setAttribute(ContextAttributes.ATTR_ENVIRONMENT, api.getEnvironmentId());
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER, defaultInvoker);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);
    }

    private void prepareMetrics(HttpExecutionContext ctx) {
        final Metrics metrics = ctx.metrics();

        metrics.setApiId(api.getId());
        metrics.setApiType(api.getDefinition().getType().getLabel());
    }

    private Completable handleRequest(final MutableExecutionContext ctx) {
        // Setup all processors before handling the request (ex: logging).
        return new CompletableReactorChain(executeProcessorChain(ctx, beforeHandleProcessors, REQUEST))
            // Execute platform flow chain.
            .chainWith(platformFlowChain.execute(ctx, REQUEST))
            // Execute security chain.
            .chainWith(securityChain.execute(ctx))
            // Before flows processors.
            .chainWith(executeProcessorChain(ctx, beforeApiExecutionProcessors, REQUEST))
            // Resolve entrypoint and prepare request to be handled.
            .chainWith(handleEntrypointRequest(ctx))
            // After entrypoint response
            .chainWithIf(executeProcessorChain(ctx, afterEntrypointRequestProcessors, MESSAGE_REQUEST), isEventNative)
            .chainWithIf(platformFlowChain.execute(ctx, MESSAGE_REQUEST), isEventNative)
            // Execute all flows for request and message request phases.
            .chainWith(apiPlanFlowChain.execute(ctx, REQUEST))
            .chainWithIf(apiPlanFlowChain.execute(ctx, MESSAGE_REQUEST), isEventNative)
            .chainWith(apiFlowChain.execute(ctx, REQUEST))
            .chainWithIf(apiFlowChain.execute(ctx, MESSAGE_REQUEST), isEventNative)
            // Invoke the backend.
            .chainWith(invokeBackend(ctx))
            // Execute all flows for response and message response phases.
            .chainWith(apiPlanFlowChain.execute(ctx, RESPONSE))
            .chainWithIf(apiPlanFlowChain.execute(ctx, MESSAGE_RESPONSE), isEventNative)
            .chainWith(apiFlowChain.execute(ctx, RESPONSE))
            .chainWithIf(apiFlowChain.execute(ctx, MESSAGE_RESPONSE), isEventNative)
            // After flows processors.
            .chainWith(executeProcessorChain(ctx, afterApiExecutionProcessors, RESPONSE))
            .chainWithIf(executeProcessorChain(ctx, afterApiExecutionMessageProcessors, MESSAGE_RESPONSE), isEventNative)
            .chainWithOnError(error -> processThrowable(ctx, error))
            .chainWith(upstream -> timeout(upstream, ctx))
            // Platform post flows must always be executed (whatever timeout or error).
            .chainWith(
                new CompletableReactorChain(platformFlowChain.execute(ctx, RESPONSE))
                    .chainWithIf(platformFlowChain.execute(ctx, MESSAGE_RESPONSE), isEventNative)
                    .chainWith(upstream -> timeout(upstream, ctx))
            )
            // Before entrypoint response
            .chainWithIf(executeProcessorChain(ctx, beforeEntrypointResponseProcessors, MESSAGE_RESPONSE), isEventNative)
            // Handle entrypoint response.
            .chainWith(handleEntrypointResponse(ctx))
            // Catch possible unexpected errors before executing after Handle Processors
            .chainWithOnError(t -> handleUnexpectedError(ctx, t))
            .chainWith(executeProcessorChain(ctx, afterHandleProcessors, RESPONSE))
            // Catch all possible unexpected errors
            .chainWithOnError(t -> handleUnexpectedError(ctx, t))
            // Finally, end the response.
            .chainWith(ctx.response().end(ctx))
            .doOnSubscribe(disposable -> pendingRequests.incrementAndGet())
            .doFinally(pendingRequests::decrementAndGet);
    }

    private Completable handleEntrypointRequest(final MutableExecutionContext ctx) {
        return Completable.defer(
            () -> {
                final EntrypointConnector entrypointConnector = entrypointConnectorResolver.resolve(ctx);
                if (entrypointConnector == null) {
                    return ctx.interruptWith(
                        new ExecutionFailure(HttpStatusCode.NOT_FOUND_404).message("No entrypoint matches the incoming request")
                    );
                }

                // Add the resolved entrypoint connector into the internal attributes, so it can be used later (ex: for endpoint connector resolution).
                ctx.setInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR, entrypointConnector);

                return entrypointConnector.handleRequest(ctx);
            }
        );
    }

    private Completable handleEntrypointResponse(final MutableExecutionContext ctx) {
        return Completable
            .defer(
                () -> {
                    if (ctx.getInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE) == null) {
                        final EntrypointConnector entrypointConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);
                        if (entrypointConnector != null) {
                            return entrypointConnector.handleResponse(ctx);
                        }
                    }
                    return Completable.complete();
                }
            )
            .compose(upstream -> timeout(upstream, ctx));
    }

    private Completable executeProcessorChain(
        final MutableExecutionContext ctx,
        final ProcessorChain processorChain,
        final ExecutionPhase phase
    ) {
        return HookHelper.hook(() -> processorChain.execute(ctx, phase), processorChain.getId(), processorChainHooks, ctx, phase);
    }

    private Completable invokeBackend(final MutableExecutionContext ctx) {
        return defer(
                () -> {
                    if (!TRUE.equals(ctx.<Boolean>getInternalAttribute(ATTR_INTERNAL_INVOKER_SKIP))) {
                        Invoker invoker = getInvoker(ctx);

                        if (invoker != null) {
                            return HookHelper.hook(() -> invoker.invoke(ctx), invoker.getId(), invokerHooks, ctx, null);
                        }
                    }
                    return Completable.complete();
                }
            )
            .doOnSubscribe(disposable -> initEndpointResponseTimeMetric(ctx))
            .doFinally(() -> computeEndpointResponseTimeMetric(ctx));
    }

    private Invoker getInvoker(final MutableExecutionContext ctx) {
        final Object invoker = ctx.getInternalAttribute(ATTR_INTERNAL_INVOKER);

        if (invoker == null) {
            return null;
        }

        if (invoker instanceof Invoker) {
            return (Invoker) invoker;
        } else if (invoker instanceof io.gravitee.gateway.api.Invoker) {
            return new InvokerAdapter((io.gravitee.gateway.api.Invoker) invoker);
        }

        return null;
    }

    /**
     * Process the given throwable by checking the type of interruption and execute the right processor chain accordingly
     *
     * @param ctx the current context
     * @param throwable the source error
     * @return a {@link Completable} that will complete once processor chain has been fully executed or source error rethrown
     */
    private Completable processThrowable(final MutableExecutionContext ctx, final Throwable throwable) {
        if (InterruptionHelper.isInterruption(throwable)) {
            // In case of any interruption without failure, execute api post processor chain and resume the execution
            return executeProcessorChain(ctx, afterApiExecutionProcessors, RESPONSE);
        } else if (InterruptionHelper.isInterruptionWithFailure(throwable)) {
            // In case of any interruption with execution failure, execute api error processor chain and resume the execution
            return executeProcessorChain(ctx, onErrorProcessors, RESPONSE);
        } else {
            // In case of any error exception, log original exception, execute api error processor chain and resume the execution
            log.error("Unexpected error while handling request", throwable);
            return executeProcessorChain(ctx, onErrorProcessors, RESPONSE);
        }
    }

    private Completable handleUnexpectedError(final ExecutionContext ctx, final Throwable throwable) {
        return Completable.fromRunnable(
            () -> {
                log.error("Unexpected error while handling request", throwable);
                computeEndpointResponseTimeMetric(ctx);

                ctx.response().status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                ctx.response().reason(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
            }
        );
    }

    private void initEndpointResponseTimeMetric(ExecutionContext ctx) {
        Metrics metrics = ctx.metrics();
        if (!isEventNative) {
            // Initialize the response time with current time millis which is higher than Integer.MAX_VALUE
            metrics.setEndpointResponseTimeMs(System.currentTimeMillis());
        }
    }

    private void computeEndpointResponseTimeMetric(ExecutionContext ctx) {
        Metrics metrics = ctx.metrics();
        // If the response time is higher than Integer.MAX_VALUE, that means it has not been already computed (see init method)
        if (!isEventNative && metrics.getEndpointResponseTimeMs() > Integer.MAX_VALUE) {
            metrics.setEndpointResponseTimeMs(System.currentTimeMillis() - metrics.getEndpointResponseTimeMs());
        }
    }

    private Completable timeout(final Completable upstream, MutableExecutionContext ctx) {
        // When timeout is configured with 0 or less, consider it as infinity: no timeout operator to use in the chain.
        if (requestTimeoutConfiguration.getRequestTimeout() <= 0) {
            return upstream;
        }

        return Completable.defer(
            () ->
                upstream.timeout(
                    Math.max(
                        requestTimeoutConfiguration.getRequestTimeoutGraceDelay(),
                        requestTimeoutConfiguration.getRequestTimeout() - (System.currentTimeMillis() - ctx.request().timestamp())
                    ),
                    TimeUnit.MILLISECONDS,
                    ctx
                        .interruptWith(
                            new ExecutionFailure(HttpStatusCode.GATEWAY_TIMEOUT_504).key(REQUEST_TIMEOUT_KEY).message("Request timeout")
                        )
                        .onErrorResumeNext(error -> executeProcessorChain(ctx, onErrorProcessors, RESPONSE))
                )
        );
    }

    @Override
    public List<Acceptor<?>> acceptors() {
        final List<Acceptor<?>> acceptors = new ArrayList<>();

        for (Listener listener : api.getDefinition().getListeners()) {
            if (listener.getType() == ListenerType.HTTP) {
                acceptors.addAll(
                    ((HttpListener) listener).getPaths()
                        .stream()
                        .map(path -> new DefaultHttpAcceptor(path.getHost(), path.getPath(), this))
                        .collect(Collectors.toList())
                );
            } else if (listener.getType().equals(ListenerType.SUBSCRIPTION)) {
                acceptors.add(new DefaultSubscriptionAcceptor(this, api.getId()));
            }
        }

        return acceptors;
    }

    @Override
    public Lifecycle.State lifecycleState() {
        return lifecycleState;
    }

    @Override
    protected void doStart() throws Exception {
        log.debug("API reactor is now starting, preparing API context...");
        long startTime = System.currentTimeMillis(); // Get the start Time

        // Start resources before
        resourceLifecycleManager.start();
        policyManager.start();

        // Create securityChain once policy manager has been started.
        this.securityChain = new SecurityChain(api.getDefinition(), policyManager, ExecutionPhase.REQUEST);

        if (tracingEnabled) {
            processorChainHooks.add(new TracingHook("processor-chain"));
            invokerHooks.add(new TracingHook("invoker"));
            securityChain.addHooks(new TracingHook("security-plan"));
        }

        analyticsContext =
            new AnalyticsContext(api.getDefinition().getAnalytics(), isEventNative, loggingMaxSize, loggingExcludedResponseType);
        if (analyticsContext.isEnabled()) {
            if (analyticsContext.isLoggingEnabled()) {
                invokerHooks.add(new LoggingHook());
            }
            if (isEventNative) {
                invokerHooks.add(new AnalyticsMessageHook(reporterService));
            }
        }

        long endTime = System.currentTimeMillis(); // Get the end Time
        log.debug("API reactor started in {} ms", (endTime - startTime));

        endpointManager.start();

        this.lifecycleState = Lifecycle.State.STARTED;
        dumpAcceptors();
    }

    @Override
    protected void doStop() throws Exception {
        this.lifecycleState = Lifecycle.State.STOPPING;

        try {
            entrypointConnectorResolver.preStop();
            endpointManager.preStop();

            if (!node.lifecycleState().equals(Lifecycle.State.STARTED)) {
                log.debug("Current node is not started, API handler will be stopped immediately");
                stopNow();
            } else {
                log.debug("Current node is started, API handler will wait for pending requests before stopping");
                stopUntil().onErrorComplete().subscribe();
            }
        } catch (Exception e) {
            log.warn("An error occurred when trying to stop the api reactor {}", this);
        }
    }

    private Completable stopUntil() {
        return interval(STOP_UNTIL_INTERVAL_PERIOD_MS, TimeUnit.MILLISECONDS)
            .timestamp()
            .takeWhile(t -> pendingRequests.get() > 0 && (t.value() + 1) * STOP_UNTIL_INTERVAL_PERIOD_MS < pendingRequestsTimeout)
            .ignoreElements()
            .onErrorComplete()
            .doFinally(this::stopNow);
    }

    private void stopNow() throws Exception {
        log.debug("API reactor is now stopping, closing context for {} ...", this);

        entrypointConnectorResolver.stop();
        endpointManager.stop();
        policyManager.stop();
        resourceLifecycleManager.stop();

        lifecycleState = Lifecycle.State.STOPPED;

        log.debug("API reactor is now stopped: {}", this);
    }

    @Override
    public String toString() {
        return "ApiReactor API id[" + api.getId() + "] name[" + api.getName() + "] version[" + api.getApiVersion() + ']';
    }

    protected void dumpAcceptors() {
        List<Acceptor<?>> acceptors = acceptors();
        log.debug("{} ready to accept requests on:", this);
        acceptors.forEach(acceptor -> log.debug("\t{}", acceptor));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultApiReactor that = (DefaultApiReactor) o;
        return api.equals(that.api);
    }

    @Override
    public int hashCode() {
        return Objects.hash(api);
    }
}
