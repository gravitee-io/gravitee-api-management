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
package io.gravitee.gateway.jupiter.handlers.api.v4.flow.resolver;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.gateway.jupiter.core.condition.ConditionFilter;
import io.gravitee.gateway.jupiter.handlers.api.v4.Api;
import io.gravitee.gateway.jupiter.v4.flow.BestMatchFlowResolver;
import io.gravitee.gateway.jupiter.v4.flow.FlowResolver;

/**
 * Factory allowing to create a {@link FlowResolver} to be used to resolve flows to execute at api plan level, api level or platform level.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("common-java:DuplicatedBlocks") // Needed for v4 definition. Will replace the other one at the end.
public class FlowResolverFactory {

    private final ConditionFilter<Flow> syncApiFlowFilter;
    private final ConditionFilter<Flow> asyncApiFlowFilter;

    public FlowResolverFactory(final ConditionFilter<Flow> syncApiFilter, final ConditionFilter<Flow> asyncApiFilter) {
        this.syncApiFlowFilter = syncApiFilter;
        this.asyncApiFlowFilter = asyncApiFilter;
    }

    public FlowResolver forApi(Api api) {
        ApiFlowResolver apiFlowResolver = new ApiFlowResolver(api.getDefinition(), getConditionFilter(api));
        if (isBestMatchFlowMode(api.getDefinition().getFlowExecution())) {
            return new BestMatchFlowResolver(apiFlowResolver);
        }
        return apiFlowResolver;
    }

    public FlowResolver forApiPlan(Api api) {
        ApiPlanFlowResolver apiPlanFlowResolver = new ApiPlanFlowResolver(api.getDefinition(), getConditionFilter(api));
        if (isBestMatchFlowMode(api.getDefinition().getFlowExecution())) {
            return new BestMatchFlowResolver(apiPlanFlowResolver);
        }
        return apiPlanFlowResolver;
    }

    private ConditionFilter<Flow> getConditionFilter(final Api api) {
        if (ApiType.SYNC == api.getDefinition().getType()) {
            return syncApiFlowFilter;
        } else if (ApiType.ASYNC == api.getDefinition().getType()) {
            return asyncApiFlowFilter;
        }
        throw new IllegalArgumentException("Api type unsupported");
    }

    private static boolean isBestMatchFlowMode(final FlowExecution flowExecution) {
        return flowExecution != null && flowExecution.getMode() == FlowMode.BEST_MATCH;
    }
}
