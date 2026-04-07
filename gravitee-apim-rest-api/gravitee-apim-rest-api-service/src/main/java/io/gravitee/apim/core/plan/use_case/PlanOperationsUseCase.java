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
import io.gravitee.apim.core.plan.domain_service.DeletePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeprecatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import lombok.Builder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class PlanOperationsUseCase {

    private final PlanSearchQueryService planSearchQueryService;
    private final PlanCrudService planCrudService;
    private final DeletePlanDomainService deletePlanDomainService;
    private final PublishPlanDomainService publishPlanDomainService;
    private final ClosePlanDomainService closePlanDomainService;
    private final DeprecatePlanDomainService deprecatePlanDomainService;

    public Output execute(Input input) {
        log.debug("Executing plan operation {} for plan {} on reference {}", input.operation, input.planId, input.referenceId);
        final Plan plan = planSearchQueryService.findByPlanIdAndReferenceIdAndReferenceType(
            input.planId,
            input.referenceId,
            input.referenceType
        );
        return switch (input.operation) {
            case DELETE -> {
                deletePlanDomainService.delete(plan, input.auditInfo);
                yield new Output(plan);
            }
            case CLOSE -> {
                closePlanDomainService.close(input.planId, input.auditInfo);
                yield new Output(planCrudService.getById(input.planId));
            }
            case PUBLISH -> new Output(publishPlanDomainService.publish(input.auditInfo, input.planId));
            case DEPRECATE -> {
                deprecatePlanDomainService.deprecate(input.planId, input.auditInfo, false);
                yield new Output(planCrudService.getById(input.planId));
            }
        };
    }

    @Builder
    public record Input(
        String planId,
        String referenceId,
        GenericPlanEntity.ReferenceType referenceType,
        Operation operation,
        AuditInfo auditInfo
    ) {}

    public record Output(Plan plan) {}

    public enum Operation {
        DELETE,
        PUBLISH,
        CLOSE,
        DEPRECATE,
    }
}
