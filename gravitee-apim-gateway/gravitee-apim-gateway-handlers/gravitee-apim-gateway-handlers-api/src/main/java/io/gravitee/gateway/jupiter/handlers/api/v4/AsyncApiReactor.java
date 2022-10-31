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

import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.PENDING_REQUESTS_TIMEOUT_PROPERTY;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.MESSAGE_REQUEST;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.MESSAGE_RESPONSE;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.REQUEST;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.RESPONSE;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static io.reactivex.rxjava3.core.Completable.defer;
import static io.reactivex.rxjava3.core.Observable.interval;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.hook.ChainHook;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionHelper;
import io.gravitee.gateway.jupiter.core.hook.HookHelper;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.core.v4.endpoint.DefaultEndpointConnectorResolver;
import io.gravitee.gateway.jupiter.core.v4.entrypoint.DefaultEntrypointConnectorResolver;
import io.gravitee.gateway.jupiter.core.v4.invoker.EndpointInvoker;
import io.gravitee.gateway.jupiter.handlers.api.adapter.invoker.InvokerAdapter;
import io.gravitee.gateway.jupiter.handlers.api.v4.flow.FlowChain;
import io.gravitee.gateway.jupiter.handlers.api.v4.flow.FlowChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.ApiMessageProcessorChainFactory;
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
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AsyncApiReactor extends AbstractLifecycleComponent<ReactorHandler> implements ApiReactor {

    private static final Logger log = LoggerFactory.getLogger(AsyncApiReactor.class);
    private static final String ATTR_INVOKER_SKIP = "invoker.skip";
    protected static final int STOP_UNTIL_INTERVAL_PERIOD_MS = 100;
    protected final List<ChainHook> processorChainHooks;
    private final Api api;
    private final ComponentProvider componentProvider;
    private final PolicyManager policyManager;
    private final DefaultEntrypointConnectorResolver asyncEntrypointResolver;
    private final EndpointInvoker defaultInvoker;
    private final ResourceLifecycleManager resourceLifecycleManager;
    private final ProcessorChain apiPreProcessorChain;
    private final ProcessorChain apiPostProcessorChain;
    private final ProcessorChain apiErrorProcessorChain;
    private final ProcessorChain apiMessageProcessorChain;
    private final io.gravitee.gateway.jupiter.handlers.api.flow.FlowChain platformFlowChain;
    private final FlowChain apiPlanFlowChain;
    private final FlowChain apiFlowChain;
    private final Node node;
    private final long pendingRequestsTimeout;
    private final AtomicLong pendingRequests = new AtomicLong(0);
    private SecurityChain securityChain;
    private Lifecycle.State lifecycleState;

    public AsyncApiReactor(
        final Api api,
        final ComponentProvider apiComponentProvider,
        final PolicyManager policyManager,
        final EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        final EndpointConnectorPluginManager endpointConnectorPluginManager,
        final ResourceLifecycleManager resourceLifecycleManager,
        final ApiProcessorChainFactory apiProcessorChainFactory,
        final ApiMessageProcessorChainFactory apiMessageProcessorChainFactory,
        final io.gravitee.gateway.jupiter.handlers.api.flow.FlowChainFactory flowChainFactory,
        final FlowChainFactory v4FlowChainFactory,
        final Configuration configuration,
        final Node node
    ) {
        this.api = api;
        this.componentProvider = apiComponentProvider;
        this.policyManager = policyManager;
        this.resourceLifecycleManager = resourceLifecycleManager;

        this.asyncEntrypointResolver = new DefaultEntrypointConnectorResolver(api.getDefinition(), entrypointConnectorPluginManager);
        this.defaultInvoker =
            new EndpointInvoker(new DefaultEndpointConnectorResolver(api.getDefinition(), endpointConnectorPluginManager));

        this.apiPreProcessorChain = apiProcessorChainFactory.preProcessorChain(api);
        this.apiPostProcessorChain = apiProcessorChainFactory.postProcessorChain(api);
        this.apiErrorProcessorChain = apiProcessorChainFactory.errorProcessorChain(api);
        this.apiMessageProcessorChain = apiMessageProcessorChainFactory.messageProcessorChain(api);
        this.platformFlowChain = flowChainFactory.createPlatformFlow(api);
        this.apiPlanFlowChain = v4FlowChainFactory.createPlanFlow(api);
        this.apiFlowChain = v4FlowChainFactory.createApiFlow(api);

        this.processorChainHooks = new ArrayList<>();
        this.lifecycleState = Lifecycle.State.INITIALIZED;
        this.pendingRequestsTimeout = configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L);
        this.node = node;
    }

    @Override
    public ApiType apiType() {
        return ApiType.ASYNC;
    }

    @Override
    public Completable handle(final MutableExecutionContext ctx) {
        ctx.componentProvider(componentProvider);

        // Prepare attributes and metrics before handling the request.
        prepareContextAttributes(ctx);

        return handleRequest(ctx);
    }

    private void prepareContextAttributes(MutableExecutionContext ctx) {
        ctx.setAttribute(ContextAttributes.ATTR_CONTEXT_PATH, ctx.request().contextPath());
        ctx.setAttribute(ContextAttributes.ATTR_API, api.getId());
        ctx.setAttribute(ContextAttributes.ATTR_API_DEPLOYED_AT, api.getDeployedAt().getTime());
        ctx.setAttribute(ContextAttributes.ATTR_ORGANIZATION, api.getOrganizationId());
        ctx.setAttribute(ContextAttributes.ATTR_ENVIRONMENT, api.getEnvironmentId());
        ctx.setInternalAttribute(ContextAttributes.ATTR_API, api);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER, defaultInvoker);
    }

    private Completable handleRequest(final MutableExecutionContext ctx) {
        return platformFlowChain
            .execute(ctx, REQUEST)
            .andThen(securityChain.execute(ctx))
            .andThen(handleEntrypointRequest(ctx))
            .andThen(platformFlowChain.execute(ctx, MESSAGE_REQUEST))
            .andThen(executeProcessorsChain(ctx, apiPreProcessorChain, REQUEST))
            .andThen(apiPlanFlowChain.execute(ctx, REQUEST))
            .andThen(apiPlanFlowChain.execute(ctx, MESSAGE_REQUEST))
            .andThen(apiFlowChain.execute(ctx, REQUEST))
            .andThen(apiFlowChain.execute(ctx, MESSAGE_REQUEST))
            .andThen(invokeBackend(ctx))
            .andThen(apiPlanFlowChain.execute(ctx, RESPONSE))
            .andThen(apiPlanFlowChain.execute(ctx, MESSAGE_RESPONSE))
            .andThen(apiFlowChain.execute(ctx, RESPONSE))
            .andThen(apiFlowChain.execute(ctx, MESSAGE_RESPONSE))
            .andThen(platformFlowChain.execute(ctx, RESPONSE))
            .andThen(platformFlowChain.execute(ctx, MESSAGE_RESPONSE))
            .andThen(executeProcessorsChain(ctx, apiMessageProcessorChain, MESSAGE_RESPONSE))
            .andThen(handleEntrypointResponse(ctx))
            .andThen(executeProcessorsChain(ctx, apiPostProcessorChain, RESPONSE))
            .onErrorResumeNext(error -> processThrowable(ctx, error))
            // Catch all possible unexpected errors
            .onErrorResumeNext(t -> handleUnexpectedError(ctx, t))
            // Finally, end the response.
            .andThen(ctx.response().end())
            .doOnSubscribe(disposable -> pendingRequests.incrementAndGet())
            .doFinally(pendingRequests::decrementAndGet);
    }

    private Completable handleEntrypointRequest(final MutableExecutionContext ctx) {
        return Completable.defer(
            () -> {
                EntrypointAsyncConnector entrypointConnector = asyncEntrypointResolver.resolve(ctx);
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
        return Completable.defer(
            () -> {
                EntrypointAsyncConnector entrypointConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);
                if (entrypointConnector != null) {
                    return entrypointConnector.handleResponse(ctx);
                }
                return Completable.complete();
            }
        );
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
    private Completable executeProcessorsChain(
        final MutableExecutionContext ctx,
        final ProcessorChain processorChain,
        final ExecutionPhase phase
    ) {
        return HookHelper.hook(() -> processorChain.execute(ctx, phase), processorChain.getId(), processorChainHooks, ctx, phase);
    }

    private Completable invokeBackend(final ExecutionContext ctx) {
        return defer(
                () -> {
                    if (!Objects.equals(false, ctx.<Boolean>getAttribute(ATTR_INVOKER_SKIP))) {
                        Invoker invoker = getInvoker(ctx);

                        if (invoker != null) {
                            return invoker.invoke(ctx);
                        }
                    }
                    return Completable.complete();
                }
            )
            .doOnSubscribe(disposable -> ctx.request().metrics().setApiResponseTimeMs(System.currentTimeMillis()))
            .doOnDispose(() -> setApiResponseTimeMetric(ctx))
            .doOnTerminate(() -> setApiResponseTimeMetric(ctx));
    }

    private Invoker getInvoker(ExecutionContext ctx) {
        final Object invoker = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER);

        if (invoker == null) {
            return null;
        }

        if (!(invoker instanceof Invoker)) {
            return new InvokerAdapter((io.gravitee.gateway.api.Invoker) invoker);
        }

        return (Invoker) invoker;
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
            return executeProcessorsChain(ctx, apiPostProcessorChain, RESPONSE);
        } else if (InterruptionHelper.isInterruptionWithFailure(throwable)) {
            // In case of any interruption with execution failure, execute api error processor chain and resume the execution
            return executeProcessorsChain(ctx, apiErrorProcessorChain, RESPONSE);
        } else {
            // In case of any error exception, log original exception, execute api error processor chain and resume the execution
            log.error("Unexpected error while handling request", throwable);
            return executeProcessorsChain(ctx, apiErrorProcessorChain, RESPONSE);
        }
    }

    private Completable handleUnexpectedError(final ExecutionContext ctx, final Throwable throwable) {
        return Completable.fromRunnable(
            () -> {
                log.error("Unexpected error while handling request", throwable);
                setApiResponseTimeMetric(ctx);

                ctx.response().status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                ctx.response().reason(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
            }
        );
    }

    private void setApiResponseTimeMetric(ExecutionContext ctx) {
        if (ctx.request().metrics().getApiResponseTimeMs() > Integer.MAX_VALUE) {
            ctx.request().metrics().setApiResponseTimeMs(System.currentTimeMillis() - ctx.request().metrics().getApiResponseTimeMs());
        }
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
        policyManager.start();

        // Create securityChain once policy manager has been started.
        this.securityChain = new SecurityChain(api.getDefinition(), policyManager, ExecutionPhase.REQUEST);

        long endTime = System.currentTimeMillis(); // Get the end Time
        log.debug("API reactor started in {} ms", (endTime - startTime));

        this.lifecycleState = Lifecycle.State.STARTED;
    }

    @Override
    protected void doStop() throws Exception {
        log.debug("API reactor is now stopping, closing context for {} ...", this);

        this.lifecycleState = Lifecycle.State.STOPPING;

        try {
            asyncEntrypointResolver.preStop();
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

        asyncEntrypointResolver.stop();
        defaultInvoker.stop();
        policyManager.stop();
        resourceLifecycleManager.stop();

        lifecycleState = Lifecycle.State.STOPPED;

        log.debug("API reactor is now stopped: {}", this);
    }

    @Override
    public String toString() {
        return "AsyncApiReactor API id[" + api.getId() + "] name[" + api.getName() + "] version[" + api.getApiVersion() + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsyncApiReactor that = (AsyncApiReactor) o;
        return api.equals(that.api);
    }

    @Override
    public int hashCode() {
        return Objects.hash(api);
    }
}
