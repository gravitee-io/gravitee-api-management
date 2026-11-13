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
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardians.GuardianRef;
import java.util.List;
import org.junit.jupiter.api.Test;

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
    void should_list_the_guardians_of_one_phase_in_execution_order() {
        var guardians = ApiGuardians.of(
            Flow.builder()
                .request(List.of(GuardianSteps.of("guardian-1", "One")))
                .response(List.of(GuardianSteps.of("guardian-2", "Two"), GuardianSteps.of("guardian-3", "Three")))
                .build()
        );

        assertSoftly(softly -> {
            softly.assertThat(guardians.idsOf(Phase.REQUEST)).as("guardians of the request phase").containsExactly("guardian-1");
            softly
                .assertThat(guardians.idsOf(Phase.RESPONSE))
                .as("guardians of the response phase")
                .containsExactly("guardian-2", "guardian-3");
            softly
                .assertThat(guardians.list(Phase.RESPONSE))
                .as("order of the response phase")
                .extracting(ApiGuardians.Enabled::order)
                .containsExactly(1, 2);
        });
    }

    @Test
    void should_run_the_guardians_of_a_phase_in_the_order_given() {
        var guardians = ApiGuardians.of(
            Flow.builder().request(List.of(GuardianSteps.of("guardian-1", "One"), GuardianSteps.of("guardian-2", "Two"))).build()
        );

        var replaced = guardians.replace(
            Phase.REQUEST,
            List.of(new GuardianRef("guardian-2", "Two"), new GuardianRef("guardian-3", "Three"))
        );

        assertSoftly(softly -> {
            softly
                .assertThat(replaced.list())
                .as("enabled guardians")
                .containsExactly(
                    new ApiGuardians.Enabled("guardian-2", Phase.REQUEST, 1),
                    new ApiGuardians.Enabled("guardian-3", Phase.REQUEST, 2)
                );
            softly
                .assertThat(replaced.internalFlow().getRequest())
                .as("request steps")
                .containsExactly(GuardianSteps.of("guardian-2", "Two"), GuardianSteps.of("guardian-3", "Three"));
        });
    }

    @Test
    void should_leave_the_other_phase_as_it_was() {
        var guardians = ApiGuardians.of(
            Flow.builder()
                .request(List.of(GuardianSteps.of("guardian-1", "One")))
                .response(List.of(GuardianSteps.of("guardian-2", "Two")))
                .build()
        );

        var replaced = guardians.replace(Phase.REQUEST, List.of());

        assertThat(replaced.list()).as("enabled guardians").containsExactly(new ApiGuardians.Enabled("guardian-2", Phase.RESPONSE, 1));
    }

    @Test
    void should_run_a_guardian_on_an_api_that_ran_none() {
        var replaced = ApiGuardians.of(null).replace(Phase.RESPONSE, List.of(new GuardianRef("guardian-1", "One")));

        assertThat(replaced.internalFlow().getResponse()).as("response steps").containsExactly(GuardianSteps.of("guardian-1", "One"));
    }

    @Test
    void should_run_no_guardian_at_all_on_an_empty_list() {
        var guardians = ApiGuardians.of(Flow.builder().request(List.of(GuardianSteps.of("guardian-1", "One"), aPolicy())).build());

        var replaced = guardians.replace(Phase.REQUEST, List.of());

        assertSoftly(softly -> {
            softly.assertThat(replaced.list()).as("enabled guardians").isEmpty();
            softly.assertThat(replaced.internalFlow().getRequest()).as("request steps").containsExactly(aPolicy());
        });
    }

    @Test
    void should_keep_the_other_steps_where_the_guardians_already_ran() {
        var guardians = ApiGuardians.of(
            Flow.builder().request(List.of(aPolicy(), GuardianSteps.of("guardian-1", "One"), anotherPolicy())).build()
        );

        var replaced = guardians.replace(
            Phase.REQUEST,
            List.of(new GuardianRef("guardian-2", "Two"), new GuardianRef("guardian-3", "Three"))
        );

        assertThat(replaced.internalFlow().getRequest())
            .as("request steps")
            .containsExactly(aPolicy(), GuardianSteps.of("guardian-2", "Two"), GuardianSteps.of("guardian-3", "Three"), anotherPolicy());
    }

    @Test
    void should_run_the_guardians_first_when_the_phase_ran_none() {
        var guardians = ApiGuardians.of(Flow.builder().request(List.of(aPolicy())).build());

        var replaced = guardians.replace(Phase.REQUEST, List.of(new GuardianRef("guardian-1", "One")));

        assertThat(replaced.internalFlow().getRequest())
            .as("request steps")
            .containsExactly(GuardianSteps.of("guardian-1", "One"), aPolicy());
    }

    private static Step aPolicy() {
        return Step.builder().name("Rate limit").policy("rate-limit").build();
    }

    private static Step anotherPolicy() {
        return Step.builder().name("Transform headers").policy("transform-headers").build();
    }
}
