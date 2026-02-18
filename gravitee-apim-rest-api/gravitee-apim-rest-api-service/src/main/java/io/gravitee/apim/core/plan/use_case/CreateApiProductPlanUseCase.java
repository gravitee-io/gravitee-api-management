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

import static io.gravitee.definition.model.DefinitionVersion.V4;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.function.Function;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
@CustomLog
public class CreateApiProductPlanUseCase {

    private final CreatePlanDomainService createPlanDomainService;
    private final ApiProductCrudService apiProductCrudService;

    public Output execute(Input input) {
        log.debug("Creating plan for API Product {}", input.apiProductId());
        ApiProduct apiProduct = apiProductCrudService.get(input.apiProductId());
        Plan plan = input.toPlan.apply(apiProduct);

        plan.setEnvironmentId(apiProduct.getEnvironmentId());
        plan.setReferenceType(GenericPlanEntity.ReferenceType.API_PRODUCT);
        plan.setReferenceId(input.apiProductId);
        plan.setDefinitionVersion(V4);
        plan.setPlanStatus(PlanStatus.STAGING);
        if (plan.getPlanMode() == null) {
            plan.setPlanMode(PlanMode.STANDARD);
        }

        Plan created = createPlanDomainService.createApiProductPlan(plan, apiProduct, input.auditInfo);
        log.debug("Plan {} created for API Product {}", created.getId(), input.apiProductId);
        return new Output(created.getId(), created);
    }

    public record Input(String apiProductId, Function<ApiProduct, Plan> toPlan, AuditInfo auditInfo) {}

    public record Output(String id, Plan plan) {}
}
