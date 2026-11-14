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
package io.gravitee.rest.api.service.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.application.AgentSettings;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import org.junit.jupiter.api.Test;

public class ApplicationConverterTest {

    ApplicationConverter applicationConverter = new ApplicationConverter();

    @Test
    public void newApplicationEntity_toApplication_should_derive_agent_type_from_agent_settings() {
        Application application = applicationConverter.toApplication(newEntityWithAgentSettings("agent.support", "agent-client-id"));

        assertSame(ApplicationType.AGENT, application.getType());
    }

    @Test
    public void newApplicationEntity_toApplication_should_store_agent_link_and_client_id_as_metadata() {
        Application application = applicationConverter.toApplication(newEntityWithAgentSettings("agent.support", "agent-client-id"));

        assertEquals("agent.support", application.getMetadata().get("agent_entity_id"));
        assertEquals("agent-client-id", application.getMetadata().get("client_id"));
    }

    @Test
    public void newApplicationEntity_toApplication_should_keep_simple_type_for_app_settings_whatever_their_type_string() {
        SimpleApplicationSettings app = new SimpleApplicationSettings();
        app.setType("agent");
        ApplicationSettings settings = new ApplicationSettings();
        settings.setApp(app);
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setSettings(settings);

        Application application = applicationConverter.toApplication(newApplicationEntity);

        assertSame(ApplicationType.SIMPLE, application.getType());
    }

    private static NewApplicationEntity newEntityWithAgentSettings(String entityId, String clientId) {
        AgentSettings agent = new AgentSettings();
        agent.setEntityId(entityId);
        agent.setClientId(clientId);
        ApplicationSettings settings = new ApplicationSettings();
        settings.setAgent(agent);
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setSettings(settings);
        return newApplicationEntity;
    }

    @Test
    public void newApplicationEntity_toApplication_should_convert_ApiKeyMode() {
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setApiKeyMode(ApiKeyMode.EXCLUSIVE);

        Application application = applicationConverter.toApplication(newApplicationEntity);

        assertSame(io.gravitee.repository.management.model.ApiKeyMode.EXCLUSIVE, application.getApiKeyMode());
    }

    @Test
    public void newApplicationEntity_toApplication_should_set_unspecified_byDefault_if_null_ApiKeyMode() {
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setApiKeyMode(null);

        Application application = applicationConverter.toApplication(newApplicationEntity);

        assertSame(io.gravitee.repository.management.model.ApiKeyMode.UNSPECIFIED, application.getApiKeyMode());
    }

    @Test
    public void updateApplicationEntity_toApplication_should_convert_ApiKeyMode() {
        UpdateApplicationEntity updateApplicationEntity = new UpdateApplicationEntity();
        updateApplicationEntity.setApiKeyMode(ApiKeyMode.EXCLUSIVE);

        Application application = applicationConverter.toApplication(updateApplicationEntity);

        assertSame(io.gravitee.repository.management.model.ApiKeyMode.EXCLUSIVE, application.getApiKeyMode());
    }

    @Test
    public void updateApplicationEntity_toApplication_should_set_unspecified_byDefault_if_null_ApiKeyMode() {
        UpdateApplicationEntity updateApplicationEntity = new UpdateApplicationEntity();
        updateApplicationEntity.setApiKeyMode(null);

        Application application = applicationConverter.toApplication(updateApplicationEntity);

        assertSame(io.gravitee.repository.management.model.ApiKeyMode.UNSPECIFIED, application.getApiKeyMode());
    }
}
