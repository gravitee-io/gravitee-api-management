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
package io.gravitee.gateway.jupiter.handlers.api.flow.resolver;

import io.gravitee.definition.model.FlowMode;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.jupiter.flow.BestMatchFlowResolver;
import io.gravitee.gateway.jupiter.flow.FlowResolver;
import io.gravitee.gateway.jupiter.handlers.api.adapter.condition.ConditionEvaluatorAdapter;
import io.gravitee.gateway.platform.Organization;
import io.gravitee.gateway.platform.manager.OrganizationManager;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowResolverFactory {

    public static FlowResolver forApi(Api api) {
        ApiFlowResolver flowProvider = new ApiFlowResolver(api, ConditionEvaluatorAdapter.DEFAULT_FLOW_EVALUATOR);

        if (isBestMatchFlowMode(api.getFlowMode())) {
            return new BestMatchFlowResolver(flowProvider);
        }

        return flowProvider;
    }

    public static FlowResolver forApiPlan(Api api) {
        ApiPlanFlowResolver flowProvider = new ApiPlanFlowResolver(api, ConditionEvaluatorAdapter.DEFAULT_FLOW_EVALUATOR);

        if (isBestMatchFlowMode(api.getFlowMode())) {
            return new BestMatchFlowResolver(flowProvider);
        }

        return flowProvider;
    }

    public static FlowResolver forPlatform(Api api, OrganizationManager organizationManager) {
        PlatformFlowResolver flowProvider = new PlatformFlowResolver(
            api,
            organizationManager,
            ConditionEvaluatorAdapter.DEFAULT_FLOW_EVALUATOR
        );

        final Organization organization = organizationManager.getCurrentOrganization();
        if (organization != null) {
            if (isBestMatchFlowMode(organization.getFlowMode())) {
                return new BestMatchFlowResolver(flowProvider);
            }
        }

        return flowProvider;
    }

    private static boolean isBestMatchFlowMode(FlowMode flowMode) {
        return flowMode == FlowMode.BEST_MATCH;
    }
}
