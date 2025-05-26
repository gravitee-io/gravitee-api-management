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
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class CreatePlanUseCase {

    private final CreatePlanDomainService createPlanDomainService;
    private final ApiCrudService apiCrudService;

    public Output execute(Input input) {
        var api = apiCrudService.get(input.apiId());
        if (EnumSet.of(DefinitionVersion.FEDERATED, DefinitionVersion.FEDERATED_AGENT).contains(api.getDefinitionVersion())) {
            throw new PlanInvalidException("Can't manually create Federated Plan");
        }

        var plan = input.toPlan.apply(api);

        if (isMtls(api, plan) && api.getApiDefinitionHttpV4() != null && api.getApiDefinitionHttpV4().isTcpProxy()) {
            throw new PlanInvalidException("Cannot create mTLS plan for TCP API");
        }

        plan.setEnvironmentId(api.getEnvironmentId());
        plan.setApiId(input.apiId);
        plan.setType(Plan.PlanType.API);
        plan.setPlanStatus(PlanStatus.STAGING);
        if (plan.getPlanMode() == null) {
            plan.setPlanMode(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD);
        }

        var flows = input.flowProvider.apply(api);

        PlanWithFlows createdPlan = createPlanDomainService.create(plan, flows, api, input.auditInfo);

        return new Output(createdPlan.getId(), createdPlan);
    }

    private static boolean isMtls(Api api, Plan plan) {
        return (
            api.getDefinitionVersion() == DefinitionVersion.V4 &&
            !isNull(plan.getPlanSecurity()) &&
            plan.getPlanSecurity().getType().equalsIgnoreCase("mtls")
        );
    }

    public record Input(
        String apiId,
        Function<Api, Plan> toPlan,
        Function<Api, List<? extends AbstractFlow>> flowProvider,
        AuditInfo auditInfo
    ) {}

    public record Output(String id, PlanWithFlows plan) {}
}
