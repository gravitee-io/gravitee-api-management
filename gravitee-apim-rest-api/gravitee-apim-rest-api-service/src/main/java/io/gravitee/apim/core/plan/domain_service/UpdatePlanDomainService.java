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

import static java.util.stream.Collectors.toMap;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.Date;
import java.util.List;
import java.util.Map;

@DomainService
public class UpdatePlanDomainService {

    private final PlanQueryService planQueryService;
    private final PlanCrudService planCrudService;
    private final PlanValidatorDomainService planValidatorDomainService;
    private final FlowValidationDomainService flowValidationDomainService;
    private final FlowCrudService flowCrudService;
    private final AuditDomainService auditService;
    private final PlanSynchronizationService planSynchronizationService;
    private final ReorderPlanDomainService reorderPlanDomainService;

    public UpdatePlanDomainService(
        PlanQueryService planQueryService,
        PlanCrudService planCrudService,
        PlanValidatorDomainService planValidatorDomainService,
        FlowValidationDomainService flowValidationDomainService,
        FlowCrudService flowCrudService,
        AuditDomainService auditService,
        PlanSynchronizationService planSynchronizationService,
        ReorderPlanDomainService reorderPlanDomainService
    ) {
        this.planQueryService = planQueryService;
        this.planCrudService = planCrudService;
        this.planValidatorDomainService = planValidatorDomainService;
        this.flowValidationDomainService = flowValidationDomainService;
        this.flowCrudService = flowCrudService;
        this.auditService = auditService;
        this.planSynchronizationService = planSynchronizationService;
        this.reorderPlanDomainService = reorderPlanDomainService;
    }

    public Plan update(Plan planToUpdate, List<Flow> flows, Map<String, PlanStatus> existingPlanStatuses, Api api, AuditInfo auditInfo) {
        return switch (planToUpdate.getDefinitionVersion()) {
            case V4 -> {
                if (existingPlanStatuses == null) {
                    List<Plan> existingPlans = planQueryService.findAllByApiId(api.getId());
                    existingPlanStatuses = existingPlans.stream().collect(toMap(Plan::getId, Plan::getPlanStatus));
                }
                yield updateV4ApiPlan(planToUpdate, flows, existingPlanStatuses, api, auditInfo);
            }
            case FEDERATED -> updateFederatedApiPlan(planToUpdate, auditInfo);
            default -> throw new IllegalStateException(api.getDefinitionVersion() + " is not supported");
        };
    }

    /**
     * Update plans of V4 API.
     *
     * @param planToUpdate The plan to update
     * @param api The API to update.
     * @param auditInfo The audit information
     * @return The updated plan
     */
    private Plan updateV4ApiPlan(
        Plan planToUpdate,
        List<Flow> flows,
        Map<String, PlanStatus> existingPlanStatuses,
        Api api,
        AuditInfo auditInfo
    ) {
        if (
            existingPlanStatuses.containsKey(planToUpdate.getId()) &&
            existingPlanStatuses.get(planToUpdate.getId()) == PlanStatus.CLOSED &&
            existingPlanStatuses.get(planToUpdate.getId()) != planToUpdate.getPlanStatus()
        ) {
            throw new ValidationDomainException("Invalid status for plan '" + planToUpdate.getName() + "'");
        }

        planValidatorDomainService.validatePlanSecurity(planToUpdate, auditInfo.organizationId(), auditInfo.environmentId());
        planValidatorDomainService.validatePlanTagsAgainstApiTags(planToUpdate.getPlanDefinitionV4().getTags(), api.getTags());
        planValidatorDomainService.validateGeneralConditionsPageStatus(planToUpdate);

        var sanitizedFlows = flowValidationDomainService.validateAndSanitizeHttpV4(api.getType(), flows);
        flowValidationDomainService.validatePathParameters(
            api.getType(),
            api.getApiDefinitionV4().getFlows().stream(),
            sanitizedFlows.stream()
        );

        var existingPlan = planCrudService.getById(planToUpdate.getId());
        var toUpdate = existingPlan.update(planToUpdate);
        if (!planSynchronizationService.checkSynchronized(existingPlan, List.of(), toUpdate, sanitizedFlows)) {
            toUpdate.setNeedRedeployAt(Date.from(toUpdate.getUpdatedAt().toInstant()));
        }

        Plan updated;
        if (toUpdate.getOrder() != existingPlan.getOrder()) {
            updated = reorderPlanDomainService.reorderAfterUpdate(toUpdate);
        } else {
            updated = planCrudService.update(toUpdate);
        }

        flowCrudService.savePlanFlows(updated.getId(), sanitizedFlows);
        createAuditLog(existingPlan, updated, auditInfo);

        return updated;
    }

    /**
     * Update plans of Federated API.
     *
     * @param planToUpdate The plan to update
     * @param auditInfo The audit information
     * @return The updated plan
     */
    private Plan updateFederatedApiPlan(Plan planToUpdate, AuditInfo auditInfo) {
        planValidatorDomainService.validateGeneralConditionsPageStatus(planToUpdate);

        var existingPlan = planCrudService.getById(planToUpdate.getId());
        if (existingPlan.getPlanStatus() == PlanStatus.CLOSED && existingPlan.getPlanStatus() != planToUpdate.getPlanStatus()) {
            throw new ValidationDomainException("Invalid status for planToUpdate '" + planToUpdate.getName() + "'");
        }

        var toUpdate = existingPlan.update(planToUpdate);

        Plan updated;
        if (toUpdate.getOrder() != existingPlan.getOrder()) {
            updated = reorderPlanDomainService.reorderAfterUpdate(toUpdate);
        } else {
            updated = planCrudService.update(toUpdate);
        }

        createAuditLog(existingPlan, updated, auditInfo);

        return updated;
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
}
