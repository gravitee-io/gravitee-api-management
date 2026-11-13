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
package io.gravitee.apim.core.api.domain_service;

import static java.util.stream.Collectors.toMap;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.domain_service.property.PropertyDomainService;
import io.gravitee.apim.core.api.exception.ApiTypeChangeNotAllowedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryIdsDomainService;
import io.gravitee.apim.core.cluster.domain_service.ClusterBindingPermissionDomainService;
import io.gravitee.apim.core.cluster.domain_service.ValidateApiClusterBindingService;
import io.gravitee.apim.core.cluster.domain_service.ValidateKafkaClusterBindingDomainService;
import io.gravitee.apim.core.documentation.domain_service.ValidatePagesDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageParentException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.factory.PageModelFactory;
import io.gravitee.apim.core.exception.AbstractDomainException;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.flow.exception.InvalidFlowException;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.member.domain_service.CRDMembersDomainService;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.notification.domain_service.ValidatePortalNotificationDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.ValidatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateApiCRDDomainService implements Validator<ValidateApiCRDDomainService.Input> {

    public record Input(AuditInfo auditInfo, ApiCRDSpec spec) implements Validator.Input {}

    private final ValidateCategoryIdsDomainService categoryIdsValidator;

    private final VerifyApiPathDomainService apiPathValidator;

    private final VerifyApiHostsDomainService apiHostValidator;

    private final ValidateCRDMembersDomainService membersValidator;

    private final ValidateGroupsDomainService groupsValidator;

    private final ValidateResourceDomainService resourceValidator;

    private final ValidatePagesDomainService pagesValidator;

    private final ValidatePlanDomainService planValidator;

    private final ValidatePortalNotificationDomainService portalNotificationValidator;

    private final ValidateHealthCheckScheduleDomainService healthCheckScheduleValidator;

    private final ApiCRDTargetDomainService apiCRDTargetDomainService;

    private final PropertyDomainService propertyDomainService;

    private final CRDMembersDomainService membersDomainService;

    private final ValidateKafkaClusterBindingDomainService kafkaClusterBindingValidator;

    private final ClusterBindingPermissionDomainService clusterBindingPermissionDomainService;

    private final ValidateApiClusterBindingService apiClusterBindingValidator;

    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;

    private final ValidateApiDomainService apiValidator;

    private final UpdateApiDomainService updateApiDomainService;

    private final FlowValidationDomainService flowValidator;

    private final PlanQueryService planQueryService;

    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;

    private final ApiFolderConflictsDomainService apiFolderConflictsValidator;

    @Override
    public Validator.Result<ValidateApiCRDDomainService.Input> validateAndSanitize(ValidateApiCRDDomainService.Input input) {
        var existingApi = apiCRDTargetDomainService.findExistingApi(input.auditInfo().environmentId(), input.spec());
        existingApi.ifPresent(api -> validateAgainstExistingApi(api, input.spec()));

        var errors = new ArrayList<Error>();

        var sanitizedBuilder = input.spec().toBuilder();

        if (input.spec.isNative()) {
            validateAndSanitizeNativeV4ForCreation(input, sanitizedBuilder, errors);
        } else if (!input.spec.isOnlySubscription()) {
            validateAndSanitizeHttpV4ForCreation(input, sanitizedBuilder, errors);
            healthCheckScheduleValidator.validate(input.spec().getEndpointGroups(), errors);
        }

        categoryIdsValidator
            .validateAndSanitize(
                new ValidateCategoryIdsDomainService.Input(input.auditInfo().environmentId(), input.spec().getCategories())
            )
            .peek(sanitized -> sanitizedBuilder.categories(sanitized.idOrKeys()), errors::addAll);

        membersValidator
            .validateAndSanitize(
                new ValidateCRDMembersDomainService.Input(input.auditInfo(), MembershipReferenceType.API, input.spec().getMembers())
            )
            .peek(sanitized -> sanitizedBuilder.members(sanitized.members()), errors::addAll);

        groupsValidator
            .validateAndSanitize(
                new ValidateGroupsDomainService.Input(
                    input.auditInfo.environmentId(),
                    // ignoreGroups: validate nothing that will not be applied; default groups are still injected on create
                    input.spec().isIgnoreGroups() ? null : input.spec().getGroups(),
                    input.spec().getDefinitionVersion(),
                    input.spec().getDefinitionContext() != null ? input.spec().getDefinitionContext().getOrigin() : null,
                    Group.GroupEvent.API_CREATE,
                    true
                )
            )
            .peek(sanitized -> sanitizedBuilder.groups(sanitized.groups()), errors::addAll);

        portalNotificationValidator
            .validateAndSanitize(
                new ValidatePortalNotificationDomainService.Input(
                    input.spec().getConsoleNotificationConfiguration(),
                    input.spec().getDefinitionVersion(),
                    input.spec().getDefinitionContext() != null ? input.spec().getDefinitionContext().getOrigin() : null,
                    sanitizedBuilder.build().getGroups(),
                    input.auditInfo()
                )
            )
            .peek(sanitized -> sanitizedBuilder.consoleNotificationConfiguration(sanitized.portalNotificationConfig()), errors::addAll);

        resourceValidator
            .validateAndSanitize(new ValidateResourceDomainService.Input(input.auditInfo.environmentId(), input.spec().getResources()))
            .peek(sanitized -> sanitizedBuilder.resources(sanitized.resources()), errors::addAll);

        pagesValidator
            .validateAndSanitize(
                new ValidatePagesDomainService.Input(input.auditInfo, input.spec.getId(), input.spec.getHrid(), input.spec.getPages())
            )
            .peek(sanitized -> sanitizedBuilder.pages(sanitized.pages()), errors::addAll);

        var pages = Optional.ofNullable(input.spec().getPages())
            .orElse(Map.of())
            .entrySet()
            .stream()
            .map(entry -> PageModelFactory.fromCRDSpec(entry.getKey(), entry.getValue()))
            .toList();

        planValidator
            .validateAndSanitize(
                new ValidatePlanDomainService.Input(
                    input.auditInfo,
                    input.spec,
                    existingApi.map(Api::getId).orElse(input.spec.getId()),
                    input.spec.getPlans(),
                    pages
                )
            )
            .peek(sanitized -> sanitizedBuilder.plans(sanitized.plans()), errors::addAll);

        var sanitizedSpec = sanitizedBuilder.build();
        // Checked here, after the members are sanitized, so a dry run refuses what the import would refuse: a member change, a cluster binding
        existingApi.ifPresent(api ->
            membersDomainService.checkApiMembersUpdate(input.auditInfo(), api.getId(), sanitizedSpec.getMembers())
        );
        clusterBindingPermissionDomainService.validate(
            input.auditInfo().organizationId(),
            input.auditInfo().environmentId(),
            // No actor, as for a system call: not checked
            input.auditInfo().actor() == null ? null : input.auditInfo().actor().userId(),
            existingApi.orElse(null),
            apiType(sanitizedSpec),
            sanitizedSpec.getEndpointGroups()
        );
        int errorsBeforeBindings = errors.size();
        if (apiType(sanitizedSpec) == ApiType.MESSAGE) {
            try {
                kafkaClusterBindingValidator.validate(
                    input.auditInfo().environmentId(),
                    existingApi
                        .map(Api::getApiDefinitionHttpV4)
                        .map(definition -> definition.getEndpointGroups())
                        .orElse(null),
                    httpEndpointGroups(sanitizedSpec)
                );
            } catch (InvalidDataException e) {
                errors.add(Error.severe(e.getMessage()));
            }
        }
        try {
            apiClusterBindingValidator.validateSavable(
                input.auditInfo().environmentId(),
                apiType(sanitizedSpec),
                existingApi.map(Api::getApiDefinitionNativeV4).map(NativeApi::getEndpointGroups).orElse(null),
                sanitizedSpec.getEndpointGroups()
            );
        } catch (InvalidDataException e) {
            errors.add(Error.severe(e.getMessage()));
        }
        if (sanitizedSpec.getState() != null && !isLifecycleState(sanitizedSpec.getState())) {
            errors.add(Error.severe("State [%s] is invalid, expected STARTED or STOPPED", sanitizedSpec.getState()));
        }
        // The import persists the API before starting or deploying it: refuse here, or a failure leaves it saved but not deployed.
        // A spec without state keeps the stored one, so a stored STARTED API is checked too.
        // Not when a binding is already refused above, which the start check would report again.
        var lifecycleState = sanitizedSpec.getState() != null
            ? sanitizedSpec.getState()
            : existingApi.map(Api::getLifecycleState).map(Enum::name).orElse(null);
        if (errors.size() == errorsBeforeBindings && Api.LifecycleState.STARTED.name().equalsIgnoreCase(lifecycleState)) {
            try {
                apiClusterBindingValidator.validateStartable(
                    sanitizedSpec.getId(),
                    input.auditInfo().environmentId(),
                    apiType(sanitizedSpec),
                    sanitizedSpec.getEndpointGroups()
                );
            } catch (ApiNotDeployableException e) {
                errors.add(Error.severe(e.getMessage()));
            }
        }
        if (hasEmptyPortalNavigationPath(sanitizedSpec)) {
            errors.add(Error.severe("A portal navigation path must not be empty"));
        }
        // The import refuses severe errors before any write, and writes as its actor: a system call has none
        if (errors.stream().noneMatch(Error::isSevere) && input.auditInfo().actor() != null) {
            validateTheImportWrites(input.auditInfo(), sanitizedSpec, existingApi, pages, errors);
        }

        return Validator.Result.ofBoth(new ValidateApiCRDDomainService.Input(input.auditInfo(), sanitizedSpec), errors);
    }

    // A dry run refuses what the import would refuse: run the validation of the import's writes, without writing.
    // Its first refusal is a severe error with the import's message; a technical failure still propagates.
    private void validateTheImportWrites(
        AuditInfo auditInfo,
        ApiCRDSpec spec,
        Optional<Api> existingApi,
        List<Page> pages,
        List<Error> errors
    ) {
        try {
            apiFolderConflictsValidator.validateApiFolderConflictsForApi(
                auditInfo,
                existingApi.map(Api::getId).orElse(spec.getId()),
                spec.getPortalNavigation()
            );
            existingApi.ifPresentOrElse(api -> validateApiUpdate(auditInfo, spec, api), () -> validateApiCreation(auditInfo, spec));
            validatePageParents(pages);
            validatePlanWrites(auditInfo, spec, existingApi);
        } catch (TechnicalDomainException e) {
            throw e;
        } catch (AbstractDomainException e) {
            errors.add(Error.severe("%s", e.getMessage()));
        } catch (AbstractManagementException e) {
            if (e.getHttpStatusCode() >= 500) {
                throw e;
            }
            errors.add(Error.severe("%s", e.getMessage()));
        }
    }

    private void validateApiCreation(AuditInfo auditInfo, ApiCRDSpec spec) {
        var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            auditInfo.actor().userId()
        );
        apiValidator.validateAndSanitizeForCreation(
            ApiModelFactory.fromCrd(spec, auditInfo.environmentId()),
            primaryOwner,
            auditInfo.environmentId(),
            auditInfo.organizationId()
        );
    }

    private void validateApiUpdate(AuditInfo auditInfo, ApiCRDSpec spec, Api existingApi) {
        // The import updates a native API and an HTTP API through different paths: neither can take the other type
        if (existingApi.isNative() != spec.isNative()) {
            throw new ApiTypeChangeNotAllowedException(existingApi.getId());
        }
        // As the import does: ignoreGroups keeps the stored groups
        var specToUpdate = spec.isIgnoreGroups() ? spec.toBuilder().groups(existingApi.getGroups()).build() : spec;
        if (existingApi.isNative()) {
            validateNativeApiUpdate(auditInfo, specToUpdate, existingApi);
        } else {
            updateApiDomainService.validate(existingApi.getId(), specToUpdate, auditInfo);
        }
    }

    // As UpdateNativeApiUseCase does, without writing. The update does not validate properties: the stored ones stand in
    private void validateNativeApiUpdate(AuditInfo auditInfo, ApiCRDSpec spec, Api existingApi) {
        var storedProperties = existingApi.getApiDefinitionValue() instanceof NativeApi nativeApi ? nativeApi.getProperties() : null;
        apiValidator.validateAndSanitizeForUpdate(
            existingApi,
            ApiModelFactory.toUpdateNativeApi(spec).applyTo(existingApi, storedProperties),
            apiPrimaryOwnerDomainService.getApiPrimaryOwner(auditInfo.organizationId(), existingApi.getId()),
            auditInfo.environmentId(),
            auditInfo.organizationId()
        );
    }

    private static void validatePageParents(List<Page> pages) {
        pages
            .stream()
            .map(Page::getParentId)
            .filter(Objects::nonNull)
            .flatMap(parentId -> pages.stream().filter(page -> parentId.equals(page.getId())))
            .filter(parent -> !(parent.isFolder() || parent.isRoot()))
            .findFirst()
            .ifPresent(parent -> {
                throw new InvalidPageParentException(parent.getId());
            });
    }

    // The plan writes' own rules, on the API as the import leaves it
    private void validatePlanWrites(AuditInfo auditInfo, ApiCRDSpec spec, Optional<Api> existingApi) {
        var api = apiAfterImport(auditInfo, spec, existingApi);
        Map<String, PlanStatus> storedStatuses = existingApi
            .map(stored -> planQueryService.findAllByApiId(stored.getId()).stream().collect(toMap(Plan::getId, Plan::getPlanStatus)))
            .orElseGet(HashMap::new);
        plansOf(spec).forEach(planCRD -> {
            if (storedStatuses.containsKey(planCRD.getId())) {
                UpdatePlanDomainService.assertNotClosedStatus(planCRD.getId(), planCRD.getName(), planCRD.getStatus(), storedStatuses);
            } else {
                CreatePlanDomainService.assertApiAcceptsNewPlans(api);
            }
            if (api.isNative()) {
                flowValidator.validateAndSanitizeNativeV4(nativeFlows(planCRD.getFlows()));
            } else {
                flowValidator.validateAndSanitizeHttpV4PlanFlows(api, httpFlows(planCRD.getFlows()));
            }
        });
    }

    // What the plan rules read: the lifecycle state (the stored one when the spec has none), the API flows and resources
    private static Api apiAfterImport(AuditInfo auditInfo, ApiCRDSpec spec, Optional<Api> existingApi) {
        if (existingApi.isEmpty()) {
            return ApiModelFactory.fromCrd(spec, auditInfo.environmentId());
        }
        var stored = existingApi.get();
        var api = stored.toBuilder().apiLifecycleState(lifecycleStateAfterImport(spec, stored));
        if (stored.getApiDefinitionHttpV4() != null) {
            api.apiDefinitionValue(
                stored.getApiDefinitionHttpV4().toBuilder().flows(httpFlows(spec.getFlows())).resources(spec.getResources()).build()
            );
        }
        return api.build();
    }

    private static Api.ApiLifecycleState lifecycleStateAfterImport(ApiCRDSpec spec, Api stored) {
        return Arrays.stream(Api.ApiLifecycleState.values())
            .filter(state -> state.name().equalsIgnoreCase(spec.getLifecycleState()))
            .findFirst()
            .orElse(stored.getApiLifecycleState());
    }

    private static List<PlanCRD> plansOf(ApiCRDSpec spec) {
        return spec.getPlans() == null ? List.of() : List.copyOf(spec.getPlans().values());
    }

    private static List<Flow> httpFlows(List<? extends AbstractFlow> flows) {
        return flows == null ? List.of() : flows.stream().filter(Flow.class::isInstance).map(Flow.class::cast).toList();
    }

    private static List<NativeFlow> nativeFlows(List<? extends AbstractFlow> flows) {
        return flows == null ? null : flows.stream().filter(NativeFlow.class::isInstance).map(NativeFlow.class::cast).toList();
    }

    private static boolean hasEmptyPortalNavigationPath(ApiCRDSpec spec) {
        return (
            spec.getPortalNavigation() != null &&
            spec
                .getPortalNavigation()
                .stream()
                .anyMatch(entry -> entry.path().isEmpty())
        );
    }

    private static boolean isLifecycleState(String state) {
        return Arrays.stream(Api.LifecycleState.values()).anyMatch(lifecycleState -> lifecycleState.name().equals(state));
    }

    // The type the import reads, null for a type it refuses
    private static ApiType apiType(ApiCRDSpec spec) {
        return Arrays.stream(ApiType.values())
            .filter(type -> type.name().equalsIgnoreCase(spec.getType()))
            .findFirst()
            .orElse(null);
    }

    private static List<EndpointGroup> httpEndpointGroups(ApiCRDSpec spec) {
        return spec.getEndpointGroups() == null
            ? null
            : spec.getEndpointGroups().stream().filter(EndpointGroup.class::isInstance).map(EndpointGroup.class::cast).toList();
    }

    private void validateAgainstExistingApi(Api existingApi, ApiCRDSpec spec) {
        rejectAnotherApiId(existingApi, spec);
        propertyDomainService.validateClassification(existingApi.getApiDefinitionValue(), spec.getProperties());
    }

    // An import updates the API found by cross id: every write must target that same API
    private static void rejectAnotherApiId(Api existingApi, ApiCRDSpec spec) {
        if (spec.getId() != null && !spec.getId().equals(existingApi.getId())) {
            throw new ValidationDomainException(
                String.format("API id [%s] does not match the API with cross id [%s]", spec.getId(), spec.getCrossId())
            );
        }
    }

    private void validateAndSanitizeHttpV4ForCreation(Input input, ApiCRDSpec.ApiCRDSpecBuilder sanitizedBuilder, ArrayList<Error> errors) {
        apiPathValidator
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(input.auditInfo.environmentId(), input.spec.getId(), input.spec.getPaths())
            )
            .peek(sanitized -> sanitizedBuilder.paths(sanitized.paths()), errors::addAll);

        checkApiFlowsPathOperator(input.spec.getFlows());
    }

    private void checkApiFlowsPathOperator(List<? extends AbstractFlow> flows) {
        if (flows == null) {
            return;
        }
        flows
            .stream()
            .filter(Flow.class::isInstance)
            .map(Flow.class::cast)
            .filter(flow -> flow.getSelectors() != null)
            .forEach(flow -> {
                boolean hasNullPathOperator = flow
                    .getSelectors()
                    .stream()
                    .anyMatch(FlowValidationDomainService.HTTP_SELECTOR_WITH_NULL_PATH_OPERATOR);
                if (hasNullPathOperator) {
                    throw InvalidFlowException.missingPathOperator(flow.getName());
                }
                flow
                    .getSelectors()
                    .stream()
                    .filter(FlowValidationDomainService.HTTP_SELECTOR_WITH_INVALID_WILDCARD_PATH)
                    .findFirst()
                    .ifPresent(selector -> {
                        if (selector instanceof HttpSelector httpSelector) {
                            throw InvalidFlowException.invalidWildcardPath(flow.getName(), httpSelector.getPath());
                        }
                    });
            });
    }

    private void validateAndSanitizeNativeV4ForCreation(
        Input input,
        ApiCRDSpec.ApiCRDSpecBuilder sanitizedBuilder,
        ArrayList<Error> errors
    ) {
        var listeners = new ArrayList<KafkaListener>();

        input.spec
            .getListeners()
            .forEach(listener -> {
                try {
                    KafkaListener kafkaListener = (KafkaListener) listener;
                    if (
                        apiHostValidator.checkApiHosts(
                            input.auditInfo.environmentId(),
                            input.spec.getId(),
                            List.of(kafkaListener.getHost()),
                            ListenerType.KAFKA
                        )
                    ) {
                        listeners.add((KafkaListener) listener);
                    }
                } catch (Exception e) {
                    errors.add(Error.severe(e.getMessage()));
                }
            });

        sanitizedBuilder.listeners(listeners);
    }
}
