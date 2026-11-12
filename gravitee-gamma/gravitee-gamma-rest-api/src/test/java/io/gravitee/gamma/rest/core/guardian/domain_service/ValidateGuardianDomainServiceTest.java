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
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateGuardianDomainServiceTest {

    private final Map<String, PolicyCapabilities> policies = new LinkedHashMap<>(
        Map.of(
            "data-logging-masking",
            new PolicyCapabilities("Data logging masking", true, Map.of(ApiType.LLM_PROXY, Set.of(Phase.REQUEST, Phase.RESPONSE)))
        )
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
        policies.put(
            "data-logging-masking",
            new PolicyCapabilities("Data logging masking", true, Map.of(ApiType.MCP_PROXY, Set.of(Phase.REQUEST)))
        );
        policies.put(
            "other-guardrail",
            new PolicyCapabilities("Data logging masking", true, Map.of(ApiType.A2A_PROXY, Set.of(Phase.REQUEST)))
        );
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
    void should_refuse_a_guardrail_that_does_not_run_in_the_guardian_phase() {
        policies.put(
            "data-logging-masking",
            new PolicyCapabilities("Data logging masking", true, Map.of(ApiType.LLM_PROXY, Set.of(Phase.RESPONSE)))
        );
        var guardian = aGuardian(definition -> definition.phase(Phase.REQUEST)).build();

        assertThat(validate(guardian).severe().orElseThrow())
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "data-logging-masking", "REQUEST", "llm-proxy");
    }

    @Test
    void should_accept_a_guardrail_that_runs_in_the_guardian_phase() {
        policies.put(
            "data-logging-masking",
            new PolicyCapabilities("Data logging masking", true, Map.of(ApiType.LLM_PROXY, Set.of(Phase.RESPONSE)))
        );
        var guardian = aGuardian(definition -> definition.phase(Phase.RESPONSE)).build();

        assertThat(validate(guardian).severe()).isEmpty();
    }

    @Test
    void should_ignore_the_phases_of_a_conventional_policy() {
        policies.put(
            "data-logging-masking",
            new PolicyCapabilities("Data logging masking", false, Map.of(ApiType.LLM_PROXY, Set.of(Phase.RESPONSE)))
        );
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
        policies.put("data-logging-masking", new PolicyCapabilities("Data logging masking", true, Map.of()));

        assertThat(validate(aGuardian().build()).severe().orElseThrow())
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "llm-proxy");
    }

    @Test
    void should_ignore_an_unknown_disabled_step() {
        policies.clear();
        var guardian = aGuardian(definition -> definition.policies(List.of(step("Disabled guardrail", "not-installed", false)))).build();

        assertThat(validate(guardian).severe()).isEmpty();
    }

    @Test
    void should_keep_supporting_known_conventional_policies() {
        policies.put("data-logging-masking", new PolicyCapabilities("Data logging masking", false, Map.of()));

        assertThat(validate(aGuardian().build()).severe()).isEmpty();
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

    private static Step step(String name, String policy, boolean enabled) {
        return Step.builder().name(name).policy(policy).enabled(enabled).build();
    }
}
