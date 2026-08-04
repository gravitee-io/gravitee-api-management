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
package io.gravitee.gateway.reactive.handlers.api.v4.flow;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_FLOW_STAGE;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.ChainHook;
import io.gravitee.gateway.reactive.api.hook.Hookable;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.core.hook.HookHelper;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver.FlowResolverFactory;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.v4.flow.FlowResolver;
import io.gravitee.gateway.reactive.v4.policy.PolicyChainFactory;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.ArrayList;
import java.util.List;
import lombok.CustomLog;

/**
 * A flow chain basically allows to execute all the policies configured on a list of flows.
 * Each flow can define policies, either on request or response phase. The purpose of the flow chain is to execute the policies in the right order, while multiple flows can be involved.
 * The list of the flows is resolved dynamically thanks to a given {@link FlowResolver} as the flows need to match the current execution context (match the path, http method, condition, ...).
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("common-java:DuplicatedBlocks") // Needed for v4 definition. Will replace the other one at the end.
@CustomLog
public class FlowChain implements Hookable<ChainHook> {

    protected static final String INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED = "flowExecution.flowsMatched";

    private static final String EXECUTION_FAILURE_KEY_FAILURE = "FLOW_EXECUTION_FLOW_MATCHED_FAILURE";
    private final String id;
    private final FlowResolver flowResolver;
    private final String resolvedFlowAttribute;
    private final PolicyChainFactory<HttpPolicyChain, Flow> policyChainFactory;
    private final ConditionFilter<BaseExecutionContext, Flow> conditionFilter;
    private final boolean validateFlowMatching;
    private final boolean interruptIfNoMatch;
    private List<ChainHook> hooks;

    public FlowChain(final String id, final FlowResolver flowResolver, final PolicyChainFactory<HttpPolicyChain, Flow> policyChainFactory) {
        this(id, flowResolver, policyChainFactory, FlowResolverFactory.NO_DEFERRED_CONDITION, false, false);
    }

    public FlowChain(
        final String id,
        final FlowResolver flowResolver,
        final PolicyChainFactory<HttpPolicyChain, Flow> policyChainFactory,
        final ConditionFilter<BaseExecutionContext, Flow> conditionFilter,
        final boolean validateFlowMatching,
        final boolean interruptIfNoMatch
    ) {
        this.id = id;
        this.flowResolver = flowResolver;
        this.resolvedFlowAttribute = "flow." + id;
        this.policyChainFactory = policyChainFactory;
        this.conditionFilter = conditionFilter;
        this.validateFlowMatching = validateFlowMatching;
        this.interruptIfNoMatch = interruptIfNoMatch;
    }

    @Override
    public void addHooks(final List<ChainHook> hooks) {
        if (this.hooks == null) {
            this.hooks = new ArrayList<>();
        }
        this.hooks.addAll(hooks);
    }

    /**
     * Executes the flow chain for the specified phase.
     * The flows composing the chain are resolved dynamically at the first execution, and the condition of each of
     * them is evaluated right before it is executed, so that it observes what the previous flows did, whether they
     * completed synchronously or not.
     * The flows that have been executed are stored into an internal attribute of the context: subsequent executions
     * related to other phases replay exactly the same flows, without evaluating their condition again.
     *
     * @param ctx the execution context that will be passed to each policy of each resolved flow.
     * @param phase the phase to execute.
     *
     * @return a {@link Completable} that completes when all the policies of the resolved flows have been executed for the specified phase or the chain has been interrupted.
     * The {@link Completable} may complete in error in case of any error occurred during the execution.
     */
    public Completable execute(HttpExecutionContext ctx, ExecutionPhase phase) {
        return Completable.defer(() -> {
            final List<Flow> alreadyExecutedFlows = ctx.getInternalAttribute(resolvedFlowAttribute);

            return alreadyExecutedFlows != null
                ? replayExecutedFlows(ctx, phase, alreadyExecutedFlows)
                : resolveAndExecuteFlows(ctx, phase);
        }).doOnComplete(() -> ctx.removeInternalAttribute(ATTR_INTERNAL_FLOW_STAGE));
    }

    /**
     * Replays, in the same order, the flows a previous phase has executed. Their condition is not evaluated again:
     * it is evaluated once for the whole chain, and the outcome then applies to every phase.
     */
    private Completable replayExecutedFlows(final HttpExecutionContext ctx, final ExecutionPhase phase, final List<Flow> executedFlows) {
        return Flowable.fromIterable(executedFlows).concatMapCompletable(flow -> executeFlow(ctx, flow, phase));
    }

    /**
     * Resolves the flows and executes those whose condition matches, keeping them for the next phases.
     * The list is published into the context before the resolution starts, so that a phase running after an
     * interruption still finds it. It is then filled as the flows run: {@link Flowable#concatMapCompletable} runs
     * them one at a time and serializes the access, hence a plain list.
     */
    private Completable resolveAndExecuteFlows(final HttpExecutionContext ctx, final ExecutionPhase phase) {
        final List<Flow> executedFlows = new ArrayList<>();
        ctx.setInternalAttribute(resolvedFlowAttribute, executedFlows);

        final Flowable<Flow> resolvedFlows = flowResolver.resolve(ctx);

        return resolvedFlows
            .concatMapCompletable(flow -> executeFlowIfConditionMatches(ctx, flow, phase, executedFlows))
            .andThen(Completable.defer(() -> interruptIfNoFlowExecuted(ctx, phase, executedFlows.isEmpty())));
    }

    /**
     * Evaluates the condition of the given flow and, when it matches, executes it and keeps it for the next phases.
     * The condition is evaluated here, and not while the flows are resolved, so that it is only evaluated once the
     * previous flow of the chain has fully completed.
     */
    private Completable executeFlowIfConditionMatches(
        final HttpExecutionContext ctx,
        final Flow flow,
        final ExecutionPhase phase,
        final List<Flow> executedFlows
    ) {
        return conditionFilter
            .filter(ctx, flow)
            .flatMapCompletable(matchedFlow -> {
                executedFlows.add(matchedFlow);
                return executeFlow(ctx, matchedFlow, phase);
            });
    }

    private Completable interruptIfNoFlowExecuted(
        final HttpExecutionContext ctx,
        final ExecutionPhase phase,
        final boolean noFlowExecuted
    ) {
        // Chains sharing the same request report whether any of them matched a flow, so that the last one can
        // interrupt with a 404 when none did. Only the request phase feeds that decision.
        if (!noFlowExecuted || !validateFlowMatching || ExecutionPhase.REQUEST != phase) {
            return Completable.complete();
        }

        boolean flowsMatch = false;
        // Retrieve previous flow chain resolution value
        final Boolean previousChainFlowsMatch = ctx.getInternalAttribute(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED);
        if (previousChainFlowsMatch == null) {
            ctx.setInternalAttribute(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED, false);
        } else {
            flowsMatch = previousChainFlowsMatch;
        }

        if (interruptIfNoMatch && !flowsMatch) {
            ctx.withLogger(log).debug("No flow matched for chain [{}], interrupting with 404", id);
            return ctx.interruptWith(new ExecutionFailure(HttpStatusCode.NOT_FOUND_404).key(EXECUTION_FAILURE_KEY_FAILURE));
        }

        return Completable.complete();
    }

    /**
     * Executes the policies of the given flow for the specified phase.
     * If the phase is {@link ExecutionPhase#RESPONSE}, any action registered during the request phase will be executed before executing the response policies.
     *
     * @param ctx the execution context that will be passed to each policy of each resolved flow.
     * @param flow the flow to execute.
     * @param phase the phase to execute.
     *
     * @return a {@link Completable} that completes when the flow policy chain completes.
     */
    private Completable executeFlow(final HttpExecutionContext ctx, final Flow flow, final ExecutionPhase phase) {
        ctx.withLogger(log).debug("Executing flow {} ({} level, {} phase)", flow.getName(), id, phase.name());
        ctx.putInternalAttribute(ATTR_INTERNAL_FLOW_STAGE, id);

        // Report to the other chains of this request that a flow did match, see interruptIfNoFlowExecuted.
        if (validateFlowMatching && phase == ExecutionPhase.REQUEST) {
            ctx.setInternalAttribute(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED, true);
        }

        if (phase == ExecutionPhase.RESPONSE) {
            // Before executing response phase, execute eventual response actions registered during request phase.
            final HttpPolicyChain policyChain = policyChainFactory.create(id, flow, ExecutionPhase.REQUEST);
            return policyChain.executeActionsOnResponse(ctx).andThen(executeFlow0(ctx, flow, phase));
        }

        return executeFlow0(ctx, flow, phase);
    }

    private @NonNull Completable executeFlow0(HttpExecutionContext ctx, Flow flow, ExecutionPhase phase) {
        final HttpPolicyChain policyChain = policyChainFactory.create(id, flow, phase);

        return HookHelper.hook(() -> policyChain.execute(ctx), policyChain.getId(), hooks, ctx, phase).doOnSubscribe(subscription ->
            ctx.withLogger(log).debug("\t-> Executing flow {} ({} level, {} phase)", flow.getName(), id, phase.name())
        );
    }
}
