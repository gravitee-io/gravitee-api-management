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
package io.gravitee.gateway.reactive.handlers.api.flow.resolver;

import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.flow.FlowEntity;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.platform.Organization;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.flow.BestMatchFlowResolver;
import io.gravitee.gateway.reactive.flow.FlowResolver;
import io.gravitee.gateway.reactor.ReactableApi;

/**
 * Factory allowing to create a {@link FlowResolver} to be used to resolve flows to execute at api plan level, api level or platform level.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowResolverFactory {

    private final ConditionFilter<FlowEntity> flowFilter;

    public FlowResolverFactory(ConditionFilter<FlowEntity> flowFilter) {
        this.flowFilter = flowFilter;
    }

    public FlowResolver forApi(Api api) {
        ApiFlowResolver flowResolver = new ApiFlowResolver(api.getDefinition(), flowFilter);

        if (isBestMatchFlowMode(api.getDefinition().getFlowMode())) {
            return new BestMatchFlowResolver(flowResolver);
        }

        return flowResolver;
    }

    public FlowResolver forApiPlan(Api api) {
        ApiPlanFlowResolver flowResolver = new ApiPlanFlowResolver(api.getDefinition(), flowFilter);

        if (isBestMatchFlowMode(api.getDefinition().getFlowMode())) {
            return new BestMatchFlowResolver(flowResolver);
        }

        return flowResolver;
    }

    public FlowResolver forPlatform(ReactableApi<?> api, OrganizationManager organizationManager) {
        final PlatformFlowResolver flowResolver = new PlatformFlowResolver(api, organizationManager, flowFilter);
        final Organization organization = organizationManager.getCurrentOrganization();

        if (organization != null && isBestMatchFlowMode(organization.getFlowMode())) {
            return new BestMatchFlowResolver(flowResolver);
        }

        return flowResolver;
    }

    private static boolean isBestMatchFlowMode(FlowMode flowMode) {
        return flowMode == FlowMode.BEST_MATCH;
    }
}
