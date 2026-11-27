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
package io.gravitee.definition.model.guardrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuardrailStepsTest {

    @Test
    void should_build_a_step_running_the_guardrail() {
        var step = GuardrailSteps.of("guardrail-1", "Credential leakage");

        assertSoftly(softly -> {
            softly.assertThat(step.getPolicy()).as("policy").isEqualTo("guardrail-run");
            softly.assertThat(step.getName()).as("name").isEqualTo("Credential leakage");
            softly.assertThat(step.isEnabled()).as("enabled").isTrue();
            softly.assertThat(GuardrailSteps.guardrailIdOf(step)).as("guardrail id").contains("guardrail-1");
        });
    }

    @Test
    void should_build_a_step_running_the_guardrail_only_on_the_calls_its_condition_selects() {
        var step = GuardrailSteps.of("guardrail-1", "Credential leakage", "{#context.attributes['team'] == 'team-a'}");

        assertSoftly(softly -> {
            softly.assertThat(step.getCondition()).as("condition").isEqualTo("{#context.attributes['team'] == 'team-a'}");
            softly.assertThat(GuardrailSteps.guardrailIdOf(step)).as("guardrail id").contains("guardrail-1");
        });
    }

    @Test
    void should_build_a_step_running_the_guardrail_on_every_call_without_a_condition() {
        assertSoftly(softly -> {
            softly
                .assertThat(GuardrailSteps.of("guardrail-1", "Credential leakage").getCondition())
                .as("condition of the short form")
                .isNull();
            softly
                .assertThat(GuardrailSteps.of("guardrail-1", "Credential leakage", null))
                .isEqualTo(GuardrailSteps.of("guardrail-1", "Credential leakage"));
        });
    }

    @Test
    void should_read_the_guardrails_of_both_phases_in_execution_order() {
        var flow = Flow.builder()
            .request(List.of(GuardrailSteps.of("guardrail-1", "One"), aPolicy(), GuardrailSteps.of("guardrail-2", "Two")))
            .response(List.of(GuardrailSteps.of("guardrail-3", "Three")))
            .build();

        assertThat(GuardrailSteps.guardrailIdsOf(flow))
            .as("guardrails of the flow")
            .containsExactly("guardrail-1", "guardrail-2", "guardrail-3");
    }

    @Test
    void should_read_no_guardrail_from_a_missing_flow() {
        assertThat(GuardrailSteps.guardrailIdsOf(null)).as("guardrails of no flow").isEmpty();
    }

    @Test
    void should_not_read_a_guardrail_from_another_policy() {
        assertThat(GuardrailSteps.guardrailIdOf(aPolicy())).as("guardrail id of another policy").isEmpty();
    }

    @Test
    void should_not_read_a_guardrail_from_a_step_without_its_id() {
        var step = Step.builder().policy("guardrail-run").configuration("{}").build();

        assertThat(GuardrailSteps.guardrailIdOf(step)).as("guardrail id of a step without one").isEmpty();
    }

    /**
     * The step is read back through the class the gateway deserialises it into: a renamed field would fail here rather
     * than at runtime, on APIs whose steps the gateway can no longer read.
     */
    @Test
    void should_write_the_configuration_the_gateway_reads() throws JsonProcessingException {
        var step = GuardrailSteps.of("guardrail-1", "Credential leakage");

        var configuration = new ObjectMapper().readValue(step.getConfiguration(), GuardrailRunPolicyConfiguration.class);

        assertThat(configuration.getGuardrailId()).as("guardrail id the policy configuration carries").isEqualTo("guardrail-1");
    }

    @Test
    void should_read_a_guardrail_from_a_configuration_carrying_more_than_its_id() {
        var step = Step.builder().policy("guardrail-run").configuration("{\"guardrailId\":\"guardrail-1\",\"onError\":\"BLOCK\"}").build();

        assertThat(GuardrailSteps.guardrailIdOf(step)).as("guardrail id of a richer configuration").contains("guardrail-1");
    }

    private static Step aPolicy() {
        return Step.builder().policy("rate-limit").configuration("{\"guardrailId\":\"not-a-guardrail\"}").build();
    }
}
