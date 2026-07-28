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
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.core.condition.CompositeConditionFilter;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.v4.flow.AbstractBestMatchFlowSelector;
import io.gravitee.gateway.reactive.v4.flow.BestMatchFlowResolver;
import io.gravitee.gateway.reactive.v4.flow.FlowResolver;

/**
 * Factory allowing to create a {@link FlowResolver} to be used to resolve flows to execute at api plan level, api level or platform level.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("common-java:DuplicatedBlocks") // Needed for v4 definition. Will replace the other one at the end.
public class FlowResolverFactory {

    private final ConditionFilter<BaseExecutionContext, Flow> flowFilter;
    private final ConditionFilter<BaseExecutionContext, Flow> bestMatchFlowFilter;
    private final AbstractBestMatchFlowSelector<Flow> bestMatchFlowSelector;

    public FlowResolverFactory(
        final ConditionFilter<BaseExecutionContext, Flow> apiFlowFilter,
        final AbstractBestMatchFlowSelector<Flow> bestMatchFlowSelector
    ) {
        this.flowFilter = apiFlowFilter;
        this.bestMatchFlowFilter = apiFlowFilter;
        this.bestMatchFlowSelector = bestMatchFlowSelector;
    }

    public FlowResolverFactory(
        final ConditionFilter<BaseExecutionContext, Flow> flowFilter,
        final ConditionFilter<BaseExecutionContext, Flow> conditionFilter,
        final AbstractBestMatchFlowSelector<Flow> bestMatchFlowSelector
    ) {
        this.flowFilter = flowFilter;
        this.bestMatchFlowFilter = new CompositeConditionFilter<>(flowFilter, conditionFilter);
        this.bestMatchFlowSelector = bestMatchFlowSelector;
    }

    public FlowResolver<? super HttpExecutionContext> forApi(Api api) {
        ApiFlowResolver apiFlowResolver = new ApiFlowResolver(api.getDefinition(), flowFilter(api.getDefinition().getFlowExecution()));
        if (isBestMatchFlowMode(api.getDefinition().getFlowExecution())) {
            return new BestMatchFlowResolver(apiFlowResolver, bestMatchFlowSelector);
        }
        return apiFlowResolver;
    }

    public FlowResolver<? super HttpExecutionContext> forApiPlan(Api api) {
        ApiPlanFlowResolver apiPlanFlowResolver = new ApiPlanFlowResolver(
            api.getDefinition(),
            flowFilter(api.getDefinition().getFlowExecution())
        );
        if (isBestMatchFlowMode(api.getDefinition().getFlowExecution())) {
            return new BestMatchFlowResolver(apiPlanFlowResolver, bestMatchFlowSelector);
        }
        return apiPlanFlowResolver;
    }

    public FlowResolver<? super HttpExecutionContext> forApiProductPlan(
        Api api,
        String environmentId,
        ApiProductRegistry apiProductRegistry
    ) {
        ApiProductPlanFlowResolver apiProductPlanFlowResolver = new ApiProductPlanFlowResolver(
            api.getDefinition(),
            environmentId,
            apiProductRegistry,
            flowFilter(api.getDefinition().getFlowExecution())
        );
        if (isBestMatchFlowMode(api.getDefinition().getFlowExecution())) {
            return new BestMatchFlowResolver(apiProductPlanFlowResolver, bestMatchFlowSelector);
        }
        return apiProductPlanFlowResolver;
    }

    private ConditionFilter<BaseExecutionContext, Flow> flowFilter(final FlowExecution flowExecution) {
        return isBestMatchFlowMode(flowExecution) ? bestMatchFlowFilter : flowFilter;
    }

    private static boolean isBestMatchFlowMode(final FlowExecution flowExecution) {
        return flowExecution != null && flowExecution.getMode() == FlowMode.BEST_MATCH;
    }
}
