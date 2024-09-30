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

import static java.util.Objects.isNull;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.exception.PlanInvalidException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class CreatePlanUseCase {

    private final CreatePlanDomainService createPlanDomainService;
    private final ApiCrudService apiCrudService;

    public Output execute(Input input) {
        var api = apiCrudService.get(input.apiId());
        if (api.getDefinitionVersion() == DefinitionVersion.FEDERATED) {
            throw new PlanInvalidException("Can't manually create Federated Plan");
        }

        if (isMtls(api, input) && api.getApiDefinitionV4().isTcpProxy()) {
            throw new PlanInvalidException("Cannot create mTLS plan for TCP API");
        }

        var plan = input.plan;
        plan.setEnvironmentId(api.getEnvironmentId());
        plan.setApiId(input.apiId);
        plan.setType(Plan.PlanType.API);
        plan.setPlanStatus(PlanStatus.STAGING);
        if (plan.getPlanMode() == null) {
            plan.setPlanMode(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD);
        }

        PlanWithFlows createdPlan = createPlanDomainService.create(
            plan,
            input.flows == null ? List.of() : input.flows,
            api,
            input.auditInfo
        );

        return new Output(createdPlan.getId(), createdPlan);
    }

    private static boolean isMtls(Api api, Input input) {
        return (
            api.getDefinitionVersion() == DefinitionVersion.V4 &&
            !isNull(input.plan().getPlanSecurity()) &&
            input.plan().getPlanSecurity().getType().equalsIgnoreCase("mtls")
        );
    }

    public record Input(String apiId, Plan plan, List<Flow> flows, AuditInfo auditInfo) {}

    public record Output(String id, PlanWithFlows plan) {}
}
