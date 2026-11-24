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
package io.gravitee.gamma.rest.core.guardian.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuardrailPolicyEffectsTest {

    @Test
    void should_report_what_a_guardrail_policy_does_on_a_match_and_on_no_match() {
        var definition = aGuardianRunning(
            aGuardrailPolicy(
                """
                {"onMatch":{"action":"BLOCK"},"onNoMatch":{"action":"CONTINUE"}}"""
            )
        );

        assertThat(GuardrailPolicyEffects.of(definition))
            .as("effects")
            .containsExactly(GuardrailPolicyEffect.BLOCK, GuardrailPolicyEffect.CONTINUE);
    }

    @Test
    void should_report_the_effects_of_the_whole_sequence_once_each_and_strongest_first() {
        var definition = aGuardianRunning(
            aGuardrailPolicy(
                """
                {"onMatch":{"action":"CONTINUE"},"onNoMatch":{"action":"CONTINUE"}}"""
            ),
            aGuardrailPolicy(
                """
                {"onMatch":{"action":"MASK"},"onNoMatch":{"action":"BLOCK"}}"""
            )
        );

        assertThat(GuardrailPolicyEffects.of(definition))
            .as("effects")
            .containsExactly(GuardrailPolicyEffect.BLOCK, GuardrailPolicyEffect.MASK, GuardrailPolicyEffect.CONTINUE);
    }

    /**
     * An action the enum does not hold is reported as no effect, so the set has to name every action a guardrail policy
     * policy accepts, and the transformations have to be ordered the way the gateway resolves a verdict.
     */
    @Test
    void should_name_every_action_a_guardrail_policy_accepts_from_the_most_to_the_least_destructive() {
        assertThat(GuardrailPolicyEffect.values())
            .as("the effects a guardrail policy can apply, strongest first")
            .containsExactly(
                GuardrailPolicyEffect.BLOCK,
                GuardrailPolicyEffect.REWRITE,
                GuardrailPolicyEffect.REDACT,
                GuardrailPolicyEffect.MASK,
                GuardrailPolicyEffect.CONTINUE
            );
    }

    @Test
    void should_report_a_guardrail_policy_that_rewrites_what_it_matches() {
        var definition = aGuardianRunning(
            aGuardrailPolicy(
                """
                {"onMatch":{"action":"REWRITE"},"onNoMatch":{"action":"CONTINUE"}}"""
            )
        );

        assertThat(GuardrailPolicyEffects.of(definition))
            .as("effects")
            .containsExactly(GuardrailPolicyEffect.REWRITE, GuardrailPolicyEffect.CONTINUE);
    }

    @Test
    void should_report_an_effect_a_guardrail_policy_applies_without_blocking() {
        var definition = aGuardianRunning(
            aGuardrailPolicy(
                """
                {"onMatch":{"action":"REDACT"}}"""
            )
        );

        assertThat(GuardrailPolicyEffects.of(definition)).as("effects").containsExactly(GuardrailPolicyEffect.REDACT);
    }

    @Test
    void should_ignore_a_guardrail_policy_the_sequence_does_not_run() {
        var definition = aGuardianRunning(
            Step.builder()
                .policy("guardrails")
                .enabled(false)
                .configuration(
                    """
                    {"onMatch":{"action":"BLOCK"}}"""
                )
                .build(),
            aGuardrailPolicy(
                """
                {"onMatch":{"action":"CONTINUE"}}"""
            )
        );

        assertThat(GuardrailPolicyEffects.of(definition)).as("effects").containsExactly(GuardrailPolicyEffect.CONTINUE);
    }

    @Test
    void should_report_nothing_for_a_guardrail_policy_that_does_not_say_what_it_does() {
        var definition = aGuardianRunning(
            aGuardrailPolicy(
                """
                {"requestPolicy":"BLOCK_REQUEST"}"""
            ),
            aGuardrailPolicy("not json at all"),
            aGuardrailPolicy(null),
            aGuardrailPolicy(
                """
                {"onMatch":{"action":"SOMETHING_ELSE"}}"""
            )
        );

        assertThat(GuardrailPolicyEffects.of(definition)).as("effects").isEmpty();
    }

    @Test
    void should_report_nothing_for_a_guardian_running_no_guardrail_policy() {
        assertThat(GuardrailPolicyEffects.of(Guardian.builder().build())).as("effects of an empty sequence").isEmpty();
        assertThat(GuardrailPolicyEffects.of(null)).as("effects of no definition").isEmpty();
    }

    private static Guardian aGuardianRunning(Step... guardrailPolicies) {
        return Guardian.builder().policies(List.of(guardrailPolicies)).build();
    }

    private static Step aGuardrailPolicy(String configuration) {
        return Step.builder().policy("guardrails").configuration(configuration).build();
    }
}
