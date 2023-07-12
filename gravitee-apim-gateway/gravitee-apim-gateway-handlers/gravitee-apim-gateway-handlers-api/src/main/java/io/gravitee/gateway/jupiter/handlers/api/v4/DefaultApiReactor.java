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
package io.gravitee.gateway.jupiter.handlers.api.v4;

import static io.gravitee.gateway.jupiter.api.ExecutionPhase.*;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.*;
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
import io.gravitee.gateway.core.logging.LoggingContext;
import io.gravitee.gateway.core.logging.utils.LoggingUtils;
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
import io.gravitee.gateway.jupiter.core.v4.endpoint.DefaultEndpointConnectorResolver;
import io.gravitee.gateway.jupiter.core.v4.entrypoint.DefaultEntrypointConnectorResolver;
import io.gravitee.gateway.jupiter.core.v4.invoker.EndpointInvoker;
import io.gravitee.gateway.jupiter.handlers.api.adapter.invoker.InvokerAdapter;
import io.gravitee.gateway.jupiter.handlers.api.hook.logging.LoggingHook;
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
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.reporter.api.http.Metrics;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultApiReactor extends AbstractLifecycleComponent<ReactorHandler> implements ApiReactor {

    public static final String PENDING_REQUESTS_TIMEOUT_PROPERTY = "api.pending_requests_timeout";
    protected static final String REQUEST_TIMEOUT_KEY = "REQUEST_TIMEOUT";
    protected static final String SERVICES_TRACING_ENABLED_PROPERTY = "services.tracing.enabled";
    static final int STOP_UNTIL_INTERVAL_PERIOD_MS = 100;
    private static final Logger log = LoggerFactory.getLogger(DefaultApiReactor.class);
    protected final List<ChainHook> processorChainHooks;
    protected final List<InvokerHook> invokerHooks;
    private final Api api;
    private final ComponentProvider componentProvider;
    private final List<TemplateVariableProvider> ctxTemplateVariableProviders;
    private final PolicyManager policyManager;
    private final DefaultEntrypointConnectorResolver entrypointConnectorResolver;
    private final EndpointInvoker defaultInvoker;
    private final ResourceLifecycleManager resourceLifecycleManager;
    private final ProcessorChain beforeHandleProcessors;
    private final ProcessorChain afterHandleProcessors;
    private final ProcessorChain beforeApiFlowsProcessors;
    private final ProcessorChain afterApiFlowsProcessors;
    private final ProcessorChain onErrorProcessors;
    private final ProcessorChain onMessageProcessors;
    private final io.gravitee.gateway.jupiter.handlers.api.flow.FlowChain platformFlowChain;
    private final FlowChain apiPlanFlowChain;
    private final FlowChain apiFlowChain;
    private final Node node;
    private final boolean tracingEnabled;
    private final RequestTimeoutConfiguration requestTimeoutConfiguration;
    private final long pendingRequestsTimeout;
    private final AtomicLong pendingRequests = new AtomicLong(0);
    private final boolean isEventNative;
    private LoggingContext loggingContext;
    private SecurityChain securityChain;
    private Lifecycle.State lifecycleState;

    public DefaultApiReactor(
        final Api api,
        final DeploymentContext deploymentContext,
        final ComponentProvider componentProvider,
        final List<TemplateVariableProvider> ctxTemplateVariableProviders,
        final PolicyManager policyManager,
        final EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        final EndpointConnectorPluginManager endpointConnectorPluginManager,
        final ResourceLifecycleManager resourceLifecycleManager,
        final ApiProcessorChainFactory apiProcessorChainFactory,
        final io.gravitee.gateway.jupiter.handlers.api.flow.FlowChainFactory flowChainFactory,
        final FlowChainFactory v4FlowChainFactory,
        final Configuration configuration,
        final Node node,
        final RequestTimeoutConfiguration requestTimeoutConfiguration
    ) {
        this.api = api;
        this.componentProvider = componentProvider;
        this.ctxTemplateVariableProviders = ctxTemplateVariableProviders;
        this.policyManager = policyManager;
        this.entrypointConnectorResolver =
            new DefaultEntrypointConnectorResolver(api.getDefinition(), deploymentContext, entrypointConnectorPluginManager);
        this.defaultInvoker =
            new EndpointInvoker(
                new DefaultEndpointConnectorResolver(api.getDefinition(), deploymentContext, endpointConnectorPluginManager)
            );

        this.resourceLifecycleManager = resourceLifecycleManager;

        this.beforeHandleProcessors = apiProcessorChainFactory.beforeHandle(api);
        this.afterHandleProcessors = apiProcessorChainFactory.afterHandle(api);
        this.beforeApiFlowsProcessors = apiProcessorChainFactory.beforeApiExecution(api);
        this.afterApiFlowsProcessors = apiProcessorChainFactory.afterApiExecution(api);
        this.onMessageProcessors = apiProcessorChainFactory.onMessage(api);
        this.onErrorProcessors = apiProcessorChainFactory.onError(api);

        this.platformFlowChain = flowChainFactory.createPlatformFlow(api);
        this.apiPlanFlowChain = v4FlowChainFactory.createPlanFlow(api);
        this.apiFlowChain = v4FlowChainFactory.createApiFlow(api);

        this.node = node;
        this.lifecycleState = Lifecycle.State.INITIALIZED;
        this.tracingEnabled = configuration.getProperty(SERVICES_TRACING_ENABLED_PROPERTY, Boolean.class, false);
        this.pendingRequestsTimeout = configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L);
        this.requestTimeoutConfiguration = requestTimeoutConfiguration;

        this.processorChainHooks = new ArrayList<>();
        this.invokerHooks = new ArrayList<>();
        this.isEventNative = this.api.getDefinition().getType() == ApiType.ASYNC;
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
        ctx.setInternalAttribute(ContextAttributes.ATTR_API, api);
        ctx.setInternalAttribute(ATTR_INTERNAL_INVOKER, defaultInvoker);
        ctx.setInternalAttribute(LoggingContext.ATTR_INTERNAL_LOGGING_CONTEXT, loggingContext);
    }

    private void prepareMetrics(HttpExecutionContext ctx) {
        final GenericRequest request = ctx.request();
        final Metrics metrics = request.metrics();

        metrics.setApi(api.getId());
        metrics.setPath(request.pathInfo());
    }

    private Completable handleRequest(final MutableExecutionContext ctx) {
        // Setup all processors before handling the request (ex: logging).
        return new CompletableReactorChain(executeProcessorChain(ctx, beforeHandleProcessors, REQUEST))
            // Execute platform flow chain.
            .chainWith(platformFlowChain.execute(ctx, REQUEST))
            // Execute security chain.
            .chainWith(securityChain.execute(ctx))
            // Before flows processors.
            .chainWith(executeProcessorChain(ctx, beforeApiFlowsProcessors, REQUEST))
            // Resolve entrypoint and prepare request to be handled.
            .chainWith(handleEntrypointRequest(ctx))
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
            .chainWith(executeProcessorChain(ctx, afterApiFlowsProcessors, RESPONSE))
            .chainWithIf(executeProcessorChain(ctx, onMessageProcessors, MESSAGE_RESPONSE), isEventNative)
            .chainWithOnError(error -> processThrowable(ctx, error))
            .chainWith(upstream -> timeout(upstream, ctx))
            // Platform post flows must always be executed (whatever timeout or error).
            .chainWith(
                new CompletableReactorChain(platformFlowChain.execute(ctx, RESPONSE))
                    .chainWithIf(platformFlowChain.execute(ctx, MESSAGE_RESPONSE), isEventNative)
                    .chainWith(upstream -> timeout(upstream, ctx))
            )
            // Handle entrypoint response.
            .chainWith(handleEntrypointResponse(ctx))
            // Catch possible unexpected errors before executing after Handle Processors
            .chainWithOnError(t -> handleUnexpectedError(ctx, t))
            .chainWith(executeProcessorChain(ctx, afterHandleProcessors, RESPONSE))
            // Catch all possible unexpected errors
            .chainWithOnError(t -> handleUnexpectedError(ctx, t))
            // Finally, end the response.
            .chainWith(ctx.response().end())
            .doOnSubscribe(disposable -> pendingRequests.incrementAndGet())
            .doFinally(pendingRequests::decrementAndGet);
    }

    private Completable handleEntrypointRequest(final MutableExecutionContext ctx) {
        return Completable.defer(() -> {
            final EntrypointConnector entrypointConnector = entrypointConnectorResolver.resolve(ctx);
            if (entrypointConnector == null) {
                return ctx.interruptWith(
                    new ExecutionFailure(HttpStatusCode.NOT_FOUND_404).message("No entrypoint matches the incoming request")
                );
            }

            // Add the resolved entrypoint connector into the internal attributes, so it can be used later (ex: for endpoint connector resolution).
            ctx.setInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR, entrypointConnector);

            return entrypointConnector.handleRequest(ctx);
        });
    }

    private Completable handleEntrypointResponse(final MutableExecutionContext ctx) {
        return Completable
            .defer(() -> {
                if (ctx.getInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE) == null) {
                    final EntrypointConnector entrypointConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);
                    if (entrypointConnector != null) {
                        return entrypointConnector.handleResponse(ctx);
                    }
                }
                return Completable.complete();
            })
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
        return defer(() -> {
                if (!TRUE.equals(ctx.<Boolean>getInternalAttribute(ATTR_INTERNAL_INVOKER_SKIP))) {
                    Invoker invoker = getInvoker(ctx);

                    if (invoker != null) {
                        return HookHelper.hook(() -> invoker.invoke(ctx), invoker.getId(), invokerHooks, ctx, null);
                    }
                }
                return Completable.complete();
            })
            .doOnSubscribe(disposable -> ctx.request().metrics().setApiResponseTimeMs(System.currentTimeMillis()))
            .doOnDispose(() -> setApiResponseTimeMetric(ctx))
            .doOnTerminate(() -> setApiResponseTimeMetric(ctx));
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
            return executeProcessorChain(ctx, afterApiFlowsProcessors, RESPONSE);
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
        return Completable.fromRunnable(() -> {
            log.error("Unexpected error while handling request", throwable);
            setApiResponseTimeMetric(ctx);

            ctx.response().status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            ctx.response().reason(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
        });
    }

    private void setApiResponseTimeMetric(final ExecutionContext ctx) {
        final Metrics metrics = ctx.request().metrics();
        if (metrics.getApiResponseTimeMs() > Integer.MAX_VALUE) {
            metrics.setApiResponseTimeMs(System.currentTimeMillis() - metrics.getApiResponseTimeMs());
        }
    }

    private Completable timeout(final Completable upstream, MutableExecutionContext ctx) {
        // When timeout is configured with 0 or less, consider it as infinity: no timeout operator to use in the chain.
        if (requestTimeoutConfiguration.getRequestTimeout() <= 0) {
            return upstream;
        }

        return Completable.defer(() ->
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

        api
            .getDefinition()
            .getListeners()
            .stream()
            .filter(listener -> listener.getType() == ListenerType.HTTP)
            .findFirst()
            .ifPresent(listener -> this.loggingContext = LoggingUtils.getLoggingContext(((HttpListener) listener).getLogging()));

        if (loggingContext != null) {
            invokerHooks.add(new LoggingHook());
        }

        long endTime = System.currentTimeMillis(); // Get the end Time
        log.debug("API reactor started in {} ms", (endTime - startTime));

        this.lifecycleState = Lifecycle.State.STARTED;
        dumpAcceptors();
    }

    @Override
    protected void doStop() throws Exception {
        this.lifecycleState = Lifecycle.State.STOPPING;

        try {
            entrypointConnectorResolver.preStop();
            defaultInvoker.preStop();

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
        defaultInvoker.stop();
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
