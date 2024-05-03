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
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.Map;
import lombok.Builder;

@UseCase
public class UpdateFederatedPlanUseCase {

    private final PlanCrudService planCrudService;
    private final PlanValidatorDomainService planValidatorService;
    private final ReorderPlanDomainService reorderPlanDomainService;
    private final AuditDomainService auditService;

    public UpdateFederatedPlanUseCase(
        PlanCrudService planCrudService,
        PlanValidatorDomainService planValidatorService,
        ReorderPlanDomainService reorderPlanDomainService,
        AuditDomainService auditService
    ) {
        this.planCrudService = planCrudService;
        this.planValidatorService = planValidatorService;
        this.reorderPlanDomainService = reorderPlanDomainService;
        this.auditService = auditService;
    }

    public Output execute(Input input) {
        if (input.planToUpdate.getDefinitionVersion() != DefinitionVersion.FEDERATED) {
            throw new IllegalArgumentException(String.format("Can't update a %s plan", input.planToUpdate.getDefinitionVersion()));
        }
        planValidatorService.validateGeneralConditionsPageStatus(input.planToUpdate);

        var existingPlan = planCrudService.findById(input.planToUpdate.getId());
        if (existingPlan.getPlanStatus() == PlanStatus.CLOSED && existingPlan.getPlanStatus() != input.planToUpdate.getPlanStatus()) {
            throw new ValidationDomainException("Invalid status for plan '" + input.planToUpdate.getName() + "'");
        }

        var toUpdate = existingPlan.update(input.planToUpdate);

        Plan updated;
        if (toUpdate.getOrder() != existingPlan.getOrder()) {
            updated = reorderPlanDomainService.reorderAfterUpdate(toUpdate);
        } else {
            updated = planCrudService.update(toUpdate);
        }

        createAuditLog(existingPlan, updated, input.auditInfo);

        return new Output(updated);
    }

    private void createAuditLog(Plan oldPlan, Plan newPlan, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(newPlan.getApiId())
                .event(PlanAuditEvent.PLAN_UPDATED)
                .actor(auditInfo.actor())
                .oldValue(oldPlan)
                .newValue(newPlan)
                .createdAt(newPlan.getUpdatedAt())
                .properties(Map.of(AuditProperties.PLAN, newPlan.getId()))
                .build()
        );
    }

    @Builder
    public record Input(Plan planToUpdate, AuditInfo auditInfo) {}

    public record Output(Plan updated) {}
}
