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
package io.gravitee.definition.model.guardian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuardianStepsTest {

    @Test
    void should_build_a_step_running_the_guardian() {
        var step = GuardianSteps.of("guardian-1", "Credential leakage");

        assertSoftly(softly -> {
            softly.assertThat(step.getPolicy()).as("policy").isEqualTo("guardian-policy");
            softly.assertThat(step.getName()).as("name").isEqualTo("Credential leakage");
            softly.assertThat(step.isEnabled()).as("enabled").isTrue();
            softly.assertThat(GuardianSteps.guardianIdOf(step)).as("guardian id").contains("guardian-1");
        });
    }

    @Test
    void should_read_the_guardians_of_both_phases_in_execution_order() {
        var flow = Flow.builder()
            .request(List.of(GuardianSteps.of("guardian-1", "One"), aPolicy(), GuardianSteps.of("guardian-2", "Two")))
            .response(List.of(GuardianSteps.of("guardian-3", "Three")))
            .build();

        assertThat(GuardianSteps.guardianIdsOf(flow)).as("guardians of the flow").containsExactly("guardian-1", "guardian-2", "guardian-3");
    }

    @Test
    void should_read_no_guardian_from_a_missing_flow() {
        assertThat(GuardianSteps.guardianIdsOf(null)).as("guardians of no flow").isEmpty();
    }

    @Test
    void should_not_read_a_guardian_from_another_policy() {
        assertThat(GuardianSteps.guardianIdOf(aPolicy())).as("guardian id of another policy").isEmpty();
    }

    @Test
    void should_not_read_a_guardian_from_a_step_without_its_id() {
        var step = Step.builder().policy("guardian-policy").configuration("{}").build();

        assertThat(GuardianSteps.guardianIdOf(step)).as("guardian id of a step without one").isEmpty();
    }

    private static Step aPolicy() {
        return Step.builder().policy("rate-limit").configuration("{\"guardianId\":\"not-a-guardian\"}").build();
    }
}
