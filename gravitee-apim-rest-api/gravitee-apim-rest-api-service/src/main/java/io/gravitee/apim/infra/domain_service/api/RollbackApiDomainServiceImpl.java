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
package io.gravitee.apim.infra.domain_service.api;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.RollbackApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.plan.domain_service.ClosePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiService;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class RollbackApiDomainServiceImpl implements RollbackApiDomainService {

    private final AuditDomainService auditService;
    private final EventCrudService eventCrudService;
    private final PlanQueryService planQueryService;
    private final CreatePlanDomainService createPlanDomainService;
    private final UpdatePlanDomainService updatePlanDomainService;
    private final ClosePlanDomainService closePlanDomainService;
    private final ApiCrudService apiCrudService;

    private final ApiService delegateApiService;

    public void rollback(String eventId, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

        Event event = eventCrudService.get(auditInfo.organizationId(), auditInfo.environmentId(), eventId);

        if (!event.getType().equals(EventType.PUBLISH_API) || event.getPayload() == null) {
            throw new IllegalStateException("Cannot rollback an event that is not a publish event!");
        }

        try {
            // Read API repository model from event
            var apiRepositoryModel = GraviteeJacksonMapper
                .getInstance()
                .readValue(event.getPayload(), io.gravitee.repository.management.model.Api.class);

            if (apiRepositoryModel.getDefinitionVersion() == null) {
                throw new IllegalStateException("Cannot determine API definition version from event" + eventId);
            }

            if (apiRepositoryModel.getDefinitionVersion().equals(DefinitionVersion.FEDERATED)) {
                throw new IllegalStateException("Cannot rollback a federated API");
            }

            if (!apiRepositoryModel.getDefinitionVersion().equals(DefinitionVersion.V4)) {
                // TODO: Support rollback for V2 APIs
                throw new IllegalStateException("Cannot rollback an API that is not a V4 API");
            } else {
                // Read API definition from API repository model
                var apiDefinition = GraviteeJacksonMapper
                    .getInstance()
                    .readValue(apiRepositoryModel.getDefinition(), io.gravitee.definition.model.v4.Api.class);

                var apiUpdated = rollbackApiV4(auditInfo, apiDefinition, executionContext);

                createAuditLog(auditInfo, apiUpdated.getId(), apiUpdated.getUpdatedAt());
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read API definition from event" + eventId, e);
        }
    }

    private Api rollbackApiV4(AuditInfo auditInfo, io.gravitee.definition.model.v4.Api apiDefinition, ExecutionContext executionContext) {
        var existingApiToUpdate = apiCrudService.get(apiDefinition.getId());

        // Rollback API from API definition
        existingApiToUpdate.setName(apiDefinition.getName());
        existingApiToUpdate.setVersion(apiDefinition.getApiVersion());
        existingApiToUpdate.setTag(apiDefinition.getTags());

        var existingApiDefinitionV4ToUpdate = existingApiToUpdate.getApiDefinitionV4();
        existingApiDefinitionV4ToUpdate.setTags(apiDefinition.getTags());
        existingApiDefinitionV4ToUpdate.setListeners(apiDefinition.getListeners());
        existingApiDefinitionV4ToUpdate.setEndpointGroups(apiDefinition.getEndpointGroups());
        existingApiDefinitionV4ToUpdate.setAnalytics(apiDefinition.getAnalytics());
        existingApiDefinitionV4ToUpdate.setProperties(apiDefinition.getProperties());
        existingApiDefinitionV4ToUpdate.setResources(apiDefinition.getResources());
        existingApiDefinitionV4ToUpdate.setFailover(apiDefinition.getFailover());
        existingApiDefinitionV4ToUpdate.setFlowExecution(apiDefinition.getFlowExecution());
        existingApiDefinitionV4ToUpdate.setFlows(apiDefinition.getFlows());
        existingApiDefinitionV4ToUpdate.setResponseTemplates(apiDefinition.getResponseTemplates());
        existingApiDefinitionV4ToUpdate.setServices(apiDefinition.getServices());

        // Ignore plans update with API update
        existingApiDefinitionV4ToUpdate.setPlans(null);

        var updateApiEntity = ApiAdapter.INSTANCE.toUpdateApiEntity(existingApiToUpdate, existingApiDefinitionV4ToUpdate);

        this.delegateApiService.update(executionContext, existingApiToUpdate.getId(), updateApiEntity, false, auditInfo.actor().userId());

        var apiUpdated = apiCrudService.get(apiDefinition.getId());

        // Rollback plans from API definition plans
        rollbackPlansV4(apiDefinition.getPlans(), apiUpdated, auditInfo);

        return apiUpdated;
    }

    private void rollbackPlansV4(List<io.gravitee.definition.model.v4.plan.Plan> apiDefinitionPlans, Api api, AuditInfo auditInfo) {
        if (apiDefinitionPlans == null) {
            return;
        }

        Set<Plan> plansToAdd = new HashSet<>();
        Set<Plan> plansToUpdate = new HashSet<>();

        Map<String, Plan> existingPlans = planQueryService
            .findAllByApiId(api.getId())
            .stream()
            .collect(toMap(Plan::getId, Function.identity()));

        for (io.gravitee.definition.model.v4.plan.Plan apiDefinitionPlan : apiDefinitionPlans) {
            Plan existingPlan = apiDefinitionPlan.getId() != null ? existingPlans.get(apiDefinitionPlan.getId()) : null;

            if (existingPlan == null) {
                // If plan not exist create new plan from API definition plan
                var planToAdd = PlanAdapter.INSTANCE.fromPlanV4(apiDefinitionPlan);
                planToAdd.setType(Plan.PlanType.API);
                planToAdd.setApiId(api.getId());
                plansToAdd.add(planToAdd);
            } else {
                var existingPlanDefinitionV4 = existingPlan.getPlanDefinitionV4();

                // Update plan properties from API definition
                existingPlanDefinitionV4.setName(apiDefinitionPlan.getName());
                existingPlan.setName(apiDefinitionPlan.getName());
                existingPlanDefinitionV4.setTags(apiDefinitionPlan.getTags());
                existingPlanDefinitionV4.setSecurity(apiDefinitionPlan.getSecurity());
                existingPlanDefinitionV4.setFlows(apiDefinitionPlan.getFlows());
                existingPlanDefinitionV4.setSelectionRule(apiDefinitionPlan.getSelectionRule());

                // Special case if plan are closed or deprecated we restore status from API definition
                if (existingPlan.getPlanStatus() != apiDefinitionPlan.getStatus()) {
                    existingPlanDefinitionV4.setStatus(apiDefinitionPlan.getStatus());
                    existingPlan.setClosedAt(null);
                    existingPlan.setUpdatedAt(TimeProvider.now());
                    existingPlan.setNeedRedeployAt(Date.from(existingPlan.getUpdatedAt().toInstant()));
                }

                plansToUpdate.add(existingPlan);
            }
        }

        // Add new plans
        plansToAdd.forEach(planToAdd ->
            createPlanDomainService.create(
                planToAdd,
                planToAdd.getPlanDefinitionV4().getFlows() == null ? List.of() : planToAdd.getPlanDefinitionV4().getFlows(),
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
                planToUpdate.getPlanDefinitionV4().getFlows() == null ? List.of() : planToUpdate.getPlanDefinitionV4().getFlows(),
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
                !plansToUpdate.stream().map(Plan::getId).collect(toSet()).contains(existingPlan.getId())
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
