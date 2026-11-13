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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.exception.InvalidGuardianOrderException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ApiGuardiansTest {

    @Test
    void should_list_the_guardians_of_each_phase_in_execution_order() {
        var internalFlow = Flow.builder()
            .request(List.of(GuardianSteps.of("guardian-1", "One"), aPolicy(), GuardianSteps.of("guardian-2", "Two")))
            .response(List.of(GuardianSteps.of("guardian-3", "Three")))
            .build();

        assertThat(ApiGuardians.of(internalFlow).list())
            .as("enabled guardians")
            .containsExactly(
                new ApiGuardians.Enabled("guardian-1", Phase.REQUEST, 1),
                new ApiGuardians.Enabled("guardian-2", Phase.REQUEST, 2),
                new ApiGuardians.Enabled("guardian-3", Phase.RESPONSE, 1)
            );
    }

    @Test
    void should_list_no_guardian_when_the_api_has_no_internal_flow() {
        assertThat(ApiGuardians.of(null).list()).as("enabled guardians").isEmpty();
    }

    @Test
    void should_enable_a_guardian_after_those_of_its_phase() {
        var guardians = ApiGuardians.of(Flow.builder().request(List.of(GuardianSteps.of("guardian-1", "One"))).build());

        var enabled = guardians.enable("guardian-2", "Two", Phase.REQUEST);

        assertThat(enabled.list())
            .as("enabled guardians")
            .containsExactly(
                new ApiGuardians.Enabled("guardian-1", Phase.REQUEST, 1),
                new ApiGuardians.Enabled("guardian-2", Phase.REQUEST, 2)
            );
    }

    @Test
    void should_enable_a_guardian_on_an_api_without_internal_flow() {
        var enabled = ApiGuardians.of(null).enable("guardian-1", "One", Phase.RESPONSE);

        assertThat(enabled.internalFlow().getResponse()).as("response steps").containsExactly(GuardianSteps.of("guardian-1", "One"));
    }

    @Test
    void should_keep_the_other_steps_of_the_internal_flow_when_enabling() {
        var guardians = ApiGuardians.of(Flow.builder().request(List.of(aPolicy())).build());

        var enabled = guardians.enable("guardian-1", "One", Phase.REQUEST);

        assertThat(enabled.internalFlow().getRequest())
            .as("request steps")
            .containsExactly(aPolicy(), GuardianSteps.of("guardian-1", "One"));
    }

    @Test
    void should_tell_whether_a_guardian_is_enabled() {
        var guardians = ApiGuardians.of(Flow.builder().response(List.of(GuardianSteps.of("guardian-1", "One"))).build());

        assertThat(guardians.isEnabled("guardian-1")).as("guardian-1 enabled").isTrue();
        assertThat(guardians.isEnabled("guardian-2")).as("guardian-2 enabled").isFalse();
    }

    @Test
    void should_reorder_the_guardians_of_a_phase() {
        var guardians = ApiGuardians.of(
            Flow.builder()
                .request(List.of(GuardianSteps.of("guardian-1", "One"), GuardianSteps.of("guardian-2", "Two")))
                .response(List.of(GuardianSteps.of("guardian-3", "Three")))
                .build()
        );

        var reordered = guardians.reorder(Phase.REQUEST, List.of("guardian-2", "guardian-1"));

        assertThat(reordered.list())
            .as("enabled guardians")
            .containsExactly(
                new ApiGuardians.Enabled("guardian-2", Phase.REQUEST, 1),
                new ApiGuardians.Enabled("guardian-1", Phase.REQUEST, 2),
                new ApiGuardians.Enabled("guardian-3", Phase.RESPONSE, 1)
            );
    }

    @Test
    void should_keep_the_other_steps_in_place_when_reordering() {
        var guardians = ApiGuardians.of(
            Flow.builder().request(List.of(GuardianSteps.of("guardian-1", "One"), aPolicy(), GuardianSteps.of("guardian-2", "Two"))).build()
        );

        var reordered = guardians.reorder(Phase.REQUEST, List.of("guardian-2", "guardian-1"));

        assertThat(reordered.internalFlow().getRequest())
            .as("request steps")
            .containsExactly(GuardianSteps.of("guardian-2", "Two"), aPolicy(), GuardianSteps.of("guardian-1", "One"));
    }

    @ParameterizedTest
    @MethodSource("orders_that_are_not_the_guardians_of_the_phase")
    void should_refuse_an_order_that_is_not_the_guardians_of_the_phase(List<String> order) {
        var guardians = ApiGuardians.of(
            Flow.builder()
                .request(List.of(GuardianSteps.of("guardian-1", "One"), GuardianSteps.of("guardian-2", "Two")))
                .response(List.of(GuardianSteps.of("guardian-3", "Three")))
                .build()
        );

        assertThatThrownBy(() -> guardians.reorder(Phase.REQUEST, order))
            .as("order " + order)
            .isInstanceOf(InvalidGuardianOrderException.class);
    }

    static Stream<List<String>> orders_that_are_not_the_guardians_of_the_phase() {
        return Stream.of(
            List.of("guardian-1"),
            List.of("guardian-1", "guardian-2", "guardian-3"),
            List.of("guardian-1", "guardian-1"),
            List.of("guardian-1", "guardian-4")
        );
    }

    private static Step aPolicy() {
        return Step.builder().name("Rate limit").policy("rate-limit").build();
    }
}
