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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.core.plan.domain_service.ClosePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class RollbackApiUseCase {

    private final EventQueryService eventQueryService;
    private final ApiCrudService apiCrudService;
    private final UpdateApiDomainService updateApiDomainService;
    private final PlanQueryService planQueryService;
    private final CreatePlanDomainService createPlanDomainService;
    private final UpdatePlanDomainService updatePlanDomainService;
    private final ClosePlanDomainService closePlanDomainService;
    private final AuditDomainService auditService;

    public void execute(Input input) {
        Api api = eventQueryService
            .findApiFromPublishApiEvent(input.eventId)
            .orElseThrow(() -> new IllegalStateException("Cannot rollback an event that is not a publish event!"));
        ensureApiDefinitionVersionForRollback(api, input.eventId());

        var apiDefinition = api.getApiDefinitionV4();

        var toRollback = apiCrudService.get(apiDefinition.getId());

        // Rollback API from API definition without plans
        var rollbackedApi = toRollback.rollbackTo(apiDefinition);
        var apiUpdated = updateApiDomainService.updateV4(rollbackedApi, input.auditInfo);

        // Rollback plans from API definition plans
        rollbackPlansV4(apiDefinition.getPlans(), apiUpdated, input.auditInfo);

        createAuditLog(input.auditInfo, apiUpdated.getId(), apiUpdated.getUpdatedAt());
    }

    public record Input(String eventId, AuditInfo auditInfo) {}

    private void ensureApiDefinitionVersionForRollback(Api api, String eventId) {
        if (api.getDefinitionVersion() == null) {
            throw new IllegalStateException("Cannot determine API definition version from event" + eventId);
        }

        if (api.getDefinitionVersion().equals(DefinitionVersion.FEDERATED)) {
            throw new IllegalStateException("Cannot rollback a federated API");
        }

        if (!api.getDefinitionVersion().equals(DefinitionVersion.V4)) {
            // TODO: Support rollback for V2 APIs
            throw new IllegalStateException("Cannot rollback an API that is not a V4 API");
        }
    }

    private void rollbackPlansV4(List<Plan> apiDefinitionPlans, Api api, AuditInfo auditInfo) {
        if (apiDefinitionPlans == null) {
            return;
        }

        Set<io.gravitee.apim.core.plan.model.Plan> plansToAdd = new HashSet<>();
        Set<io.gravitee.apim.core.plan.model.Plan> plansToUpdate = new HashSet<>();

        Map<String, io.gravitee.apim.core.plan.model.Plan> existingPlans = planQueryService
            .findAllByApiId(api.getId())
            .stream()
            .collect(toMap(io.gravitee.apim.core.plan.model.Plan::getId, Function.identity()));

        for (io.gravitee.definition.model.v4.plan.Plan apiDefinitionPlan : apiDefinitionPlans) {
            io.gravitee.apim.core.plan.model.Plan existingPlan = apiDefinitionPlan.getId() != null
                ? existingPlans.get(apiDefinitionPlan.getId())
                : null;

            if (existingPlan == null) {
                // If plan not exist create new plan from API definition plan
                plansToAdd.add(new io.gravitee.apim.core.plan.model.Plan(api.getId(), apiDefinitionPlan));
            } else {
                plansToUpdate.add(existingPlan.rollbackTo(apiDefinitionPlan));
            }
        }

        // Add new plans
        plansToAdd.forEach(planToAdd ->
            createPlanDomainService.create(
                planToAdd,
                planToAdd.getPlanDefinitionHttpV4().getFlows() == null ? List.of() : planToAdd.getPlanDefinitionHttpV4().getFlows(),
                api,
                auditInfo
            )
        );

        // Update existing plans
        // Set existingPlanStatuses to empty map because we don't check status for rollback. Allowing to rollback closed plans
        Map<String, PlanStatus> existingPlanStatuses = Map.of();
        plansToUpdate.forEach(planToUpdate ->
            updatePlanDomainService.update(
                planToUpdate,
                planToUpdate.getPlanDefinitionHttpV4().getFlows() == null ? List.of() : planToUpdate.getPlanDefinitionHttpV4().getFlows(),
                existingPlanStatuses,
                api,
                auditInfo
            )
        );

        // Close plans that are not in the API definition
        existingPlans
            .values()
            .stream()
            .filter(existingPlan ->
                existingPlan.getPlanStatus() != PlanStatus.CLOSED &&
                !plansToUpdate.stream().map(io.gravitee.apim.core.plan.model.Plan::getId).collect(toSet()).contains(existingPlan.getId())
            )
            .forEach(existingPlan -> closePlanDomainService.close(existingPlan.getId(), auditInfo));
    }

    private void createAuditLog(AuditInfo auditInfo, String apiId, ZonedDateTime auditCreatedAt) {
        this.auditService.createApiAuditLog(
                ApiAuditLogEntity
                    .builder()
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
}
