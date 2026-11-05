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
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.GUARDIAN_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetGuardianUseCaseTest {

    private final InMemoryGuardianRepository guardianRepository = new InMemoryGuardianRepository();

    private GetGuardianUseCase useCase;

    @BeforeEach
    void setUp() {
        guardianRepository.reset();
        guardianRepository.givenGuardian(aGuardian().build());
        useCase = new GetGuardianUseCase(guardianRepository);
    }

    @Test
    void should_return_the_guardian_of_the_environment() {
        var output = useCase.execute(GetGuardianUseCase.Input.builder().environmentId(ENVIRONMENT_ID).guardianId(GUARDIAN_ID).build());

        assertThat(output.guardian().getId()).isEqualTo(GUARDIAN_ID);
    }

    @Test
    void should_not_find_an_unknown_guardian() {
        assertThatThrownBy(() ->
            useCase.execute(GetGuardianUseCase.Input.builder().environmentId(ENVIRONMENT_ID).guardianId("nope").build())
        ).isInstanceOf(GuardianNotFoundException.class);
    }

    @Test
    void should_not_find_a_guardian_of_another_environment() {
        assertThatThrownBy(() ->
            useCase.execute(GetGuardianUseCase.Input.builder().environmentId("other-env").guardianId(GUARDIAN_ID).build())
        ).isInstanceOf(GuardianNotFoundException.class);
    }
}
