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
package io.gravitee.apim.core.scoring.use_case;

import static assertions.CoreAssertions.assertThat;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.ScoringFunctionFixture;
import inmemory.InMemoryAlternative;
import inmemory.ScoringFunctionCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.scoring.model.ScoringFunction;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteEnvironmentFunctionUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    ScoringFunctionCrudServiceInMemory scoringFunctionCrudService = new ScoringFunctionCrudServiceInMemory();

    DeleteEnvironmentFunctionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteEnvironmentFunctionUseCase(scoringFunctionCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(scoringFunctionCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_delete_a_function() {
        // Given
        var functionId = "function-id";
        givenExistingFunctions(ScoringFunctionFixture.aFunction(functionId).withReferenceId(ENVIRONMENT_ID));

        // When
        useCase.execute(new DeleteEnvironmentFunctionUseCase.Input(functionId, AUDIT_INFO));

        // Then
        assertThat(scoringFunctionCrudService.storage()).isEmpty();
    }

    @Test
    void should_do_nothing_when_no_function() {
        // When
        useCase.execute(new DeleteEnvironmentFunctionUseCase.Input("unknown", AUDIT_INFO));

        // Then
        assertThat(scoringFunctionCrudService.storage()).isEmpty();
    }

    @Test
    void should_do_nothing_when_function_found_is_from_another_env() {
        // Given
        var functionId = "function-id";
        givenExistingFunctions(ScoringFunctionFixture.aFunction(functionId).withReferenceId("other"));

        // When
        useCase.execute(new DeleteEnvironmentFunctionUseCase.Input(functionId, AUDIT_INFO));

        // Then
        assertThat(scoringFunctionCrudService.storage()).hasSize(1);
    }

    private void givenExistingFunctions(ScoringFunction... functions) {
        scoringFunctionCrudService.initWith(List.of(functions));
    }
}
