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
package io.gravitee.gamma.rest.infra.service_provider.gravitee_plugin;

import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.A2A_PROXY;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.GUARDIAN;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.HTTP_PROXY;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.LLM_PROXY;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.MCP_PROXY;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianValidator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonPointer;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryPolicyValidation;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.DeclaredFlows;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.ResourceReference;
import io.gravitee.gamma.rest.core.guardian.GuardianFixtures;
import io.gravitee.gamma.rest.core.guardian.domain_service.ValidateGuardianDomainService;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gateway.reactive.api.policy.guardrail.GuardrailPolicy;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

class DefaultPolicyPluginProviderTest {

    private final ConfigurablePluginManager<PolicyPlugin<?>> pluginManager = mock(ConfigurablePluginManager.class);
    private final LicenseManager licenseManager = mock(LicenseManager.class);
    private final DefaultPolicyPluginProvider provider = new DefaultPolicyPluginProvider(pluginManager, licenseManager);

    private void given_installed_policies(PolicyPlugin<?>... plugins) {
        when(pluginManager.findAll(true)).thenReturn(List.of(plugins));
    }

    private PolicyPlugin<?> a_policy(String id, Class<?> policyClass, String category, Map<String, String> properties) {
        var manifest = mock(PluginManifest.class);
        when(manifest.name()).thenReturn("Name of " + id);
        when(manifest.category()).thenReturn(category);
        when(manifest.properties()).thenReturn(properties);
        PolicyPlugin<?> plugin = mock(PolicyPlugin.class);
        when(plugin.id()).thenReturn(id);
        when(plugin.policy()).thenReturn((Class) policyClass);
        when(plugin.manifest()).thenReturn(manifest);
        when(pluginManager.get(id, true)).thenReturn((PolicyPlugin) plugin);
        return plugin;
    }

    @Test
    void should_map_the_installed_guardrail_protocols_without_imposing_a_guardian_phase() {
        given_installed_policies(
            a_policy(
                "rule-guardrail",
                GuardrailPolicy.class,
                "guardrail",
                Map.of("llm_proxy", "RESPONSE", "mcp_proxy", "REQUEST", "a2a_proxy", " ", "http_proxy", "")
            )
        );

        assertThat(provider.findAllCapabilities())
            .containsOnlyKeys("rule-guardrail")
            .extractingByKey("rule-guardrail")
            .satisfies(policy ->
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(policy.guardrail()).as("guardrail").isTrue();
                    softly
                        .assertThat(policy.flows())
                        .as("the declared flows, blank ones left out")
                        .isEqualTo(new DeclaredFlows(Map.of(LLM_PROXY, List.of("RESPONSE"), MCP_PROXY, List.of("REQUEST"))));
                })
            );
    }

    @Test
    void should_trim_the_declared_phases_without_reducing_them() {
        given_installed_policies(
            a_policy(
                "rule-guardrail",
                GuardrailPolicy.class,
                "ai",
                Map.of("llm_proxy", " REQUEST , RESPONSE ", "mcp_proxy", "INTERACT,REQUEST", "a2a_proxy", "MESSAGE_REQUEST")
            )
        );

        assertThat(provider.findAllCapabilities().get("rule-guardrail").flows())
            .as("phases are trimmed, the ones a Guardian never runs in kept: the projection drops them")
            .isEqualTo(
                new DeclaredFlows(
                    Map.of(
                        LLM_PROXY,
                        List.of("REQUEST", "RESPONSE"),
                        MCP_PROXY,
                        List.of("INTERACT", "REQUEST"),
                        A2A_PROXY,
                        List.of("MESSAGE_REQUEST")
                    )
                )
            );
    }

    @Test
    void should_read_the_capabilities_from_the_flows_the_policy_listing_exposes() {
        given_installed_policies(
            a_policy(
                "rule-guardrail",
                GuardrailPolicy.class,
                "guardrail",
                Map.of("proxy", "REQUEST", "llm_proxy", "REQUEST", "guardian", " REQUEST , INTERACT ")
            )
        );
        var license = mock(License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform("org")).thenReturn(license);

        var capabilities = provider.findAllCapabilities().get("rule-guardrail");

        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThat(capabilities.flows())
                .as("one manifest parser for the listing and the Guardian validation")
                .isEqualTo(provider.findPolicyById("org", "rule-guardrail").flows());
            softly
                .assertThat(capabilities.flows())
                .as("the deprecated proxy key read for http-proxy")
                .isEqualTo(
                    new DeclaredFlows(
                        Map.of(HTTP_PROXY, List.of("REQUEST"), LLM_PROXY, List.of("REQUEST"), GUARDIAN, List.of("REQUEST", "INTERACT"))
                    )
                );
            softly.assertThat(capabilities.supports(Set.of(ApiType.LLM_PROXY))).as("supports llm-proxy").isTrue();
            softly.assertThat(capabilities.guardianPhases()).as("guardian phases, INTERACT dropped").containsExactly(Phase.REQUEST);
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "guardrail", "security" })
    void should_reject_an_incompatible_runtime_guardrail_regardless_of_category(String category) {
        given_installed_policies(
            a_policy("rule-guardrail", GuardrailPolicy.class, category, Map.of("llm_proxy", "REQUEST", "guardian", "REQUEST"))
        );
        var guardian = GuardianFixtures.aGuardian(definition ->
            definition.policies(
                List.of(
                    Step.builder()
                        .name("MCP guardrail")
                        .policy("rule-guardrail")
                        .configuration("{\"scope\":{\"protocol\":\"MCP\"}}")
                        .build()
                )
            )
        ).build();

        var result = aGuardianValidator(new InMemoryGuardianRepository(), provider, new InMemoryPolicyValidation()).validateAndSanitize(
            new ValidateGuardianDomainService.Input(guardian)
        );

        assertThat(result.severe()).as("every runtime guardrail is validated, including alternate or absent categories").isPresent();
        assertThat(result.severe().orElseThrow())
            .singleElement()
            .satisfies(error -> assertThat(error.getMessage()).contains("MCP guardrail", "MCP", "llm-proxy"));
    }

    @Test
    void should_not_classify_a_conventional_policy_as_a_guardrail_from_its_category() {
        given_installed_policies(a_policy("ordinary-policy", HttpPolicy.class, "guardrail", Map.of()));

        assertThat(provider.findAllCapabilities()).containsEntry(
            "ordinary-policy",
            new PolicyCapabilities(false, new DeclaredFlows(Map.of()))
        );
    }

    @Test
    void should_refuse_a_policy_that_is_no_longer_installed_without_a_server_error() {
        given_installed_policies();
        var guardian = GuardianFixtures.aGuardian(definition ->
            definition.policies(List.of(Step.builder().name("Removed step").policy("removed-policy").build()))
        ).build();

        var result = aGuardianValidator(new InMemoryGuardianRepository(), provider, new InMemoryPolicyValidation()).validateAndSanitize(
            new ValidateGuardianDomainService.Input(guardian)
        );

        assertThat(result.severe().orElseThrow())
            .singleElement()
            .satisfies(error -> assertThat(error.getMessage()).contains("Removed step", "removed-policy", "not installed"));
    }

    @Test
    void should_distinguish_conventional_policies_and_guardrails_without_metadata() {
        given_installed_policies(
            a_policy("transform-headers", HttpPolicy.class, "transformation", Map.of()),
            a_policy("rule-guardrail", GuardrailPolicy.class, "guardrail", null)
        );

        var policies = provider.findAllCapabilities();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(policies).containsEntry("transform-headers", new PolicyCapabilities(false, new DeclaredFlows(Map.of())));
            softly.assertThat(policies).containsEntry("rule-guardrail", new PolicyCapabilities(true, new DeclaredFlows(Map.of())));
        });
    }

    @Nested
    class ResourceReferences {

        private String the_bundled_schema(String name) throws Exception {
            try (var schema = getClass().getResourceAsStream("/schemas/" + name)) {
                return new String(schema.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        private void given_the_schema(String schema) throws Exception {
            a_policy("guardrail-decision", GuardrailPolicy.class, "guardrail", Map.of());
            when(pluginManager.getSchema("guardrail-decision", true)).thenReturn(schema);
        }

        @Test
        void should_read_a_property_the_schema_marks_as_a_resource_picker() throws Exception {
            given_the_schema(
                """
                {"type":"object","properties":{
                  "resource":{"type":"string","gioConfig":{"uiType":"resource-type","uiTypeProps":{"resourceType":"ai-model-decision"}}},
                  "scope":{"type":"object","properties":{"protocol":{"type":"string"}}}
                }}
                """
            );

            assertThat(provider.findResourceReferences("guardrail-decision")).containsExactly(
                new ResourceReference(JsonPointer.compile("/resource"), List.of("ai-model-decision"))
            );
        }

        @Test
        void should_read_a_nested_property_accepting_several_types() throws Exception {
            given_the_schema(
                """
                {"type":"object","properties":{"model":{"type":"object","properties":{
                  "name":{"type":"string","gioConfig":{"uiType":"resource-type","uiTypeProps":{"resourceType":["ai-model-decision","ai-model-text-classification"]}}}
                }}}}
                """
            );

            assertThat(provider.findResourceReferences("guardrail-decision")).containsExactly(
                new ResourceReference(JsonPointer.compile("/model/name"), List.of("ai-model-decision", "ai-model-text-classification"))
            );
        }

        /** No picker means any resource, but still a resource of the Guardian. */
        @Test
        void should_accept_any_type_when_the_picker_names_none() throws Exception {
            given_the_schema(
                """
                {"type":"object","properties":{"resource":{"type":"string","gioConfig":{"uiType":"resource-type"}}}}
                """
            );

            assertThat(provider.findResourceReferences("guardrail-decision")).containsExactly(
                new ResourceReference(JsonPointer.compile("/resource"), List.of())
            );
        }

        /**
         * The console only offers a picker for {@code uiType: resource-type}: the validation reads the same marker. The
         * flat {@code gioConfig.resourceType} is what guardrail-decision declared before 1.0.0-alpha.7, which nothing reads.
         */
        @Test
        void should_ignore_a_resource_type_declared_without_the_picker() throws Exception {
            given_the_schema(
                """
                {"type":"object","properties":{"resource":{"type":"string","gioConfig":{"resourceType":"ai-model-decision"}}}}
                """
            );

            assertThat(provider.findResourceReferences("guardrail-decision")).isEmpty();
        }

        /** The schema the distribution bundles, so that a marker renamed in the policy is caught here. */
        @Test
        void should_read_the_resource_picker_of_the_bundled_guardrail_decision_policy() throws Exception {
            given_the_schema(the_bundled_schema("guardrail-decision-1.0.0-alpha.7.json"));

            assertThat(provider.findResourceReferences("guardrail-decision")).containsExactly(
                new ResourceReference(JsonPointer.compile("/resource"), List.of("ai-model-decision"))
            );
        }

        @Test
        void should_escape_a_property_name_that_is_not_a_plain_identifier() throws Exception {
            given_the_schema(
                """
                {"type":"object","properties":{"a/b~c":{"type":"string","gioConfig":{"uiType":"resource-type"}}}}
                """
            );

            assertThat(provider.findResourceReferences("guardrail-decision")).containsExactly(
                new ResourceReference(JsonPointer.compile("/a~1b~0c"), List.of())
            );
        }

        /** What a picker holds is a resource name: properties under it are not read as further pickers. */
        @Test
        void should_not_read_under_a_property_marked_as_a_picker() throws Exception {
            given_the_schema(
                """
                {"type":"object","properties":{"resource":{"type":"object","gioConfig":{"uiType":"resource-type"},"properties":{
                  "inner":{"type":"string","gioConfig":{"uiType":"resource-type"}}
                }}}}
                """
            );

            assertThat(provider.findResourceReferences("guardrail-decision")).containsExactly(
                new ResourceReference(JsonPointer.compile("/resource"), List.of())
            );
        }

        /** The step naming it is refused as not installed before its references are read: nothing to add here. */
        @Test
        void should_find_nothing_for_a_policy_that_is_not_installed() {
            assertThat(provider.findResourceReferences("uninstalled")).isEmpty();
        }

        /** Rethrown without logging: the REST layer answering the 500 logs it, once. */
        @Test
        void should_fail_on_a_schema_that_is_no_json() throws Exception {
            given_the_schema("not json");
            var logger = (Logger) LoggerFactory.getLogger(DefaultPolicyPluginProvider.class);
            var logs = new ListAppender<ILoggingEvent>();
            logs.start();
            logger.addAppender(logs);
            try {
                assertThatThrownBy(() -> provider.findResourceReferences("guardrail-decision")).isInstanceOf(
                    TechnicalDomainException.class
                );
                assertThat(logs.list).as("logged by the provider").isEmpty();
            } finally {
                logger.detachAppender(logs);
            }
        }

        @Test
        void should_find_nothing_in_a_policy_without_schema() throws Exception {
            given_the_schema(null);

            assertThat(provider.findResourceReferences("guardrail-decision")).isEmpty();
        }
    }
}
