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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.DeployApiDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRD;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeletePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ImportCRDUseCase {

    private final ApiQueryService apiQueryService;
    private final CreateApiDomainService createApiDomainService;
    private final CreatePlanDomainService createPlanDomainService;
    private final ApiMetadataDomainService apiMetadataDomainService;
    private final DeployApiDomainService deployApiDomainService;
    private final UpdateApiDomainService updateApiDomainService;
    private final ApiCrudService apiCrudService;
    private final PlanQueryService planQueryService;
    private final UpdatePlanDomainService updatePlanDomainService;
    private final DeletePlanDomainService deletePlanDomainService;
    private final SubscriptionQueryService subscriptionQueryService;
    private final CloseSubscriptionDomainService closeSubscriptionDomainService;
    private final ReorderPlanDomainService reorderPlanDomainService;

    public ImportCRDUseCase(
        ApiCrudService apiCrudService,
        ApiQueryService apiQueryService,
        CreateApiDomainService createApiDomainService,
        CreatePlanDomainService createPlanDomainService,
        ApiMetadataDomainService apiMetadataDomainService,
        DeployApiDomainService deployApiDomainService,
        UpdateApiDomainService updateApiDomainService,
        PlanQueryService planQueryService,
        UpdatePlanDomainService updatePlanDomainService,
        DeletePlanDomainService deletePlanDomainService,
        SubscriptionQueryService subscriptionQueryService,
        CloseSubscriptionDomainService closeSubscriptionDomainService,
        ReorderPlanDomainService reorderPlanDomainService
    ) {
        this.apiCrudService = apiCrudService;
        this.apiQueryService = apiQueryService;
        this.createApiDomainService = createApiDomainService;
        this.createPlanDomainService = createPlanDomainService;
        this.apiMetadataDomainService = apiMetadataDomainService;
        this.deployApiDomainService = deployApiDomainService;
        this.updateApiDomainService = updateApiDomainService;
        this.planQueryService = planQueryService;
        this.updatePlanDomainService = updatePlanDomainService;
        this.deletePlanDomainService = deletePlanDomainService;
        this.subscriptionQueryService = subscriptionQueryService;
        this.closeSubscriptionDomainService = closeSubscriptionDomainService;
        this.reorderPlanDomainService = reorderPlanDomainService;
    }

    public record Output(ApiCRDStatus status) {}

    public record Input(AuditInfo auditInfo, ApiCRD crd) {}

    public Output execute(Input input) {
        var api = apiQueryService.findByEnvironmentIdAndCrossId(input.auditInfo.environmentId(), input.crd.getCrossId());

        var status = api.map(exiting -> this.update(input, exiting)).orElseGet(() -> this.create(input));

        return new Output(status);
    }

    private ApiCRDStatus create(Input input) {
        try {
            var api = createApiDomainService.create(input.crd, input.auditInfo);
            apiMetadataDomainService.saveApiMetadata(api.getId(), input.crd.getMetadata(), input.auditInfo);

            var planNameIdMapping = input.crd
                .getPlans()
                .entrySet()
                .stream()
                .map(entry ->
                    Map.entry(
                        entry.getKey(),
                        createPlanDomainService
                            .create(initPlanFromCRD(entry.getValue()), entry.getValue().getFlows(), api, input.auditInfo)
                            .getId()
                    )
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (input.crd.getDefinitionContext().getSyncFrom().equals(DefinitionContext.ORIGIN_MANAGEMENT)) {
                deployApiDomainService.deploy(api, "Import via Kubernetes operator", input.auditInfo);
            }

            return ApiCRDStatus
                .builder()
                .id(api.getId())
                .crossId(api.getCrossId())
                .environmentId(input.auditInfo.environmentId())
                .organizationId(input.auditInfo.organizationId())
                .state(api.getLifecycleState().name())
                .plans(planNameIdMapping)
                .build();
        } catch (Exception e) {
            throw new TechnicalManagementException(e);
        }
    }

    private ApiCRDStatus update(Input input, Api existingApi) {
        try {
            var updated = updateApiDomainService.update(existingApi.getId(), input.crd, input.auditInfo);
            // update state and definition context because legacy service does not update it
            var api = apiCrudService.update(
                updated
                    .toBuilder()
                    .definitionContext(input.crd().getDefinitionContext())
                    .lifecycleState(Api.LifecycleState.valueOf(input.crd().getState()))
                    .build()
            );
            apiMetadataDomainService.saveApiMetadata(api.getId(), input.crd.getMetadata(), input.auditInfo);

            List<Plan> existingPlans = planQueryService.findAllByApiId(api.getId());
            Map<String, PlanStatus> existingPlanStatuses = existingPlans.stream().collect(toMap(Plan::getId, Plan::getStatus));

            var planKeyIdMapping = input
                .crd()
                .getPlans()
                .entrySet()
                .stream()
                .map(entry -> {
                    var key = entry.getKey();
                    var plan = entry.getValue();

                    if (existingPlanStatuses.containsKey(plan.getId())) {
                        return Map.entry(
                            key,
                            updatePlanDomainService
                                .update(initPlanFromCRD(plan), plan.getFlows(), existingPlanStatuses, api, input.auditInfo)
                                .getId()
                        );
                    }

                    return Map.entry(
                        key,
                        createPlanDomainService.create(initPlanFromCRD(plan), plan.getFlows(), api, input.auditInfo).getId()
                    );
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            deletePlans(api, existingPlans, planKeyIdMapping, input);

            if (input.crd.getDefinitionContext().getSyncFrom().equals(DefinitionContext.ORIGIN_MANAGEMENT)) {
                deployApiDomainService.deploy(api, "Import via Kubernetes operator", input.auditInfo);
            }

            return ApiCRDStatus
                .builder()
                .id(api.getId())
                .crossId(api.getCrossId())
                .environmentId(api.getEnvironmentId())
                .organizationId(input.auditInfo.organizationId())
                .state(api.getLifecycleState().name())
                .plans(planKeyIdMapping)
                .build();
        } catch (Exception e) {
            throw new TechnicalManagementException(e);
        }
    }

    private void deletePlans(Api api, List<Plan> existingPlans, Map<String, String> planKeyIdMapping, Input input) {
        var plansToDelete = existingPlans
            .stream()
            .filter(plan ->
                // Ignore already processed plans
                !planKeyIdMapping.containsValue(plan.getId())
            )
            .filter(plan ->
                // Keep existing plans that are not in the CRD
                !input.crd.getPlans().containsKey(plan.getId())
            )
            .toList();
        plansToDelete.forEach(plan -> {
            subscriptionQueryService
                .findActiveSubscriptionsByPlan(plan.getId())
                .forEach(subscription -> closeSubscriptionDomainService.closeSubscription(subscription.getId(), input.auditInfo));

            deletePlanDomainService.delete(plan, input.auditInfo);
        });

        reorderPlanDomainService.refreshOrderAfterDelete(api.getId());
    }

    private Plan initPlanFromCRD(PlanCRD planCRD) {
        return Plan
            .builder()
            .id(planCRD.getId())
            .name(planCRD.getName())
            .description(planCRD.getDescription())
            .security(planCRD.getSecurity())
            .characteristics(planCRD.getCharacteristics())
            .commentMessage(planCRD.getCommentMessage())
            .commentRequired(planCRD.isCommentRequired())
            .crossId(planCRD.getCrossId())
            .excludedGroups(planCRD.getExcludedGroups())
            .generalConditions(planCRD.getGeneralConditions())
            .order(planCRD.getOrder())
            .publishedAt(planCRD.getPublishedAt())
            .selectionRule(planCRD.getSelectionRule())
            .status(planCRD.getStatus())
            .tags(planCRD.getTags())
            .type(planCRD.getType())
            .validation(planCRD.getValidation())
            .mode(planCRD.getMode())
            .build();
    }
}
