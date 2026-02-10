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
package io.gravitee.apim.core.plan.use_case.api_product;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.PublishPlanDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.ClosePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeprecatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.ApiProductPlanSearchQueryService;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import lombok.Builder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class ApiProductPlanOperationsUseCase {

    private final ApiProductPlanSearchQueryService apiProductPlanSearchQueryService;
    private final PlanCrudService planCrudService;
    private final PublishPlanDomainService publishPlanDomainService;
    private final ClosePlanDomainService closePlanDomainService;
    private final DeprecatePlanDomainService deprecatePlanDomainService;

    public Output execute(Input input) {
        log.debug(
            "Executing API Product plan operation {} for plan {} on API Product {}",
            input.operation,
            input.planId,
            input.apiProductId
        );
        final Plan plan = apiProductPlanSearchQueryService.findByPlanIdIdForApiProduct(input.planId, input.apiProductId);
        if (
            plan.getReferenceType().equals(GenericPlanEntity.ReferenceType.API_PRODUCT) && !plan.getReferenceId().equals(input.apiProductId)
        ) {
            throw new PlanNotFoundException(input.planId);
        }
        if (input.operation.equals(Operation.DELETE.name())) {
            planCrudService.delete(input.planId);
            log.debug("Plan {} deleted for API Product {}", input.planId, input.apiProductId);
            return new Output(plan);
        } else if (input.operation.equals(Operation.CLOSE.name())) {
            log.debug("Plan {} closed for API Product {}", input.planId, input.apiProductId);
            closePlanDomainService.close(input.planId, input.auditInfo);
            return new Output(plan);
        } else if (input.operation.equals(Operation.PUBLISH.name())) {
            log.debug("Plan {} published for API Product {}", input.planId, input.apiProductId);
            return new Output(publishPlanDomainService.publish(GraviteeContext.getExecutionContext(), input.planId));
        } else if (input.operation.equals(Operation.DEPRECATE.name())) {
            log.debug("Plan {} deprecated for API Product {}", input.planId, input.apiProductId);
            deprecatePlanDomainService.deprecate(input.planId, input.auditInfo, false);
            return new Output(plan);
        } else {
            log.debug("Unsupported operation {} for plan {} on API Product {}", input.operation, input.planId, input.apiProductId);
            return null;
        }
    }

    @Builder
    public record Input(String planId, String apiProductId, String operation, AuditInfo auditInfo) {}

    public record Output(Plan plan) {}

    public enum Operation {
        DELETE,
        PUBLISH,
        CLOSE,
        DEPRECATE,
    }
}
