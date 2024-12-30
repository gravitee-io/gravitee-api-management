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
package io.gravitee.apim.core.plan.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.exception.InvalidPlanStatusForDeprecationException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@DomainService
public class DeprecatePlanDomainService {

    private final PlanCrudService planCrudService;
    private final AuditDomainService auditService;

    public void deprecate(String planId, AuditInfo auditInfo, boolean allowStaging) {
        var originalPlan = planCrudService.getById(planId);

        if (
            PlanStatus.DEPRECATED == originalPlan.getPlanStatus() ||
            PlanStatus.CLOSED == originalPlan.getPlanStatus() ||
            (PlanStatus.STAGING == originalPlan.getPlanStatus() && !allowStaging)
        ) {
            throw new InvalidPlanStatusForDeprecationException(originalPlan.getId(), originalPlan.getPlanStatus());
        }

        var planToDeprecate = originalPlan.copy();

        planToDeprecate.setUpdatedAt(TimeProvider.now());
        planToDeprecate.setPlanStatus(PlanStatus.DEPRECATED);

        var deprecatedPlan = planCrudService.update(planToDeprecate);
        createAuditLog(originalPlan, deprecatedPlan, auditInfo);
    }

    private void createAuditLog(Plan originalPlan, Plan planUpdated, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(originalPlan.getApiId())
                .event(PlanAuditEvent.PLAN_DEPRECATED)
                .actor(auditInfo.actor())
                .oldValue(originalPlan)
                .newValue(planUpdated)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.PLAN, originalPlan.getId()))
                .build()
        );
    }
}
