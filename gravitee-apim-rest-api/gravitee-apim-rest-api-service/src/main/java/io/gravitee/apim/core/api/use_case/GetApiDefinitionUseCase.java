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
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
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

        var apiDefinition = switch (api.getApiDefinitionValue()) {
            case NativeApi nativeApi -> enrichApiDefinitionNative(api, nativeApi);
            case io.gravitee.definition.model.v4.Api v4Api -> enrichApiDefinitionV4(api, v4Api);
            case io.gravitee.definition.model.Api v2Api -> enrichApiDefinitionV2(api, v2Api);
            default -> null;
        };

        return new Output(apiDefinition);
    }

    private ApiDefinition enrichApiDefinitionV4(Api api, io.gravitee.definition.model.v4.Api apiDefinitionHttpV4) {
        apiDefinitionHttpV4.setFlows(flowCrudService.getApiV4Flows(api.getId()));
        var plans = planQueryService
            .findAllByReferenceIdAndReferenceType(api.getId(), GenericPlanEntity.ReferenceType.API.name())
            .stream()
            .filter(plan -> plan.getPlanStatus() == PlanStatus.PUBLISHED || plan.getPlanStatus() == PlanStatus.DEPRECATED)
            .map(Plan::getPlanDefinitionHttpV4)
            .peek(planDefinition -> planDefinition.setFlows(flowCrudService.getPlanV4Flows(planDefinition.getId())))
            .toList();
        apiDefinitionHttpV4.setPlans(plans);
        return apiDefinitionHttpV4;
    }

    private ApiDefinition enrichApiDefinitionNative(Api api, NativeApi apiDefinitionNativeV4) {
        apiDefinitionNativeV4.setFlows(flowCrudService.getNativeApiFlows(api.getId()));
        var plans = planQueryService
            .findAllByReferenceIdAndReferenceType(api.getId(), GenericPlanEntity.ReferenceType.API.name())
            .stream()
            .filter(plan -> plan.getPlanStatus() == PlanStatus.PUBLISHED || plan.getPlanStatus() == PlanStatus.DEPRECATED)
            .map(Plan::getPlanDefinitionNativeV4)
            .peek(planDefinition -> planDefinition.setFlows(flowCrudService.getNativePlanFlows(planDefinition.getId())))
            .toList();
        apiDefinitionNativeV4.setPlans(plans);
        return apiDefinitionNativeV4;
    }

    private ApiDefinition enrichApiDefinitionV2(Api api, io.gravitee.definition.model.Api apiDefinition) {
        apiDefinition.setFlows(flowCrudService.getApiV2Flows(api.getId()));

        var plans = planQueryService
            .findAllByReferenceIdAndReferenceType(api.getId(), GenericPlanEntity.ReferenceType.API.name())
            .stream()
            .filter(plan -> plan.getPlanStatus() == PlanStatus.PUBLISHED || plan.getPlanStatus() == PlanStatus.DEPRECATED)
            .map(Plan::getPlanDefinitionV2)
            .peek(planDefinition -> planDefinition.setFlows(flowCrudService.getPlanV2Flows(planDefinition.getId())))
            .toList();
        apiDefinition.setPlans(plans);
        return apiDefinition;
    }
}
