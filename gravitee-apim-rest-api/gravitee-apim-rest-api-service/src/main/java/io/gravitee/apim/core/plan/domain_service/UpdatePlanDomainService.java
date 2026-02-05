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
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

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

    public void bulkUpdate(
        List<Plan> plansToUpdate,
        Map<String, PlanStatus> existingPlanStatuses,
        Map<String, List<? extends AbstractFlow>> flows,
        Api api,
        AuditInfo auditInfo
    ) {
        Objects.requireNonNull(existingPlanStatuses, "existingPlanStatuses must not be null");
        Objects.requireNonNull(flows, "flows must not be null");
        for (Plan planToUpdate : plansToUpdate) {
            switch (planToUpdate.getDefinitionVersion()) {
                case V4 -> updateV4ApiPlan(
                    planToUpdate,
                    flows.get(planToUpdate.getId()),
                    existingPlanStatuses,
                    api,
                    auditInfo,
                    (existing, update) -> planCrudService.update(update)
                );
                case FEDERATED -> updateFederatedApiPlan(planToUpdate, auditInfo);
                case V2 -> updateV2ApiPlan(planToUpdate, existingPlanStatuses, api, auditInfo);
                default -> throw new IllegalStateException(api.getDefinitionVersion() + " is not supported");
            }
        }
    }

    public Plan update(
        Plan planToUpdate,
        List<? extends AbstractFlow> flows,
        Map<String, PlanStatus> existingPlanStatuses,
        Api api,
        AuditInfo auditInfo
    ) {
        return switch (planToUpdate.getDefinitionVersion()) {
            case V4 -> {
                if (existingPlanStatuses == null) {
                    existingPlanStatuses = getPlanStatusMap(api);
                }
                yield updateV4ApiPlan(planToUpdate, flows, existingPlanStatuses, api, auditInfo, this::orderAwareUpdate);
            }
            case FEDERATED -> updateFederatedApiPlan(planToUpdate, auditInfo);
            case V2 -> {
                if (existingPlanStatuses == null) {
                    existingPlanStatuses = getPlanStatusMap(api);
                }
                yield updateV2ApiPlan(planToUpdate, existingPlanStatuses, api, auditInfo);
            }
            default -> throw new IllegalStateException(api.getDefinitionVersion() + " is not supported");
        };
    }

    private Map<String, PlanStatus> getPlanStatusMap(Api api) {
        List<Plan> existingPlans = planQueryService.findAllByApiId(api.getId());
        return existingPlans.stream().collect(toMap(Plan::getId, Plan::getPlanStatus));
    }

    /**
     * Update plans of V4 API.
     *
     * @param planToUpdate The plan to update
     * @param api          The API to update.
     * @param auditInfo    The audit information
     * @return The updated plan
     */
    private Plan updateV4ApiPlan(
        Plan planToUpdate,
        List<? extends AbstractFlow> flows,
        Map<String, PlanStatus> existingPlanStatuses,
        Api api,
        AuditInfo auditInfo,
        BinaryOperator<Plan> updateFunction
    ) {
        updatePreFlightChecks(planToUpdate, existingPlanStatuses, api, auditInfo);

        Plan existingPlan = planCrudService.getById(planToUpdate.getId());
        Plan updatePlan = existingPlan.update(planToUpdate);

        if (api.isNative()) {
            return updateNativeV4ApiPlan(existingPlan, updatePlan, (List<NativeFlow>) flows, auditInfo, updateFunction);
        }

        return updateHttpV4ApiPlan(existingPlan, updatePlan, (List<Flow>) flows, api, auditInfo, updateFunction);
    }

    private Plan updateV2ApiPlan(Plan planToUpdate, Map<String, PlanStatus> existingPlanStatuses, Api api, AuditInfo auditInfo) {
        updatePreFlightChecks(planToUpdate, existingPlanStatuses, api, auditInfo);

        Plan existingPlan = planCrudService.getById(planToUpdate.getId());
        Plan updatePlan = existingPlan.update(planToUpdate);

        if (!planSynchronizationService.checkSynchronized(existingPlan, List.of(), updatePlan, List.of() /*sanitizedFlows TODO */)) {
            updatePlan.setNeedRedeployAt(Date.from(updatePlan.getUpdatedAt().toInstant()));
        }

        var updated = orderAwareUpdate(existingPlan, updatePlan);

        // TODO flowCrudService.savePlanFlows(updated.getId(), sanitizedFlows);

        createAuditLog(existingPlan, updated, auditInfo);
        return updated;
    }

    private Plan updateHttpV4ApiPlan(
        Plan existingPlan,
        Plan updatePlan,
        List<Flow> flows,
        Api api,
        AuditInfo auditInfo,
        BinaryOperator<Plan> updateFunction
    ) {
        var sanitizedFlows = flowValidationDomainService.validateAndSanitizeHttpV4(api.getType(), flows);
        flowValidationDomainService.validatePathParameters(
            api.getType(),
            api.getApiDefinitionHttpV4().getFlows() != null ? api.getApiDefinitionHttpV4().getFlows().stream() : Stream.empty(),
            sanitizedFlows.stream()
        );

        if (!planSynchronizationService.checkSynchronized(existingPlan, List.of(), updatePlan, sanitizedFlows)) {
            updatePlan.setNeedRedeployAt(Date.from(updatePlan.getUpdatedAt().toInstant()));
        }

        Plan updated = updateFunction.apply(existingPlan, updatePlan);

        flowCrudService.savePlanFlows(updated.getId(), sanitizedFlows);

        createAuditLog(existingPlan, updated, auditInfo);
        return updated;
    }

    private Plan updateNativeV4ApiPlan(
        Plan existingPlan,
        Plan updatePlan,
        List<NativeFlow> flows,
        AuditInfo auditInfo,
        BinaryOperator<Plan> updateFunction
    ) {
        var sanitizedNativeFlows = flowValidationDomainService.validateAndSanitizeNativeV4(flows);

        if (!planSynchronizationService.checkNativePlanSynchronized(existingPlan, List.of(), updatePlan, sanitizedNativeFlows)) {
            updatePlan.setNeedRedeployAt(Date.from(updatePlan.getUpdatedAt().toInstant()));
        }

        Plan updated = updateFunction.apply(existingPlan, updatePlan);

        flowCrudService.saveNativePlanFlows(updated.getId(), sanitizedNativeFlows);

        createAuditLog(existingPlan, updated, auditInfo);
        return updated;
    }

    /**
     * Update plans of Federated API.
     *
     * @param planToUpdate The plan to update
     * @param auditInfo    The audit information
     * @return The updated plan
     */
    private Plan updateFederatedApiPlan(Plan planToUpdate, AuditInfo auditInfo) {
        planValidatorDomainService.validateGeneralConditionsPageStatus(planToUpdate);

        var existingPlan = planCrudService.getById(planToUpdate.getId());
        if (existingPlan.getPlanStatus() == PlanStatus.CLOSED && existingPlan.getPlanStatus() != planToUpdate.getPlanStatus()) {
            throw new ValidationDomainException("Invalid status for planToUpdate '" + planToUpdate.getName() + "'");
        }

        var toUpdate = existingPlan.update(planToUpdate);

        Plan updated = orderAwareUpdate(existingPlan, toUpdate);

        createAuditLog(existingPlan, updated, auditInfo);

        return updated;
    }

    private Plan orderAwareUpdate(Plan existingPlan, Plan planToUpdate) {
        if (planToUpdate.getOrder() != existingPlan.getOrder()) {
            return reorderPlanDomainService.reorderAfterUpdate(planToUpdate);
        } else {
            return planCrudService.update(planToUpdate);
        }
    }

    private Plan orderAwareUpdateForApiProduct(Plan existingPlan, Plan planToUpdate) {
        if (planToUpdate.getOrder() != existingPlan.getOrder()) {
            return reorderPlanDomainService.reorderAfterUpdateForApiProduct(planToUpdate);
        } else {
            return planCrudService.update(planToUpdate);
        }
    }

    private void updatePreFlightChecks(Plan planToUpdate, Map<String, PlanStatus> existingPlanStatuses, Api api, AuditInfo auditInfo) {
        if (
            existingPlanStatuses.containsKey(planToUpdate.getId()) &&
            existingPlanStatuses.get(planToUpdate.getId()) == PlanStatus.CLOSED &&
            existingPlanStatuses.get(planToUpdate.getId()) != planToUpdate.getPlanStatus()
        ) {
            throw new ValidationDomainException("Invalid status for plan '" + planToUpdate.getName() + "'");
        }

        planValidatorDomainService.validatePlanSecurity(planToUpdate, auditInfo.organizationId(), auditInfo.environmentId(), api.getType());
        planValidatorDomainService.validatePlanTagsAgainstApiTags(planToUpdate.getTags(), api.getTags());
        planValidatorDomainService.validateGeneralConditionsPageStatus(planToUpdate);
    }

    public Plan updatePlanForApiProduct(
        Plan planToUpdate,
        Map<String, PlanStatus> existingPlanStatuses,
        ApiProduct apiProduct,
        AuditInfo auditInfo
    ) {
        if (existingPlanStatuses == null) {
            existingPlanStatuses = getPlanStatusMapForApiProduct(apiProduct);
        }
        updatePreFlightChecksForApiProduct(planToUpdate, existingPlanStatuses, auditInfo);

        Plan existingPlan = planCrudService.getById(planToUpdate.getId());
        Plan updatePlan = existingPlan.update(planToUpdate);

        Plan updated = orderAwareUpdateForApiProduct(existingPlan, updatePlan);

        createApiProductAuditLog(existingPlan, updated, auditInfo);
        return updated;
    }

    private void updatePreFlightChecksForApiProduct(Plan planToUpdate, Map<String, PlanStatus> existingPlanStatuses, AuditInfo auditInfo) {
        if (
            existingPlanStatuses.containsKey(planToUpdate.getId()) &&
            existingPlanStatuses.get(planToUpdate.getId()) == PlanStatus.CLOSED &&
            existingPlanStatuses.get(planToUpdate.getId()) != planToUpdate.getPlanStatus()
        ) {
            throw new ValidationDomainException("Invalid status for plan '" + planToUpdate.getName() + "'");
        }

        planValidatorDomainService.validatePlanSecurity(planToUpdate, auditInfo.organizationId(), auditInfo.environmentId(), null);
        planValidatorDomainService.validateGeneralConditionsPageStatus(planToUpdate);
    }

    private Map<String, PlanStatus> getPlanStatusMapForApiProduct(ApiProduct apiProduct) {
        List<Plan> existingPlans = planQueryService.findAllForApiProduct(apiProduct.getId());
        return existingPlans.stream().collect(toMap(Plan::getId, Plan::getPlanStatus));
    }

    private void createAuditLog(Plan oldPlan, Plan newPlan, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(newPlan.getReferenceId())
                .event(PlanAuditEvent.PLAN_UPDATED)
                .actor(auditInfo.actor())
                .oldValue(oldPlan)
                .newValue(newPlan)
                .createdAt(newPlan.getUpdatedAt())
                .properties(Map.of(AuditProperties.PLAN, newPlan.getId()))
                .build()
        );
    }

    private void createApiProductAuditLog(Plan oldPlan, Plan newPlan, AuditInfo auditInfo) {
        auditService.createApiProductAuditLog(
            ApiProductAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiProductId(newPlan.getReferenceId())
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
