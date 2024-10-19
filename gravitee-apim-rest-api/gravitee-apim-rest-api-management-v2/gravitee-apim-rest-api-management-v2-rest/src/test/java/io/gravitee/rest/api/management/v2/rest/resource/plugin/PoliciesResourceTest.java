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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import inmemory.PolicyPluginQueryServiceInMemory;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.PolicyPlugin;
import io.gravitee.rest.api.management.v2.rest.model.PolicyPluginAllOfFlowPhaseCompatibility;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import io.gravitee.rest.api.model.v4.policy.ApiProtocolType;
import io.gravitee.rest.api.model.v4.policy.FlowPhase;
import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PoliciesResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "/plugins/policies";
    }

    @Autowired
    protected PolicyPluginQueryServiceInMemory policyPluginQueryServiceInMemory;

    @Autowired
    protected LicenseManager licenseManager;

    private static final String FAKE_POLICY_ID = "my_policy";

    @AfterEach
    public void tearDown() {
        super.tearDown();
        reset(policyPluginService);
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldReturnSortedPolicies() {
        policyPluginQueryServiceInMemory.initWith(
            List.of(
                io.gravitee.apim.core.plugin.model.PolicyPlugin
                    .builder()
                    .id("policy-1")
                    .name("policy-1")
                    .feature("feature-1")
                    .deployed(false)
                    .build(),
                io.gravitee.apim.core.plugin.model.PolicyPlugin
                    .builder()
                    .id("policy-2")
                    .name("policy-2")
                    .feature("feature-2")
                    .deployed(true)
                    .build(),
                io.gravitee.apim.core.plugin.model.PolicyPlugin
                    .builder()
                    .id("policy-3")
                    .name("policy-3")
                    .feature("feature-3")
                    .flowPhaseCompatibility(
                        Map.of(
                            ApiProtocolType.HTTP_PROXY,
                            Set.of(FlowPhase.REQUEST, FlowPhase.RESPONSE),
                            ApiProtocolType.HTTP_MESSAGE,
                            Set.of(FlowPhase.PUBLISH),
                            ApiProtocolType.NATIVE_KAFKA,
                            Set.of(FlowPhase.REQUEST, FlowPhase.RESPONSE, FlowPhase.PUBLISH)
                        )
                    )
                    .deployed(true)
                    .build()
            )
        );

        var license = mock(License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform("fake-org")).thenReturn(license);
        when(license.isFeatureEnabled("feature-1")).thenReturn(true);
        when(license.isFeatureEnabled("feature-2")).thenReturn(false);
        when(license.isFeatureEnabled("feature-3")).thenReturn(true);

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check response content
        final Set<PolicyPlugin> policyPlugins = response.readEntity(new GenericType<>() {});

        assertThat(policyPlugins)
            .containsExactlyInAnyOrder(
                PolicyPlugin.builder().id("policy-2").name("policy-2").deployed(false).build(),
                PolicyPlugin
                    .builder()
                    .id("policy-3")
                    .name("policy-3")
                    .deployed(true)
                    .flowPhaseCompatibility(
                        PolicyPluginAllOfFlowPhaseCompatibility
                            .builder()
                            .HTTP_PROXY(
                                Set.of(
                                    io.gravitee.rest.api.management.v2.rest.model.FlowPhase.REQUEST,
                                    io.gravitee.rest.api.management.v2.rest.model.FlowPhase.RESPONSE
                                )
                            )
                            .HTTP_MESSAGE(Set.of(io.gravitee.rest.api.management.v2.rest.model.FlowPhase.PUBLISH))
                            .NATIVE_KAFKA(
                                Set.of(
                                    io.gravitee.rest.api.management.v2.rest.model.FlowPhase.REQUEST,
                                    io.gravitee.rest.api.management.v2.rest.model.FlowPhase.RESPONSE,
                                    io.gravitee.rest.api.management.v2.rest.model.FlowPhase.PUBLISH
                                )
                            )
                            .build()
                    )
                    .build(),
                PolicyPlugin.builder().id("policy-1").name("policy-1").deployed(false).build()
            );
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
        assertThat(result).isEqualTo("schemaResponse");
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
        assertThat(result).isEqualTo("schemaResponse");
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

        assertThat(error).isEqualTo(expectedError);
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
        assertThat(result).isEqualTo("documentationResponse");
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

        assertThat(error).isEqualTo(expectedError);
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
        policyPlugin.putFlowPhaseCompatibility(ApiProtocolType.HTTP_PROXY, Set.of(FlowPhase.REQUEST, FlowPhase.RESPONSE));
        policyPlugin.putFlowPhaseCompatibility(ApiProtocolType.HTTP_MESSAGE, Set.of(FlowPhase.PUBLISH));
        return policyPlugin;
    }
}
