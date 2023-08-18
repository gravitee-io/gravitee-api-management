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
package io.gravitee.rest.api.management.v2.rest.resource.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.ExecutionPhase;
import io.gravitee.rest.api.management.v2.rest.model.PolicyPlugin;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class PoliciesResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "/plugins/policies";
    }

    private static final String FAKE_POLICY_ID = "my_policy";

    @AfterEach
    void tearDown() {
        reset(policyPluginService);
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldReturnSortedPolicies() {
        PolicyPluginEntity policyPlugin = getPolicyPluginEntity();
        PolicyPluginEntity anotherPolicyPlugin = getPolicyPluginEntity();
        anotherPolicyPlugin.setId("another-policy");
        anotherPolicyPlugin.setName("another policy plugin");

        when(policyPluginService.findAll()).thenReturn(Set.of(policyPlugin, anotherPolicyPlugin));

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check response content
        final Set<PolicyPlugin> policyPlugins = response.readEntity(new GenericType<>() {});

        // Check data
        PolicyPlugin expectedPlugin1 = new PolicyPlugin()
            .id("another-policy")
            .name("another policy plugin")
            .version("1.0")
            .icon("my-icon")
            .category("my-category")
            .description("my-description")
            .deployed(true)
            .addProxyItem(ExecutionPhase.REQUEST)
            .addProxyItem(ExecutionPhase.RESPONSE)
            .addMessageItem(ExecutionPhase.MESSAGE_REQUEST);

        PolicyPlugin expectedPlugin2 = new PolicyPlugin()
            .id("id")
            .name("name")
            .version("1.0")
            .icon("my-icon")
            .category("my-category")
            .description("my-description")
            .deployed(true)
            .addProxyItem(ExecutionPhase.REQUEST)
            .addProxyItem(ExecutionPhase.RESPONSE)
            .addMessageItem(ExecutionPhase.MESSAGE_REQUEST);

        assertEquals(Set.of(expectedPlugin1, expectedPlugin2), policyPlugins);
    }

    @Test
    public void shouldGetPolicySchema() {
        PolicyPluginEntity policyPlugin = new PolicyPluginEntity();
        policyPlugin.setId(FAKE_POLICY_ID);
        policyPlugin.setName("Fake Endpoint");
        policyPlugin.setVersion("1.0");
        policyPlugin.setIcon("my-icon");
        policyPlugin.setCategory("my-category");
        policyPlugin.setDescription("my-description");
        when(policyPluginService.findById(FAKE_POLICY_ID)).thenReturn(policyPlugin);
        when(policyPluginService.getSchema(FAKE_POLICY_ID)).thenReturn("schemaResponse");

        final Response response = rootTarget(FAKE_POLICY_ID).path("schema").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("schemaResponse");
    }

    @Test
    public void shouldGetPolicySchemaWithDisplay() {
        PolicyPluginEntity policyPlugin = new PolicyPluginEntity();
        policyPlugin.setId(FAKE_POLICY_ID);
        policyPlugin.setName("Fake Endpoint");
        policyPlugin.setVersion("1.0");
        policyPlugin.setIcon("my-icon");
        policyPlugin.setCategory("my-category");
        policyPlugin.setDescription("my-description");
        when(policyPluginService.findById(FAKE_POLICY_ID)).thenReturn(policyPlugin);
        when(policyPluginService.getSchema(FAKE_POLICY_ID, SchemaDisplayFormat.GV_SCHEMA_FORM)).thenReturn("schemaResponse");

        final Response response = rootTarget(FAKE_POLICY_ID).path("schema").queryParam("display", "gv-schema-form").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("schemaResponse");
    }

    @Test
    public void shouldNotGetPolicySchemaWhenPluginNotFound() {
        PolicyPluginEntity policyPlugin = new PolicyPluginEntity();
        policyPlugin.setId(FAKE_POLICY_ID);
        policyPlugin.setName("Fake Endpoint");
        policyPlugin.setVersion("1.0");
        policyPlugin.setIcon("my-icon");
        policyPlugin.setCategory("my-category");
        policyPlugin.setDescription("my-description");
        when(policyPluginService.findById(FAKE_POLICY_ID)).thenThrow(new PluginNotFoundException(FAKE_POLICY_ID));

        final Response response = rootTarget(FAKE_POLICY_ID).path("schema").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        final Error error = response.readEntity(Error.class);

        final Error expectedError = new Error();
        expectedError.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedError.setMessage("Plugin [" + FAKE_POLICY_ID + "] cannot be found.");
        expectedError.setTechnicalCode("plugin.notFound");
        expectedError.setParameters(Map.of("plugin", FAKE_POLICY_ID));

        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @Test
    public void shouldGetPolicyDocumentation() {
        PolicyPluginEntity policyPlugin = new PolicyPluginEntity();
        policyPlugin.setId(FAKE_POLICY_ID);
        policyPlugin.setName("Fake Endpoint");
        policyPlugin.setVersion("1.0");
        policyPlugin.setIcon("my-icon");
        policyPlugin.setCategory("my-category");
        policyPlugin.setDescription("my-description");
        when(policyPluginService.findById(FAKE_POLICY_ID)).thenReturn(policyPlugin);
        when(policyPluginService.getDocumentation(FAKE_POLICY_ID)).thenReturn("documentationResponse");

        final Response response = rootTarget(FAKE_POLICY_ID).path("documentation").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("documentationResponse");
    }

    @Test
    public void shouldNotGetPolicyDocumentationWhenPluginNotFound() {
        PolicyPluginEntity policyPlugin = new PolicyPluginEntity();
        policyPlugin.setId(FAKE_POLICY_ID);
        policyPlugin.setName("Fake Endpoint");
        policyPlugin.setVersion("1.0");
        policyPlugin.setIcon("my-icon");
        policyPlugin.setCategory("my-category");
        policyPlugin.setDescription("my-description");
        when(policyPluginService.findById(FAKE_POLICY_ID)).thenThrow(new PluginNotFoundException(FAKE_POLICY_ID));

        final Response response = rootTarget(FAKE_POLICY_ID).path("documentation").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());

        final Error error = response.readEntity(Error.class);

        final Error expectedError = new Error();
        expectedError.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedError.setMessage("Plugin [" + FAKE_POLICY_ID + "] cannot be found.");
        expectedError.setTechnicalCode("plugin.notFound");
        expectedError.setParameters(Map.of("plugin", FAKE_POLICY_ID));

        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @Test
    public void shouldGetPolicyById() {
        PolicyPluginEntity policyPlugin = new PolicyPluginEntity();
        policyPlugin.setId(FAKE_POLICY_ID);
        policyPlugin.setName("Fake Endpoint");
        policyPlugin.setVersion("1.0");
        policyPlugin.setIcon("my-icon");
        policyPlugin.setCategory("my-category");
        policyPlugin.setDescription("my-description");
        when(policyPluginService.findById(FAKE_POLICY_ID)).thenReturn(policyPlugin);

        final Response response = rootTarget(FAKE_POLICY_ID).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final PolicyPlugin policy = response.readEntity(PolicyPlugin.class);
        assertNotNull(policy);
        assertEquals(FAKE_POLICY_ID, policy.getId());
        assertEquals("Fake Endpoint", policy.getName());
        assertEquals("1.0", policy.getVersion());
        assertEquals("my-icon", policy.getIcon());
        assertEquals("my-category", policy.getCategory());
        assertEquals("my-description", policy.getDescription());
    }

    @NotNull
    private PolicyPluginEntity getPolicyPluginEntity() {
        PolicyPluginEntity policyPlugin = new PolicyPluginEntity();
        policyPlugin.setId("id");
        policyPlugin.setName("name");
        policyPlugin.setVersion("1.0");
        policyPlugin.setIcon("my-icon");
        policyPlugin.setCategory("my-category");
        policyPlugin.setDescription("my-description");
        policyPlugin.setDeployed(true);
        policyPlugin.setProxy(
            Set.of(
                io.gravitee.rest.api.model.v4.policy.ExecutionPhase.REQUEST,
                io.gravitee.rest.api.model.v4.policy.ExecutionPhase.RESPONSE
            )
        );
        policyPlugin.setMessage(Set.of(io.gravitee.rest.api.model.v4.policy.ExecutionPhase.MESSAGE_REQUEST));
        return policyPlugin;
    }
}
