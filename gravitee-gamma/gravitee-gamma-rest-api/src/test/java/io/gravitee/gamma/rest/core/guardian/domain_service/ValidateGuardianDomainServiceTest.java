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
package io.gravitee.gamma.rest.core.guardian.domain_service;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aPolicy;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aPolicyPluginProvider;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.flows;
import static io.gravitee.rest.api.model.v4.policy.ApiProtocolType.A2A_PROXY;
import static io.gravitee.rest.api.model.v4.policy.ApiProtocolType.GUARDIAN;
import static io.gravitee.rest.api.model.v4.policy.ApiProtocolType.LLM_PROXY;
import static io.gravitee.rest.api.model.v4.policy.ApiProtocolType.MCP_PROXY;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.DeclaredFlows;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateGuardianDomainServiceTest {

    private final Map<String, PolicyCapabilities> policies = new LinkedHashMap<>(
        Map.of("data-logging-masking", aGuardrail(flows(LLM_PROXY, "REQUEST,RESPONSE", GUARDIAN, "REQUEST,RESPONSE")))
    );
    private final ValidateGuardianDomainService validator = new ValidateGuardianDomainService(
        new InMemoryGuardianRepository(),
        aPolicyPluginProvider(() -> policies),
        new JacksonJsonDeserializer()
    );

    @Test
    void should_accept_a_guardrail_supporting_the_guardian_protocol() {
        assertThat(validate(aGuardian().build()).severe()).isEmpty();
    }

    @Test
    void should_name_each_incompatible_guardrail_and_the_required_protocol() {
        policies.put("data-logging-masking", aGuardrail(flows(MCP_PROXY, "REQUEST", GUARDIAN, "REQUEST")));
        policies.put("other-guardrail", aGuardrail(flows(A2A_PROXY, "REQUEST", GUARDIAN, "REQUEST")));
        var guardian = aGuardian(definition ->
            definition.policies(List.of(aPolicy(), step("Other guardrail", "other-guardrail", true)))
        ).build();

        var errors = validate(guardian).severe().orElseThrow();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).as("both incompatible steps are reported together").hasSize(2);
            softly
                .assertThat(errors)
                .extracting(Validator.Error::getMessage)
                .anySatisfy(message -> assertThat(message).contains("Detect credentials", "data-logging-masking", "llm-proxy"));
            softly
                .assertThat(errors)
                .extracting(Validator.Error::getMessage)
                .anySatisfy(message -> assertThat(message).contains("Other guardrail", "other-guardrail", "llm-proxy"));
        });
    }

    @Test
    void should_refuse_a_guardrail_that_does_not_run_in_the_guardian_phase_for_the_api_type() {
        policies.put("data-logging-masking", aGuardrail(flows(LLM_PROXY, "RESPONSE", GUARDIAN, "REQUEST,RESPONSE")));
        var guardian = aGuardian(definition -> definition.phase(Phase.REQUEST)).build();

        assertThat(validate(guardian).severe().orElseThrow())
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "data-logging-masking", "REQUEST", "llm-proxy");
    }

    @Test
    void should_accept_a_guardrail_that_runs_in_the_guardian_phase() {
        policies.put("data-logging-masking", aGuardrail(flows(LLM_PROXY, "RESPONSE", GUARDIAN, "RESPONSE")));
        var guardian = aGuardian(definition -> definition.phase(Phase.RESPONSE)).build();

        assertThat(validate(guardian).severe()).isEmpty();
    }

    @Test
    void should_ignore_the_api_type_phases_of_a_conventional_policy() {
        policies.put("data-logging-masking", aConventionalPolicy(flows(LLM_PROXY, "RESPONSE", GUARDIAN, "REQUEST")));
        var guardian = aGuardian(definition -> definition.phase(Phase.REQUEST)).build();

        assertThat(validate(guardian).severe()).isEmpty();
    }

    @Test
    void should_refuse_an_unknown_enabled_policy() {
        policies.clear();

        assertThat(validate(aGuardian().build()).severe().orElseThrow())
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "data-logging-masking", "not installed");
    }

    @Test
    void should_refuse_a_guardrail_without_protocol_metadata() {
        policies.put("data-logging-masking", aGuardrail(flows()));

        assertThat(validate(aGuardian().build()).severe().orElseThrow())
            .as("every refusal of the step is reported at once")
            .extracting(Validator.Error::getMessage)
            .satisfiesExactlyInAnyOrder(
                message -> assertThat(message).contains("Detect credentials", "does not support Guardians"),
                message -> assertThat(message).contains("Detect credentials", "llm-proxy")
            );
    }

    @Test
    void should_ignore_an_unknown_disabled_step() {
        policies.clear();
        var guardian = aGuardian(definition -> definition.policies(List.of(step("Disabled guardrail", "not-installed", false)))).build();

        assertThat(validate(guardian).severe()).isEmpty();
    }

    @Test
    void should_accept_a_conventional_policy_declaring_the_guardian_phase() {
        policies.put("data-logging-masking", aConventionalPolicy(flows(GUARDIAN, "REQUEST")));

        assertThat(validate(aGuardian().build()).severe()).isEmpty();
    }

    @Test
    void should_refuse_a_conventional_policy_not_declaring_the_guardian_flow() {
        policies.put("data-logging-masking", aConventionalPolicy(flows(LLM_PROXY, "REQUEST,RESPONSE")));

        assertThat(validate(aGuardian().build()).severe().orElseThrow())
            .as("a policy that does not declare guardian= cannot run in a Guardian")
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "data-logging-masking", "does not support Guardians", "install a version that supports them");
    }

    @Test
    void should_refuse_a_guardrail_not_declaring_the_guardian_phase() {
        policies.put("data-logging-masking", aGuardrail(flows(LLM_PROXY, "REQUEST,RESPONSE", GUARDIAN, "RESPONSE")));
        var guardian = aGuardian(definition -> definition.phase(Phase.REQUEST)).build();

        assertThat(validate(guardian).severe().orElseThrow())
            .as("a guardrail declaring guardian=RESPONSE only cannot run in a REQUEST Guardian")
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "data-logging-masking", "phase [REQUEST]", "only supports Guardians of phase [RESPONSE]");
    }

    @Test
    void should_refuse_a_guardian_without_phase() {
        var guardian = aGuardian(definition -> definition.phase(null)).build();

        assertThat(validate(guardian).severe().orElseThrow())
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .isEqualTo("A Guardian phase is required.");
    }

    @Test
    void should_accept_a_guardrail_declaring_the_guardian_phase_and_the_api_type() {
        policies.put("data-logging-masking", aGuardrail(flows(LLM_PROXY, "REQUEST,RESPONSE", GUARDIAN, "REQUEST,RESPONSE")));

        SoftAssertions.assertSoftly(softly -> {
            for (var phase : Phase.values()) {
                var guardian = aGuardian(definition -> definition.phase(phase)).build();
                softly.assertThat(validate(guardian).severe()).as("%s Guardian", phase).isEmpty();
            }
        });
    }

    @Test
    void should_accept_an_empty_guardrail_sequence() {
        var guardian = aGuardian(definition -> definition.policies(List.of())).build();

        assertThat(validate(guardian).severe()).isEmpty();
    }

    @Test
    void should_refuse_an_explicit_scope_protocol_even_when_the_manifest_supports_the_api_type() {
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"protocol\":\"MCP\"}}");
        var guardian = aGuardian(definition -> definition.policies(List.of(policy))).build();

        assertThat(validate(guardian).severe().orElseThrow())
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "MCP", "llm-proxy");
    }

    @Test
    void should_accept_an_explicit_compatible_scope_protocol() {
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"protocol\":\"LLM\"}}");
        var guardian = aGuardian(definition -> definition.policies(List.of(policy))).build();

        assertThat(validate(guardian).severe()).isEmpty();
    }

    @Test
    void should_accept_the_default_scope_when_no_protocol_is_declared() {
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"patterns\":[\"sensitive\"]}}");
        var guardian = aGuardian(definition -> definition.policies(List.of(policy))).build();

        assertThat(validate(guardian).severe()).isEmpty();
    }

    @Test
    void should_refuse_invalid_guardrail_json_without_exposing_the_configuration() {
        var policy = aPolicy();
        policy.setConfiguration("{invalid-sensitive-configuration");
        var guardian = aGuardian(definition -> definition.policies(List.of(policy))).build();

        assertThat(validate(guardian).severe().orElseThrow())
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "invalid configuration")
            .doesNotContain("sensitive-configuration");
    }

    @Test
    void should_refuse_an_unknown_or_non_textual_scope_protocol() {
        SoftAssertions.assertSoftly(softly -> {
            for (String protocol : List.of("\"UNKNOWN\"", "42", "null")) {
                var policy = aPolicy();
                policy.setConfiguration("{\"scope\":{\"protocol\":" + protocol + "}}");
                var guardian = aGuardian(definition -> definition.policies(List.of(policy))).build();

                softly.assertThat(validate(guardian).severe()).as("invalid protocol %s", protocol).isPresent();
            }
        });
    }

    @Test
    void should_map_each_scope_protocol_to_the_api_type_the_gateway_expects() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(scopeErrors("LLM")).as("LLM designates llm-proxy").isEmpty();
            softly
                .assertThat(scopeErrors("MCP"))
                .as("MCP designates mcp-proxy")
                .singleElement()
                .asString()
                .contains("scope protocol [MCP], incompatible");
            softly
                .assertThat(scopeErrors("A2A"))
                .as("A2A designates a2a-proxy")
                .singleElement()
                .asString()
                .contains("scope protocol [A2A], incompatible");
            for (String protocol : List.of("llm", "HTTP", "GUARDIAN")) {
                softly
                    .assertThat(scopeErrors(protocol))
                    .as("%s is no scope protocol", protocol)
                    .singleElement()
                    .asString()
                    .contains("invalid scope protocol");
            }
        });
    }

    private List<String> scopeErrors(String protocol) {
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"protocol\":\"" + protocol + "\"}}");
        var guardian = aGuardian(definition -> definition.policies(List.of(policy))).build();
        return validate(guardian).severe().stream().flatMap(List::stream).map(Validator.Error::getMessage).toList();
    }

    @Test
    void should_refuse_non_object_configuration_or_scope() {
        SoftAssertions.assertSoftly(softly -> {
            for (String configuration : List.of("[]", "42", "null", "{\"scope\":[]}", "{\"scope\":\"invalid\"}")) {
                var policy = aPolicy();
                policy.setConfiguration(configuration);
                var guardian = aGuardian(definition -> definition.policies(List.of(policy))).build();

                softly.assertThat(validate(guardian).severe()).as("invalid configuration shape %s", configuration).isPresent();
            }
        });
    }

    private Validator.Result<ValidateGuardianDomainService.Input> validate(Guardian guardian) {
        return validator.validateAndSanitize(new ValidateGuardianDomainService.Input(guardian));
    }

    private static PolicyCapabilities aGuardrail(DeclaredFlows flows) {
        return new PolicyCapabilities(true, flows);
    }

    private static PolicyCapabilities aConventionalPolicy(DeclaredFlows flows) {
        return new PolicyCapabilities(false, flows);
    }

    private static Step step(String name, String policy, boolean enabled) {
        return Step.builder().name(name).policy(policy).enabled(enabled).build();
    }
}
