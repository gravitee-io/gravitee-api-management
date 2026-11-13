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
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.ApiReference;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListGuardianApisUseCaseTest {

    private final InMemoryGuardianRepository guardians = new InMemoryGuardianRepository();
    private final InMemoryGuardedApiPort apis = new InMemoryGuardedApiPort();
    private final ListGuardianApisUseCase useCase = new ListGuardianApisUseCase(guardians, apis);

    @Test
    void should_list_the_apis_of_the_environment_the_guardian_is_enabled_on_by_name() {
        guardians.givenGuardian(aGuardian().build());
        var runningIt = Flow.builder().request(List.of(GuardianSteps.of(GUARDIAN_ID, "Credential leakage"))).build();
        apis.givenApi(aGuardedApi().id("api-2").name("Support assistant").internalFlow(runningIt).build());
        apis.givenApi(aGuardedApi().id("api-1").name("Coding assistant").internalFlow(runningIt).build());
        apis.givenApi(aGuardedApi().id("api-3").name("Translator").build());
        apis.givenApi(aGuardedApi().id("api-4").environmentId("other-env").internalFlow(runningIt).build());

        var output = useCase.execute(new ListGuardianApisUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID));

        assertThat(output.apis())
            .as("APIs running the guardian")
            .containsExactly(new ApiReference("api-1", "Coding assistant"), new ApiReference("api-2", "Support assistant"));
    }

    @Test
    void should_refuse_a_guardian_of_another_environment() {
        guardians.givenGuardian(aGuardian(d -> d.environmentId("other-env")).build());

        assertThatThrownBy(() -> useCase.execute(new ListGuardianApisUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID)))
            .as("guardian of another environment")
            .isInstanceOf(GuardianNotFoundException.class);
    }
}
