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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanUpdates;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.util.Map;
import lombok.Builder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class UpdateApiProductPlanUseCase {

    private final UpdatePlanDomainService updatePlanDomainService;
    private final PlanCrudService planCrudService;
    private final ApiProductCrudService apiProductCrudService;

    public Output execute(Input input) {
        log.debug("Updating API Product plan {} for API Product {}", input.planToUpdate.getId(), input.apiProductId);

        Plan planEntity = planCrudService
            .findByPlanIdAndReferenceIdAndReferenceType(
                input.planToUpdate.getId(),
                input.apiProductId,
                GenericPlanEntity.ReferenceType.API_PRODUCT.name()
            )
            .orElseThrow(() -> new PlanNotFoundException(input.planToUpdate.getId()));

        if (input.planToUpdate.getValidation() == null) {
            input.planToUpdate.setValidation(planEntity.getValidation());
        }

        var updatedEntity = input.planToUpdate.applyTo(planEntity);
        ApiProduct apiProduct = apiProductCrudService.get(input.apiProductId);
        Plan updated = updatePlanDomainService.updatePlanForApiProduct(updatedEntity, Map.of(), apiProduct, input.auditInfo);

        log.debug("Plan {} updated for API Product {}", updated.getId(), input.apiProductId);
        return new Output(updated);
    }

    @Builder
    public record Input(PlanUpdates planToUpdate, String apiProductId, AuditInfo auditInfo) {}

    public record Output(Plan updated) {}
}
