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
import io.gravitee.apim.core.api.domain_service.ApiImportDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiCRDDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.api.model.crd.MemberCRD;
import io.gravitee.apim.core.api.model.crd.PageCRD;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.apim.core.api.model.import_definition.ApiMemberRole;
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
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeletePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@Slf4j
public class ImportApiCRDUseCase {

    private final ApiQueryService apiQueryService;
    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final ValidateApiDomainService validateApiDomainService;
    private final CreateApiDomainService createApiDomainService;
    private final CreatePlanDomainService createPlanDomainService;
    private final ApiStateDomainService apiStateDomainService;
    private final UpdateApiDomainService updateApiDomainService;
    private final ApiCrudService apiCrudService;
    private final PlanQueryService planQueryService;
    private final PageQueryService pageQueryService;
    private final PageCrudService pageCrudService;
    private final UpdatePlanDomainService updatePlanDomainService;
    private final DeletePlanDomainService deletePlanDomainService;
    private final SubscriptionQueryService subscriptionQueryService;
    private final CloseSubscriptionDomainService closeSubscriptionDomainService;
    private final ReorderPlanDomainService reorderPlanDomainService;
    private final ApiImportDomainService apiImportDomainService;
    private final ApiPrimaryOwnerDomainService primaryOwnerDomainService;
    private final MembershipCrudService membershipCrudService;
    private final MembershipQueryService membershipQueryService;
    private final ApiMetadataDomainService apiMetadataDomainService;
    private final CreateApiDocumentationDomainService createApiDocumentationDomainService;
    private final UpdateApiDocumentationDomainService updateApiDocumentationDomainService;
    private final ValidateApiCRDDomainService validateCRDDomainService;

    public ImportApiCRDUseCase(
        ApiCrudService apiCrudService,
        ApiQueryService apiQueryService,
        ApiPrimaryOwnerFactory apiPrimaryOwnerFactory,
        ValidateApiDomainService validateApiDomainService,
        CreateApiDomainService createApiDomainService,
        CreatePlanDomainService createPlanDomainService,
        ApiStateDomainService apiStateDomainService,
        UpdateApiDomainService updateApiDomainService,
        PlanQueryService planQueryService,
        UpdatePlanDomainService updatePlanDomainService,
        DeletePlanDomainService deletePlanDomainService,
        SubscriptionQueryService subscriptionQueryService,
        CloseSubscriptionDomainService closeSubscriptionDomainService,
        ReorderPlanDomainService reorderPlanDomainService,
        ApiImportDomainService apiImportDomainService,
        ApiPrimaryOwnerDomainService primaryOwnerDomainService,
        MembershipCrudService membershipCrudService,
        MembershipQueryService membershipQueryService,
        ApiMetadataDomainService apiMetadataDomainService,
        PageQueryService pageQueryService,
        PageCrudService pageCrudService,
        CreateApiDocumentationDomainService createApiDocumentationDomainService,
        UpdateApiDocumentationDomainService updateApiDocumentationDomainService,
        ValidateApiCRDDomainService validateCRDDomainService
    ) {
        this.apiCrudService = apiCrudService;
        this.apiQueryService = apiQueryService;
        this.apiPrimaryOwnerFactory = apiPrimaryOwnerFactory;
        this.validateApiDomainService = validateApiDomainService;
        this.createApiDomainService = createApiDomainService;
        this.createPlanDomainService = createPlanDomainService;
        this.apiStateDomainService = apiStateDomainService;
        this.updateApiDomainService = updateApiDomainService;
        this.planQueryService = planQueryService;
        this.updatePlanDomainService = updatePlanDomainService;
        this.deletePlanDomainService = deletePlanDomainService;
        this.subscriptionQueryService = subscriptionQueryService;
        this.closeSubscriptionDomainService = closeSubscriptionDomainService;
        this.reorderPlanDomainService = reorderPlanDomainService;
        this.apiImportDomainService = apiImportDomainService;
        this.primaryOwnerDomainService = primaryOwnerDomainService;
        this.membershipCrudService = membershipCrudService;
        this.membershipQueryService = membershipQueryService;
        this.apiMetadataDomainService = apiMetadataDomainService;
        this.pageQueryService = pageQueryService;
        this.pageCrudService = pageCrudService;
        this.createApiDocumentationDomainService = createApiDocumentationDomainService;
        this.updateApiDocumentationDomainService = updateApiDocumentationDomainService;
        this.validateCRDDomainService = validateCRDDomainService;
    }

    public record Output(ApiCRDStatus status) {}

    public record Input(AuditInfo auditInfo, ApiCRDSpec spec) {}

    public Output execute(Input input) {
        var api = apiQueryService.findByEnvironmentIdAndCrossId(input.auditInfo.environmentId(), input.spec.getCrossId());

        var status = api.map(exiting -> this.update(input, exiting)).orElseGet(() -> this.create(input));

        return new Output(status);
    }

    private ApiCRDStatus create(Input input) {
        try {
            String environmentId = input.auditInfo.environmentId();
            String organizationId = input.auditInfo.organizationId();

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

            var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(
                organizationId,
                environmentId,
                sanitizedInput.auditInfo.actor().userId()
            );

            var createdApi = createApiDomainService.create(
                ApiModelFactory.fromCrd(sanitizedInput.spec, environmentId),
                primaryOwner,
                sanitizedInput.auditInfo,
                api -> validateApiDomainService.validateAndSanitizeForCreation(api, primaryOwner, environmentId, organizationId)
            );

            var planNameIdMapping = sanitizedInput.spec
                .getPlans()
                .entrySet()
                .stream()
                .map(entry ->
                    Map.entry(
                        entry.getKey(),
                        createPlanDomainService
                            .create(initPlanFromCRD(entry.getValue()), entry.getValue().getFlows(), createdApi, sanitizedInput.auditInfo)
                            .getId()
                    )
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            createMembers(sanitizedInput, createdApi.getId());
            createOrUpdatePages(sanitizedInput.spec.getPages(), createdApi.getId(), sanitizedInput.auditInfo);

            apiMetadataDomainService.importApiMetadata(createdApi.getId(), sanitizedInput.spec.getMetadata(), sanitizedInput.auditInfo);

            if (shouldDeploy(sanitizedInput.spec())) {
                if (createdApi.getLifecycleState() == Api.LifecycleState.STOPPED) {
                    apiStateDomainService.stop(createdApi, sanitizedInput.auditInfo);
                } else {
                    apiStateDomainService.start(createdApi, sanitizedInput.auditInfo);
                }
            }

            return ApiCRDStatus
                .builder()
                .id(createdApi.getId())
                .crossId(createdApi.getCrossId())
                .environmentId(environmentId)
                .organizationId(organizationId)
                .state(createdApi.getLifecycleState().name())
                .plans(planNameIdMapping)
                .errors(ApiCRDStatus.Errors.fromErrorList(warnings))
                .build();
        } catch (AbstractDomainException e) {
            throw e;
        } catch (Exception e) {
            throw new TechnicalManagementException(e);
        }
    }

    private ApiCRDStatus update(Input input, Api existingApi) {
        try {
            var validationResult = validateCRDDomainService
                .validateAndSanitize(new ValidateApiCRDDomainService.Input(input.auditInfo(), input.spec()))
                .map(sanitized -> new Input(sanitized.auditInfo(), sanitized.spec()));

            var warnings = validationResult.warning().orElseGet(List::of);

            var sanitizedInput = validationResult.value().orElseThrow(() -> new ValidationDomainException("Unable to sanitize CRD spec"));

            var updatedApi = updateApiDomainService.update(existingApi.getId(), sanitizedInput.spec, sanitizedInput.auditInfo);

            // update state and definition context because legacy service does not update it
            // Why are we getting MANAGEMENT as an origin here ? the API has been saved as kubernetes before
            var api = apiCrudService.update(
                updatedApi
                    .toBuilder()
                    .originContext(
                        new OriginContext.Kubernetes(
                            OriginContext.Kubernetes.Mode.valueOf(sanitizedInput.spec().getDefinitionContext().getMode().toUpperCase()),
                            sanitizedInput.spec().getDefinitionContext().getSyncFrom().toUpperCase()
                        )
                    )
                    .lifecycleState(Api.LifecycleState.valueOf(sanitizedInput.spec().getState()))
                    .build()
            );

            List<Plan> existingPlans = planQueryService.findAllByApiId(api.getId());
            Map<String, PlanStatus> existingPlanStatuses = existingPlans.stream().collect(toMap(Plan::getId, Plan::getPlanStatus));

            var planKeyIdMapping = sanitizedInput
                .spec()
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
                                .update(initPlanFromCRD(plan), plan.getFlows(), existingPlanStatuses, api, sanitizedInput.auditInfo)
                                .getId()
                        );
                    }

                    return Map.entry(
                        key,
                        createPlanDomainService.create(initPlanFromCRD(plan), plan.getFlows(), api, sanitizedInput.auditInfo).getId()
                    );
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            deletePlans(api, existingPlans, planKeyIdMapping, sanitizedInput);

            if (shouldDeploy(sanitizedInput.spec())) {
                if (api.getLifecycleState() == Api.LifecycleState.STOPPED) {
                    apiStateDomainService.stop(api, sanitizedInput.auditInfo);
                } else {
                    apiStateDomainService.start(api, sanitizedInput.auditInfo);
                }
            }

            createMembers(sanitizedInput, updatedApi.getId());
            deleteOrphanMemberships(updatedApi.getId(), sanitizedInput);

            createOrUpdatePages(sanitizedInput.spec.getPages(), updatedApi.getId(), sanitizedInput.auditInfo);
            deleteRemovedPages(sanitizedInput.spec.getPages(), updatedApi.getId());

            apiMetadataDomainService.importApiMetadata(api.getId(), sanitizedInput.spec.getMetadata(), sanitizedInput.auditInfo);

            return ApiCRDStatus
                .builder()
                .id(api.getId())
                .crossId(api.getCrossId())
                .environmentId(api.getEnvironmentId())
                .organizationId(sanitizedInput.auditInfo.organizationId())
                .state(api.getLifecycleState().name())
                .plans(planKeyIdMapping)
                .errors(ApiCRDStatus.Errors.fromErrorList(warnings))
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
                !input.spec.getPlans().containsKey(plan.getId())
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
            .planDefinitionV4(
                io.gravitee.definition.model.v4.plan.Plan
                    .builder()
                    .security(planCRD.getSecurity())
                    .selectionRule(planCRD.getSelectionRule())
                    .status(planCRD.getStatus())
                    .tags(planCRD.getTags())
                    .mode(planCRD.getMode())
                    .build()
            )
            .characteristics(planCRD.getCharacteristics())
            .crossId(planCRD.getCrossId())
            .excludedGroups(planCRD.getExcludedGroups())
            .generalConditions(planCRD.getGeneralConditions())
            .order(planCRD.getOrder())
            .type(planCRD.getType())
            .validation(planCRD.getValidation())
            .build();
    }

    private void createMembers(Input input, String apiId) {
        if (!CollectionUtils.isEmpty(input.spec.getMembers())) {
            apiImportDomainService.createMembers(input.spec.getMembers().stream().map(this::initApiMemberFromCRD).collect(toSet()), apiId);
        }
    }

    private void deleteOrphanMemberships(String apiId, Input input) {
        PrimaryOwnerEntity po = primaryOwnerDomainService.getApiPrimaryOwner(input.auditInfo.organizationId(), apiId);
        Map<String, String> existingApiMembers = membershipQueryService
            .findByReference(Membership.ReferenceType.API, apiId)
            .stream()
            .filter(m -> !m.getMemberId().equals(po.id()))
            .collect(toMap(Membership::getMemberId, Membership::getId));

        if (input.spec != null && input.spec.getMembers() != null) {
            input.spec.getMembers().forEach(am -> existingApiMembers.remove(am.getId()));
        }

        existingApiMembers.forEach((k, v) -> membershipCrudService.delete(v));
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
        var pages = pageCRDs.values().stream().map(PageModelFactory::fromCRDSpec).toList();

        pages.forEach(page -> {
            page.setReferenceId(apiId);
            page.setReferenceType(Page.ReferenceType.API);
            if (page.getParentId() != null) {
                validatePageParent(pages, page.getParentId());
            }

            pageCrudService
                .findById(page.getId())
                .ifPresentOrElse(
                    oldPage -> {
                        updateApiDocumentationDomainService.updatePage(
                            page.toBuilder().createdAt(oldPage.getCreatedAt()).updatedAt(now).build(),
                            oldPage,
                            auditInfo
                        );
                    },
                    () -> {
                        createApiDocumentationDomainService.createPage(page.toBuilder().createdAt(now).updatedAt(now).build(), auditInfo);
                    }
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

    private ApiMember initApiMemberFromCRD(MemberCRD crd) {
        return ApiMember
            .builder()
            .id(crd.getId())
            .displayName(crd.getDisplayName())
            .roles(List.of(new ApiMemberRole(crd.getRole(), RoleScope.API)))
            .build();
    }

    private static boolean shouldDeploy(ApiCRDSpec spec) {
        return (
            spec.getDefinitionContext().getSyncFrom().equalsIgnoreCase(DefinitionContext.ORIGIN_MANAGEMENT) &&
            spec
                .getPlans()
                .values()
                .stream()
                .anyMatch(plan -> plan.getStatus() == PlanStatus.PUBLISHED || plan.getStatus() == PlanStatus.DEPRECATED)
        );
    }
}
