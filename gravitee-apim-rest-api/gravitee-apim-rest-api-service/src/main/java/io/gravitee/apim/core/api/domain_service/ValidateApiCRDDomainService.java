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
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryIdsDomainService;
import io.gravitee.apim.core.documentation.domain_service.ValidatePagesDomainService;
import io.gravitee.apim.core.documentation.model.factory.PageModelFactory;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.notification.domain_service.ValidatePortalNotificationDomainService;
import io.gravitee.apim.core.plan.domain_service.ValidatePlanDomainService;
import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.rest.api.service.common.IdBuilder;
import java.util.ArrayList;
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

    @Override
    public Validator.Result<ValidateApiCRDDomainService.Input> validateAndSanitize(ValidateApiCRDDomainService.Input input) {
        var errors = new ArrayList<Error>();

        if (input.spec.getId() == null) {
            IdBuilder idBuilder = IdBuilder.builder(input.auditInfo, input.spec.getHrid());
            input.spec.setId(idBuilder.buildId());
            input.spec.setCrossId(idBuilder.buildCrossId());
        }

        var sanitizedBuilder = input.spec().toBuilder();

        if (input.spec.isNative()) {
            validateAndSanitizeNativeV4ForCreation(input, sanitizedBuilder, errors);
        } else {
            validateAndSanitizeHttpV4ForCreation(input, sanitizedBuilder, errors);
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
                    input.spec().getGroups(),
                    input.spec().getDefinitionVersion(),
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

        return Validator.Result.ofBoth(new ValidateApiCRDDomainService.Input(input.auditInfo(), sanitizedBuilder.build()), errors);
    }

    private void validateAndSanitizeHttpV4ForCreation(Input input, ApiCRDSpec.ApiCRDSpecBuilder sanitizedBuilder, ArrayList<Error> errors) {
        apiPathValidator
            .validateAndSanitize(
                new VerifyApiPathDomainService.Input(input.auditInfo.environmentId(), input.spec.getId(), input.spec.getPaths())
            )
            .peek(sanitized -> sanitizedBuilder.paths(sanitized.paths()), errors::addAll);
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
