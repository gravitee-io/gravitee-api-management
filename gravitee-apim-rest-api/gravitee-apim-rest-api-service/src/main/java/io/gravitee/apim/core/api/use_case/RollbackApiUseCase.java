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
package io.gravitee.apim.core.api.use_case;

import static io.gravitee.apim.core.utils.CollectionUtils.size;
import static io.gravitee.apim.core.utils.CollectionUtils.stream;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateNativeApiDomainService;
import io.gravitee.apim.core.api.domain_service.property.PropertyDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.mapper.V4toV2RollbackOperator;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.ClosePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.plan.AbstractPlan;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class RollbackApiUseCase {

    private static final V4toV2RollbackOperator ROLLBACK_OPERATOR = new V4toV2RollbackOperator();
    private static final String ROLLBACK = "rollback";
    private static final String CLOSE = "close";
    private static final String REOPEN = "reopen";

    private final EventQueryService eventQueryService;
    private final ApiCrudService apiCrudService;
    private final UpdateApiDomainService updateApiDomainService;
    private final PlanQueryService planQueryService;
    private final CreatePlanDomainService createPlanDomainService;
    private final UpdatePlanDomainService updatePlanDomainService;
    private final ClosePlanDomainService closePlanDomainService;
    private final PlanCrudService planCrudService;
    private final AuditDomainService auditService;
    private final FlowCrudService flowCrudService;
    private final ApiIndexerDomainService apiIndexerDomainService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final ApiStateDomainService apiStateService;
    private final UpdateNativeApiDomainService updateNativeApiDomainService;
    private final PropertyDomainService propertyDomainService;

    public void execute(Input input) {
        Api api = eventQueryService
            .findApiFromPublishApiEvent(input.eventId)
            .orElseThrow(() -> new IllegalStateException("Cannot rollback an event that is not a publish event!"));
        var apiUpdated = switch (api.getApiDefinitionValue()) {
            case io.gravitee.definition.model.v4.Api apiDefinition -> {
                var toRollback = apiCrudService.get(apiDefinition.getId());

                // Rollback API from API definition without plans
                var rollbackedApi = toRollback.rollbackTo(apiDefinition);

                //update the description since the description is not stored in the definition
                rollbackedApi.setDescription(api.getDescription());

                keepStoredEncryption(toRollback.getApiDefinitionHttpV4(), rollbackedApi.getApiDefinitionHttpV4());

                var apiUpdatedV4 = updateApiDomainService.updateV4(rollbackedApi, input.auditInfo);

                // Rollback plans from API definition plans
                rollbackPlansV4(apiDefinition.getPlans(), apiUpdatedV4, input.auditInfo);
                yield apiUpdatedV4;
            }
            case io.gravitee.definition.model.Api apiDefinition -> {
                var toRollback = apiCrudService.get(api.getId());
                if (toRollback.getDefinitionVersion() != io.gravitee.definition.model.DefinitionVersion.V4) {
                    throw new IllegalStateException("The migration is only built for rollback migration from V2 to V4.");
                }
                if (
                    apiDefinition.getServices().getDynamicPropertyService() != null &&
                    apiDefinition.getServices().getDynamicPropertyService().isEnabled()
                ) {
                    apiStateService.stopV4DynamicProperties(api.getId());
                }
                Api rollbackedApi = ROLLBACK_OPERATOR.rollback(toRollback, apiDefinition);

                var apiPrimaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(input.auditInfo().organizationId(), api.getId());
                var indexerContext = new ApiIndexerDomainService.Context(input.auditInfo(), false);
                apiIndexerDomainService.delete(indexerContext, toRollback);
                apiIndexerDomainService.index(indexerContext, rollbackedApi, apiPrimaryOwner);

                var apiUpdatedV2 = apiCrudService.update(rollbackedApi);

                // Rollback plans from API definition plans
                var plans = rollbackPlansV2(apiDefinition.getPlans(), apiUpdatedV2, input.auditInfo);
                flowCrudService.saveApiFlowsV2(apiDefinition.getId(), apiDefinition.getFlows());
                for (var plan : apiDefinition.getPlans()) {
                    flowCrudService.savePlanFlowsV2(plan.getId(), plan.getFlows());
                }
                for (String planId : plans.closedPlans()) {
                    flowCrudService.savePlanFlowsV2(planId, List.of());
                }
                if (
                    apiDefinition.getServices().getDynamicPropertyService() != null &&
                    apiDefinition.getServices().getDynamicPropertyService().isEnabled()
                ) {
                    apiStateService.startV2DynamicProperties(rollbackedApi.getId());
                }
                yield apiUpdatedV2;
            }
            case NativeApi apiDefinition -> {
                var primaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(
                    input.auditInfo().organizationId(),
                    apiDefinition.getId()
                );

                var apiUpdatedNativeV4 = updateNativeApiDomainService.update(
                    apiDefinition.getId(),
                    toRollback -> {
                        // Rollback API from API definition without plans
                        var rollbackedApi = toRollback.rollbackTo(apiDefinition);
                        // update the description since the description is not stored in the definition
                        rollbackedApi.setDescription(api.getDescription());
                        keepStoredEncryption(toRollback.getApiDefinitionNativeV4(), rollbackedApi.getApiDefinitionNativeV4());
                        return rollbackedApi;
                    },
                    // no sanitizing: a rollback restores a definition that was already validated when it was deployed,
                    // which is what the HTTP branch gets too
                    (existing, updated) -> updated,
                    input.auditInfo,
                    primaryOwner,
                    new ApiIndexerDomainService.Context(input.auditInfo(), false)
                );

                // Rollback plans from API definition plans
                rollbackPlansV4(apiDefinition.getPlans(), apiUpdatedNativeV4, input.auditInfo);
                yield apiUpdatedNativeV4;
            }
            case null, default -> throw new IllegalStateException(
                "Cannot rollback this API: only V2 and V4 APIs are supported (%s)".formatted(input.eventId)
            );
        };

        createAuditLog(input.auditInfo, apiUpdated.getId(), apiUpdated.getUpdatedAt());
    }

    private void keepStoredEncryption(AbstractApi storedDefinition, AbstractApi restoredDefinition) {
        restoredDefinition.setProperties(
            propertyDomainService.encryptRestoredValuesOfEncryptedKeys(storedDefinition.getProperties(), restoredDefinition.getProperties())
        );
    }

    public record Input(String eventId, AuditInfo auditInfo) {}

    /**
     * Rolls plans back for both V4 API types: the plan domain services already dispatch on {@code api.isNative()}, so
     * only reading the definition's flows and building a core plan from it need to know the concrete type.
     */
    private void rollbackPlansV4(List<? extends AbstractPlan> apiDefinitionPlans, Api api, AuditInfo auditInfo) {
        if (apiDefinitionPlans == null) {
            return;
        }

        Set<io.gravitee.apim.core.plan.model.Plan> plansToAdd = new HashSet<>();
        Set<io.gravitee.apim.core.plan.model.Plan> plansToUpdate = new HashSet<>();

        Map<String, io.gravitee.apim.core.plan.model.Plan> existingPlans = planQueryService
            .findAllByApiId(api.getId())
            .stream()
            .collect(toMap(io.gravitee.apim.core.plan.model.Plan::getId, Function.identity()));

        for (AbstractPlan apiDefinitionPlan : apiDefinitionPlans) {
            io.gravitee.apim.core.plan.model.Plan existingPlan = apiDefinitionPlan.getId() != null
                ? existingPlans.get(apiDefinitionPlan.getId())
                : null;

            if (existingPlan == null) {
                // If plan not exist create new plan from API definition plan
                plansToAdd.add(corePlanOf(api.getId(), apiDefinitionPlan));
            } else {
                plansToUpdate.add(rollbackPlanTo(existingPlan, apiDefinitionPlan));
            }
        }

        // Add new plans
        plansToAdd.forEach(planToAdd -> createPlanDomainService.create(planToAdd, planFlowsOf(planToAdd), api, auditInfo));

        // Update existing plans
        // Set existingPlanStatuses to empty map because we don't check status for rollback. Allowing to rollback closed plans
        Map<String, PlanStatus> existingPlanStatuses = Map.of();
        plansToUpdate.forEach(planToUpdate ->
            updatePlanDomainService.update(planToUpdate, planFlowsOf(planToUpdate), existingPlanStatuses, api, auditInfo)
        );

        // Close plans that are not in the API definition
        existingPlans
            .values()
            .stream()
            .filter(
                existingPlan ->
                    existingPlan.getPlanStatus() != PlanStatus.CLOSED &&
                    !plansToUpdate
                        .stream()
                        .map(io.gravitee.apim.core.plan.model.Plan::getId)
                        .collect(toSet())
                        .contains(existingPlan.getId())
            )
            .forEach(existingPlan -> closePlanDomainService.close(existingPlan.getId(), auditInfo));
    }

    private static io.gravitee.apim.core.plan.model.Plan corePlanOf(String apiId, AbstractPlan apiDefinitionPlan) {
        return switch (apiDefinitionPlan) {
            case Plan httpV4 -> new io.gravitee.apim.core.plan.model.Plan(apiId, httpV4);
            case NativePlan nativeV4 -> new io.gravitee.apim.core.plan.model.Plan(apiId, nativeV4);
            default -> throw new IllegalStateException("Unsupported V4 plan definition: " + apiDefinitionPlan.getClass());
        };
    }

    private static io.gravitee.apim.core.plan.model.Plan rollbackPlanTo(
        io.gravitee.apim.core.plan.model.Plan existingPlan,
        AbstractPlan apiDefinitionPlan
    ) {
        return switch (apiDefinitionPlan) {
            case Plan httpV4 -> existingPlan.rollbackTo(httpV4);
            case NativePlan nativeV4 -> existingPlan.rollbackTo(nativeV4);
            default -> throw new IllegalStateException("Unsupported V4 plan definition: " + apiDefinitionPlan.getClass());
        };
    }

    private static List<? extends AbstractFlow> planFlowsOf(io.gravitee.apim.core.plan.model.Plan plan) {
        var nativeDefinition = plan.getPlanDefinitionNativeV4();
        if (nativeDefinition != null) {
            return nativeDefinition.getFlows() == null ? List.of() : nativeDefinition.getFlows();
        }
        var httpDefinition = plan.getPlanDefinitionHttpV4();
        return httpDefinition == null || httpDefinition.getFlows() == null ? List.of() : httpDefinition.getFlows();
    }

    private void createAuditLog(AuditInfo auditInfo, String apiId, ZonedDateTime auditCreatedAt) {
        this.auditService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(apiId)
                .event(ApiAuditEvent.API_ROLLBACKED)
                .actor(auditInfo.actor())
                .createdAt(auditCreatedAt)
                .properties(Collections.emptyMap())
                .build()
        );
    }

    private Plans rollbackPlansV2(List<io.gravitee.definition.model.Plan> apiDefinitionPlans, Api api, AuditInfo auditInfo) {
        if (apiDefinitionPlans == null) {
            return new Plans(List.of(), List.of());
        }

        var plansToUpdateById = stream(apiDefinitionPlans).collect(
            Collectors.toMap(io.gravitee.definition.model.Plan::getId, Function.identity())
        );

        var existingPlansMustBeRollbackOrClose = planQueryService
            .findAllByApiId(api.getId())
            .stream()
            .collect(
                Collectors.groupingBy(currentPlan -> {
                    var targetOfRollback = plansToUpdateById.get(currentPlan.getId());
                    if (targetOfRollback == null) {
                        return CLOSE;
                    } else if (
                        PlanStatus.valueOf(targetOfRollback.getStatus()) != PlanStatus.CLOSED &&
                        currentPlan.getPlanStatus() == PlanStatus.CLOSED
                    ) {
                        currentPlan.setPlanStatus(PlanStatus.valueOf(targetOfRollback.getStatus()));
                        return REOPEN;
                    }
                    return ROLLBACK;
                })
            );
        if (
            Stream.of(ROLLBACK, REOPEN)
                .mapToInt(c -> size(existingPlansMustBeRollbackOrClose.get(c)))
                .sum() <
            apiDefinitionPlans.size()
        ) {
            throw new IllegalStateException("Cannot rollback plans because some plans have been removed");
        }

        // Close plans
        existingPlansMustBeRollbackOrClose
            .getOrDefault(CLOSE, List.of())
            .forEach(plan -> {
                if (plan.getPlanStatus() != PlanStatus.CLOSED) {
                    closePlanDomainService.close(plan.getId(), auditInfo);
                }
            });

        // Rollback plans
        existingPlansMustBeRollbackOrClose
            .getOrDefault(ROLLBACK, List.of())
            .forEach(planToUpdate -> {
                var plan = plansToUpdateById.get(planToUpdate.getId());
                planCrudService.update(ROLLBACK_OPERATOR.rollback(planToUpdate, plan));
            });

        // Reopen plans
        existingPlansMustBeRollbackOrClose.getOrDefault(REOPEN, List.of()).forEach(planCrudService::update);
        var opens = Stream.concat(
            existingPlansMustBeRollbackOrClose.getOrDefault(ROLLBACK, List.of()).stream(),
            existingPlansMustBeRollbackOrClose.getOrDefault(REOPEN, List.of()).stream()
        )
            .map(io.gravitee.apim.core.plan.model.Plan::getId)
            .toList();
        var closes = existingPlansMustBeRollbackOrClose
            .getOrDefault(CLOSE, List.of())
            .stream()
            .map(io.gravitee.apim.core.plan.model.Plan::getId)
            .toList();
        return new Plans(opens, closes);
    }

    private record Plans(Collection<String> openPlans, Collection<String> closedPlans) {}
}
