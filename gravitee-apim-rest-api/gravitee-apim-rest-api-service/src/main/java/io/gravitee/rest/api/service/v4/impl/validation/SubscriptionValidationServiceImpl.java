/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.v4.impl.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.SubscriptionEntrypointIdMissingException;
import io.gravitee.rest.api.service.v4.validation.SubscriptionValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionValidationServiceImpl extends TransactionalService implements SubscriptionValidationService {

    private final EntrypointConnectorPluginService entrypointService;

    @Override
    public void validateAndSanitize(final GenericPlanEntity genericPlanEntity, final NewSubscriptionEntity subscription) {
        subscription.setConfiguration(validateAndSanitizeSubscriptionConfiguration(genericPlanEntity, subscription.getConfiguration()));
    }

    @Override
    public void validateAndSanitize(final GenericPlanEntity genericPlanEntity, final UpdateSubscriptionEntity subscription) {
        subscription.setConfiguration(validateAndSanitizeSubscriptionConfiguration(genericPlanEntity, subscription.getConfiguration()));
    }

    @Override
    public void validateAndSanitize(
        final GenericPlanEntity genericPlanEntity,
        final UpdateSubscriptionConfigurationEntity subscriptionConfiguration
    ) {
        subscriptionConfiguration.setConfiguration(
            validateAndSanitizeSubscriptionConfiguration(genericPlanEntity, subscriptionConfiguration.getConfiguration())
        );
    }

    private SubscriptionConfigurationEntity validateAndSanitizeSubscriptionConfiguration(
        final GenericPlanEntity genericPlanEntity,
        final SubscriptionConfigurationEntity configuration
    ) {
        if (
            genericPlanEntity.getPlanSecurity() != null &&
            genericPlanEntity.getPlanSecurity().getType().equals(PlanSecurityType.SUBSCRIPTION.getLabel())
        ) {
            if (configuration.getEntrypointId() == null || configuration.getEntrypointId().isEmpty()) {
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
