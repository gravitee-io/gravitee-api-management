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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.SubscriptionEntrypointIdMissingException;
import io.gravitee.rest.api.service.v4.validation.SubscriptionValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class SubscriptionValidationServiceImpl extends TransactionalService implements SubscriptionValidationService {

    private final EntrypointConnectorPluginService entrypointService;
    private final ObjectMapper objectMapper;

    public SubscriptionValidationServiceImpl(final EntrypointConnectorPluginService entrypointService, final ObjectMapper objectMapper) {
        this.entrypointService = entrypointService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void validateAndSanitize(NewSubscriptionEntity subscription) {
        subscription.setConfiguration(validateAndSanitizeSubscriptionConfiguration(subscription.getConfiguration()));
    }

    @Override
    public void validateAndSanitize(UpdateSubscriptionEntity subscription) {
        subscription.setConfiguration(validateAndSanitizeSubscriptionConfiguration(subscription.getConfiguration()));
    }

    @Override
    public void validateAndSanitize(UpdateSubscriptionConfigurationEntity subscriptionConfiguration) {
        subscriptionConfiguration.setConfiguration(
            validateAndSanitizeSubscriptionConfiguration(subscriptionConfiguration.getConfiguration())
        );
    }

    private String validateAndSanitizeSubscriptionConfiguration(String configuration) {
        if (configuration == null) {
            return null;
        }
        try {
            String connectorId = objectMapper.readTree(configuration).path("entrypointId").asText();
            if (connectorId == null || connectorId.isEmpty()) {
                throw new SubscriptionEntrypointIdMissingException();
            }
            return entrypointService.validateEntrypointSubscriptionConfiguration(connectorId, configuration);
        } catch (JsonProcessingException e) {
            log.error("Failed to read subscription configuration", e);
            throw new TechnicalManagementException("An error occurs while trying to process promotion", e);
        }
    }
}
