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
package io.gravitee.gamma.infra.gravitee_plugin.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.core.domain.gravitee_plugin.exception.PluginNotFoundException;
import io.gravitee.gamma.core.domain.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.gamma.core.port.service_provider.gravitee_plugin.AgentProviderPluginProvider;
import io.gravitee.gamma.infra.gravitee_plugin.resource.AgentProviderPluginResourceTest.AgentProviderPluginTestConfiguration;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { ResourceContextConfiguration.class, AgentProviderPluginTestConfiguration.class })
class AgentProviderPluginResourceTest extends AbstractResourceTest {

    private static final String PLUGIN_ID = "azure-foundry";

    @Inject
    private AgentProviderPluginProvider agentProviderPluginProvider;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/plugins/agent-providers";
    }

    @AfterEach
    void resetProvider() {
        reset(agentProviderPluginProvider);
    }

    @Test
    void should_list_agent_provider_plugins() {
        when(agentProviderPluginProvider.findAll()).thenReturn(Set.of(aPlugin()));

        Response response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        JsonNode body = response.readEntity(JsonNode.class);
        assertThat(body).hasSize(1);
        assertPlugin(body.get(0));
    }

    @Test
    void should_return_empty_list_when_no_plugin_loaded() {
        when(agentProviderPluginProvider.findAll()).thenReturn(Set.of());

        Response response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(response.readEntity(JsonNode.class)).isEmpty();
    }

    @Test
    void should_get_agent_provider_plugin() {
        when(agentProviderPluginProvider.findById(PLUGIN_ID)).thenReturn(aPlugin());

        Response response = rootTarget(PLUGIN_ID).request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        assertPlugin(response.readEntity(JsonNode.class));
    }

    @Test
    void should_return_404_when_plugin_not_found() {
        when(agentProviderPluginProvider.findById("unknown")).thenThrow(new PluginNotFoundException("unknown"));

        Response response = rootTarget("unknown").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    void should_get_agent_provider_plugin_schema() {
        when(agentProviderPluginProvider.getSchema(PLUGIN_ID)).thenReturn("{\"type\":\"object\"}");

        Response response = rootTarget(PLUGIN_ID + "/schema").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(response.readEntity(JsonNode.class).get("type").asText()).isEqualTo("object");
    }

    private static PlatformPlugin aPlugin() {
        return PlatformPlugin.builder()
            .id(PLUGIN_ID)
            .name("Azure Foundry")
            .description("Azure AI Foundry agent provider")
            .category("agent-provider")
            .version("1.0.0")
            .icon("data:image/svg+xml;base64,icon")
            .feature("apim-agent-provider")
            .deployed(true)
            .build();
    }

    private static void assertPlugin(JsonNode plugin) {
        assertThat(plugin.get("id").asText()).isEqualTo(PLUGIN_ID);
        assertThat(plugin.get("name").asText()).isEqualTo("Azure Foundry");
        assertThat(plugin.get("description").asText()).isEqualTo("Azure AI Foundry agent provider");
        assertThat(plugin.get("category").asText()).isEqualTo("agent-provider");
        assertThat(plugin.get("version").asText()).isEqualTo("1.0.0");
        assertThat(plugin.get("icon").asText()).isEqualTo("data:image/svg+xml;base64,icon");
        assertThat(plugin.get("feature").asText()).isEqualTo("apim-agent-provider");
        assertThat(plugin.get("deployed").asBoolean()).isTrue();
    }

    @Configuration
    static class AgentProviderPluginTestConfiguration {

        @Bean
        AgentProviderPluginProvider agentProviderPluginProvider() {
            return mock(AgentProviderPluginProvider.class);
        }
    }
}
