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
package io.gravitee.gateway.jupiter.handlers.api;

import static io.gravitee.gateway.jupiter.api.ExecutionPhase.REQUEST;
import static io.gravitee.gateway.jupiter.api.ExecutionPhase.RESPONSE;
import static io.reactivex.Completable.defer;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecycleManager;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.hook.Hook;
import io.gravitee.gateway.jupiter.api.hook.InvokerHook;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.core.context.MutableRequestExecutionContext;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionHelper;
import io.gravitee.gateway.jupiter.core.hook.HookHelper;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.core.tracing.TracingHook;
import io.gravitee.gateway.jupiter.handlers.api.adapter.invoker.InvokerAdapter;
import io.gravitee.gateway.jupiter.handlers.api.flow.FlowChain;
import io.gravitee.gateway.jupiter.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.jupiter.handlers.api.security.SecurityChain;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.Entrypoint;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.reporter.api.http.Metrics;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SyncApiReactor extends AbstractLifecycleComponent<ReactorHandler> implements ApiReactor, ReactorHandler {

    private static final Logger log = LoggerFactory.getLogger(SyncApiReactor.class);
    protected static final String ATTR_INVOKER_SKIP = "invoker.skip";
    private final Api api;
    private final ComponentProvider componentProvider;
    private final List<TemplateVariableProvider> templateVariableProviders;
    private final Invoker defaultInvoker;
    private final ResourceLifecycleManager resourceLifecycleManager;
    private final PolicyManager policyManager;
    private final GroupLifecycleManager groupLifecycleManager;
    private final List<Hook> processorChainHooks;
    private final List<InvokerHook> invokerHooks;
    private final FlowChain platformFlowChain;
    private final FlowChain apiPlanFlowChain;
    private final FlowChain apiFlowChain;
    private final ProcessorChain apiPreProcessorChain;
    private final ProcessorChain apiPostProcessorChain;
    private final ProcessorChain apiErrorProcessorChain;
    private SecurityChain securityChain;

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
        final Configuration configuration
    ) {
        this.api = api;
        this.componentProvider = componentProvider;
        this.templateVariableProviders = templateVariableProviders;
        this.defaultInvoker = defaultInvoker;
        this.resourceLifecycleManager = resourceLifecycleManager;
        this.policyManager = policyManager;
        this.groupLifecycleManager = groupLifecycleManager;

        this.apiPreProcessorChain = apiProcessorChainFactory.preProcessorChain(api);
        this.apiPostProcessorChain = apiProcessorChainFactory.postProcessorChain(api);
        this.apiErrorProcessorChain = apiProcessorChainFactory.errorProcessorChain(api);

        this.platformFlowChain = flowChainFactory.createPlatformFlow(api);
        this.apiPlanFlowChain = flowChainFactory.createPlanFlow(api);
        this.apiFlowChain = flowChainFactory.createApiFlow(api);

        boolean tracing = configuration.getProperty("services.tracing.enabled", Boolean.class, false);

        processorChainHooks = new ArrayList<>();
        invokerHooks = new ArrayList<>();
        if (tracing) {
            processorChainHooks.add(new TracingHook("processor-chain"));
            invokerHooks.add(new TracingHook("invoker"));
        }
    }

    @Override
    public ExecutionMode executionMode() {
        return ExecutionMode.JUPITER;
    }

    @Override
    public Completable handle(final MutableRequestExecutionContext ctx) {
        ctx.componentProvider(componentProvider).templateVariableProviders(templateVariableProviders);

        // Prepare attributes and metrics before handling the request.
        prepareContextAttributes(ctx);
        prepareMetrics(ctx);

        return handleRequest(ctx);
    }

    private void prepareContextAttributes(RequestExecutionContext ctx) {
        ctx.setAttribute(ExecutionContext.ATTR_CONTEXT_PATH, ctx.request().contextPath());
        ctx.setAttribute(ExecutionContext.ATTR_API, api.getId());
        ctx.setAttribute(ExecutionContext.ATTR_API_DEPLOYED_AT, api.getDeployedAt().getTime());
        ctx.setAttribute(ExecutionContext.ATTR_INVOKER, defaultInvoker);
        ctx.setAttribute(ExecutionContext.ATTR_ORGANIZATION, api.getOrganizationId());
        ctx.setAttribute(ExecutionContext.ATTR_ENVIRONMENT, api.getEnvironmentId());
    }

    private void prepareMetrics(RequestExecutionContext ctx) {
        final Request request = ctx.request();
        final Metrics metrics = request.metrics();

        metrics.setApi(api.getId());
        metrics.setPath(request.pathInfo());
    }

    private Completable handleRequest(final RequestExecutionContext ctx) {
        // Execute platform flow chain.
        return platformFlowChain
            .execute(ctx, REQUEST)
            // Execute security chain.
            .andThen(securityChain.execute(ctx))
            // Execute pre api processor chain
            .andThen(executeProcessorsChain(ctx, apiPreProcessorChain, REQUEST))
            .andThen(executeFlowChain(ctx, apiPlanFlowChain, REQUEST))
            .andThen(executeFlowChain(ctx, apiFlowChain, REQUEST))
            // All request flows have been executed. Invokes backend.
            .andThen(invokeBackend(ctx))
            // Execute response flows (api plan, api, platform).
            .andThen(executeFlowChain(ctx, apiPlanFlowChain, RESPONSE))
            .andThen(executeFlowChain(ctx, apiFlowChain, RESPONSE))
            // Execute post api processor chain
            .andThen(executeProcessorsChain(ctx, apiPostProcessorChain, RESPONSE))
            .onErrorResumeNext(error -> processThrowable(ctx, error))
            // Platform post flows must always be executed
            .andThen(executeFlowChain(ctx, platformFlowChain, RESPONSE))
            // Catch all possible unexpected errors.
            .onErrorResumeNext(t -> handleError(ctx, t))
            // Finally, end the response.
            .andThen(endResponse(ctx))
            .doOnComplete(() -> log.debug("Response has ended"));
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
        final RequestExecutionContext ctx,
        final ProcessorChain processorChain,
        final ExecutionPhase phase
    ) {
        return defer(() -> HookHelper.hook(processorChain.execute(ctx, phase), processorChain.getId(), processorChainHooks, ctx, phase));
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
    private Completable executeFlowChain(final RequestExecutionContext ctx, final FlowChain flowChain, final ExecutionPhase phase) {
        return defer(() -> flowChain.execute(ctx, phase));
    }

    /**
     * Invokes the backend if the context is not interrupted and if there is nothing in the context that indicates to skip the invoker.
     *
     * @param ctx the current execution context.
     *
     * @return a {@link Completable} that will complete once the invoker has been invoked or that completes immediately if execution isn't required.
     */
    private Completable invokeBackend(final RequestExecutionContext ctx) {
        return defer(
                () -> {
                    if (!Objects.equals(false, ctx.<Boolean>getAttribute(ATTR_INVOKER_SKIP))) {
                        Invoker invoker = getInvoker(ctx);

                        if (invoker != null) {
                            return HookHelper.hook(invoker.invoke(ctx), invoker.getId(), invokerHooks, ctx, null);
                        }
                    }
                    return Completable.complete();
                }
            )
            .doOnSubscribe(disposable -> ctx.request().metrics().setApiResponseTimeMs(System.currentTimeMillis()))
            .doOnTerminate(
                () ->
                    ctx
                        .request()
                        .metrics()
                        .setApiResponseTimeMs(System.currentTimeMillis() - ctx.request().metrics().getApiResponseTimeMs())
            );
    }

    /**
     * Get the invoker currently referenced in the execution context.
     * If the invoker has been overridden by a v3 policy, the {@link io.gravitee.gateway.api.Invoker} will be adapted to match with the expected jupiter {@link Invoker} type.
     *
     * @param ctx the current context where the invoker is referenced.
     * @return the current invoker in the expected type.
     */
    private Invoker getInvoker(RequestExecutionContext ctx) {
        final Object invoker = ctx.getAttribute(ExecutionContext.ATTR_INVOKER);

        if (invoker == null) {
            return null;
        }

        if (!(invoker instanceof InvokerAdapter)) {
            return new InvokerAdapter((io.gravitee.gateway.api.Invoker) invoker);
        }

        return (Invoker) invoker;
    }

    private Completable endResponse(RequestExecutionContext ctx) {
        return ctx.response().end();
    }

    /**
     * Process the given throwable by checking the type of interruption and execute the right processor chain accordingly
     *
     * @param ctx the current context
     * @param throwable the source error
     * @return a {@link Completable} that will complete once processor chain has been fully executed or source error rethrown
     */
    private Completable processThrowable(final RequestExecutionContext ctx, final Throwable throwable) {
        if (InterruptionHelper.isInterruption(throwable)) {
            // In case of any interruption without failure, execute api post processor chain and resume the execution
            return executeProcessorsChain(ctx, apiPostProcessorChain, RESPONSE);
        } else if (InterruptionHelper.isInterruptionWithFailure(throwable)) {
            // In case of any interruption with execution failure, execute api error processor chain and resume the execution
            return executeProcessorsChain(ctx, apiErrorProcessorChain, RESPONSE);
        } else {
            // In case of any error exception, rethrow original exception
            return Completable.error(throwable);
        }
    }

    private Completable handleError(final RequestExecutionContext ctx, final Throwable throwable) {
        return Completable.fromRunnable(
            () -> {
                log.error("Unexpected error while handling request", throwable);
                if (ctx.request().metrics().getApiResponseTimeMs() > Integer.MAX_VALUE) {
                    ctx
                        .request()
                        .metrics()
                        .setApiResponseTimeMs(System.currentTimeMillis() - ctx.request().metrics().getApiResponseTimeMs());
                }

                ctx.response().status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                ctx.response().reason(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
            }
        );
    }

    @Override
    public Reactable reactable() {
        return api;
    }

    @Override
    protected void doStart() throws Exception {
        log.debug("API handler is now starting, preparing API context...");
        long startTime = System.currentTimeMillis(); // Get the start Time

        // Start resources before
        resourceLifecycleManager.start();
        policyManager.start();
        groupLifecycleManager.start();

        dumpVirtualHosts();

        long endTime = System.currentTimeMillis(); // Get the end Time

        // Create securityChain once policy manager has been started.
        this.securityChain = new SecurityChain(api, policyManager);

        log.debug("API reactor started in {} ms", (endTime - startTime));
    }

    @Override
    protected void doStop() throws Exception {
        log.debug("API reactor is now stopping, closing context for {} ...", this);

        policyManager.stop();
        resourceLifecycleManager.stop();
        groupLifecycleManager.stop();

        log.debug("API reactor is now stopped: {}", this);
    }

    @Override
    public String toString() {
        return "SyncApiReactor API id[" + api.getId() + "] name[" + api.getName() + "] version[" + api.getVersion() + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncApiReactor that = (SyncApiReactor) o;
        return api.equals(that.api);
    }

    protected void dumpVirtualHosts() {
        List<Entrypoint> entrypoints = api.entrypoints();
        log.debug("{} ready to accept requests on:", this);
        entrypoints.forEach(entrypoint -> log.debug("\t{}", entrypoint));
    }

    @Override
    public void handle(io.gravitee.gateway.api.ExecutionContext ctx, Handler<io.gravitee.gateway.api.ExecutionContext> endHandler) {
        throw new RuntimeException(new IllegalAccessException("Handle method can be called on SyncApiReactor"));
    }
}
