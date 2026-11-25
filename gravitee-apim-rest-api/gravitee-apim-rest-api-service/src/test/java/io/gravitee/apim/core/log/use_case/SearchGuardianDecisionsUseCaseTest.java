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
package io.gravitee.apim.core.log.use_case;

import static fixtures.core.model.GuardianDecisionFixtures.ENVIRONMENT_ID;
import static fixtures.core.model.GuardianDecisionFixtures.ORGANIZATION_ID;
import static fixtures.core.model.GuardianDecisionFixtures.TIMESTAMP;
import static fixtures.core.model.GuardianDecisionFixtures.aDeniedGuardianDecision;
import static fixtures.core.model.GuardianDecisionFixtures.aGuardianDecision;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.GuardianDecisionQueryServiceInMemory;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.log.model.GuardianDecision;
import io.gravitee.apim.core.log.model.GuardianDecisionFilters;
import io.gravitee.apim.core.log.use_case.SearchGuardianDecisionsUseCase.Input;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchGuardianDecisionsUseCaseTest {

    private static final ExecutionContext CONTEXT = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
    private static final GuardianDecisionFilters NO_FILTER = GuardianDecisionFilters.builder().build();

    private final GuardianDecisionQueryServiceInMemory guardianDecisionQueryService = new GuardianDecisionQueryServiceInMemory();
    private SearchGuardianDecisionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchGuardianDecisionsUseCase(guardianDecisionQueryService);
        guardianDecisionQueryService.initWith(
            List.of(
                aGuardianDecision().id("oldest").timestamp(TIMESTAMP - 2_000).requestId("req-1").build(),
                aDeniedGuardianDecision().id("denied").timestamp(TIMESTAMP - 1_000).requestId("req-2").build(),
                aGuardianDecision()
                    .id("transformed")
                    .timestamp(TIMESTAMP)
                    .guardianId("other-guardian")
                    .checkpoint("response.llm-proxy")
                    .outcome("TRANSFORM")
                    .requestId("req-2")
                    .build(),
                aGuardianDecision().id("other-api").timestamp(TIMESTAMP).apiId("other-api").requestId("REQ-3").build(),
                aGuardianDecision().id("other-environment").environmentId("other-environment").build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        guardianDecisionQueryService.reset();
    }

    @Test
    void should_return_every_decision_of_the_environment_newest_first_with_the_total() {
        var output = useCase.execute(new Input(CONTEXT, NO_FILTER, 1, 20));

        assertThat(output.total()).isEqualTo(4);
        assertThat(output.decisions()).extracting(GuardianDecision::id).containsExactly("other-api", "transformed", "denied", "oldest");
    }

    @Test
    void should_return_one_page_and_the_total_of_every_page() {
        var output = useCase.execute(new Input(CONTEXT, NO_FILTER, 2, 3));

        assertThat(output.total()).isEqualTo(4);
        assertThat(output.decisions()).extracting(GuardianDecision::id).containsExactly("oldest");
    }

    @Test
    void should_narrow_on_the_apis_the_guardians_and_the_outcomes() {
        var filters = GuardianDecisionFilters.builder()
            .apiIds(Set.of("api-id"))
            .guardianIds(Set.of("guardian-id"))
            .outcomes(Set.of("DENY", "TRANSFORM"))
            .build();

        var output = useCase.execute(new Input(CONTEXT, filters, 1, 20));

        assertThat(output.decisions()).extracting(GuardianDecision::id).containsExactly("denied");
    }

    @Test
    void should_return_no_decision_when_no_api_is_in_scope() {
        var output = useCase.execute(new Input(CONTEXT, GuardianDecisionFilters.builder().apiIds(Set.of()).build(), 1, 20));

        assertThat(output.total()).isZero();
        assertThat(output.decisions()).isEmpty();
    }

    @Test
    void should_return_the_decisions_of_a_call() {
        var output = useCase.execute(new Input(CONTEXT, GuardianDecisionFilters.builder().requestIds(Set.of("req-2")).build(), 1, 20));

        assertThat(output.decisions()).extracting(GuardianDecision::id).containsExactly("transformed", "denied");
    }

    @Test
    void should_match_a_fragment_of_the_request_id_ignoring_case() {
        var output = useCase.execute(new Input(CONTEXT, GuardianDecisionFilters.builder().requestIdContains("q-3").build(), 1, 20));

        assertThat(output.decisions()).extracting(GuardianDecision::id).containsExactly("other-api");
    }

    @Test
    void should_keep_the_decisions_of_the_time_range() {
        var filters = GuardianDecisionFilters.builder().from(TIMESTAMP - 1_500).to(TIMESTAMP - 500).build();

        var output = useCase.execute(new Input(CONTEXT, filters, 1, 20));

        assertThat(output.decisions()).extracting(GuardianDecision::id).containsExactly("denied");
    }

    @Test
    void should_reject_a_page_below_one() {
        assertThatThrownBy(() -> useCase.execute(new Input(CONTEXT, NO_FILTER, 0, 20)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("page");
    }

    @Test
    void should_reject_a_page_size_out_of_bounds() {
        assertThatThrownBy(() -> useCase.execute(new Input(CONTEXT, NO_FILTER, 1, 0))).isInstanceOf(ValidationDomainException.class);
        assertThatThrownBy(() -> useCase.execute(new Input(CONTEXT, NO_FILTER, 1, SearchGuardianDecisionsUseCase.MAX_PAGE_SIZE + 1)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining(String.valueOf(SearchGuardianDecisionsUseCase.MAX_PAGE_SIZE));
    }

    @Test
    void should_reject_an_inverted_time_range() {
        var filters = GuardianDecisionFilters.builder().from(TIMESTAMP).to(TIMESTAMP - 1).build();

        assertThatThrownBy(() -> useCase.execute(new Input(CONTEXT, filters, 1, 20)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("from");
    }
}
