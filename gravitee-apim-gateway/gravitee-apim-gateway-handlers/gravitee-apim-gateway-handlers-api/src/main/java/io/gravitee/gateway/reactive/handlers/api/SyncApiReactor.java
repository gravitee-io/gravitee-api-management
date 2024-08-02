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
package io.gravitee.gateway.reactive.handlers.api;

import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.PENDING_REQUESTS_TIMEOUT_PROPERTY;
import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.REPORTERS_LOGGING_EXCLUDED_RESPONSE_TYPES_PROPERTY;
import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.REPORTERS_LOGGING_MAX_SIZE_PROPERTY;
import static io.gravitee.gateway.reactive.api.ExecutionPhase.REQUEST;
import static io.gravitee.gateway.reactive.api.ExecutionPhase.RESPONSE;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_INVOKER;
import static io.reactivex.rxjava3.core.Completable.defer;
import static io.reactivex.rxjava3.core.Observable.interval;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecycleManager;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.GenericRequest;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.hook.ChainHook;
import io.gravitee.gateway.reactive.api.hook.InvokerHook;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionHelper;
import io.gravitee.gateway.reactive.core.hook.HookHelper;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsUtils;
import io.gravitee.gateway.reactive.handlers.api.adapter.invoker.InvokerAdapter;
import io.gravitee.gateway.reactive.handlers.api.flow.FlowChain;
import io.gravitee.gateway.reactive.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.reactive.handlers.api.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.handlers.api.security.SecurityChain;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.LoggingHook;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactive.reactor.ApiReactor;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.AccessPointHttpAcceptor;
import io.gravitee.gateway.reactor.handler.DefaultHttpAcceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SyncApiReactor extends AbstractLifecycleComponent<ReactorHandler> implements ApiReactor<Api> {

    private static final Logger log = LoggerFactory.getLogger(SyncApiReactor.class);
    protected final Api api;
    protected final List<ChainHook> processorChainHooks;
    protected final List<InvokerHook> invokerHooks;
    protected final ComponentProvider componentProvider;
    protected final List<TemplateVariableProvider> templateVariableProviders;
    protected final Invoker defaultInvoker;
    protected final ResourceLifecycleManager resourceLifecycleManager;
    protected final PolicyManager policyManager;
    protected final GroupLifecycleManager groupLifecycleManager;
    protected final FlowChain organizationFlowChain;
    protected final FlowChain apiPlanFlowChain;
    protected final FlowChain apiFlowChain;
    private final ProcessorChain beforeHandleProcessors;
    private final ProcessorChain afterHandleProcessors;
    protected final ProcessorChain beforeSecurityChainProcessors;
    protected final ProcessorChain beforeApiFlowsProcessors;
    protected final ProcessorChain afterApiFlowsProcessors;
    protected final ProcessorChain onErrorProcessors;
    protected final Node node;
    private final RequestTimeoutConfiguration requestTimeoutConfiguration;
    private final AccessPointManager accessPointManager;
    private final EventManager eventManager;
    private final boolean tracingEnabled;
    private final String loggingExcludedResponseType;
    private final String loggingMaxSize;
    private final AtomicInteger pendingRequests = new AtomicInteger(0);
    private final long pendingRequestsTimeout;
    protected AnalyticsContext analyticsContext;
    protected SecurityChain securityChain;

    public SyncApiReactor(
        final Api api,
        final ComponentProvider componentProvider,
        final List<TemplateVariableProvider> templateVariableProviders,
        final Invoker defaultInvoker,
        final ResourceLifecycleManager resourceLifecycleManager,
        final ApiProcessorChainFactory apiProcessorChainFactory,
        final PolicyManager policyManager,
        final FlowChainFactory flowChainFactory,
        final GroupLifecycleManager groupLifecycleManager,
        final Configuration configuration,
        final Node node,
        final RequestTimeoutConfiguration requestTimeoutConfiguration,
        final AccessPointManager accessPointManager,
        final EventManager eventManager
    ) {
        this.api = api;
        this.componentProvider = componentProvider;
        this.templateVariableProviders = templateVariableProviders;
        this.defaultInvoker = defaultInvoker;
        this.resourceLifecycleManager = resourceLifecycleManager;
        this.policyManager = policyManager;
        this.groupLifecycleManager = groupLifecycleManager;
        this.requestTimeoutConfiguration = requestTimeoutConfiguration;
        this.accessPointManager = accessPointManager;
        this.eventManager = eventManager;

        this.beforeHandleProcessors = apiProcessorChainFactory.beforeHandle(api);
        this.afterHandleProcessors = apiProcessorChainFactory.afterHandle(api);
        this.beforeSecurityChainProcessors = apiProcessorChainFactory.beforeSecurityChain(api);
        this.beforeApiFlowsProcessors = apiProcessorChainFactory.beforeApiExecution(api);
        this.afterApiFlowsProcessors = apiProcessorChainFactory.afterApiExecution(api);
        this.onErrorProcessors = apiProcessorChainFactory.onError(api);

        this.organizationFlowChain = flowChainFactory.createOrganizationFlow(api);
        this.apiPlanFlowChain = flowChainFactory.createPlanFlow(api);
        this.apiFlowChain = flowChainFactory.createApiFlow(api);

        this.node = node;

        this.tracingEnabled = configuration.getProperty("services.tracing.enabled", Boolean.class, false);
        this.pendingRequestsTimeout = configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L);
        this.loggingExcludedResponseType =
            configuration.getProperty(REPORTERS_LOGGING_EXCLUDED_RESPONSE_TYPES_PROPERTY, String.class, null);
        this.loggingMaxSize = configuration.getProperty(REPORTERS_LOGGING_MAX_SIZE_PROPERTY, String.class, null);

        this.processorChainHooks = new ArrayList<>();
        this.invokerHooks = new ArrayList<>();
    }

    @Override
    public Api api() {
        return api;
    }

    @Override
    public Completable handle(final MutableExecutionContext ctx) {
        ctx.componentProvider(componentProvider);
        if (templateVariableProviders != null) {
            ctx.templateVariableProviders(new HashSet<>(templateVariableProviders));
        }

        // Prepare attributes and metrics before handling the request.
        prepareContextAttributes(ctx);
        prepareMetrics(ctx);

        pendingRequests.incrementAndGet();
        return handleRequest(ctx).doFinally(pendingRequests::decrementAndGet);
    }

    private void prepareContextAttributes(MutableExecutionContext ctx) {
        ctx.setAttribute(ContextAttributes.ATTR_CONTEXT_PATH, ctx.request().contextPath());
        ctx.setAttribute(ContextAttributes.ATTR_API, api.getId());
        ctx.setAttribute(ContextAttributes.ATTR_API_DEPLOYED_AT, api.getDeployedAt().getTime());
        ctx.setAttribute(ContextAttributes.ATTR_ORGANIZATION, api.getOrganizationId());
        ctx.setAttribute(ContextAttributes.ATTR_ENVIRONMENT, api.getEnvironmentId());
        ctx.setInternalAttribute(ATTR_INTERNAL_INVOKER, defaultInvoker);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);
    }

    private void prepareMetrics(HttpExecutionContext ctx) {
        final GenericRequest request = ctx.request();
        final Metrics metrics = ctx.metrics();

        metrics.setApiId(api.getId());
        metrics.setApiName(api.getName());
        metrics.setPathInfo(request.pathInfo());
    }

    private void setApiResponseTimeMetric(HttpExecutionContext ctx) {
        if (ctx.metrics().getEndpointResponseTimeMs() > Integer.MAX_VALUE) {
            ctx.metrics().setEndpointResponseTimeMs(System.currentTimeMillis() - ctx.metrics().getEndpointResponseTimeMs());
        }
    }

    private Completable handleRequest(final MutableExecutionContext ctx) {
        // Setup all processors before handling the request (ex: logging).
        return executeProcessorChain(ctx, beforeHandleProcessors, REQUEST)
            // Execute organization flow chain
            .andThen(organizationFlowChain.execute(ctx, REQUEST))
            // Before Security Chain.
            .andThen(executeProcessorChain(ctx, beforeSecurityChainProcessors, REQUEST))
            // Execute security chain.
            .andThen(securityChain.execute(ctx))
            // Execute before flows processors
            .andThen(executeProcessorChain(ctx, beforeApiFlowsProcessors, REQUEST))
            .andThen(executeFlowChain(ctx, apiPlanFlowChain, REQUEST))
            .andThen(executeFlowChain(ctx, apiFlowChain, REQUEST))
            // All request flows have been executed. Invokes backend.
            .andThen(invokeBackend(ctx))
            // Execute response flows (api plan, api, platform).
            .andThen(executeFlowChain(ctx, apiPlanFlowChain, RESPONSE))
            .andThen(executeFlowChain(ctx, apiFlowChain, RESPONSE))
            // Execute after flows processors
            .andThen(executeProcessorChain(ctx, afterApiFlowsProcessors, RESPONSE))
            .onErrorResumeNext(error -> processThrowable(ctx, error))
            .compose(upstream -> timeout(upstream, ctx))
            // Platform post flows must always be executed
            .andThen(executeFlowChain(ctx, organizationFlowChain, RESPONSE).compose(upstream -> timeout(upstream, ctx)))
            // Catch all possible unexpected errors.
            .onErrorResumeNext(t -> handleUnexpectedError(ctx, t))
            .andThen(executeProcessorChain(ctx, afterHandleProcessors, RESPONSE))
            // Finally, end the response.
            .andThen(endResponse(ctx));
    }

    /**
     * Executes the processor chain without taking care if the current context is interrupted or not.
     *
     * @param ctx the current context
     * @param processorChain the processor chain
     * @param phase the phase to execute.
     *
     * @return a {@link Completable} that will complete once the flow chain phase has been fully executed.
     */
    private Completable executeProcessorChain(
        final MutableExecutionContext ctx,
        final ProcessorChain processorChain,
        final ExecutionPhase phase
    ) {
        return HookHelper.hook(() -> processorChain.execute(ctx, phase), processorChain.getId(), processorChainHooks, ctx, phase);
    }

    /**
     * Executes the flow chain for the specified phase without taking care if the current context is interrupted or not.
     * This is particularly useful to execute platform flow chain which must always be executed whatever the result of the other flow chain executions.
     *
     * @param ctx the current context
     * @param flowChain the flow chain
     * @param phase the phase to execute
     *
     * @return a {@link Completable} that will complete once the flow chain phase has been fully executed.
     */
    private Completable executeFlowChain(final MutableExecutionContext ctx, final FlowChain flowChain, final ExecutionPhase phase) {
        return defer(() -> flowChain.execute(ctx, phase));
    }

    /**
     * Invokes the backend if the context is not interrupted and if there is nothing in the context that indicates to skip the invoker.
     *
     * @param ctx the current execution context.
     *
     * @return a {@link Completable} that will complete once the invoker has been invoked or that completes immediately if execution isn't required.
     */
    private Completable invokeBackend(final MutableExecutionContext ctx) {
        return defer(() -> {
                if (!Objects.equals(false, ctx.<Boolean>getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER))) {
                    Invoker invoker = getInvoker(ctx);

                    if (invoker != null) {
                        return HookHelper.hook(() -> invoker.invoke(ctx), invoker.getId(), invokerHooks, ctx, null);
                    }
                }
                return Completable.complete();
            })
            .doOnSubscribe(disposable -> ctx.metrics().setEndpointResponseTimeMs(System.currentTimeMillis()))
            .doOnDispose(() -> setApiResponseTimeMetric(ctx))
            .doOnTerminate(() -> setApiResponseTimeMetric(ctx));
    }

    /**
     * Get the invoker currently referenced in the execution context.
     * If the invoker has been overridden by a v3 policy, the {@link io.gravitee.gateway.api.Invoker} will be adapted to match with the expected v4 emulation {@link Invoker} type.
     *
     * @param ctx the current context where the invoker is referenced.
     * @return the current invoker in the expected type.
     */
    private Invoker getInvoker(HttpExecutionContext ctx) {
        final Object invoker = ctx.getInternalAttribute(ATTR_INTERNAL_INVOKER);

        if (invoker == null) {
            return null;
        }

        if ((invoker instanceof io.gravitee.gateway.api.Invoker) && !(invoker instanceof InvokerAdapter)) {
            return new InvokerAdapter((io.gravitee.gateway.api.Invoker) invoker);
        }

        return (Invoker) invoker;
    }

    private Completable endResponse(MutableExecutionContext ctx) {
        return ctx.response().end(ctx);
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
                        new ExecutionFailure(HttpStatusCode.GATEWAY_TIMEOUT_504).key("REQUEST_TIMEOUT").message("Request timeout")
                    )
                    .onErrorResumeNext(error -> executeProcessorChain(ctx, onErrorProcessors, RESPONSE))
            )
        );
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

    private Completable handleUnexpectedError(final HttpExecutionContext ctx, final Throwable throwable) {
        return Completable.fromRunnable(() -> {
            log.error("Unexpected error while handling request", throwable);
            setApiResponseTimeMetric(ctx);

            ctx.response().status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            ctx.response().reason(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
        });
    }

    @Override
    public List<Acceptor<?>> acceptors() {
        try {
            return api
                .getDefinition()
                .getProxy()
                .getVirtualHosts()
                .stream()
                .map(virtualHost -> {
                    if (virtualHost.getHost() != null) {
                        return new DefaultHttpAcceptor(
                            virtualHost.getHost(),
                            virtualHost.getPath(),
                            this,
                            api.getDefinition().getProxy().getServers()
                        );
                    } else {
                        return new AccessPointHttpAcceptor(
                            eventManager,
                            api.getEnvironmentId(),
                            accessPointManager.getByEnvironmentId(api.getEnvironmentId()),
                            virtualHost.getPath(),
                            this,
                            api.getDefinition().getProxy().getServers()
                        );
                    }
                })
                .collect(Collectors.toList());
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    @Override
    protected void doStart() throws Exception {
        log.debug("API reactor is now starting, preparing API context...");
        long startTime = System.currentTimeMillis(); // Get the start Time

        // Start resources before
        resourceLifecycleManager.start();
        policyManager.start();
        groupLifecycleManager.start();

        dumpVirtualHosts();

        // Create securityChain once policy manager has been started.
        this.securityChain = new SecurityChain(api.getDefinition(), policyManager, REQUEST);
        if (tracingEnabled) {
            processorChainHooks.add(new TracingHook("processor-chain"));
            invokerHooks.add(new TracingHook("invoker"));
            securityChain.addHooks(new TracingHook("security-plan"));
        }

        this.analyticsContext = AnalyticsUtils.createAnalyticsContext(api.getDefinition(), loggingMaxSize, loggingExcludedResponseType);
        if (analyticsContext.isLoggingEnabled()) {
            invokerHooks.add(new LoggingHook());
        }

        long endTime = System.currentTimeMillis(); // Get the end Time
        log.debug("API reactor started in {} ms", (endTime - startTime));
    }

    @Override
    protected void doStop() throws Exception {
        if (!node.lifecycleState().equals(Lifecycle.State.STARTED)) {
            log.debug("Current node is not started, API handler will be stopped immediately");
            stopNow();
        } else {
            log.debug("Current node is started, API handler will wait for pending requests before stopping");
            long timeout = System.currentTimeMillis() + pendingRequestsTimeout;
            stopUntil(timeout).subscribe();
        }
    }

    private void stopNow() throws Exception {
        log.debug("API reactor is now stopping, closing context for {} ...", this);

        policyManager.stop();
        resourceLifecycleManager.stop();
        groupLifecycleManager.stop();

        log.debug("API reactor is now stopped: {}", this);
    }

    protected Observable<Long> stopUntil(long timeout) {
        return interval(100, TimeUnit.MILLISECONDS)
            .timeout(timeout, TimeUnit.MILLISECONDS)
            .takeWhile(t -> pendingRequests.get() > 0)
            .doFinally(this::stopNow);
    }

    @Override
    public String toString() {
        return "SyncApiReactor API id[" + api.getId() + "] name[" + api.getName() + "] version[" + api.getApiVersion() + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncApiReactor that = (SyncApiReactor) o;
        return api.equals(that.api);
    }

    @Override
    public int hashCode() {
        return Objects.hash(api);
    }

    protected void dumpVirtualHosts() {
        List<Acceptor<?>> acceptors = acceptors();
        log.debug("{} ready to accept requests on:", this);
        acceptors.forEach(acceptor -> log.debug("\t{}", acceptor));
    }
}
