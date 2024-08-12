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
package io.gravitee.gateway.reactive.handlers.api.v4;

import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.REPORTERS_LOGGING_EXCLUDED_RESPONSE_TYPES_PROPERTY;
import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.REPORTERS_LOGGING_MAX_SIZE_PROPERTY;
import static io.gravitee.gateway.reactive.api.ExecutionPhase.REQUEST;
import static io.gravitee.gateway.reactive.api.ExecutionPhase.RESPONSE;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_INVOKER;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_INVOKER_SKIP;
import static io.reactivex.rxjava3.core.Completable.defer;
import static java.lang.Boolean.TRUE;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.apiservice.ApiService;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.hook.ChainHook;
import io.gravitee.gateway.reactive.api.hook.InvokerHook;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionHelper;
import io.gravitee.gateway.reactive.core.failover.FailoverInvoker;
import io.gravitee.gateway.reactive.core.hook.HookHelper;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.entrypoint.DefaultEntrypointConnectorResolver;
import io.gravitee.gateway.reactive.core.v4.invoker.EndpointInvoker;
import io.gravitee.gateway.reactive.handlers.api.adapter.invoker.InvokerAdapter;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.LoggingHook;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChain;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChainFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.security.SecurityChain;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.AccessPointHttpAcceptor;
import io.gravitee.gateway.reactor.handler.DefaultHttpAcceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultApiReactor extends AbstractApiReactor {

    public static final String SERVICES_TRACING_ENABLED_PROPERTY = "services.tracing.enabled";
    public static final String API_VALIDATE_SUBSCRIPTION_PROPERTY = "api.validateSubscription";

    private static final Logger log = LoggerFactory.getLogger(DefaultApiReactor.class);
    protected final List<ChainHook> processorChainHooks;
    protected final List<InvokerHook> invokerHooks;

    @Getter
    private final ComponentProvider componentProvider;

    @Getter
    private final List<TemplateVariableProvider> ctxTemplateVariableProviders;

    private final PolicyManager policyManager;
    private final ApiServicePluginManager apiServicePluginManager;
    private final EndpointManager endpointManager;
    protected final ReporterService reporterService;
    private final AccessPointManager accessPointManager;
    private final EventManager eventManager;
    private final ResourceLifecycleManager resourceLifecycleManager;
    protected final ProcessorChain beforeHandleProcessors;
    protected final ProcessorChain afterHandleProcessors;
    protected final ProcessorChain beforeSecurityChainProcessors;
    protected final ProcessorChain beforeApiExecutionProcessors;
    protected final ProcessorChain afterApiExecutionProcessors;
    protected final ProcessorChain onErrorProcessors;
    protected final io.gravitee.gateway.reactive.handlers.api.flow.FlowChain organizationFlowChain;
    protected final FlowChain apiPlanFlowChain;
    protected final FlowChain apiFlowChain;
    private final Node node;
    private final boolean tracingEnabled;
    protected final String loggingExcludedResponseType;
    protected final String loggingMaxSize;
    private final DeploymentContext deploymentContext;
    protected SecurityChain securityChain;
    private Lifecycle.State lifecycleState;
    protected AnalyticsContext analyticsContext;
    private List<ApiService> services;
    private final boolean validateSubscriptionEnabled;
    private List<Acceptor<?>> acceptors;

    public DefaultApiReactor(
        final Api api,
        final DeploymentContext deploymentContext,
        final ComponentProvider componentProvider,
        final List<TemplateVariableProvider> ctxTemplateVariableProviders,
        final PolicyManager policyManager,
        final EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        final ApiServicePluginManager apiServicePluginManager,
        final EndpointManager endpointManager,
        final ResourceLifecycleManager resourceLifecycleManager,
        final ApiProcessorChainFactory apiProcessorChainFactory,
        final io.gravitee.gateway.reactive.handlers.api.flow.FlowChainFactory flowChainFactory,
        final FlowChainFactory v4FlowChainFactory,
        final Configuration configuration,
        final Node node,
        final RequestTimeoutConfiguration requestTimeoutConfiguration,
        final ReporterService reporterService,
        final AccessPointManager accessPointManager,
        final EventManager eventManager
    ) {
        super(
            configuration,
            api,
            new DefaultEntrypointConnectorResolver(api.getDefinition(), deploymentContext, entrypointConnectorPluginManager),
            requestTimeoutConfiguration
        );
        this.deploymentContext = deploymentContext;
        this.componentProvider = componentProvider;
        this.ctxTemplateVariableProviders = ctxTemplateVariableProviders;
        this.policyManager = policyManager;
        this.apiServicePluginManager = apiServicePluginManager;
        this.endpointManager = endpointManager;
        this.reporterService = reporterService;
        this.accessPointManager = accessPointManager;
        this.eventManager = eventManager;
        this.defaultInvoker = endpointInvoker(endpointManager);

        this.resourceLifecycleManager = resourceLifecycleManager;

        this.beforeHandleProcessors = apiProcessorChainFactory.beforeHandle(api);
        this.afterHandleProcessors = apiProcessorChainFactory.afterHandle(api);
        this.beforeSecurityChainProcessors = apiProcessorChainFactory.beforeSecurityChain(api);
        this.beforeApiExecutionProcessors = apiProcessorChainFactory.beforeApiExecution(api);
        this.afterApiExecutionProcessors = apiProcessorChainFactory.afterApiExecution(api);

        this.onErrorProcessors = apiProcessorChainFactory.onError(api);

        this.organizationFlowChain = flowChainFactory.createOrganizationFlow(api);
        this.apiPlanFlowChain = v4FlowChainFactory.createPlanFlow(api);
        this.apiFlowChain = v4FlowChainFactory.createApiFlow(api);

        this.node = node;
        this.lifecycleState = Lifecycle.State.INITIALIZED;
        this.tracingEnabled = configuration.getProperty(SERVICES_TRACING_ENABLED_PROPERTY, Boolean.class, false);
        this.validateSubscriptionEnabled = configuration.getProperty(API_VALIDATE_SUBSCRIPTION_PROPERTY, Boolean.class, true);
        this.loggingExcludedResponseType =
            configuration.getProperty(REPORTERS_LOGGING_EXCLUDED_RESPONSE_TYPES_PROPERTY, String.class, null);
        this.loggingMaxSize = configuration.getProperty(REPORTERS_LOGGING_MAX_SIZE_PROPERTY, String.class, null);

        this.processorChainHooks = new ArrayList<>();
        this.invokerHooks = new ArrayList<>();
    }

    protected Invoker endpointInvoker(EndpointManager endpointManager) {
        final EndpointInvoker endpointInvoker = new EndpointInvoker(endpointManager);
        if (api.getDefinition().failoverEnabled()) {
            return new FailoverInvoker(endpointInvoker, api.getDefinition().getFailover(), api.getId());
        }
        return endpointInvoker;
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
        prepareCommonAttributes(ctx);
        ctx.setAttribute(ContextAttributes.ATTR_CONTEXT_PATH, ctx.request().contextPath());
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_VALIDATE_SUBSCRIPTION, validateSubscriptionEnabled);
    }

    private void prepareMetrics(HttpExecutionContext ctx) {
        final Metrics metrics = ctx.metrics();

        metrics.setApiId(api.getId());
        metrics.setApiType(api.getDefinition().getType().getLabel());
    }

    @Override
    ExecutionFailure noEntrypointFailure() {
        return new ExecutionFailure(HttpStatusCode.NOT_FOUND_404).message(NO_ENTRYPOINT_FAILURE_MESSAGE);
    }

    protected Completable handleRequest(final MutableExecutionContext ctx) {
        // Setup all processors before handling the request (ex: logging).
        return new CompletableReactorChain(executeProcessorChain(ctx, beforeHandleProcessors, REQUEST))
            // Execute organization flow chain.
            .chainWith(organizationFlowChain.execute(ctx, REQUEST))
            // Before Security Chain.
            .chainWith(executeProcessorChain(ctx, beforeSecurityChainProcessors, REQUEST))
            // Execute security chain.
            .chainWith(securityChain.execute(ctx))
            // Before flows processors.
            .chainWith(executeProcessorChain(ctx, beforeApiExecutionProcessors, REQUEST))
            // Resolve entrypoint and prepare request to be handled.
            .chainWith(handleEntrypointRequest(ctx))
            // Execute all flows for request phases.
            .chainWith(apiPlanFlowChain.execute(ctx, REQUEST))
            .chainWith(apiFlowChain.execute(ctx, REQUEST))
            // Invoke the backend.
            .chainWith(invokeBackend(ctx))
            // Execute all flows for response phases.
            .chainWith(apiPlanFlowChain.execute(ctx, RESPONSE))
            .chainWith(apiFlowChain.execute(ctx, RESPONSE))
            // After flows processors.
            .chainWith(executeProcessorChain(ctx, afterApiExecutionProcessors, RESPONSE))
            .chainWithOnError(error -> processThrowable(ctx, error))
            .chainWith(upstream -> timeout(upstream, ctx))
            // Platform post flows must always be executed (whatever timeout or error).
            .chainWith(
                new CompletableReactorChain(organizationFlowChain.execute(ctx, RESPONSE)).chainWith(upstream -> timeout(upstream, ctx))
            )
            // Before entrypoint response
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

    protected Completable executeProcessorChain(
        final MutableExecutionContext ctx,
        final ProcessorChain processorChain,
        final ExecutionPhase phase
    ) {
        return HookHelper.hook(() -> processorChain.execute(ctx, phase), processorChain.getId(), processorChainHooks, ctx, phase);
    }

    protected Completable invokeBackend(final MutableExecutionContext ctx) {
        return defer(() -> {
                if (!TRUE.equals(ctx.<Boolean>getInternalAttribute(ATTR_INTERNAL_INVOKER_SKIP))) {
                    Invoker invoker = getInvoker(ctx);

                    if (invoker != null) {
                        return HookHelper.hook(() -> invoker.invoke(ctx), invoker.getId(), invokerHooks, ctx, null);
                    }
                }
                return Completable.complete();
            })
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

    @Override
    Completable onTimeout(MutableExecutionContext ctx) {
        return ctx
            .interruptWith(
                new ExecutionFailure(HttpStatusCode.GATEWAY_TIMEOUT_504).key(REQUEST_TIMEOUT_KEY).message(REQUEST_TIMEOUT_MESSAGE)
            )
            .onErrorResumeNext(error -> executeProcessorChain(ctx, onErrorProcessors, RESPONSE));
    }

    /**
     * Process the given throwable by checking the type of interruption and execute the right processor chain accordingly
     *
     * @param ctx the current context
     * @param throwable the source error
     * @return a {@link Completable} that will complete once processor chain has been fully executed or source error rethrown
     */
    protected Completable processThrowable(final MutableExecutionContext ctx, final Throwable throwable) {
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

    protected Completable handleUnexpectedError(final ExecutionContext ctx, final Throwable throwable) {
        return Completable.fromRunnable(() -> {
            log.error("Unexpected error while handling request", throwable);
            computeEndpointResponseTimeMetric(ctx);

            ctx.response().status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            ctx.response().reason(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
        });
    }

    private void initEndpointResponseTimeMetric(ExecutionContext ctx) {
        Metrics metrics = ctx.metrics();
        // Initialize the response time with current time millis which is higher than Integer.MAX_VALUE
        metrics.setEndpointResponseTimeMs(System.currentTimeMillis());
    }

    private void computeEndpointResponseTimeMetric(ExecutionContext ctx) {
        Metrics metrics = ctx.metrics();
        // If the response time is higher than Integer.MAX_VALUE, that means it has not been already computed (see init method)
        if (metrics.getEndpointResponseTimeMs() > Integer.MAX_VALUE) {
            metrics.setEndpointResponseTimeMs(System.currentTimeMillis() - metrics.getEndpointResponseTimeMs());
        }
    }

    @Override
    public List<Acceptor<?>> acceptors() {
        if (acceptors == null) {
            acceptors = new ArrayList<>();

            for (Listener listener : api.getDefinition().getListeners()) {
                if (listener.getType() == ListenerType.HTTP) {
                    acceptors.addAll(prepareHttpAcceptors(listener));
                }
            }
        }

        return acceptors;
    }

    protected List<HttpAcceptor> prepareHttpAcceptors(Listener listener) {
        return ((HttpListener) listener).getPaths()
            .stream()
            .map(path -> {
                if (path.getHost() != null) {
                    return new DefaultHttpAcceptor(path.getHost(), path.getPath(), this, listener.getServers());
                } else {
                    return new AccessPointHttpAcceptor(
                        eventManager,
                        api.getEnvironmentId(),
                        accessPointManager.getByEnvironmentId(api.getEnvironmentId()),
                        path.getPath(),
                        this,
                        listener.getServers()
                    );
                }
            })
            .toList();
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

        analyticsContext = analyticsContext();
        if (analyticsContext.isEnabled()) {
            if (analyticsContext.isLoggingEnabled()) {
                invokerHooks.add(new LoggingHook());
            }
        }

        addInvokerHooks(invokerHooks);

        endpointManager.start();

        services =
            apiServicePluginManager
                .getAllFactories()
                .stream()
                .map(apiServiceFactory -> apiServiceFactory.createService(deploymentContext))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Completable.concat(services.stream().map(ApiService::start).collect(Collectors.toList())).blockingAwait();

        this.lifecycleState = Lifecycle.State.STARTED;

        long endTime = System.currentTimeMillis(); // Get the end Time
        log.debug("API reactor started in {} ms", (endTime - startTime));

        dumpAcceptors();
    }

    protected void addInvokerHooks(List<InvokerHook> invokerHooks) {}

    protected AnalyticsContext analyticsContext() {
        return new AnalyticsContext(api.getDefinition().getAnalytics(), loggingMaxSize, loggingExcludedResponseType);
    }

    @Override
    protected void doStop() throws Exception {
        this.lifecycleState = Lifecycle.State.STOPPING;

        try {
            Completable.concat(services.stream().map(ApiService::stop).collect(Collectors.toList())).blockingAwait();

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

    @Override
    void stopNow() throws Exception {
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
