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
package io.gravitee.gateway.reactive.handlers.api.flow.resolver;

import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.flow.BestMatchFlowResolver;
import io.gravitee.gateway.reactive.flow.FlowResolver;
import io.gravitee.gateway.reactive.platform.organization.flow.OrganizationFlowResolver;
import io.gravitee.gateway.reactive.v4.flow.AbstractBestMatchFlowSelector;

/**
 * Factory allowing to create a {@link FlowResolver} to be used to resolve flows to execute at api plan level, api level or platform level.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowResolverFactory {

    private final ConditionFilter<HttpBaseExecutionContext, Flow> flowFilter;
    private final AbstractBestMatchFlowSelector<Flow> bestMatchFlowSelector;

    public FlowResolverFactory(
        ConditionFilter<HttpBaseExecutionContext, Flow> flowFilter,
        AbstractBestMatchFlowSelector<Flow> bestMatchFlowSelector
    ) {
        this.flowFilter = flowFilter;
        this.bestMatchFlowSelector = bestMatchFlowSelector;
    }

    public FlowResolver forApi(Api api) {
        ApiFlowResolver flowResolver = new ApiFlowResolver(api.getDefinition(), flowFilter);

        if (isBestMatchFlowMode(api.getDefinition().getFlowMode())) {
            return new BestMatchFlowResolver(flowResolver, bestMatchFlowSelector);
        }

        return flowResolver;
    }

    public FlowResolver forApiPlan(Api api) {
        ApiPlanFlowResolver flowResolver = new ApiPlanFlowResolver(api.getDefinition(), flowFilter);

        if (isBestMatchFlowMode(api.getDefinition().getFlowMode())) {
            return new BestMatchFlowResolver(flowResolver, bestMatchFlowSelector);
        }

        return flowResolver;
    }

    public FlowResolver forOrganization(final String organizationId, OrganizationManager organizationManager) {
        return new OrganizationFlowResolver(organizationId, organizationManager, flowFilter, bestMatchFlowSelector);
    }

    private static boolean isBestMatchFlowMode(FlowMode flowMode) {
        return flowMode == FlowMode.BEST_MATCH;
    }
}
