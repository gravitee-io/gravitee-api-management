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
import static fixtures.core.model.GuardianDecisionFixtures.aDeniedGuardianDecision;
import static org.assertj.core.api.Assertions.assertThat;

import inmemory.GuardianDecisionQueryServiceInMemory;
import io.gravitee.apim.core.log.use_case.GetGuardianDecisionUseCase.Input;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetGuardianDecisionUseCaseTest {

    private static final ExecutionContext CONTEXT = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

    private final GuardianDecisionQueryServiceInMemory guardianDecisionQueryService = new GuardianDecisionQueryServiceInMemory();
    private GetGuardianDecisionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetGuardianDecisionUseCase(guardianDecisionQueryService);
        guardianDecisionQueryService.initWith(
            List.of(
                aDeniedGuardianDecision().id("decision-1").apiId("api-1").build(),
                aDeniedGuardianDecision().id("decision-2").environmentId("other-environment").build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        guardianDecisionQueryService.reset();
    }

    @Test
    void should_return_the_decision_with_everything_the_gateway_reported() {
        var output = useCase.execute(new Input(CONTEXT, "decision-1", null));

        assertThat(output.decision()).contains(aDeniedGuardianDecision().id("decision-1").apiId("api-1").build());
    }

    @Test
    void should_return_the_decision_of_an_api_in_scope() {
        assertThat(useCase.execute(new Input(CONTEXT, "decision-1", Set.of("api-1", "api-2"))).decision()).isPresent();
    }

    @Test
    void should_not_return_the_decision_of_an_api_out_of_scope() {
        assertThat(useCase.execute(new Input(CONTEXT, "decision-1", Set.of("api-2"))).decision()).isEmpty();
        assertThat(useCase.execute(new Input(CONTEXT, "decision-1", Set.of())).decision()).isEmpty();
    }

    @Test
    void should_not_return_the_decision_of_another_environment() {
        assertThat(useCase.execute(new Input(CONTEXT, "decision-2", null)).decision()).isEmpty();
    }

    @Test
    void should_return_nothing_for_an_unknown_decision() {
        assertThat(useCase.execute(new Input(CONTEXT, "unknown", null)).decision()).isEmpty();
    }
}
