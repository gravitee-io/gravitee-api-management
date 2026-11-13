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
package io.gravitee.gamma.rest.resources.gravitee_plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.infra.config.GammaPluginsConfiguration;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resources.gravitee_plugin.PolicyPluginResourceTest.PolicyPluginTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.agentprovider.AgentProviderPlugin;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.rest.api.model.permissions.RolePermission;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { ResourceContextConfiguration.class, PolicyPluginTestConfiguration.class })
class PolicyPluginResourceTest extends AbstractResourceTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FEATURE = "apim-guardian-policy-rule";

    @Inject
    private ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager;

    @Inject
    private LicenseManager licenseManager;

    private final List<PolicyPlugin<?>> installedPolicies = new ArrayList<>();

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/plugins/policies";
    }

    @BeforeEach
    void givenALicenseEnablingTheFeature() {
        var license = mock(License.class);
        when(license.isFeatureEnabled(FEATURE)).thenReturn(true);
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION)).thenReturn(license);
        when(policyPluginManager.findAll(true)).thenReturn(installedPolicies);
    }

    @AfterEach
    void resetPlugins() {
        installedPolicies.clear();
        reset(policyPluginManager, licenseManager);
    }

    @Nested
    class List_policies {

        @Test
        void should_expose_the_plugin_and_every_declared_flow_with_its_raw_phases() {
            given_a_policy(
                "guardrail-rule",
                Map.of(
                    "http_proxy",
                    "REQUEST",
                    "http_message",
                    "PUBLISH,SUBSCRIBE",
                    "native_kafka",
                    "INTERACT",
                    "mcp_proxy",
                    "REQUEST",
                    "llm_proxy",
                    " REQUEST , RESPONSE ",
                    "a2a_proxy",
                    "MESSAGE_REQUEST",
                    "guardian",
                    "REQUEST,RESPONSE"
                )
            );

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode plugin = response.readEntity(JsonNode.class).get(0);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(plugin.get("id").asText()).as("id").isEqualTo("guardrail-rule");
                soft.assertThat(plugin.get("name").asText()).as("name").isEqualTo("guardrail-rule");
                soft.assertThat(plugin.get("description").asText()).as("description").isEqualTo("Description of guardrail-rule");
                soft.assertThat(plugin.get("category").asText()).as("category").isEqualTo("guardrail");
                soft.assertThat(plugin.get("version").asText()).as("version").isEqualTo("1.2.3");
                soft.assertThat(plugin.get("icon").asText()).as("icon").isEqualTo("data:image/svg+xml;base64,guardrail-rule");
                soft.assertThat(plugin.get("feature").asText()).as("feature").isEqualTo(FEATURE);
                soft.assertThat(plugin.get("deployed").asBoolean()).as("deployed").isTrue();
                soft
                    .assertThat(plugin.get("flow"))
                    .as("flows in kebab-case, phases trimmed but neither validated nor reduced")
                    .isEqualTo(
                        json(
                            """
                            {
                              "http-proxy": ["REQUEST"],
                              "http-message": ["PUBLISH", "SUBSCRIBE"],
                              "native-kafka": ["INTERACT"],
                              "mcp-proxy": ["REQUEST"],
                              "llm-proxy": ["REQUEST", "RESPONSE"],
                              "a2a-proxy": ["MESSAGE_REQUEST"],
                              "guardian": ["REQUEST", "RESPONSE"]
                            }
                            """
                        )
                    );
            });
        }

        @Test
        void should_read_the_deprecated_proxy_and_message_keys_when_the_flow_key_declares_nothing() {
            given_a_policy("legacy", Map.of("proxy", "REQUEST", "message", "MESSAGE_REQUEST"));
            given_a_policy("blank-flow-key", Map.of("http_proxy", " ", "proxy", "REQUEST"));
            given_a_policy("both", Map.of("http_proxy", "RESPONSE", "proxy", "REQUEST"));

            JsonNode policies = rootTarget().request().get().readEntity(JsonNode.class);

            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(flowOf(policies, "legacy"))
                    .as("deprecated keys")
                    .isEqualTo(json("{\"http-proxy\": [\"REQUEST\"], \"http-message\": [\"MESSAGE_REQUEST\"]}"));
                soft.assertThat(flowOf(policies, "blank-flow-key")).as("blank flow key").isEqualTo(json("{\"http-proxy\": [\"REQUEST\"]}"));
                soft
                    .assertThat(flowOf(policies, "both"))
                    .as("the flow key wins over the deprecated one")
                    .isEqualTo(json("{\"http-proxy\": [\"RESPONSE\"]}"));
            });
        }

        @Test
        void should_leave_out_the_flows_a_plugin_does_not_declare() {
            given_a_policy("blank", Map.of("llm_proxy", " ", "guardian", ""));
            given_a_policy("no-properties", null);

            JsonNode policies = rootTarget().request().get().readEntity(JsonNode.class);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(flowOf(policies, "blank")).as("blank declarations").isEmpty();
                soft.assertThat(flowOf(policies, "no-properties")).as("no manifest properties").isEmpty();
            });
        }

        @Test
        void should_report_a_plugin_whose_feature_the_license_disables_as_not_deployed() {
            given_a_policy("unlicensed", Map.of("guardian", "REQUEST"), manifest -> when(manifest.feature()).thenReturn("other-feature"));

            JsonNode policies = rootTarget().request().get().readEntity(JsonNode.class);

            assertThat(policies)
                .singleElement()
                .satisfies(policy -> assertThat(policy.get("deployed").asBoolean()).as("deployed").isFalse());
        }

        @Test
        void should_sort_the_policies_by_name_regardless_of_case_then_by_id() {
            given_a_policy("a-policy", Map.of(), named("gamma"));
            given_a_policy("z-twin", Map.of(), named("Delta"));
            given_a_policy("c-policy", Map.of(), named("beta"));
            given_a_policy("y-twin", Map.of(), named("Delta"));
            given_a_policy("b-policy", Map.of(), named("Alpha"));

            JsonNode policies = rootTarget().request().get().readEntity(JsonNode.class);

            assertThat(idsOf(policies))
                .as("policies sorted by name, then by id for the same name")
                .containsExactly("b-policy", "c-policy", "y-twin", "z-twin", "a-policy");
        }

        @ParameterizedTest(name = "flow={0} phase={1}")
        @MethodSource("filters")
        void should_keep_the_policies_matching_the_filters(List<String> flows, String phase, List<String> expectedIds) {
            given_a_policy("guard-both", Map.of("guardian", "REQUEST,RESPONSE", "llm_proxy", "REQUEST,RESPONSE"));
            given_a_policy("guard-request-llm-response", Map.of("guardian", "REQUEST", "llm_proxy", "RESPONSE"));
            given_a_policy("llm-only", Map.of("llm_proxy", "REQUEST"));
            given_a_policy("http-only", Map.of("http_proxy", "REQUEST"));
            WebTarget target = rootTarget();
            if (!flows.isEmpty()) {
                target = target.queryParam("flow", flows.toArray());
            }
            if (phase != null) {
                target = target.queryParam("phase", phase);
            }

            Response response = target.request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(idsOf(response.readEntity(JsonNode.class))).as("kept policies").containsExactlyInAnyOrderElementsOf(expectedIds);
        }

        static Stream<Arguments> filters() {
            return Stream.of(
                Arguments.of(List.of(), null, List.of("guard-both", "guard-request-llm-response", "llm-only", "http-only")),
                Arguments.of(List.of("guardian"), null, List.of("guard-both", "guard-request-llm-response")),
                Arguments.of(List.of("guardian", "llm-proxy"), null, List.of("guard-both", "guard-request-llm-response")),
                Arguments.of(List.of("http-proxy", "guardian"), null, List.of()),
                Arguments.of(List.of(), "RESPONSE", List.of("guard-both", "guard-request-llm-response")),
                Arguments.of(List.of("guardian"), "RESPONSE", List.of("guard-both")),
                Arguments.of(List.of("llm-proxy"), "RESPONSE", List.of("guard-both", "guard-request-llm-response")),
                Arguments.of(List.of("guardian", "llm-proxy"), "REQUEST", List.of("guard-both")),
                Arguments.of(List.of(), "request", List.of()),
                Arguments.of(List.of(), " ", List.of("guard-both", "guard-request-llm-response", "llm-only", "http-only")),
                Arguments.of(List.of("guardian"), "", List.of("guard-both", "guard-request-llm-response"))
            );
        }

        @ParameterizedTest
        @ValueSource(strings = { "unknown", "llm_proxy", "LLM_PROXY" })
        void should_refuse_an_unknown_flow(String flow) {
            given_a_policy("guard-both", Map.of("guardian", "REQUEST"));

            Response response = rootTarget().queryParam("flow", "guardian", flow).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("message").asText()).as("error message").contains(flow);
        }
    }

    @Nested
    class Get_policy {

        @Test
        void should_get_a_policy_with_its_flows() {
            given_a_policy("guardrail-rule", Map.of("guardian", "REQUEST,RESPONSE", "llm_proxy", "REQUEST"));

            Response response = rootTarget("guardrail-rule").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode plugin = response.readEntity(JsonNode.class);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(plugin.get("id").asText()).as("id").isEqualTo("guardrail-rule");
                soft.assertThat(plugin.get("category").asText()).as("category").isEqualTo("guardrail");
                soft.assertThat(plugin.get("icon").asText()).as("icon").isEqualTo("data:image/svg+xml;base64,guardrail-rule");
                soft
                    .assertThat(plugin.get("flow"))
                    .as("flow")
                    .isEqualTo(json("{\"llm-proxy\": [\"REQUEST\"], \"guardian\": [\"REQUEST\", \"RESPONSE\"]}"));
            });
        }

        @Test
        void should_return_404_when_the_policy_is_unknown() {
            Response response = rootTarget("unknown").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_get_the_schema_of_a_policy() throws IOException {
            given_a_policy("guardrail-rule", Map.of());
            when(policyPluginManager.getSchema("guardrail-rule", true)).thenReturn("{\"type\":\"object\"}");

            Response response = rootTarget("guardrail-rule/schema").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.readEntity(JsonNode.class)).as("schema").isEqualTo(json("{\"type\":\"object\"}"));
        }

        @Test
        void should_return_404_when_getting_the_schema_of_an_unknown_policy() {
            Response response = rootTarget("unknown/schema").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_get_the_documentation_of_a_policy() throws IOException {
            given_a_policy("guardrail-rule", Map.of());
            when(policyPluginManager.getDocumentation("guardrail-rule", true)).thenReturn("# Guardrail rule");

            Response response = rootTarget("guardrail-rule/documentation").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.readEntity(String.class)).as("documentation").isEqualTo("# Guardrail rule");
        }

        @Test
        void should_return_404_when_getting_the_documentation_of_an_unknown_policy() {
            Response response = rootTarget("unknown/documentation").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }
    }

    /**
     * One case per endpoint: {@code @Permissions} is declared per method, so a single case would pass while another
     * endpoint lost its guard.
     */
    @Nested
    class Authorization {

        @BeforeEach
        void given_an_installed_policy() throws IOException {
            given_a_policy("guardrail-rule", Map.of("guardian", "REQUEST"));
            when(policyPluginManager.getSchema("guardrail-rule", true)).thenReturn("{}");
            when(policyPluginManager.getDocumentation("guardrail-rule", true)).thenReturn("# Guardrail rule");
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "guardrail-rule", "guardrail-rule/schema", "guardrail-rule/documentation" })
        void should_answer_with_the_organization_policies_read_permission(String path) {
            givenOnlyReadPermission(RolePermission.ORGANIZATION_POLICIES, ORGANIZATION);

            assertThat(rootTarget(path).request().get().getStatus()).as("GET /%s", path).isEqualTo(HttpStatusCode.OK_200);
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "guardrail-rule", "guardrail-rule/schema", "guardrail-rule/documentation" })
        void should_return_403_without_the_organization_policies_read_permission(String path) {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            assertThat(rootTarget(path).request().get().getStatus()).as("GET /%s", path).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }
    }

    private void given_a_policy(String id, Map<String, String> properties) {
        given_a_policy(id, properties, manifest -> {});
    }

    private void given_a_policy(String id, Map<String, String> properties, Consumer<PluginManifest> customizer) {
        var manifest = mock(PluginManifest.class);
        when(manifest.name()).thenReturn(id);
        when(manifest.description()).thenReturn("Description of " + id);
        when(manifest.category()).thenReturn("guardrail");
        when(manifest.version()).thenReturn("1.2.3");
        when(manifest.feature()).thenReturn(FEATURE);
        when(manifest.properties()).thenReturn(properties == null ? null : new HashMap<>(properties));
        customizer.accept(manifest);
        PolicyPlugin<?> plugin = mock(PolicyPlugin.class);
        when(plugin.id()).thenReturn(id);
        when(plugin.manifest()).thenReturn(manifest);
        when(plugin.deployed()).thenReturn(true);
        installedPolicies.add(plugin);
        doReturn(plugin).when(policyPluginManager).get(id, true);
        try {
            when(policyPluginManager.getIcon(id, true)).thenReturn("data:image/svg+xml;base64," + id);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonNode json(String content) {
        try {
            return JSON.readTree(content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonNode flowOf(JsonNode policies, String id) {
        var matching = StreamSupport.stream(policies.spliterator(), false)
            .filter(policy -> id.equals(policy.get("id").asText()))
            .toList();
        assertThat(matching).as("listed policies with id %s", id).hasSize(1);
        return matching.get(0).get("flow");
    }

    private static Consumer<PluginManifest> named(String name) {
        return manifest -> when(manifest.name()).thenReturn(name);
    }

    private static List<String> idsOf(JsonNode policies) {
        return StreamSupport.stream(policies.spliterator(), false)
            .map(policy -> policy.get("id").asText())
            .toList();
    }

    @Configuration
    @Import(GammaPluginsConfiguration.class)
    static class PolicyPluginTestConfiguration {

        @Bean
        ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager() {
            return mock(ConfigurablePluginManager.class);
        }

        @Bean
        ConfigurablePluginManager<AgentProviderPlugin> agentProviderPluginManager() {
            return mock(ConfigurablePluginManager.class);
        }
    }
}
