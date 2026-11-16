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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginDocumentation;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.model.v4.policy.ApiProtocolType;
import io.gravitee.rest.api.model.v4.policy.FlowPhase;
import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PolicyPluginServiceImplTest {

    private static final String PLUGIN_ID = "my-test-plugin";
    private static final String CONFIGURATION = "my-test-configuration";
    private static final String SCHEMA = "my-test-schema";

    private PolicyPluginService cut;

    @Mock
    private JsonSchemaService jsonSchemaService;

    @Mock
    private ConfigurablePluginManager<PolicyPlugin<?>> pluginManager;

    @Mock
    private PolicyPlugin mockPlugin;

    @Mock
    private PluginManifest mockPluginManifest;

    @BeforeEach
    public void setup() {
        cut = new PolicyPluginServiceImpl(jsonSchemaService, pluginManager);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(mockPlugin);
    }

    @Test
    public void shouldValidateConfiguration() throws IOException {
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(mockPlugin);
        when(pluginManager.getSchema(PLUGIN_ID, true)).thenReturn(SCHEMA);
        when(jsonSchemaService.validate(SCHEMA, CONFIGURATION)).thenReturn("fixed-configuration");

        String resultConfiguration = cut.validatePolicyConfiguration(PLUGIN_ID, CONFIGURATION);

        assertEquals("fixed-configuration", resultConfiguration);
    }

    @Test
    public void shouldValidateConfigurationWhenNullConfiguration() throws IOException {
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(mockPlugin);

        String resultConfiguration = cut.validatePolicyConfiguration(PLUGIN_ID, null);

        assertNull(resultConfiguration);
    }

    @Test
    public void should_validateConfiguration_with_internal_policy() {
        var policyId = "shared-policy-group-policy";

        String resultConfiguration = cut.validatePolicyConfiguration(policyId, CONFIGURATION);

        assertEquals(CONFIGURATION, resultConfiguration);
        verify(pluginManager, times(0)).get(policyId, true);
    }

    @Test
    public void shouldFailToValidateConfigurationWhenNoPlugin() {
        assertThrows(PluginNotFoundException.class, () -> {
            when(pluginManager.get(PLUGIN_ID, true)).thenReturn(null);

            cut.validatePolicyConfiguration(PLUGIN_ID, CONFIGURATION);
        });
    }

    @Test
    public void shouldFindById() {
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(pluginManager.get(PLUGIN_ID, true)).thenReturn(mockPlugin);

        PlatformPluginEntity result = cut.findById(PLUGIN_ID);

        assertNotNull(result);
        assertEquals(PLUGIN_ID, result.getId());
    }

    @Test
    public void shouldNotFindById() {
        assertThrows(PluginNotFoundException.class, () -> {
            when(pluginManager.get(PLUGIN_ID, true)).thenReturn(null);

            cut.findById(PLUGIN_ID);
        });
    }

    @Test
    public void should_find_all() {
        when(mockPlugin.id()).thenReturn(PLUGIN_ID);
        when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
        when(pluginManager.findAll(true)).thenReturn(List.of(mockPlugin));
        when(mockPluginManifest.properties()).thenReturn(
            Map.of(
                "http_proxy",
                "REQUEST,RESPONSE",
                "http_message",
                "PUBLISH",
                "native_kafka",
                "PUBLISH, SUBSCRIBE",
                "mcp_proxy",
                "REQUEST,RESPONSE",
                "llm_proxy",
                "REQUEST,RESPONSE",
                "a2a_proxy",
                "REQUEST,RESPONSE"
            )
        );
        Set<PolicyPluginEntity> result = cut.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        PolicyPluginEntity policyPlugin = result.iterator().next();
        assertEquals(PLUGIN_ID, policyPlugin.getId());
        assertEquals(Set.of(FlowPhase.REQUEST, FlowPhase.RESPONSE), policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.HTTP_PROXY));
        assertEquals(Set.of(FlowPhase.PUBLISH), policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.HTTP_MESSAGE));
        assertEquals(Set.of(FlowPhase.PUBLISH, FlowPhase.SUBSCRIBE), policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.NATIVE_KAFKA));
        assertEquals(Set.of(FlowPhase.REQUEST, FlowPhase.RESPONSE), policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.MCP_PROXY));
        assertEquals(Set.of(FlowPhase.REQUEST, FlowPhase.RESPONSE), policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.LLM_PROXY));
        assertEquals(Set.of(FlowPhase.REQUEST, FlowPhase.RESPONSE), policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.A2A_PROXY));
    }

    @Nested
    class GuardianFlowPhaseProperty {

        @Test
        void should_expose_guardian_phases_declared_in_manifest() {
            when(mockPluginManifest.properties()).thenReturn(Map.of("guardian", "REQUEST, RESPONSE", "llm_proxy", "REQUEST"));

            PolicyPluginEntity policyPlugin = cut.findById(PLUGIN_ID);

            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.GUARDIAN))
                    .as("guardian phases")
                    .containsExactlyInAnyOrder(FlowPhase.REQUEST, FlowPhase.RESPONSE);
                soft
                    .assertThat(policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.LLM_PROXY))
                    .as("llm-proxy phases")
                    .containsExactly(FlowPhase.REQUEST);
            });
        }

        @Test
        void should_expose_no_guardian_phase_when_manifest_does_not_declare_it() {
            when(mockPluginManifest.properties()).thenReturn(Map.of("llm_proxy", "REQUEST,RESPONSE"));

            PolicyPluginEntity policyPlugin = cut.findById(PLUGIN_ID);

            assertThat(policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.GUARDIAN)).as("guardian phases").isNotNull().isEmpty();
        }

        @Test
        void should_expose_no_guardian_phase_when_manifest_declares_it_blank() {
            when(mockPluginManifest.properties()).thenReturn(Map.of("guardian", " ", "llm_proxy", "REQUEST"));

            PolicyPluginEntity policyPlugin = cut.findById(PLUGIN_ID);

            assertThat(policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.GUARDIAN)).as("guardian phases").isNotNull().isEmpty();
        }
    }

    @Nested
    public class DeprecatedFlowPhaseProperty {

        @Test
        public void should_find_all() {
            when(mockPlugin.id()).thenReturn(PLUGIN_ID);
            when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
            when(pluginManager.findAll(true)).thenReturn(List.of(mockPlugin));
            when(mockPluginManifest.properties()).thenReturn(Map.of("proxy", "REQUEST", "message", "PUBLISH"));
            Set<PolicyPluginEntity> result = cut.findAll();

            assertNotNull(result);
            assertEquals(1, result.size());
            PolicyPluginEntity policyPlugin = result.iterator().next();
            assertEquals(PLUGIN_ID, policyPlugin.getId());
            assertEquals(Set.of(FlowPhase.REQUEST), policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.HTTP_PROXY));
            assertEquals(Set.of(FlowPhase.PUBLISH), policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.HTTP_MESSAGE));
        }

        @Test
        public void should_find_all_with_legacy_phase() {
            when(mockPlugin.id()).thenReturn(PLUGIN_ID);
            when(mockPlugin.manifest()).thenReturn(mockPluginManifest);
            when(pluginManager.findAll(true)).thenReturn(List.of(mockPlugin));
            when(mockPluginManifest.properties()).thenReturn(Map.of("message", "MESSAGE_REQUEST, MESSAGE_RESPONSE"));
            Set<PolicyPluginEntity> result = cut.findAll();

            assertNotNull(result);
            assertEquals(1, result.size());
            PolicyPluginEntity policyPlugin = result.iterator().next();
            assertEquals(PLUGIN_ID, policyPlugin.getId());
            assertEquals(
                Set.of(FlowPhase.PUBLISH, FlowPhase.SUBSCRIBE),
                policyPlugin.getFlowPhaseCompatibility(ApiProtocolType.HTTP_MESSAGE)
            );
        }
    }

    @Test
    public void should_get_schema_with_ApiProtocolType() throws IOException {
        when(pluginManager.getSchema("my-policy", "http_proxy.schema", false, true)).thenReturn("http_proxy");

        String schema = cut.getSchema("my-policy", ApiProtocolType.HTTP_PROXY, null);
        assertEquals("http_proxy", schema);
    }

    @Test
    void should_get_guardian_schema() throws IOException {
        when(pluginManager.getSchema(PLUGIN_ID, "guardian.schema", false, true)).thenReturn("guardian-schema");

        String schema = cut.getSchema(PLUGIN_ID, ApiProtocolType.GUARDIAN, null);

        assertThat(schema).as("schema resolved from the guardian.schema key").isEqualTo("guardian-schema");
    }

    @Test
    public void should_get_schema_with_fallback() throws IOException {
        when(pluginManager.getSchema("my-policy", "http_proxy.schema", false, true)).thenReturn(null);
        when(pluginManager.getSchema("my-policy", "schema", false, true)).thenReturn("schema");

        String schema = cut.getSchema("my-policy", ApiProtocolType.HTTP_PROXY, null);
        assertEquals("schema", schema);
    }

    @Test
    public void should_get_default_schema_when_IOException() throws IOException {
        when(pluginManager.getSchema("my-policy", "http_proxy.schema", false, true)).thenThrow(new IOException());
        when(pluginManager.getSchema("my-policy", true)).thenReturn("default-configuration");

        String schema = cut.getSchema("my-policy", ApiProtocolType.HTTP_PROXY, null);
        assertEquals("default-configuration", schema);
    }

    @Test
    public void should_get_documentation_with_ApiProtocolType() throws IOException {
        when(pluginManager.getPluginDocumentation("my-policy", "native_kafka.documentation", true, true)).thenReturn(
            new PluginDocumentation("documentation", PluginDocumentation.Language.ASCIIDOC)
        );

        PluginDocumentation documentation = cut.getDocumentation("my-policy", ApiProtocolType.NATIVE_KAFKA);
        assertEquals("documentation", documentation.content());
        assertEquals(PluginDocumentation.Language.ASCIIDOC, documentation.language());
    }

    @Test
    void should_get_guardian_documentation() throws IOException {
        when(pluginManager.getPluginDocumentation(PLUGIN_ID, "guardian.documentation", true, true)).thenReturn(
            new PluginDocumentation("guardian documentation", PluginDocumentation.Language.MARKDOWN)
        );

        PluginDocumentation documentation = cut.getDocumentation(PLUGIN_ID, ApiProtocolType.GUARDIAN);

        assertThat(documentation.content())
            .as("documentation resolved from the guardian.documentation key")
            .isEqualTo("guardian documentation");
    }

    @Nested
    public class SupportsMessageCondition {

        private PolicyPluginEntity findWithProperties(Map<String, String> properties) {
            when(mockPluginManifest.properties()).thenReturn(properties);
            return cut.findById(PLUGIN_ID);
        }

        @Test
        public void should_support_message_condition_by_default() {
            assertTrue(findWithProperties(Map.of("native_kafka", "PUBLISH,SUBSCRIBE")).isSupportsMessageCondition());
        }

        @Test
        public void should_not_support_message_condition_when_plugin_declares_it() {
            assertFalse(findWithProperties(Map.of("supports_message_condition", "false")).isSupportsMessageCondition());
        }

        @Test
        public void should_support_message_condition_when_plugin_has_no_properties() {
            when(mockPluginManifest.properties()).thenReturn(null);
            assertTrue(PolicyPluginServiceImpl.supportsMessageCondition(mockPlugin));
        }

        @Test
        public void should_ignore_case_and_whitespace_of_the_declaration() {
            assertFalse(findWithProperties(Map.of("supports_message_condition", " FALSE ")).isSupportsMessageCondition());
        }

        @Test
        public void should_support_message_condition_when_plugin_declares_it_supported() {
            assertTrue(findWithProperties(Map.of("supports_message_condition", "true")).isSupportsMessageCondition());
        }

        @Test
        public void should_expose_the_declaration_in_find_all() {
            when(pluginManager.findAll(true)).thenReturn(List.of(mockPlugin));
            when(mockPluginManifest.properties()).thenReturn(Map.of("supports_message_condition", "false"));

            assertFalse(cut.findAll().iterator().next().isSupportsMessageCondition());
        }
    }
}
