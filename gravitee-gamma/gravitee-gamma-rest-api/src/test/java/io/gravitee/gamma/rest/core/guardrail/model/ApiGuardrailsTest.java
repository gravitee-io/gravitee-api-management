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
package io.gravitee.gamma.rest.core.guardrail.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.gamma.rest.core.guardrail.model.ApiGuardrails.GuardrailRef;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiGuardrailsTest {

    @Test
    void should_list_the_guardrails_of_each_phase_in_execution_order() {
        var internalFlow = Flow.builder()
            .request(List.of(GuardrailSteps.of("guardrail-1", "One"), aPolicy(), GuardrailSteps.of("guardrail-2", "Two")))
            .response(List.of(GuardrailSteps.of("guardrail-3", "Three")))
            .build();

        assertThat(ApiGuardrails.of(internalFlow).list())
            .as("enabled guardrails")
            .containsExactly(
                new ApiGuardrails.Enabled("guardrail-1", Phase.REQUEST, 1, null),
                new ApiGuardrails.Enabled("guardrail-2", Phase.REQUEST, 2, null),
                new ApiGuardrails.Enabled("guardrail-3", Phase.RESPONSE, 1, null)
            );
    }

    @Test
    void should_list_no_guardrail_when_the_api_has_no_internal_flow() {
        assertThat(ApiGuardrails.of(null).list()).as("enabled guardrails").isEmpty();
    }

    @Test
    void should_list_the_guardrails_of_one_phase_in_execution_order() {
        var guardrails = ApiGuardrails.of(
            Flow.builder()
                .request(List.of(GuardrailSteps.of("guardrail-1", "One")))
                .response(List.of(GuardrailSteps.of("guardrail-2", "Two"), GuardrailSteps.of("guardrail-3", "Three")))
                .build()
        );

        assertSoftly(softly -> {
            softly.assertThat(guardrails.idsOf(Phase.REQUEST)).as("guardrails of the request phase").containsExactly("guardrail-1");
            softly
                .assertThat(guardrails.idsOf(Phase.RESPONSE))
                .as("guardrails of the response phase")
                .containsExactly("guardrail-2", "guardrail-3");
            softly
                .assertThat(guardrails.list(Phase.RESPONSE))
                .as("order of the response phase")
                .extracting(ApiGuardrails.Enabled::order)
                .containsExactly(1, 2);
        });
    }

    @Test
    void should_run_the_guardrails_of_a_phase_in_the_order_given() {
        var guardrails = ApiGuardrails.of(
            Flow.builder().request(List.of(GuardrailSteps.of("guardrail-1", "One"), GuardrailSteps.of("guardrail-2", "Two"))).build()
        );

        var replaced = guardrails.replace(
            Phase.REQUEST,
            List.of(new GuardrailRef("guardrail-2", "Two", null), new GuardrailRef("guardrail-3", "Three", null))
        );

        assertSoftly(softly -> {
            softly
                .assertThat(replaced.list())
                .as("enabled guardrails")
                .containsExactly(
                    new ApiGuardrails.Enabled("guardrail-2", Phase.REQUEST, 1, null),
                    new ApiGuardrails.Enabled("guardrail-3", Phase.REQUEST, 2, null)
                );
            softly
                .assertThat(replaced.internalFlow().getRequest())
                .as("request steps")
                .containsExactly(GuardrailSteps.of("guardrail-2", "Two"), GuardrailSteps.of("guardrail-3", "Three"));
        });
    }

    @Test
    void should_leave_the_other_phase_as_it_was() {
        var guardrails = ApiGuardrails.of(
            Flow.builder()
                .request(List.of(GuardrailSteps.of("guardrail-1", "One")))
                .response(List.of(GuardrailSteps.of("guardrail-2", "Two")))
                .build()
        );

        var replaced = guardrails.replace(Phase.REQUEST, List.of());

        assertThat(replaced.list())
            .as("enabled guardrails")
            .containsExactly(new ApiGuardrails.Enabled("guardrail-2", Phase.RESPONSE, 1, null));
    }

    @Test
    void should_run_a_guardrail_on_an_api_that_ran_none() {
        var replaced = ApiGuardrails.of(null).replace(Phase.RESPONSE, List.of(new GuardrailRef("guardrail-1", "One", null)));

        assertThat(replaced.internalFlow().getResponse()).as("response steps").containsExactly(GuardrailSteps.of("guardrail-1", "One"));
    }

    @Test
    void should_run_a_guardrail_named_without_a_condition_on_every_call() {
        var replaced = ApiGuardrails.of(null).replace(Phase.REQUEST, List.of(new GuardrailRef("guardrail-1", "One")));

        assertThat(replaced.list())
            .as("enabled guardrails")
            .containsExactly(new ApiGuardrails.Enabled("guardrail-1", Phase.REQUEST, 1, null));
    }

    @Test
    void should_run_no_guardrail_at_all_on_an_empty_list() {
        var guardrails = ApiGuardrails.of(Flow.builder().request(List.of(GuardrailSteps.of("guardrail-1", "One"), aPolicy())).build());

        var replaced = guardrails.replace(Phase.REQUEST, List.of());

        assertSoftly(softly -> {
            softly.assertThat(replaced.list()).as("enabled guardrails").isEmpty();
            softly.assertThat(replaced.internalFlow().getRequest()).as("request steps").containsExactly(aPolicy());
        });
    }

    @Test
    void should_keep_the_other_steps_where_the_guardrails_already_ran() {
        var guardrails = ApiGuardrails.of(
            Flow.builder().request(List.of(aPolicy(), GuardrailSteps.of("guardrail-1", "One"), anotherPolicy())).build()
        );

        var replaced = guardrails.replace(
            Phase.REQUEST,
            List.of(new GuardrailRef("guardrail-2", "Two", null), new GuardrailRef("guardrail-3", "Three", null))
        );

        assertThat(replaced.internalFlow().getRequest())
            .as("request steps")
            .containsExactly(
                aPolicy(),
                GuardrailSteps.of("guardrail-2", "Two"),
                GuardrailSteps.of("guardrail-3", "Three"),
                anotherPolicy()
            );
    }

    @Test
    void should_run_the_guardrails_first_when_the_phase_ran_none() {
        var guardrails = ApiGuardrails.of(Flow.builder().request(List.of(aPolicy())).build());

        var replaced = guardrails.replace(Phase.REQUEST, List.of(new GuardrailRef("guardrail-1", "One")));

        assertThat(replaced.internalFlow().getRequest())
            .as("request steps")
            .containsExactly(GuardrailSteps.of("guardrail-1", "One"), aPolicy());
    }

    @Test
    void should_run_a_first_guardrail_after_the_step_setting_the_attributes_its_condition_reads() {
        var guardrails = ApiGuardrails.of(Flow.builder().request(List.of(anAiHubStep())).build());

        var replaced = guardrails.replace(Phase.REQUEST, List.of(new GuardrailRef("guardrail-1", "One", A_CONDITION)));

        assertThat(replaced.internalFlow().getRequest())
            .as("request steps")
            .containsExactly(anAiHubStep(), GuardrailSteps.of("guardrail-1", "One", A_CONDITION));
    }

    /** The other steps set what a condition reads: a block evaluated before them would run on no call. */
    @Test
    void should_move_a_block_of_guardrails_after_the_other_steps_once_one_carries_a_condition() {
        var guardrails = ApiGuardrails.of(
            Flow.builder().request(List.of(GuardrailSteps.of("guardrail-1", "One"), anAiHubStep(), aPolicy())).build()
        );

        var replaced = guardrails.replace(
            Phase.REQUEST,
            List.of(new GuardrailRef("guardrail-1", "One"), new GuardrailRef("guardrail-2", "Two", A_CONDITION))
        );

        assertThat(replaced.internalFlow().getRequest())
            .as("request steps")
            .containsExactly(
                anAiHubStep(),
                aPolicy(),
                GuardrailSteps.of("guardrail-1", "One"),
                GuardrailSteps.of("guardrail-2", "Two", A_CONDITION)
            );
    }

    @Test
    void should_write_the_condition_of_each_guardrail_on_its_step() {
        var replaced = ApiGuardrails.of(null).replace(
            Phase.REQUEST,
            List.of(new GuardrailRef("guardrail-1", "One", A_CONDITION), new GuardrailRef("guardrail-2", "Two", null))
        );

        assertThat(replaced.internalFlow().getRequest())
            .as("request steps")
            .containsExactly(GuardrailSteps.of("guardrail-1", "One", A_CONDITION), GuardrailSteps.of("guardrail-2", "Two"));
    }

    @Test
    void should_list_the_condition_each_guardrail_runs_under() {
        var guardrails = ApiGuardrails.of(
            Flow.builder()
                .request(
                    List.of(anAiHubStep(), GuardrailSteps.of("guardrail-1", "One", A_CONDITION), GuardrailSteps.of("guardrail-2", "Two"))
                )
                .response(List.of(GuardrailSteps.of("guardrail-3", "Three", "  ")))
                .build()
        );

        assertSoftly(softly -> {
            softly
                .assertThat(guardrails.attachmentsOf(Phase.REQUEST))
                .as("attachments of the request phase")
                .containsExactly(new ApiGuardrailAttachment("guardrail-1", A_CONDITION), new ApiGuardrailAttachment("guardrail-2", null));
            softly
                .assertThat(guardrails.list())
                .as("enabled guardrails, a blank condition read as none")
                .containsExactly(
                    new ApiGuardrails.Enabled("guardrail-1", Phase.REQUEST, 1, A_CONDITION),
                    new ApiGuardrails.Enabled("guardrail-2", Phase.REQUEST, 2, null),
                    new ApiGuardrails.Enabled("guardrail-3", Phase.RESPONSE, 1, null)
                );
        });
    }

    private static final String A_CONDITION = "{#context.attributes['aihub.team'] == 'team-a'}";

    private static Step anAiHubStep() {
        return Step.builder().name("AI Hub").policy("ai-hub").build();
    }

    private static Step aPolicy() {
        return Step.builder().name("Rate limit").policy("rate-limit").build();
    }

    private static Step anotherPolicy() {
        return Step.builder().name("Transform headers").policy("transform-headers").build();
    }
}
