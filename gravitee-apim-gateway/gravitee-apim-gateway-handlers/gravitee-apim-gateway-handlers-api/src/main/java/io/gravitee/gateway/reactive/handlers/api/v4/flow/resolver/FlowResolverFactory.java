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
package io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.CompositeConditionFilter;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.v4.flow.AbstractBestMatchFlowSelector;
import io.gravitee.gateway.reactive.v4.flow.BestMatchFlowResolver;
import io.gravitee.gateway.reactive.v4.flow.FlowResolver;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Factory allowing to create a {@link FlowResolver} to be used to resolve flows to execute at api plan level, api level or platform level.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("common-java:DuplicatedBlocks") // Needed for v4 definition. Will replace the other one at the end.
public class FlowResolverFactory {

    /**
     * Filter to evaluate before running a flow when the resolver already evaluated everything it had to.
     */
    public static final ConditionFilter<BaseExecutionContext, Flow> NO_DEFERRED_CONDITION = (ctx, flow) -> Maybe.just(flow);

    private final ConditionFilter<BaseExecutionContext, Flow> apiFlowFilter;
    private final ConditionFilter<BaseExecutionContext, Flow> deferrableConditionFilter;
    private final ConditionFilter<BaseExecutionContext, Flow> bestMatchApiFlowFilter;
    private final AbstractBestMatchFlowSelector<Flow> bestMatchFlowSelector;

    /**
     * Builds a factory resolving the flows with a single filter, conditions included: nothing is left to be
     * evaluated later on, {@link #deferredConditionFilter(Api)} returns a no-op filter.
     */
    public FlowResolverFactory(
        final ConditionFilter<BaseExecutionContext, Flow> apiFlowFilter,
        final AbstractBestMatchFlowSelector<Flow> bestMatchFlowSelector
    ) {
        this.apiFlowFilter = apiFlowFilter;
        this.deferrableConditionFilter = null;
        this.bestMatchApiFlowFilter = apiFlowFilter;
        this.bestMatchFlowSelector = bestMatchFlowSelector;
    }

    /**
     * Builds a factory keeping the flow condition apart from the selection filters.
     *
     * @param apiFlowFilter the filter applied while resolving the flows, in every mode.
     * @param conditionFilter the flow condition filter. It is applied here in BEST_MATCH mode only, where the
     * condition takes part in the selection of the single flow to execute. In the other modes it is left to the
     * caller through {@link #deferredConditionFilter(Api)}, to be evaluated right before a flow runs so that it
     * observes what the previous flows did.
     */
    public FlowResolverFactory(
        final ConditionFilter<BaseExecutionContext, Flow> apiFlowFilter,
        final ConditionFilter<BaseExecutionContext, Flow> conditionFilter,
        final AbstractBestMatchFlowSelector<Flow> bestMatchFlowSelector
    ) {
        this.apiFlowFilter = apiFlowFilter;
        this.deferrableConditionFilter = conditionFilter;
        this.bestMatchApiFlowFilter = new CompositeConditionFilter<>(apiFlowFilter, conditionFilter);
        this.bestMatchFlowSelector = bestMatchFlowSelector;
    }

    /**
     * Returns the filter that still has to be evaluated before a flow is executed, which is what the resolvers
     * built by this factory did not evaluate themselves. This factory is the only one to know it, so that a
     * resolver evaluating conditions upfront can never see them evaluated a second time.
     */
    public ConditionFilter<BaseExecutionContext, Flow> deferredConditionFilter(final Api api) {
        if (deferrableConditionFilter == null || isBestMatchFlowMode(api.getDefinition().getFlowExecution())) {
            return NO_DEFERRED_CONDITION;
        }
        return deferrableConditionFilter;
    }

    public FlowResolver<? extends BaseExecutionContext> forApi(Api api) {
        final FlowExecution flowExecution = api.getDefinition().getFlowExecution();
        ApiFlowResolver apiFlowResolver = new ApiFlowResolver(api.getDefinition(), flowFilter(flowExecution));
        if (isBestMatchFlowMode(flowExecution)) {
            return new BestMatchFlowResolver(apiFlowResolver, bestMatchFlowSelector);
        }
        return apiFlowResolver;
    }

    public FlowResolver forApiPlan(Api api) {
        final FlowExecution flowExecution = api.getDefinition().getFlowExecution();
        ApiPlanFlowResolver apiPlanFlowResolver = new ApiPlanFlowResolver(api.getDefinition(), flowFilter(flowExecution));
        if (isBestMatchFlowMode(flowExecution)) {
            return new BestMatchFlowResolver(apiPlanFlowResolver, bestMatchFlowSelector);
        }
        return apiPlanFlowResolver;
    }

    public FlowResolver forApiProductPlan(Api api, String environmentId, ApiProductRegistry apiProductRegistry) {
        final FlowExecution flowExecution = api.getDefinition().getFlowExecution();
        ApiProductPlanFlowResolver apiProductPlanFlowResolver = new ApiProductPlanFlowResolver(
            api.getDefinition(),
            environmentId,
            apiProductRegistry,
            flowFilter(flowExecution)
        );
        if (isBestMatchFlowMode(flowExecution)) {
            return new BestMatchFlowResolver(apiProductPlanFlowResolver, bestMatchFlowSelector);
        }
        return apiProductPlanFlowResolver;
    }

    private ConditionFilter<BaseExecutionContext, Flow> flowFilter(final FlowExecution flowExecution) {
        return isBestMatchFlowMode(flowExecution) ? bestMatchApiFlowFilter : apiFlowFilter;
    }

    private static boolean isBestMatchFlowMode(final FlowExecution flowExecution) {
        return flowExecution != null && flowExecution.getMode() == FlowMode.BEST_MATCH;
    }
}
