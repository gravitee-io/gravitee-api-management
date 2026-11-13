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

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.API_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.exception.ApiCannotRunGuardiansException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardian;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardians;
import io.gravitee.gamma.rest.core.guardian.model.GuardrailEffect;
import java.time.Instant;
import java.util.List;
import java.util.function.UnaryOperator;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DescribeApiGuardiansDomainServiceTest {

    private static final Instant DEPLOYED_AT = Instant.parse("2026-11-09T10:00:00Z");

    private final InMemoryGuardianRepository repository = new InMemoryGuardianRepository();
    private final GuardianPublicationFixture publications = new GuardianPublicationFixture();
    private final DescribeApiGuardiansDomainService service = new DescribeApiGuardiansDomainService(
        publications.service(repository),
        new InMemoryGuardedApiPort()
    );

    @Test
    void should_name_the_guardians_of_each_phase_in_execution_order() {
        givenDeployed(d -> d.id("first").name("Prompt injection"));
        givenDeployed(d -> d.id("second").name("Toxicity").phase(Phase.RESPONSE));
        var enabled = ApiGuardians.of(
            Flow.builder()
                .request(List.of(GuardianSteps.of("first", "Old name"), GuardianSteps.of("missing", "Missing")))
                .response(List.of(GuardianSteps.of("second", "Old name")))
                .build()
        );

        assertThat(service.describe(ENVIRONMENT_ID, enabled))
            .as("guardians of the API, named by the revision the gateways run")
            .containsExactly(
                new ApiGuardian("first", "Prompt injection", Phase.REQUEST, 1, List.of()),
                new ApiGuardian("missing", null, Phase.REQUEST, 2, List.of()),
                new ApiGuardian("second", "Toxicity", Phase.RESPONSE, 1, List.of())
            );
    }

    /** A rename nobody deployed changes nothing the calls of the API meet, so it changes nothing here either. */
    @Test
    void should_name_a_guardian_after_its_deployed_revision_not_after_its_draft() {
        givenDeployed(d -> d.id("first").name("Prompt injection"));
        repository.update(aGuardian(d -> d.id("first").name("Renamed draft")).build());
        var enabled = ApiGuardians.of(Flow.builder().request(List.of(GuardianSteps.of("first", "Prompt injection"))).build());

        assertThat(service.describe(ENVIRONMENT_ID, enabled))
            .as("guardians of the API")
            .singleElement()
            .extracting(ApiGuardian::name)
            .isEqualTo("Prompt injection");
    }

    @Test
    void should_report_what_the_guardrails_of_the_deployed_revision_do() {
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
        var enabled = ApiGuardians.of(Flow.builder().request(List.of(GuardianSteps.of("blocking", "Blocking"))).build());

        assertThat(service.describe(ENVIRONMENT_ID, enabled))
            .as("described guardians")
            .singleElement()
            .extracting(ApiGuardian::effects, InstanceOfAssertFactories.list(GuardrailEffect.class))
            .as("effects of the sequence")
            .containsExactly(GuardrailEffect.BLOCK, GuardrailEffect.CONTINUE);
    }

    @Test
    void should_report_no_effect_for_a_guardian_no_longer_deployed() {
        repository.givenGuardian(aGuardian(d -> d.id("withdrawn").name("Withdrawn")).build());
        var enabled = ApiGuardians.of(Flow.builder().request(List.of(GuardianSteps.of("withdrawn", "Withdrawn"))).build());

        assertThat(service.describe(ENVIRONMENT_ID, enabled))
            .as("guardian without a deployed revision")
            .singleElement()
            .isEqualTo(new ApiGuardian("withdrawn", null, Phase.REQUEST, 1, List.of()));
    }

    @Test
    void should_read_nothing_when_no_guardian_is_enabled() {
        assertSoftly(softly -> {
            softly.assertThat(service.describe(ENVIRONMENT_ID, ApiGuardians.of(null))).as("no enabled Guardians").isEmpty();
            softly.assertThat(publications.reads()).as("no unnecessary reads").isZero();
        });
    }

    /** The deployment history is read for the whole list at once: two Guardians cost what one costs. */
    @Test
    void should_read_the_deployment_history_as_often_for_several_guardians_as_for_one() {
        givenDeployed(d -> d.id("first").name("One"));
        givenDeployed(d -> d.id("second").name("Two"));
        publications.resetReads();
        service.describe(ENVIRONMENT_ID, ApiGuardians.of(Flow.builder().request(List.of(GuardianSteps.of("first", "One"))).build()));
        var forOne = publications.reads();
        publications.resetReads();

        service.describe(
            ENVIRONMENT_ID,
            ApiGuardians.of(Flow.builder().request(List.of(GuardianSteps.of("first", "One"), GuardianSteps.of("second", "Two"))).build())
        );

        assertSoftly(softly -> {
            softly.assertThat(forOne).as("reads for one Guardian").isPositive();
            softly.assertThat(publications.reads()).as("reads for two Guardians").isEqualTo(forOne);
        });
    }

    /**
     * The read a module owning an API surface calls: it names an API, not a sequence of steps, so resolving the API is
     * this service's job rather than the adapter's. Its refusals are the ones setting the Guardians answers.
     */
    @Nested
    class DescribeByApiId {

        private final InMemoryGuardedApiPort apis = new InMemoryGuardedApiPort();
        private final DescribeApiGuardiansDomainService describeByApi = new DescribeApiGuardiansDomainService(
            publications.service(repository),
            apis
        );

        @Test
        void should_name_the_guardians_the_api_runs() {
            givenDeployed(d -> d.id("first").name("Prompt injection"));
            apis.givenApi(
                aGuardedApi().internalFlow(Flow.builder().request(List.of(GuardianSteps.of("first", "Old name"))).build()).build()
            );

            assertThat(describeByApi.describe(ENVIRONMENT_ID, API_ID))
                .as("guardians of the API, named by the revision the gateways run")
                .containsExactly(new ApiGuardian("first", "Prompt injection", Phase.REQUEST, 1, List.of()));
        }

        @Test
        void should_name_no_guardian_on_an_api_running_none() {
            apis.givenApi(aGuardedApi().build());

            assertThat(describeByApi.describe(ENVIRONMENT_ID, API_ID)).as("guardians of the API").isEmpty();
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
        void should_refuse_an_api_that_cannot_run_guardians() {
            apis.givenApi(aGuardedApi().supportsGuardians(false).build());

            assertThatThrownBy(() -> describeByApi.describe(ENVIRONMENT_ID, API_ID))
                .as("API without an internal flow")
                .isInstanceOf(ApiCannotRunGuardiansException.class);
        }
    }

    private void givenDeployed(UnaryOperator<io.gravitee.definition.model.v4.guardian.Guardian.GuardianBuilder> definition) {
        var guardian = aGuardian(definition).build();
        repository.givenGuardian(guardian);
        publications.givenPublication(guardian.getDefinition(), DEPLOYED_AT);
    }
}
