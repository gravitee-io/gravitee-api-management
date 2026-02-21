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

import static io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService.oneShotIndexation;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.NotificationCRDDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiCRDDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.api.model.crd.PageCRD;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageParentException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.factory.PageModelFactory;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.exception.AbstractDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.member.domain_service.CRDMembersDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeletePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.CustomLog;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@CustomLog
public class ImportApiCRDUseCase {

    private final ApiQueryService apiQueryService;
    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final ValidateApiDomainService validateApiDomainService;
    private final CreateApiDomainService createApiDomainService;
    private final CreatePlanDomainService createPlanDomainService;
    private final ApiStateDomainService apiStateDomainService;
    private final UpdateApiDomainService updateApiDomainService;
    private final UpdateNativeApiUseCase updateNativeApiUseCase;
    private final ApiCrudService apiCrudService;
    private final PlanQueryService planQueryService;
    private final PageQueryService pageQueryService;
    private final PageCrudService pageCrudService;
    private final UpdatePlanDomainService updatePlanDomainService;
    private final DeletePlanDomainService deletePlanDomainService;
    private final SubscriptionQueryService subscriptionQueryService;
    private final CloseSubscriptionDomainService closeSubscriptionDomainService;
    private final ReorderPlanDomainService reorderPlanDomainService;
    private final CRDMembersDomainService membersDomainService;
    private final ApiMetadataDomainService apiMetadataDomainService;
    private final CreateApiDocumentationDomainService createApiDocumentationDomainService;
    private final UpdateApiDocumentationDomainService updateApiDocumentationDomainService;
    private final ValidateApiCRDDomainService validateCRDDomainService;
    private final NotificationCRDDomainService notificationCRDService;

    public ImportApiCRDUseCase(
        ApiCrudService apiCrudService,
        ApiQueryService apiQueryService,
        ApiPrimaryOwnerFactory apiPrimaryOwnerFactory,
        ValidateApiDomainService validateApiDomainService,
        CreateApiDomainService createApiDomainService,
        CreatePlanDomainService createPlanDomainService,
        ApiStateDomainService apiStateDomainService,
        UpdateApiDomainService updateApiDomainService,
        UpdateNativeApiUseCase updateNativeApiUseCase,
        PlanQueryService planQueryService,
        UpdatePlanDomainService updatePlanDomainService,
        DeletePlanDomainService deletePlanDomainService,
        SubscriptionQueryService subscriptionQueryService,
        CloseSubscriptionDomainService closeSubscriptionDomainService,
        ReorderPlanDomainService reorderPlanDomainService,
        CRDMembersDomainService membersDomainService,
        ApiMetadataDomainService apiMetadataDomainService,
        PageQueryService pageQueryService,
        PageCrudService pageCrudService,
        CreateApiDocumentationDomainService createApiDocumentationDomainService,
        UpdateApiDocumentationDomainService updateApiDocumentationDomainService,
        ValidateApiCRDDomainService validateCRDDomainService,
        NotificationCRDDomainService notificationCRDService
    ) {
        this.apiCrudService = apiCrudService;
        this.apiQueryService = apiQueryService;
        this.apiPrimaryOwnerFactory = apiPrimaryOwnerFactory;
        this.validateApiDomainService = validateApiDomainService;
        this.createApiDomainService = createApiDomainService;
        this.createPlanDomainService = createPlanDomainService;
        this.apiStateDomainService = apiStateDomainService;
        this.updateApiDomainService = updateApiDomainService;
        this.updateNativeApiUseCase = updateNativeApiUseCase;
        this.planQueryService = planQueryService;
        this.updatePlanDomainService = updatePlanDomainService;
        this.deletePlanDomainService = deletePlanDomainService;
        this.subscriptionQueryService = subscriptionQueryService;
        this.closeSubscriptionDomainService = closeSubscriptionDomainService;
        this.reorderPlanDomainService = reorderPlanDomainService;
        this.membersDomainService = membersDomainService;
        this.apiMetadataDomainService = apiMetadataDomainService;
        this.pageQueryService = pageQueryService;
        this.pageCrudService = pageCrudService;
        this.createApiDocumentationDomainService = createApiDocumentationDomainService;
        this.updateApiDocumentationDomainService = updateApiDocumentationDomainService;
        this.validateCRDDomainService = validateCRDDomainService;
        this.notificationCRDService = notificationCRDService;
    }

    public record Output(ApiCRDStatus status) {}

    public record Input(AuditInfo auditInfo, ApiCRDSpec spec) {}

    public Output execute(Input input) {
        var validationResult = validateCRDDomainService
            .validateAndSanitize(new ValidateApiCRDDomainService.Input(input.auditInfo(), input.spec()))
            .map(sanitized -> new Input(sanitized.auditInfo(), sanitized.spec()));

        validationResult
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(
                    String.format(
                        "Unable to import because of errors [%s]",
                        String.join(",", errors.stream().map(Validator.Error::getMessage).toList())
                    )
                );
            });

        var warnings = validationResult.warning().orElseGet(List::of);
        var sanitizedInput = validationResult.value().orElseThrow(() -> new ValidationDomainException("Unable to sanitize CRD spec"));

        var api = apiQueryService.findByEnvironmentIdAndCrossId(sanitizedInput.auditInfo.environmentId(), sanitizedInput.spec.getCrossId());

        var status = api.map(exiting -> this.update(sanitizedInput, exiting)).orElseGet(() -> this.create(sanitizedInput));
        status.setErrors(ApiCRDStatus.Errors.fromErrorList(warnings));

        return new Output(status);
    }

    private ApiCRDStatus create(Input input) {
        try {
            String environmentId = input.auditInfo.environmentId();
            String organizationId = input.auditInfo.organizationId();

            var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(organizationId, environmentId, input.auditInfo.actor().userId());

            var createdApi = createApiDomainService.create(
                ApiModelFactory.fromCrd(input.spec, environmentId),
                primaryOwner,
                input.auditInfo,
                api -> validateApiDomainService.validateAndSanitizeForCreation(api, primaryOwner, environmentId, organizationId),
                oneShotIndexation(input.auditInfo)
            );

            var planNameIdMapping = input.spec
                .getPlans()
                .entrySet()
                .stream()
                .map(entry ->
                    Map.entry(
                        entry.getKey(),
                        createPlanDomainService
                            .create(
                                initPlanFromCRD(entry.getKey(), entry.getValue(), createdApi),
                                entry.getValue().getFlows(),
                                createdApi,
                                input.auditInfo
                            )
                            .getId()
                    )
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            membersDomainService.updateApiMembers(input.auditInfo, createdApi.getId(), input.spec().getMembers());

            createOrUpdatePages(input.spec.getPages(), createdApi.getId(), input.auditInfo);

            apiMetadataDomainService.importApiMetadata(createdApi.getId(), input.spec.getMetadata(), input.auditInfo);

            notificationCRDService.syncApiPortalNotifications(
                createdApi.getId(),
                input.auditInfo.actor().userId(),
                input.spec.getConsoleNotificationConfiguration()
            );

            if (shouldDeploy(input.spec())) {
                // This will also DEPLOYS the API because it is the first time it has been started
                apiStateDomainService.start(createdApi, input.auditInfo);
            }

            return ApiCRDStatus.builder()
                .id(createdApi.getId())
                .crossId(createdApi.getCrossId())
                .environmentId(environmentId)
                .organizationId(organizationId)
                .state(createdApi.getLifecycleState().name())
                .plans(planNameIdMapping)
                .build();
        } catch (AbstractDomainException e) {
            throw e;
        } catch (Exception e) {
            throw new TechnicalManagementException(e);
        }
    }

    private ApiCRDStatus update(Input input, Api existingApi) {
        try {
            Api updatedApi;
            if (existingApi.isNative()) {
                updatedApi = updateNativeApiUseCase
                    .execute(new UpdateNativeApiUseCase.Input(ApiModelFactory.toUpdateNativeApi(input.spec), input.auditInfo))
                    .updatedApi();
            } else {
                updatedApi = updateApiDomainService.update(existingApi.getId(), input.spec, input.auditInfo);
            }
            // update state and definition context because legacy service does not update it
            // Why are we getting MANAGEMENT as an origin here ? the API has been saved as kubernetes before
            var api = apiCrudService.update(
                updatedApi
                    .toBuilder()
                    .originContext(
                        new OriginContext.Kubernetes(
                            OriginContext.Kubernetes.Mode.valueOf(input.spec().getDefinitionContext().getMode().toUpperCase()),
                            input.spec().getDefinitionContext().getSyncFrom().toUpperCase()
                        )
                    )
                    .lifecycleState(Api.LifecycleState.valueOf(input.spec().getState()))
                    .build()
            );

            // Pages
            createOrUpdatePages(input.spec.getPages(), updatedApi.getId(), input.auditInfo);
            deleteRemovedPages(input.spec.getPages(), updatedApi.getId());

            // Plans
            Map<String, String> planKeyIdMapping = handlePlanUpdate(input, api);

            // Deploy ?
            handleLifeCycle(input, existingApi, api);

            // Members
            membersDomainService.updateApiMembers(input.auditInfo, updatedApi.getId(), input.spec().getMembers());

            // Metadata
            apiMetadataDomainService.importApiMetadata(api.getId(), input.spec.getMetadata(), input.auditInfo);

            // Notifications
            notificationCRDService.syncApiPortalNotifications(
                api.getId(),
                input.auditInfo.actor().userId(),
                input.spec.getConsoleNotificationConfiguration()
            );

            return ApiCRDStatus.builder()
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

    private void handleLifeCycle(Input input, Api existingApi, Api api) {
        if (shouldDeploy(input.spec())) {
            apiStateDomainService.deploy(api, "Updated by GKO", input.auditInfo);
        }

        if (api.getLifecycleState() != existingApi.getLifecycleState()) {
            if (api.getLifecycleState() == Api.LifecycleState.STOPPED) {
                apiStateDomainService.stop(api, input.auditInfo);
            } else {
                apiStateDomainService.start(api, input.auditInfo);
            }
        }
    }

    private Map<String, String> handlePlanUpdate(Input input, Api api) {
        List<Plan> existingPlans = planQueryService.findAllByReferenceIdAndReferenceType(
            api.getId(),
            GenericPlanEntity.ReferenceType.API.name()
        );
        Map<String, PlanStatus> existingPlanStatuses = existingPlans.stream().collect(toMap(Plan::getId, Plan::getPlanStatus));
        Map<String, String> keyToIdMapping = new HashMap<>();

        Map<String, List<? extends AbstractFlow>> updateFlows = new HashMap<>();
        var plansToUpdate = input
            .spec()
            .getPlans()
            .entrySet()
            .stream()
            .filter(e -> existingPlanStatuses.containsKey(e.getValue().getId()))
            .map(e -> {
                updateFlows.put(e.getValue().getId(), e.getValue().getFlows());
                keyToIdMapping.put(e.getKey(), e.getValue().getId());
                return initPlanFromCRD(e.getKey(), e.getValue(), api);
            })
            .toList();
        updatePlanDomainService.bulkUpdate(plansToUpdate, existingPlanStatuses, updateFlows, api, input.auditInfo);

        input
            .spec()
            .getPlans()
            .entrySet()
            .stream()
            .filter(e -> !existingPlanStatuses.containsKey(e.getValue().getId()))
            .forEach(e -> {
                var plan = initPlanFromCRD(e.getKey(), e.getValue(), api);
                var created = createPlanDomainService.create(plan, e.getValue().getFlows(), api, input.auditInfo);
                keyToIdMapping.put(e.getKey(), created.getId());
            });

        var plansToDelete = existingPlans
            .stream()
            // retain plans that cannot be found in the CRD spec
            .filter(plan ->
                input.spec
                    .getPlans()
                    .values()
                    .stream()
                    .noneMatch(p -> p.getId().equals(plan.getId()))
            )
            .toList();
        deletePlans(plansToDelete, api, input.auditInfo);

        return keyToIdMapping;
    }

    private void deletePlans(List<Plan> plansToDelete, Api api, AuditInfo auditInfo) {
        plansToDelete.forEach(plan -> {
            subscriptionQueryService
                .findActiveSubscriptionsByPlan(plan.getId())
                .forEach(subscription -> closeSubscriptionDomainService.closeSubscription(subscription.getId(), api, auditInfo));

            deletePlanDomainService.delete(plan, auditInfo);
        });

        reorderPlanDomainService.refreshOrderAfterDelete(api.getId(), GenericPlanEntity.ReferenceType.API.name());
    }

    private Plan initPlanFromCRD(String hrid, PlanCRD planCRD, Api api) {
        Plan plan = Plan.builder()
            .id(planCRD.getId())
            .hrid(hrid)
            .name(planCRD.getName())
            .description(planCRD.getDescription())
            .characteristics(planCRD.getCharacteristics())
            .definitionVersion(api.getDefinitionVersion())
            .crossId(planCRD.getCrossId())
            .excludedGroups(planCRD.getExcludedGroups())
            .generalConditions(planCRD.getGeneralConditions())
            .generalConditionsHrid(planCRD.getGeneralConditionsHrid())
            .order(planCRD.getOrder())
            .type(planCRD.getType())
            .validation(planCRD.getValidation())
            .apiType(api.getType())
            .apiId(api.getId())
            .referenceId(api.getId())
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .build();

        if (ApiType.NATIVE.equals(api.getType())) {
            plan.setPlanDefinitionNativeV4(
                NativePlan.builder()
                    .security(planCRD.getSecurity())
                    .selectionRule(planCRD.getSelectionRule())
                    .status(planCRD.getStatus())
                    .tags(planCRD.getTags())
                    .mode(planCRD.getMode())
                    .name(planCRD.getName())
                    .build()
            );
        } else {
            plan.setPlanDefinitionHttpV4(
                io.gravitee.definition.model.v4.plan.Plan.builder()
                    .security(planCRD.getSecurity())
                    .selectionRule(planCRD.getSelectionRule())
                    .status(planCRD.getStatus())
                    .tags(planCRD.getTags())
                    .mode(planCRD.getMode())
                    .name(planCRD.getName())
                    .build()
            );
        }

        return plan;
    }

    private void deleteRemovedPages(Map<String, PageCRD> pages, String apiId) {
        var existingPageIds = pageQueryService.searchByApiId(apiId).stream().map(Page::getId).collect(toSet());
        if (pages != null && !pages.isEmpty()) {
            var givenPageIds = pages.values().stream().map(PageCRD::getId).collect(toSet());
            existingPageIds.removeIf(givenPageIds::contains);
        }

        try {
            for (var id : existingPageIds) {
                pageCrudService.delete(id);
            }
        } catch (RuntimeException e) {
            log.error("An error as occurred while trying to remove a page with kubernetes origin");
        }
    }

    private void createOrUpdatePages(Map<String, PageCRD> pageCRDs, String apiId, AuditInfo auditInfo) {
        if (pageCRDs == null || pageCRDs.isEmpty()) {
            return;
        }

        var now = Date.from(TimeProvider.now().toInstant());
        var pages = pageCRDs
            .entrySet()
            .stream()
            .map(entry -> PageModelFactory.fromCRDSpec(entry.getKey(), entry.getValue()))
            .toList();

        pages.forEach(page -> {
            page.setReferenceId(apiId);
            page.setReferenceType(Page.ReferenceType.API);
            if (page.getParentId() != null) {
                validatePageParent(pages, page.getParentId());
            }

            pageCrudService
                .findById(page.getId())
                .ifPresentOrElse(
                    oldPage ->
                        updateApiDocumentationDomainService.updatePage(
                            page.toBuilder().createdAt(oldPage.getCreatedAt()).updatedAt(now).build(),
                            oldPage,
                            auditInfo
                        ),
                    () -> createApiDocumentationDomainService.createPage(page.toBuilder().createdAt(now).updatedAt(now).build(), auditInfo)
                );
        });
    }

    private void validatePageParent(List<Page> pages, String parentId) {
        pages
            .stream()
            .filter(page -> parentId.equals(page.getId()))
            .findFirst()
            .ifPresent(parent -> {
                if (!(parent.isFolder() || parent.isRoot())) {
                    throw new InvalidPageParentException(parent.getId());
                }
            });
    }

    private static boolean shouldDeploy(ApiCRDSpec spec) {
        return (
            spec.getDefinitionContext().isSyncFromManagement() &&
            Api.LifecycleState.STARTED.name().equalsIgnoreCase(spec.getState()) &&
            spec
                .getPlans()
                .values()
                .stream()
                .anyMatch(plan -> plan.getStatus() == PlanStatus.PUBLISHED || plan.getStatus() == PlanStatus.DEPRECATED)
        );
    }
}
