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
package io.gravitee.apim.core.plan.use_case;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreatePlanUseCase {

    private final CreatePlanDomainService createPlanDomainService;
    private final ApiCrudService apiCrudService;

    public Output execute(Input input) {
        var plan = input.plan;
        plan.setApiId(input.apiId);
        plan.setType(Plan.PlanType.API);
        plan.setStatus(PlanStatus.STAGING);
        if (plan.getMode() == null) {
            plan.setMode(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD);
        }

        PlanWithFlows createdPlan = createPlanDomainService.create(
            input.plan,
            input.flows == null ? List.of() : input.flows,
            apiCrudService.get(input.apiId()),
            input.auditInfo
        );

        return new Output(createdPlan.getId(), createdPlan);
    }

    public record Input(String apiId, Plan plan, List<Flow> flows, AuditInfo auditInfo) {}

    public record Output(String id, PlanWithFlows plan) {}
}
