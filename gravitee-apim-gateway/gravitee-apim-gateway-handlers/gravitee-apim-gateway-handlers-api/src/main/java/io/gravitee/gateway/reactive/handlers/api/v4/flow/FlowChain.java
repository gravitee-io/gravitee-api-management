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
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.v4.flow.FlowResolver;
import io.gravitee.gateway.reactive.v4.policy.PolicyChainFactory;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
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
    private final FlowResolver<? super HttpExecutionContext> flowResolver;
    private final String resolvedFlowAttribute;
    private final PolicyChainFactory<HttpPolicyChain, Flow> policyChainFactory;
    private final ConditionFilter<BaseExecutionContext, Flow> conditionFilter;
    private final boolean evaluateConditionLazily;
    private final boolean validateFlowMatching;
    private final boolean interruptIfNoMatch;
    private List<ChainHook> hooks;

    public FlowChain(
        final String id,
        final FlowResolver<? super HttpExecutionContext> flowResolver,
        final PolicyChainFactory<HttpPolicyChain, Flow> policyChainFactory
    ) {
        this(id, flowResolver, policyChainFactory, false, false);
    }

    public FlowChain(
        final String id,
        final FlowResolver<? super HttpExecutionContext> flowResolver,
        final PolicyChainFactory<HttpPolicyChain, Flow> policyChainFactory,
        final boolean validateFlowMatching,
        final boolean interruptIfNoMatch
    ) {
        this(id, flowResolver, policyChainFactory, (ctx, flow) -> Maybe.just(flow), false, validateFlowMatching, interruptIfNoMatch);
    }

    public FlowChain(
        final String id,
        final FlowResolver<? super HttpExecutionContext> flowResolver,
        final PolicyChainFactory<HttpPolicyChain, Flow> policyChainFactory,
        final ConditionFilter<BaseExecutionContext, Flow> conditionFilter,
        final boolean evaluateConditionLazily,
        final boolean validateFlowMatching,
        final boolean interruptIfNoMatch
    ) {
        this.id = id;
        this.flowResolver = flowResolver;
        this.resolvedFlowAttribute = "flow." + id;
        this.policyChainFactory = policyChainFactory;
        this.conditionFilter = conditionFilter;
        this.evaluateConditionLazily = evaluateConditionLazily;
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
     * The flows composing the chain are resolved dynamically at the first execution.
     * Subsequent executions related to other phases will reuse the flows resolved during the previous execution to guarantee the same flows can be executed for all the phases.
     *
     * @param ctx the execution context that will be passed to each policy of each resolved flow.
     * @param phase the phase to execute.
     *
     * @return a {@link Completable} that completes when all the policies of the resolved flows have been executed for the specified phase or the chain has been interrupted.
     * The {@link Completable} may complete in error in case of any error occurred during the execution.
     */
    public Completable execute(HttpExecutionContext ctx, ExecutionPhase phase) {
        return Completable.defer(() -> {
            final List<Flow> resolvedFlows = ctx.getInternalAttribute(resolvedFlowAttribute);

            if (resolvedFlows != null) {
                return Flowable.fromIterable(resolvedFlows)
                    .concatMapCompletable(flow -> executeFlow(ctx, flow, phase))
                    .andThen(Completable.defer(() -> handleNoFlowMatched(ctx, phase, resolvedFlows.isEmpty())));
            }

            final List<Flow> matchedFlows = new ArrayList<>();
            ctx.setInternalAttribute(resolvedFlowAttribute, matchedFlows);

            return flowResolver
                .resolve(ctx)
                .concatMapCompletable(flow -> resolveAndExecuteFlow(ctx, flow, phase, matchedFlows))
                .andThen(Completable.defer(() -> handleNoFlowMatched(ctx, phase, matchedFlows.isEmpty())));
        }).doOnComplete(() -> ctx.removeInternalAttribute(ATTR_INTERNAL_FLOW_STAGE));
    }

    private Completable resolveAndExecuteFlow(
        final HttpExecutionContext ctx,
        final Flow flow,
        final ExecutionPhase phase,
        final List<Flow> matchedFlows
    ) {
        final Maybe<Flow> matchingFlow = evaluateConditionLazily ? conditionFilter.filter(ctx, flow) : Maybe.just(flow);

        return matchingFlow.flatMapCompletable(resolvedFlow -> {
            matchedFlows.add(resolvedFlow);
            return executeFlow(ctx, resolvedFlow, phase);
        });
    }

    private Completable handleNoFlowMatched(final HttpExecutionContext ctx, final ExecutionPhase phase, final boolean noFlowMatched) {
        if (!noFlowMatched || !(validateFlowMatching && ExecutionPhase.REQUEST == phase)) {
            return Completable.complete();
        }

        boolean flowsMatch = false;
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
