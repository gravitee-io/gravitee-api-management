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
package io.gravitee.gamma.rest.core.guardian.use_case;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.GuardianDeploymentFixtures;
import io.gravitee.gamma.rest.core.guardian.GuardianFixtures;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchGuardianUseCaseTest {

    private final InMemoryGuardianRepository guardianRepository = new InMemoryGuardianRepository();

    private SearchGuardianUseCase useCase;

    @BeforeEach
    void setUp() {
        guardianRepository.reset();
        guardianRepository.givenGuardian(aGuardian(d -> d.id("one").name("Credential leakage")).build());
        guardianRepository.givenGuardian(aGuardian(d -> d.id("two").name("Prompt injection")).build());
        guardianRepository.givenGuardian(aGuardian(d -> d.id("elsewhere").environmentId("other-env").name("Credential leakage")).build());
        useCase = new SearchGuardianUseCase(guardianRepository, GuardianDeploymentFixtures.deploymentService(guardianRepository));
    }

    private SearchGuardianUseCase.Output search(SearchGuardianUseCase.Input.InputBuilder input) {
        return useCase.execute(input.environmentId(ENVIRONMENT_ID).build());
    }

    @Test
    void should_list_only_the_guardians_of_the_environment() {
        var output = search(SearchGuardianUseCase.Input.builder());

        assertThat(output.guardians().getContent())
            .extracting(g -> g.getId())
            .containsExactlyInAnyOrder("one", "two");
        assertThat(output.guardians().getTotalElements()).isEqualTo(2);
    }

    @Test
    void should_filter_on_the_name() {
        var output = search(SearchGuardianUseCase.Input.builder().q("injection"));

        assertThat(output.guardians().getContent())
            .extracting(g -> g.getId())
            .containsExactly("two");
    }

    @Test
    void should_paginate() {
        var output = search(SearchGuardianUseCase.Input.builder().pageNumber(2).pageSize(1));

        assertThat(output.guardians().getContent()).hasSize(1);
        assertThat(output.guardians().getTotalElements()).isEqualTo(2);
    }

    @Test
    void should_report_the_page_size_it_resolved() {
        assertThat(search(SearchGuardianUseCase.Input.builder()).perPage()).isEqualTo(10);
        assertThat(search(SearchGuardianUseCase.Input.builder().pageSize(0)).perPage()).isEqualTo(10);
        assertThat(search(SearchGuardianUseCase.Input.builder().pageSize(3)).perPage()).isEqualTo(3);
    }

    @Test
    void should_cap_the_page_size_a_caller_can_ask_for() {
        // A Guardian row carries its serialized definition, so an uncapped perPage would read far
        // more than any list renders. ListObservabilityDashboardUseCase caps at the same 100.
        assertThat(search(SearchGuardianUseCase.Input.builder().pageSize(1_000)).perPage()).isEqualTo(100);
        assertThat(search(SearchGuardianUseCase.Input.builder().pageSize(50)).perPage()).isEqualTo(50);
    }

    @Test
    void should_sort_on_the_phase() {
        guardianRepository.reset();
        guardianRepository.givenGuardian(aGuardian(d -> d.id("out").name("Z").phase(Phase.RESPONSE)).build());
        guardianRepository.givenGuardian(aGuardian(d -> d.id("in").name("A").phase(Phase.REQUEST)).build());

        var output = search(SearchGuardianUseCase.Input.builder().sortBy("phase"));

        // REQUEST before RESPONSE: sorted on the phase, not on the name and not on the id the double
        // breaks ties with
        assertThat(output.guardians().getContent()).extracting(Guardian::getId).containsExactly("in", "out");
    }

    @Test
    void should_refuse_an_unknown_sort_field() {
        // the API types are not sortable: the definition carries them as a set, which has no single
        // value to order rows by
        assertThatThrownBy(() -> search(SearchGuardianUseCase.Input.builder().sortBy("apiType")))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("apiType");
    }

    /**
     * What a publisher is offered to turn on: the Guardians the gateways run, whose deployed revision supports the
     * type of the API. The filter runs before the page is cut, so an eligible Guardian is never lost on another page.
     */
    @Nested
    class DeployedFor {

        private final GuardianPublicationFixture publications = new GuardianPublicationFixture();
        private final SearchGuardianUseCase offering = new SearchGuardianUseCase(
            guardianRepository,
            publications.service(guardianRepository)
        );

        @BeforeEach
        void given_guardians_deployed_or_not_for_one_api_type_or_another() {
            guardianRepository.reset();
            givenDeployed("llm-1", "A llm guardian", ApiType.LLM_PROXY);
            guardianRepository.givenGuardian(
                aGuardian(d -> d.id("draft-only").name("B never deployed").apiTypes(Set.of(ApiType.LLM_PROXY))).build()
            );
            givenDeployed("proxy-only", "C proxy guardian", ApiType.PROXY);
            givenDeployed("llm-2", "D llm guardian", ApiType.LLM_PROXY);
        }

        @Test
        void should_offer_only_the_deployed_guardians_an_api_type_can_run() {
            var output = offering.execute(
                SearchGuardianUseCase.Input.builder().environmentId(ENVIRONMENT_ID).deployedFor("llm-proxy").sortBy("name").build()
            );

            assertSoftly(softly -> {
                softly
                    .assertThat(output.guardians().getContent())
                    .extracting(Guardian::getId)
                    .as("guardians offered")
                    .containsExactly("llm-1", "llm-2");
                softly.assertThat(output.guardians().getTotalElements()).as("guardians offered in all").isEqualTo(2);
            });
        }

        @Test
        void should_page_the_guardians_offered_rather_than_the_ones_stored() {
            var output = offering.execute(
                SearchGuardianUseCase.Input.builder()
                    .environmentId(ENVIRONMENT_ID)
                    .deployedFor("llm-proxy")
                    .sortBy("name")
                    .pageNumber(2)
                    .pageSize(1)
                    .build()
            );

            assertSoftly(softly -> {
                softly
                    .assertThat(output.guardians().getContent())
                    .extracting(Guardian::getId)
                    .as("second guardian offered")
                    .containsExactly("llm-2");
                softly.assertThat(output.guardians().getTotalElements()).as("guardians offered in all").isEqualTo(2);
            });
        }

        @Test
        void should_refuse_an_unknown_api_type() {
            assertThatThrownBy(() ->
                offering.execute(SearchGuardianUseCase.Input.builder().environmentId(ENVIRONMENT_ID).deployedFor("no-such-type").build())
            )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("no-such-type");
        }

        private void givenDeployed(String id, String name, ApiType apiType) {
            var guardian = aGuardian(d -> d.id(id).name(name).apiTypes(Set.of(apiType))).build();
            guardianRepository.givenGuardian(guardian);
            publications.givenPublication(guardian.getDefinition(), Instant.parse("2026-11-09T10:00:00Z"));
        }
    }
}
