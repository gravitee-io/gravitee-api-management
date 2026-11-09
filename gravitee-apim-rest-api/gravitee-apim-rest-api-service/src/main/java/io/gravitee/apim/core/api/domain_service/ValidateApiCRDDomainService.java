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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.domain_service.property.PropertyDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryIdsDomainService;
import io.gravitee.apim.core.cluster.domain_service.ClusterBindingPermissionDomainService;
import io.gravitee.apim.core.cluster.domain_service.ValidateApiClusterBindingService;
import io.gravitee.apim.core.cluster.domain_service.ValidateKafkaClusterBindingDomainService;
import io.gravitee.apim.core.documentation.domain_service.ValidatePagesDomainService;
import io.gravitee.apim.core.documentation.model.factory.PageModelFactory;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.flow.exception.InvalidFlowException;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.member.domain_service.CRDMembersDomainService;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.notification.domain_service.ValidatePortalNotificationDomainService;
import io.gravitee.apim.core.plan.domain_service.ValidatePlanDomainService;
import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    private final ApiQueryService apiQueryService;

    private final PropertyDomainService propertyDomainService;

    private final CRDMembersDomainService membersDomainService;

    private final ValidateKafkaClusterBindingDomainService kafkaClusterBindingValidator;

    private final ClusterBindingPermissionDomainService clusterBindingPermissionDomainService;

    private final ValidateApiClusterBindingService apiClusterBindingValidator;

    @Override
    public Validator.Result<ValidateApiCRDDomainService.Input> validateAndSanitize(ValidateApiCRDDomainService.Input input) {
        var existingApi = apiQueryService.findByEnvironmentIdAndCrossId(input.auditInfo().environmentId(), input.spec().getCrossId());
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

        planValidator
            .validateAndSanitize(
                new ValidatePlanDomainService.Input(
                    input.auditInfo,
                    input.spec,
                    input.spec.getPlans(),
                    Optional.ofNullable(input.spec().getPages())
                        .orElse(Map.of())
                        .entrySet()
                        .stream()
                        .map(entry -> PageModelFactory.fromCRDSpec(entry.getKey(), entry.getValue()))
                        .toList()
                )
            )
            .peek(sanitized -> sanitizedBuilder.plans(sanitized.plans()), errors::addAll);

        var sanitizedSpec = sanitizedBuilder.build();
        // Checked here, after the members are sanitized, so a dry run refuses what the import would refuse: a member change, a cluster binding
        existingApi.ifPresent(api ->
            membersDomainService.checkApiMembersUpdate(input.auditInfo(), api.getId(), sanitizedSpec.getMembers())
        );
        if (apiType(sanitizedSpec) == ApiType.MESSAGE) {
            try {
                kafkaClusterBindingValidator.validate(input.auditInfo().environmentId(), httpEndpointGroups(sanitizedSpec));
            } catch (InvalidDataException e) {
                errors.add(Error.severe(e.getMessage()));
            }
        }
        clusterBindingPermissionDomainService.validate(
            input.auditInfo().organizationId(),
            input.auditInfo().environmentId(),
            // No actor, as for a system call: not checked
            input.auditInfo().actor() == null ? null : input.auditInfo().actor().userId(),
            existingApi.orElse(null),
            apiType(sanitizedSpec),
            sanitizedSpec.getEndpointGroups()
        );
        if (sanitizedSpec.getState() != null && !isLifecycleState(sanitizedSpec.getState())) {
            errors.add(Error.severe("State [%s] is invalid, expected STARTED or STOPPED", sanitizedSpec.getState()));
        }
        // The import persists the API before starting it: refuse here, or a start failure leaves it saved STARTED but not deployed
        if (Api.LifecycleState.STARTED.name().equalsIgnoreCase(sanitizedSpec.getState())) {
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

        return Validator.Result.ofBoth(new ValidateApiCRDDomainService.Input(input.auditInfo(), sanitizedSpec), errors);
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
