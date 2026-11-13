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

import static io.gravitee.rest.api.model.v4.policy.ApiProtocolType.A2A_PROXY;
import static io.gravitee.rest.api.model.v4.policy.ApiProtocolType.GUARDIAN;
import static io.gravitee.rest.api.model.v4.policy.ApiProtocolType.HTTP_PROXY;
import static io.gravitee.rest.api.model.v4.policy.ApiProtocolType.LLM_PROXY;
import static io.gravitee.rest.api.model.v4.policy.ApiProtocolType.MCP_PROXY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.DeclaredFlows;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

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
                    softly.assertThat(policy.name()).as("name").isEqualTo("Name of rule-guardrail");
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
            softly.assertThat(capabilities.runsIn(Set.of(ApiType.LLM_PROXY), Phase.REQUEST)).as("runs in llm-proxy REQUEST").isTrue();
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

        var result = new ValidateGuardianDomainService(
            new InMemoryGuardianRepository(),
            provider,
            new JacksonJsonDeserializer()
        ).validateAndSanitize(new ValidateGuardianDomainService.Input(guardian));

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
            new PolicyCapabilities("Name of ordinary-policy", false, new DeclaredFlows(Map.of()))
        );
    }

    @Test
    void should_refuse_a_policy_that_is_no_longer_installed_without_a_server_error() {
        given_installed_policies();
        var guardian = GuardianFixtures.aGuardian(definition ->
            definition.policies(List.of(Step.builder().name("Removed step").policy("removed-policy").build()))
        ).build();

        var result = new ValidateGuardianDomainService(
            new InMemoryGuardianRepository(),
            provider,
            new JacksonJsonDeserializer()
        ).validateAndSanitize(new ValidateGuardianDomainService.Input(guardian));

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
            softly
                .assertThat(policies)
                .containsEntry(
                    "transform-headers",
                    new PolicyCapabilities("Name of transform-headers", false, new DeclaredFlows(Map.of()))
                );
            softly
                .assertThat(policies)
                .containsEntry("rule-guardrail", new PolicyCapabilities("Name of rule-guardrail", true, new DeclaredFlows(Map.of())));
        });
    }
}
