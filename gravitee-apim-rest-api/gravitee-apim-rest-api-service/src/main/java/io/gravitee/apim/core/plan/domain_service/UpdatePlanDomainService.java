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

import static io.gravitee.apim.core.utils.CollectionUtils.stream;
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
import io.gravitee.apim.core.plan.crud_service.KafkaPortRangeCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.KafkaPortRange;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
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
    private final VerifyPlanPortRangesDomainService verifyPlanPortRangesDomainService;
    private final KafkaPortRangeCrudService kafkaPortRangeCrudService;

    public UpdatePlanDomainService(
        PlanQueryService planQueryService,
        PlanCrudService planCrudService,
        PlanValidatorDomainService planValidatorDomainService,
        FlowValidationDomainService flowValidationDomainService,
        FlowCrudService flowCrudService,
        AuditDomainService auditService,
        PlanSynchronizationService planSynchronizationService,
        ReorderPlanDomainService reorderPlanDomainService,
        VerifyPlanPortRangesDomainService verifyPlanPortRangesDomainService,
        KafkaPortRangeCrudService kafkaPortRangeCrudService
    ) {
        this.planQueryService = planQueryService;
        this.planCrudService = planCrudService;
        this.planValidatorDomainService = planValidatorDomainService;
        this.flowValidationDomainService = flowValidationDomainService;
        this.flowCrudService = flowCrudService;
        this.auditService = auditService;
        this.planSynchronizationService = planSynchronizationService;
        this.reorderPlanDomainService = reorderPlanDomainService;
        this.verifyPlanPortRangesDomainService = verifyPlanPortRangesDomainService;
        this.kafkaPortRangeCrudService = kafkaPortRangeCrudService;
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

    public void validate(Plan planToUpdate, List<Flow> flows, Api api, AuditInfo auditInfo) {
        updatePreFlightChecks(planToUpdate, getPlanStatusMap(api), api, auditInfo);
        validateAndSanitizeHttpV4Flows(flows, api);
    }

    private Map<String, PlanStatus> getPlanStatusMap(Api api) {
        return getPlanStatusMap(api.getId(), GenericPlanEntity.ReferenceType.API);
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

        return switch (api.getApiDefinitionValue()) {
            case io.gravitee.definition.model.v4.Api apiDefinitionV4 -> updateHttpV4ApiPlan(
                existingPlan,
                updatePlan,
                (List<Flow>) flows,
                api,
                apiDefinitionV4,
                auditInfo,
                updateFunction
            );
            case NativeApi ignore -> updateNativeV4ApiPlan(
                existingPlan,
                updatePlan,
                (List<NativeFlow>) flows,
                api,
                auditInfo,
                updateFunction
            );
            default -> throw new IllegalStateException(api.getDefinitionVersion() + " is not supported");
        };
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
        io.gravitee.definition.model.v4.Api apiDefinitionV4,
        AuditInfo auditInfo,
        BinaryOperator<Plan> updateFunction
    ) {
        var sanitizedFlows = validateAndSanitizeHttpV4Flows(flows, api);
        var existingFlows = flowCrudService.getPlanV4Flows(existingPlan.getId());

        if (!planSynchronizationService.checkSynchronized(existingPlan, existingFlows, updatePlan, sanitizedFlows)) {
            updatePlan.setNeedRedeployAt(Date.from(updatePlan.getUpdatedAt().toInstant()));
        }

        Plan updated = updateFunction.apply(existingPlan, updatePlan);

        flowCrudService.savePlanFlows(updated.getId(), sanitizedFlows);

        createAuditLog(existingPlan, updated, auditInfo);
        return updated;
    }

    private List<Flow> validateAndSanitizeHttpV4Flows(List<Flow> flows, Api api) {
        var sanitizedFlows = flowValidationDomainService.validateAndSanitizeHttpV4(api.getType(), flows);
        var flows2 = api.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api apiDefinitionV4
            ? stream(apiDefinitionV4.getFlows())
            : Stream.<Flow>empty();
        flowValidationDomainService.validatePathParameters(api.getType(), flows2, sanitizedFlows.stream());
        return sanitizedFlows;
    }

    private Plan updateNativeV4ApiPlan(
        Plan existingPlan,
        Plan updatePlan,
        List<NativeFlow> flows,
        Api api,
        AuditInfo auditInfo,
        BinaryOperator<Plan> updateFunction
    ) {
        validatePortRoutingIfConfigured(updatePlan, api, auditInfo);

        var sanitizedNativeFlows = flowValidationDomainService.validateAndSanitizeNativeV4(flows);

        if (!planSynchronizationService.checkNativePlanSynchronized(existingPlan, List.of(), updatePlan, sanitizedNativeFlows)) {
            updatePlan.setNeedRedeployAt(Date.from(updatePlan.getUpdatedAt().toInstant()));
        }

        Plan updated = updateFunction.apply(existingPlan, updatePlan);

        flowCrudService.saveNativePlanFlows(updated.getId(), sanitizedNativeFlows);

        persistPortRangeIfConfigured(updated, api, auditInfo);

        createAuditLog(existingPlan, updated, auditInfo);
        return updated;
    }

    /**
     * Validates the updated plan's port allocation before the row is written. No-op when the plan
     * doesn't configure port-based routing. Excludes the plan itself from the sibling check so
     * unchanged allocations don't conflict with themselves.
     */
    private void validatePortRoutingIfConfigured(Plan updatePlan, Api api, AuditInfo auditInfo) {
        var nativeDefinition = updatePlan.getPlanDefinitionNativeV4();
        if (nativeDefinition == null || nativeDefinition.getBootstrapPort() == null) {
            return;
        }
        verifyPlanPortRangesDomainService.verify(
            auditInfo.environmentId(),
            updatePlan.getId(),
            nativeDefinition.getBootstrapPort(),
            nativeDefinition.getBrokerRangeStart(),
            nativeDefinition.getBrokerRangeEnd()
        );
    }

    /**
     * Upserts the {@code kafka_port_ranges} row after the plan has been updated. Creates the row
     * when the plan transitions from host to port mode, updates when it was already on port mode,
     * and deletes when the plan reverts to host mode (port fields cleared).
     *
     * <p>The JDBC ORM writes every declared column on update, so we must carry the existing
     * {@code createdAt} forward — otherwise the column would be nulled on every plan update.</p>
     */
    private void persistPortRangeIfConfigured(Plan updated, Api api, AuditInfo auditInfo) {
        var nativeDefinition = updated.getPlanDefinitionNativeV4();
        if (nativeDefinition == null || nativeDefinition.getBootstrapPort() == null) {
            kafkaPortRangeCrudService.delete(updated.getId());
            return;
        }
        var row = KafkaPortRange.builder()
            .planId(updated.getId())
            .apiId(api.getId())
            .environmentId(auditInfo.environmentId())
            .bootstrapPort(nativeDefinition.getBootstrapPort())
            .rangeStart(nativeDefinition.getBrokerRangeStart())
            .rangeEnd(nativeDefinition.getBrokerRangeEnd())
            .updatedAt(TimeProvider.now())
            .build();
        kafkaPortRangeCrudService
            .findByPlanId(updated.getId())
            .ifPresentOrElse(
                existing -> kafkaPortRangeCrudService.update(row.toBuilder().createdAt(existing.getCreatedAt()).build()),
                () -> kafkaPortRangeCrudService.create(row.toBuilder().createdAt(TimeProvider.now()).build())
            );
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

    private void updatePreFlightChecks(Plan planToUpdate, Map<String, PlanStatus> existingPlanStatuses, Api api, AuditInfo auditInfo) {
        assertNotClosedStatus(planToUpdate, existingPlanStatuses);
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

        if (!planSynchronizationService.checkSynchronized(existingPlan, List.of(), updatePlan, List.of())) {
            updatePlan.setNeedRedeployAt(Date.from(updatePlan.getUpdatedAt().toInstant()));
        }

        Plan updated = orderAwareUpdate(existingPlan, updatePlan);

        createApiProductAuditLog(existingPlan, updated, auditInfo);

        return updated;
    }

    private void updatePreFlightChecksForApiProduct(Plan planToUpdate, Map<String, PlanStatus> existingPlanStatuses, AuditInfo auditInfo) {
        assertNotClosedStatus(planToUpdate, existingPlanStatuses);
        planValidatorDomainService.validatePlanSecurity(planToUpdate, auditInfo.organizationId(), auditInfo.environmentId(), null);
        planValidatorDomainService.validateGeneralConditionsPageStatus(planToUpdate);
    }

    private void assertNotClosedStatus(Plan planToUpdate, Map<String, PlanStatus> existingPlanStatuses) {
        if (
            existingPlanStatuses.containsKey(planToUpdate.getId()) &&
            existingPlanStatuses.get(planToUpdate.getId()) == PlanStatus.CLOSED &&
            existingPlanStatuses.get(planToUpdate.getId()) != planToUpdate.getPlanStatus()
        ) {
            throw new ValidationDomainException("Invalid status for plan '" + planToUpdate.getName() + "'");
        }
    }

    private Map<String, PlanStatus> getPlanStatusMapForApiProduct(ApiProduct apiProduct) {
        return getPlanStatusMap(apiProduct.getId(), GenericPlanEntity.ReferenceType.API_PRODUCT);
    }

    private Map<String, PlanStatus> getPlanStatusMap(String referenceId, GenericPlanEntity.ReferenceType referenceType) {
        return planQueryService
            .findAllByReferenceIdAndReferenceType(referenceId, referenceType)
            .stream()
            .collect(toMap(Plan::getId, Plan::getPlanStatus));
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
