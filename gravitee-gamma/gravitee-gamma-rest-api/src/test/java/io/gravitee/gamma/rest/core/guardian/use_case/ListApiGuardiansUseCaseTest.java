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

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.API_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.domain_service.DescribeApiGuardiansDomainService;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardian;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListApiGuardiansUseCaseTest {

    private final InMemoryGuardedApiPort apis = new InMemoryGuardedApiPort();
    private final InMemoryGuardianRepository guardians = new InMemoryGuardianRepository();
    private final ListApiGuardiansUseCase useCase = new ListApiGuardiansUseCase(apis, new DescribeApiGuardiansDomainService(guardians));

    @Test
    void should_list_the_guardians_enabled_on_the_api_with_their_name() {
        guardians.givenGuardian(aGuardian(d -> d.id("guardian-1").name("Credential leakage")).build());
        guardians.givenGuardian(aGuardian(d -> d.id("guardian-2").name("Toxic reply").phase(Phase.RESPONSE)).build());
        apis.givenApi(
            aGuardedApi()
                .internalFlow(
                    Flow.builder()
                        .request(List.of(GuardianSteps.of("guardian-1", "Credential leakage")))
                        .response(List.of(GuardianSteps.of("guardian-2", "Toxic reply")))
                        .build()
                )
                .build()
        );

        var output = useCase.execute(new ListApiGuardiansUseCase.Input(ENVIRONMENT_ID, API_ID));

        assertThat(output.guardians())
            .as("guardians enabled on the API")
            .containsExactly(
                new ApiGuardian("guardian-1", "Credential leakage", Phase.REQUEST, 1),
                new ApiGuardian("guardian-2", "Toxic reply", Phase.RESPONSE, 1)
            );
    }

    @Test
    void should_list_no_guardian_on_an_api_running_none() {
        apis.givenApi(aGuardedApi().build());

        var output = useCase.execute(new ListApiGuardiansUseCase.Input(ENVIRONMENT_ID, API_ID));

        assertThat(output.guardians()).as("guardians enabled on the API").isEmpty();
    }

    @Test
    void should_refuse_an_api_of_another_environment() {
        apis.givenApi(aGuardedApi().environmentId("other-env").build());

        assertThatThrownBy(() -> useCase.execute(new ListApiGuardiansUseCase.Input(ENVIRONMENT_ID, API_ID)))
            .as("API of another environment")
            .isInstanceOf(ApiNotFoundException.class);
    }
}
