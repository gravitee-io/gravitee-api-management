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
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.PublishPlanDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.ClosePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeprecatePlanDomainService;
import io.gravitee.apim.core.plan.exception.UnsupportedPlanOperationException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import lombok.Builder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class PlanOperationsUseCase {

    private final PlanSearchQueryService planSearchQueryService;
    private final PlanCrudService planCrudService;
    private final PublishPlanDomainService publishPlanDomainService;
    private final ClosePlanDomainService closePlanDomainService;
    private final DeprecatePlanDomainService deprecatePlanDomainService;

    public Output execute(Input input) {
        log.debug("Executing plan operation {} for plan {} on reference {}", input.operation, input.planId, input.referenceId);
        Plan plan = planSearchQueryService.findByPlanIdAndReferenceIdAndReferenceType(input.planId, input.referenceId, input.referenceType);
        if (
            plan.getReferenceType().equals(GenericPlanEntity.ReferenceType.API_PRODUCT) && !plan.getReferenceId().equals(input.referenceId)
        ) {
            throw new PlanNotFoundException(input.planId);
        }

        return switch (input.operation) {
            case "DELETE" -> {
                planCrudService.delete(input.planId);
                log.debug("Plan {} deleted for reference {}", input.planId, input.referenceId);
                yield new Output(plan);
            }
            case "CLOSE" -> {
                log.debug("Plan {} closed for reference {}", input.planId, input.referenceId);
                closePlanDomainService.close(input.planId, input.auditInfo);
                yield new Output(plan);
            }
            case "PUBLISH" -> {
                log.debug("Plan {} published for reference {}", input.planId, input.referenceId);
                yield new Output(publishPlanDomainService.publish(GraviteeContext.getExecutionContext(), input.planId));
            }
            case "DEPRECATE" -> {
                log.debug("Plan {} deprecated for reference {}", input.planId, input.referenceId);
                deprecatePlanDomainService.deprecate(input.planId, input.auditInfo, false);
                yield new Output(plan);
            }
            default -> throw new UnsupportedPlanOperationException(input.operation);
        };
    }

    @Builder
    public record Input(String planId, String referenceId, String referenceType, String operation, AuditInfo auditInfo) {}

    public record Output(Plan plan) {}

    public enum Operation {
        DELETE,
        PUBLISH,
        CLOSE,
        DEPRECATE,
    }
}
