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
package io.gravitee.apim.core.guardian.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.GuardianFixtures;
import inmemory.GuardianCrudServiceInMemory;
import io.gravitee.apim.core.guardian.exception.GuardianNotFoundException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetGuardianUseCaseTest {

    private final GuardianCrudServiceInMemory guardianCrudService = new GuardianCrudServiceInMemory();
    private GetGuardianUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetGuardianUseCase(guardianCrudService);
        guardianCrudService.initWith(List.of(GuardianFixtures.aGuardian().build()));
    }

    @AfterEach
    void tearDown() {
        guardianCrudService.reset();
    }

    @Test
    void should_return_the_guardian_of_the_environment() {
        var output = useCase.execute(new GetGuardianUseCase.Input("env-id", "guardian-id"));

        assertThat(output.guardian().getName()).isEqualTo("Credential leakage");
    }

    @Test
    void should_not_return_a_guardian_of_another_environment() {
        assertThatThrownBy(() -> useCase.execute(new GetGuardianUseCase.Input("other-env", "guardian-id"))).isInstanceOf(
            GuardianNotFoundException.class
        );
    }

    @Test
    void should_throw_when_the_guardian_does_not_exist() {
        assertThatThrownBy(() -> useCase.execute(new GetGuardianUseCase.Input("env-id", "unknown"))).isInstanceOf(
            GuardianNotFoundException.class
        );
    }
}
