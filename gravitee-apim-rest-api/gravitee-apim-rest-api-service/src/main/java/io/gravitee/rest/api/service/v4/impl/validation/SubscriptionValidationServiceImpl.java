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
package io.gravitee.rest.api.service.v4.impl.validation;

import static java.util.Optional.ofNullable;

import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormElResolverDomainService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSubmissionValidator;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.SubscriptionEntrypointIdMissingException;
import io.gravitee.rest.api.service.v4.validation.SubscriptionMetadataSanitizer;
import io.gravitee.rest.api.service.v4.validation.SubscriptionValidationService;
import java.util.Map;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@RequiredArgsConstructor
@CustomLog
public class SubscriptionValidationServiceImpl extends TransactionalService implements SubscriptionValidationService {

    private final EntrypointConnectorPluginService entrypointService;
    private final SubscriptionMetadataSanitizer subscriptionMetadataSanitizer;
    private final SubscriptionFormQueryService subscriptionFormQueryService;
    private final SubscriptionFormElResolverDomainService subscriptionFormElResolver;

    @Override
    public void validateAndSanitize(final GenericPlanEntity genericPlanEntity, final NewSubscriptionEntity subscription) {
        subscription.setConfiguration(validateAndSanitizeSubscriptionConfiguration(genericPlanEntity, subscription.getConfiguration()));
        if (subscription.getMetadata() != null) {
            subscription.setMetadata(subscriptionMetadataSanitizer.sanitizeAndValidate(subscription.getMetadata()));
        }
        validateSubscriptionFormMetadataIfApplicable(genericPlanEntity, subscription.getMetadata());
    }

    private void validateSubscriptionFormMetadataIfApplicable(
        final GenericPlanEntity genericPlanEntity,
        final Map<String, String> metadata
    ) {
        var submitted = ofNullable(metadata).orElseGet(Map::of);

        subscriptionFormQueryService
            .findDefaultForEnvironmentId(genericPlanEntity.getEnvironmentId())
            .filter(SubscriptionForm::isEnabled)
            .map(SubscriptionForm::getValidationConstraints)
            .filter(constraints -> !constraints.isEmpty())
            .map(constraints ->
                GenericPlanEntity.ReferenceType.API.equals(genericPlanEntity.getReferenceType())
                    ? subscriptionFormElResolver.resolveConstraints(
                        constraints,
                        genericPlanEntity.getEnvironmentId(),
                        genericPlanEntity.getReferenceId()
                    )
                    : subscriptionFormElResolver.resolveConstraints(constraints)
            )
            .map(SubscriptionFormSubmissionValidator::new)
            .ifPresent(validator -> validator.validate(submitted));
    }

    @Override
    public void validateAndSanitize(
        final GenericPlanEntity genericPlanEntity,
        final UpdateSubscriptionEntity subscription,
        String applicationId
    ) {
        subscription.setConfiguration(validateAndSanitizeSubscriptionConfiguration(genericPlanEntity, subscription.getConfiguration()));
        if (subscription.getMetadata() != null) {
            subscription.setMetadata(subscriptionMetadataSanitizer.sanitizeAndValidate(subscription.getMetadata()));
        }
        validateSubscriptionFormMetadataIfApplicable(genericPlanEntity, subscription.getMetadata());
    }

    @Override
    public void validateAndSanitize(
        final GenericPlanEntity genericPlanEntity,
        final UpdateSubscriptionConfigurationEntity subscriptionConfiguration
    ) {
        subscriptionConfiguration.setConfiguration(
            validateAndSanitizeSubscriptionConfiguration(genericPlanEntity, subscriptionConfiguration.getConfiguration())
        );
        if (subscriptionConfiguration.getMetadata() != null) {
            subscriptionConfiguration.setMetadata(
                subscriptionMetadataSanitizer.sanitizeAndValidate(subscriptionConfiguration.getMetadata())
            );
        }
        validateSubscriptionFormMetadataIfApplicable(genericPlanEntity, subscriptionConfiguration.getMetadata());
    }

    private SubscriptionConfigurationEntity validateAndSanitizeSubscriptionConfiguration(
        final GenericPlanEntity genericPlanEntity,
        final SubscriptionConfigurationEntity configuration
    ) {
        if (genericPlanEntity.getPlanMode() != null && genericPlanEntity.getPlanMode() == PlanMode.PUSH) {
            if (configuration == null || configuration.getEntrypointId() == null || configuration.getEntrypointId().isEmpty()) {
                throw new SubscriptionEntrypointIdMissingException();
            }

            configuration.setEntrypointConfiguration(
                entrypointService.validateEntrypointSubscriptionConfiguration(
                    configuration.getEntrypointId(),
                    configuration.getEntrypointConfiguration()
                )
            );
        }
        return configuration;
    }
}
