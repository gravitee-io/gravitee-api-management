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

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.SubscriptionEntrypointIdMissingException;
import io.gravitee.rest.api.service.v4.validation.SubscriptionValidationService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SubscriptionValidationServiceImplTest {

    private SubscriptionValidationService cut;

    @Mock
    private EntrypointConnectorPluginService entrypointConnectorPluginService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        cut = new SubscriptionValidationServiceImpl(entrypointConnectorPluginService, objectMapper);
    }

    @Test(expected = SubscriptionEntrypointIdMissingException.class)
    public void shouldThrowIfNoConnectorIdProvidedInConfigurationNewSubscription() {
        NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
        newSubscriptionEntity.setConfiguration("{\"no-connector-id\": \"it-is-not-a-joke\"}");

        cut.validateAndSanitize(newSubscriptionEntity);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowIfBadConfigurationNewSubscription() {
        NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
        newSubscriptionEntity.setConfiguration("{\"connectorId\": \"invalid");

        cut.validateAndSanitize(newSubscriptionEntity);
    }

    @Test
    public void shouldSanitizeAndSetConfigurationForNewSubscription() throws JsonProcessingException {
        NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
        newSubscriptionEntity.setConfiguration(
            "{" + "\"entrypointId\": \"entrypoint_id\", " + "\"aConfigurationFieldToSanitize\": \"to_sanitize\"" + "}"
        );

        when(
            entrypointConnectorPluginService.validateEntrypointSubscriptionConfiguration(
                "entrypoint_id",
                newSubscriptionEntity.getConfiguration()
            )
        )
            .thenReturn("{" + "\"connectorId\": \"entrypoint_id\", " + "\"aConfigurationFieldToSanitize\": \"sanitized\"" + "}");

        cut.validateAndSanitize(newSubscriptionEntity);

        final String sanitizedField = objectMapper
            .readTree(newSubscriptionEntity.getConfiguration())
            .path("aConfigurationFieldToSanitize")
            .asText();
        Assertions.assertThat(sanitizedField).isEqualTo("sanitized");
    }

    @Test
    public void shouldAcceptNullConfigurationForNewSubscription() {
        NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
        newSubscriptionEntity.setConfiguration((String) null);

        cut.validateAndSanitize(newSubscriptionEntity);

        assertNull(newSubscriptionEntity.getConfiguration());
    }

    @Test(expected = SubscriptionEntrypointIdMissingException.class)
    public void shouldThrowIfNoConnectorIdProvidedInConfigurationUpdateSubscription() {
        UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
        updateSubscriptionEntity.setConfiguration("{\"no-connector-id\": \"it-is-not-a-joke\"}");

        cut.validateAndSanitize(updateSubscriptionEntity);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowIfBadConfigurationUpdateSubscription() {
        UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
        updateSubscriptionEntity.setConfiguration("{\"connectorId\": \"invalid");

        cut.validateAndSanitize(updateSubscriptionEntity);
    }

    @Test
    public void shouldSanitizeAndSetConfigurationForUpdateSubscription() throws JsonProcessingException {
        UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
        updateSubscriptionEntity.setConfiguration(
            "{" + "\"entrypointId\": \"entrypoint_id\", " + "\"aConfigurationFieldToSanitize\": \"to_sanitize\"" + "}"
        );

        when(
            entrypointConnectorPluginService.validateEntrypointSubscriptionConfiguration(
                "entrypoint_id",
                updateSubscriptionEntity.getConfiguration()
            )
        )
            .thenReturn("{" + "\"connectorId\": \"entrypoint_id\", " + "\"aConfigurationFieldToSanitize\": \"sanitized\"" + "}");

        cut.validateAndSanitize(updateSubscriptionEntity);

        final String sanitizedField = objectMapper
            .readTree(updateSubscriptionEntity.getConfiguration())
            .path("aConfigurationFieldToSanitize")
            .asText();
        Assertions.assertThat(sanitizedField).isEqualTo("sanitized");
    }

    @Test
    public void shouldAcceptNullConfigurationForUpdateSubscription() {
        UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
        updateSubscriptionEntity.setConfiguration((String) null);

        cut.validateAndSanitize(updateSubscriptionEntity);

        assertNull(updateSubscriptionEntity.getConfiguration());
    }

    @Test(expected = SubscriptionEntrypointIdMissingException.class)
    public void shouldThrowIfNoConnectorIdProvidedInConfigurationUpdateSubscriptionConfiguration() {
        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        updateSubscriptionConfigurationEntity.setConfiguration("{\"no-connector-id\": \"it-is-not-a-joke\"}");

        cut.validateAndSanitize(updateSubscriptionConfigurationEntity);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowIfBadConfigurationUpdateSubscriptionConfiguration() {
        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        updateSubscriptionConfigurationEntity.setConfiguration("{\"connectorId\": \"invalid");

        cut.validateAndSanitize(updateSubscriptionConfigurationEntity);
    }

    @Test
    public void shouldSanitizeAndSetConfigurationForUpdateSubscriptionConfiguration() throws JsonProcessingException {
        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        updateSubscriptionConfigurationEntity.setConfiguration(
            "{" + "\"entrypointId\": \"entrypoint_id\", " + "\"aConfigurationFieldToSanitize\": \"to_sanitize\"" + "}"
        );

        when(
            entrypointConnectorPluginService.validateEntrypointSubscriptionConfiguration(
                "entrypoint_id",
                updateSubscriptionConfigurationEntity.getConfiguration()
            )
        )
            .thenReturn("{" + "\"connectorId\": \"entrypoint_id\", " + "\"aConfigurationFieldToSanitize\": \"sanitized\"" + "}");

        cut.validateAndSanitize(updateSubscriptionConfigurationEntity);

        final String sanitizedField = objectMapper
            .readTree(updateSubscriptionConfigurationEntity.getConfiguration())
            .path("aConfigurationFieldToSanitize")
            .asText();
        Assertions.assertThat(sanitizedField).isEqualTo("sanitized");
    }
}
