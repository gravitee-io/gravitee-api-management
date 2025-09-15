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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.definition.model.ApiDefinition;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GetApiDefinitionUseCase {

    private final ApiCrudService apiCrudService;
    private final FlowCrudService flowCrudService;
    private final PlanQueryService planQueryService;

    public record Input(String apiId) {}

    public record Output(ApiDefinition apiDefinition) {}

    public Output execute(Input input) {
        Api api = apiCrudService.get(input.apiId);

        var apiDefinition = switch (api.getDefinitionVersion()) {
            case V1, V2 -> enrichApiDefinitionV2(api);
            case V4 -> api.getType() == ApiType.NATIVE ? enrichApiDefinitionNative(api) : enrichApiDefinitionV4(api);
            default -> null;
        };

        return new Output(apiDefinition);
    }

    private ApiDefinition enrichApiDefinitionV4(Api api) {
        var apiDefinitionHttpV4 = api.getApiDefinitionHttpV4();
        apiDefinitionHttpV4.setFlows(flowCrudService.getApiV4Flows(api.getId()));
        var plans = planQueryService
            .findAllByApiId(api.getId())
            .stream()
            .filter(plan -> plan.getPlanStatus() == PlanStatus.PUBLISHED || plan.getPlanStatus() == PlanStatus.DEPRECATED)
            .map(Plan::getPlanDefinitionHttpV4)
            .peek(planDefinition -> planDefinition.setFlows(flowCrudService.getPlanV4Flows(planDefinition.getId())))
            .toList();
        apiDefinitionHttpV4.setPlans(plans);
        return apiDefinitionHttpV4;
    }

    private ApiDefinition enrichApiDefinitionNative(Api api) {
        var apiDefinitionNativeV4 = api.getApiDefinitionNativeV4();
        apiDefinitionNativeV4.setFlows(flowCrudService.getNativeApiFlows(api.getId()));
        var plans = planQueryService
            .findAllByApiId(api.getId())
            .stream()
            .filter(plan -> plan.getPlanStatus() == PlanStatus.PUBLISHED || plan.getPlanStatus() == PlanStatus.DEPRECATED)
            .map(Plan::getPlanDefinitionNativeV4)
            .peek(planDefinition -> planDefinition.setFlows(flowCrudService.getNativePlanFlows(planDefinition.getId())))
            .toList();
        apiDefinitionNativeV4.setPlans(plans);
        return apiDefinitionNativeV4;
    }

    private ApiDefinition enrichApiDefinitionV2(Api api) {
        var apiDefinition = api.getApiDefinition();
        apiDefinition.setFlows(flowCrudService.getApiV2Flows(api.getId()));

        var plans = planQueryService
            .findAllByApiId(api.getId())
            .stream()
            .filter(plan -> plan.getPlanStatus() == PlanStatus.PUBLISHED || plan.getPlanStatus() == PlanStatus.DEPRECATED)
            .map(Plan::getPlanDefinitionV2)
            .peek(planDefinition -> planDefinition.setFlows(flowCrudService.getPlanV2Flows(planDefinition.getId())))
            .toList();
        apiDefinition.setPlans(plans);
        return apiDefinition;
    }
}
