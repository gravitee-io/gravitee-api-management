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
package io.gravitee.gamma.rest.core.guardrail.domain_service;

import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.API_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.gamma.rest.core.guardrail.GuardrailPublicationFixture;
import io.gravitee.gamma.rest.core.guardrail.exception.ApiCannotRunGuardrailsException;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.ApiGuardrail;
import io.gravitee.gamma.rest.core.guardrail.model.ApiGuardrails;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailPolicyEffect;
import java.time.Instant;
import java.util.List;
import java.util.function.UnaryOperator;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DescribeApiGuardrailsDomainServiceTest {

    private static final Instant DEPLOYED_AT = Instant.parse("2026-11-09T10:00:00Z");

    private final InMemoryGuardrailRepository repository = new InMemoryGuardrailRepository();
    private final GuardrailPublicationFixture publications = new GuardrailPublicationFixture();
    private final DescribeApiGuardrailsDomainService service = new DescribeApiGuardrailsDomainService(
        publications.service(repository),
        new InMemoryGuardedApiPort()
    );

    @Test
    void should_name_the_guardrails_of_each_phase_in_execution_order() {
        givenDeployed(d -> d.id("first").name("Prompt injection"));
        givenDeployed(d -> d.id("second").name("Toxicity").phase(Phase.RESPONSE));
        var enabled = ApiGuardrails.of(
            Flow.builder()
                .request(List.of(GuardrailSteps.of("first", "Old name"), GuardrailSteps.of("missing", "Missing")))
                .response(List.of(GuardrailSteps.of("second", "Old name")))
                .build()
        );

        assertThat(service.describe(ENVIRONMENT_ID, enabled))
            .as("guardrails of the API, named by the revision the gateways run")
            .containsExactly(
                new ApiGuardrail("first", "Prompt injection", Phase.REQUEST, 1, List.of()),
                new ApiGuardrail("missing", null, Phase.REQUEST, 2, List.of()),
                new ApiGuardrail("second", "Toxicity", Phase.RESPONSE, 1, List.of())
            );
    }

    /** What a module compares its own conditions with, to know whether the API needs them written again. */
    @Test
    void should_return_the_condition_each_guardrail_runs_under() {
        givenDeployed(d -> d.id("first").name("Prompt injection"));
        var teamA = "{#context.attributes['aihub.team'] == 'team-a'}";
        var enabled = ApiGuardrails.of(
            Flow.builder()
                .request(List.of(GuardrailSteps.of("first", "Prompt injection", teamA), GuardrailSteps.of("missing", "Missing", teamA)))
                .build()
        );

        assertThat(service.describe(ENVIRONMENT_ID, enabled))
            .as("conditions of the guardrails, also of one the environment no longer runs")
            .extracting(ApiGuardrail::guardrailId, ApiGuardrail::condition)
            .containsExactly(tuple("first", teamA), tuple("missing", teamA));
    }

    /** A rename nobody deployed changes nothing the calls of the API meet, so it changes nothing here either. */
    @Test
    void should_name_a_guardrail_after_its_deployed_revision_not_after_its_draft() {
        givenDeployed(d -> d.id("first").name("Prompt injection"));
        repository.update(aGuardrail(d -> d.id("first").name("Renamed draft")).build());
        var enabled = ApiGuardrails.of(Flow.builder().request(List.of(GuardrailSteps.of("first", "Prompt injection"))).build());

        assertThat(service.describe(ENVIRONMENT_ID, enabled))
            .as("guardrails of the API")
            .singleElement()
            .extracting(ApiGuardrail::name)
            .isEqualTo("Prompt injection");
    }

    @Test
    void should_report_what_the_guardrail_policies_of_the_deployed_revision_do() {
        givenDeployed(d ->
            d
                .id("blocking")
                .policies(
                    List.of(
                        Step.builder()
                            .policy("guardrails")
                            .configuration(
                                """
                                {"onMatch":{"action":"BLOCK"},"onNoMatch":{"action":"CONTINUE"}}"""
                            )
                            .build()
                    )
                )
        );
        var enabled = ApiGuardrails.of(Flow.builder().request(List.of(GuardrailSteps.of("blocking", "Blocking"))).build());

        assertThat(service.describe(ENVIRONMENT_ID, enabled))
            .as("described guardrails")
            .singleElement()
            .extracting(ApiGuardrail::effects, InstanceOfAssertFactories.list(GuardrailPolicyEffect.class))
            .as("effects of the sequence")
            .containsExactly(GuardrailPolicyEffect.BLOCK, GuardrailPolicyEffect.CONTINUE);
    }

    @Test
    void should_report_no_effect_for_a_guardrail_no_longer_deployed() {
        repository.givenGuardrail(aGuardrail(d -> d.id("withdrawn").name("Withdrawn")).build());
        var enabled = ApiGuardrails.of(Flow.builder().request(List.of(GuardrailSteps.of("withdrawn", "Withdrawn"))).build());

        assertThat(service.describe(ENVIRONMENT_ID, enabled))
            .as("guardrail without a deployed revision")
            .singleElement()
            .isEqualTo(new ApiGuardrail("withdrawn", null, Phase.REQUEST, 1, List.of()));
    }

    @Test
    void should_read_nothing_when_no_guardrail_is_enabled() {
        assertSoftly(softly -> {
            softly.assertThat(service.describe(ENVIRONMENT_ID, ApiGuardrails.of(null))).as("no enabled Guardrails").isEmpty();
            softly.assertThat(publications.reads()).as("no unnecessary reads").isZero();
        });
    }

    /** The deployment history is read for the whole list at once: two Guardrails cost what one costs. */
    @Test
    void should_read_the_deployment_history_as_often_for_several_guardrails_as_for_one() {
        givenDeployed(d -> d.id("first").name("One"));
        givenDeployed(d -> d.id("second").name("Two"));
        publications.resetReads();
        service.describe(ENVIRONMENT_ID, ApiGuardrails.of(Flow.builder().request(List.of(GuardrailSteps.of("first", "One"))).build()));
        var forOne = publications.reads();
        publications.resetReads();

        service.describe(
            ENVIRONMENT_ID,
            ApiGuardrails.of(Flow.builder().request(List.of(GuardrailSteps.of("first", "One"), GuardrailSteps.of("second", "Two"))).build())
        );

        assertSoftly(softly -> {
            softly.assertThat(forOne).as("reads for one Guardrail").isPositive();
            softly.assertThat(publications.reads()).as("reads for two Guardrails").isEqualTo(forOne);
        });
    }

    /**
     * The read a module owning an API surface calls: it names an API, not a sequence of steps, so resolving the API is
     * this service's job rather than the adapter's. Its refusals are the ones setting the Guardrails answers.
     */
    @Nested
    class DescribeByApiId {

        private final InMemoryGuardedApiPort apis = new InMemoryGuardedApiPort();
        private final DescribeApiGuardrailsDomainService describeByApi = new DescribeApiGuardrailsDomainService(
            publications.service(repository),
            apis
        );

        @Test
        void should_name_the_guardrails_the_api_runs() {
            givenDeployed(d -> d.id("first").name("Prompt injection"));
            apis.givenApi(
                aGuardedApi().internalFlow(Flow.builder().request(List.of(GuardrailSteps.of("first", "Old name"))).build()).build()
            );

            assertThat(describeByApi.describe(ENVIRONMENT_ID, API_ID))
                .as("guardrails of the API, named by the revision the gateways run")
                .containsExactly(new ApiGuardrail("first", "Prompt injection", Phase.REQUEST, 1, List.of(), null));
        }

        @Test
        void should_name_no_guardrail_on_an_api_running_none() {
            apis.givenApi(aGuardedApi().build());

            assertThat(describeByApi.describe(ENVIRONMENT_ID, API_ID)).as("guardrails of the API").isEmpty();
        }

        @Test
        void should_refuse_an_api_the_environment_does_not_hold() {
            apis.givenApi(aGuardedApi().environmentId("other-env").build());

            assertSoftly(softly -> {
                softly
                    .assertThatThrownBy(() -> describeByApi.describe(ENVIRONMENT_ID, API_ID))
                    .as("API of another environment")
                    .isInstanceOf(ApiNotFoundException.class);
                softly
                    .assertThatThrownBy(() -> describeByApi.describe(ENVIRONMENT_ID, "unknown-api"))
                    .as("unknown API")
                    .isInstanceOf(ApiNotFoundException.class);
            });
        }

        @Test
        void should_refuse_an_api_that_cannot_run_guardrails() {
            apis.givenApi(aGuardedApi().supportsGuardrails(false).build());

            assertThatThrownBy(() -> describeByApi.describe(ENVIRONMENT_ID, API_ID))
                .as("API without an internal flow")
                .isInstanceOf(ApiCannotRunGuardrailsException.class);
        }
    }

    private void givenDeployed(UnaryOperator<io.gravitee.definition.model.v4.guardrail.Guardrail.GuardrailBuilder> definition) {
        var guardrail = aGuardrail(definition).build();
        repository.givenGuardrail(guardrail);
        publications.givenPublication(guardrail.getDefinition(), DEPLOYED_AT);
    }
}
