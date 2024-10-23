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
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GetApiDefinitionUseCase {

    private final ApiCrudService apiCrudService;
    private final FlowCrudService flowCrudService;
    private final PlanQueryService planQueryService;

    public record Input(String apiId) {}

    public record Output(
        DefinitionVersion definitionVersion,
        io.gravitee.definition.model.v4.Api apiDefinitionV4,
        io.gravitee.definition.model.Api apiDefinition
    ) {}

    public Output execute(Input input) {
        Api api = apiCrudService.get(input.apiId);

        switch (api.getDefinitionVersion()) {
            case V1, V2 -> api.getApiDefinition().setFlows(flowCrudService.getApiV2Flows(api.getId()));
            case V4 -> api.getApiDefinitionHttpV4().setFlows(flowCrudService.getApiV4Flows(api.getId()));
        }

        List<Plan> plans = planQueryService
            .findAllByApiId(api.getId())
            .stream()
            .filter(plan -> plan.getPlanStatus() == PlanStatus.PUBLISHED || plan.getPlanStatus() == PlanStatus.DEPRECATED)
            .peek(plan -> {
                switch (plan.getDefinitionVersion()) {
                    case V1, V2 -> plan.getPlanDefinitionV2().setFlows(flowCrudService.getPlanV2Flows(plan.getId()));
                    case V4 -> plan.getPlanDefinitionHttpV4().setFlows(flowCrudService.getPlanV4Flows(plan.getId()));
                }
            })
            .toList();
        api.setPlans(plans);

        return new Output(api.getDefinitionVersion(), api.getApiDefinitionHttpV4(), api.getApiDefinition());
    }
}
