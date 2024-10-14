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
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.GenericExecutionContext;
import io.gravitee.gateway.reactive.api.hook.ChainHook;
import io.gravitee.gateway.reactive.api.hook.Hookable;
import io.gravitee.gateway.reactive.core.hook.HookHelper;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.v4.flow.FlowResolver;
import io.gravitee.gateway.reactive.v4.policy.PolicyChainFactory;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * A flow chain basically allows to execute all the policies configured on a list of flows.
 * Each flow can define policies, either on request or response phase. The purpose of the flow chain is to execute the policies in the right order, while multiple flows can be involved.
 * The list of the flows is resolved dynamically thanks to a given {@link FlowResolver} as the flows need to match the current execution context (match the path, http method, condition, ...).
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("common-java:DuplicatedBlocks") // Needed for v4 definition. Will replace the other one at the end.
@Slf4j
public class FlowChain implements Hookable<ChainHook> {

    protected static final String INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED = "flowExecution.flowsMatched";
    private static final String EXECUTION_FAILURE_KEY_FAILURE = "FLOW_EXECUTION_FLOW_MATCHED_FAILURE";
    private final String id;
    private final FlowResolver flowResolver;
    private final String resolvedFlowAttribute;
    private final PolicyChainFactory<HttpPolicyChain, Flow> policyChainFactory;
    private final boolean validateFlowMatching;
    private final boolean interruptIfNoMatch;
    private List<ChainHook> hooks;

    public FlowChain(final String id, final FlowResolver flowResolver, final PolicyChainFactory<HttpPolicyChain, Flow> policyChainFactory) {
        this(id, flowResolver, policyChainFactory, false, false);
    }

    public FlowChain(
        final String id,
        final FlowResolver flowResolver,
        final PolicyChainFactory<HttpPolicyChain, Flow> policyChainFactory,
        final boolean validateFlowMatching,
        final boolean interruptIfNoMatch
    ) {
        this.id = id;
        this.flowResolver = flowResolver;
        this.resolvedFlowAttribute = "flow." + id;
        this.policyChainFactory = policyChainFactory;
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
    public Completable execute(ExecutionContext ctx, ExecutionPhase phase) {
        Flowable<Flow> flowable = callResolveFlows(ctx, phase);

        return flowable
            .doOnNext(flow -> {
                log.debug("Executing flow {} ({} level, {} phase)", flow.getName(), id, phase.name());
                ctx.putInternalAttribute(ATTR_INTERNAL_FLOW_STAGE, id);

                // Only deal with flow matching if required
                if (validateFlowMatching && phase == ExecutionPhase.REQUEST) {
                    ctx.setInternalAttribute(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED, true);
                }
            })
            .concatMapCompletable(flow -> executeFlow(ctx, flow, phase))
            .doOnComplete(() -> ctx.removeInternalAttribute(ATTR_INTERNAL_FLOW_STAGE));
    }

    private Flowable<Flow> callResolveFlows(ExecutionContext ctx, ExecutionPhase phase) {
        if (validateFlowMatching && ExecutionPhase.REQUEST == phase) {
            // Only deal with execution flow matching if required
            return resolveFlows(ctx)
                .switchIfEmpty(
                    Flowable.defer(() -> {
                        boolean flowsMatch = false;
                        // Retrieve previous flow chain resolution value
                        Boolean previousChainFlowsMatch = ctx.getInternalAttribute(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED);
                        if (previousChainFlowsMatch == null) {
                            ctx.setInternalAttribute(INTERNAL_CONTEXT_ATTRIBUTES_FLOWS_MATCHED, false);
                        } else {
                            flowsMatch = previousChainFlowsMatch;
                        }
                        if (interruptIfNoMatch && !flowsMatch) {
                            return ctx
                                .interruptWith(new ExecutionFailure(HttpStatusCode.NOT_FOUND_404).key(EXECUTION_FAILURE_KEY_FAILURE))
                                .toFlowable();
                        }
                        return Flowable.empty();
                    })
                );
        } else {
            return resolveFlows(ctx);
        }
    }

    /**
     * Resolves the flows to execute once and stores the resolved flows into an internal attribute of the context for later reuse.
     * This allows to make sure the flow resolved during the execution phase (usually, {@link ExecutionPhase#REQUEST}) will be the same to be executed during the other phases.
     * If flows have already been resolved, they will be returned without triggering a new resolution.
     *
     * @param ctx the context used to temporary store the resolved flows.
     * @return the resolved flows.
     */
    private Flowable<Flow> resolveFlows(GenericExecutionContext ctx) {
        return Flowable.defer(() -> {
            Flowable<Flow> flows = ctx.getInternalAttribute(resolvedFlowAttribute);

            if (flows == null) {
                // Resolves the flows once. Subsequent resolutions will return the same flows.
                flows = flowResolver.resolve(ctx).cache();
                ctx.setInternalAttribute(resolvedFlowAttribute, flows);
            }

            return flows;
        });
    }

    /**
     * Execute the given flow by first checking the current execution context has not been interrupted.
     * If the current execution context is marked as interrupted, the execution will be discarded and the chain will be completed immediately.
     * If the current execution context is not interrupted, a {@link HttpPolicyChain} is created and executed.
     *
     * @param ctx the execution context that will be passed to each policy of each resolved flow.
     * @param flow the flow to execute.
     * @param phase the phase to execute.
     *
     * @return a {@link Completable} that completes when the flow policy chain completes.
     */
    private Completable executeFlow(final ExecutionContext ctx, final Flow flow, final ExecutionPhase phase) {
        HttpPolicyChain policyChain = policyChainFactory.create(id, flow, phase);
        return HookHelper
            .hook(() -> policyChain.execute(ctx), policyChain.getId(), hooks, ctx, phase)
            .doOnSubscribe(subscription -> log.debug("\t-> Executing flow {} ({} level, {} phase)", flow.getName(), id, phase.name()));
    }
}
