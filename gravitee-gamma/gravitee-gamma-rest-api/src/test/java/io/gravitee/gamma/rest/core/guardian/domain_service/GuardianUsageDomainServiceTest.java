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

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.GUARDIAN_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianInUseException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianInUseException.RefusedChange;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardedApiPort;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * What "an API runs this Guardian" means, which is the whole point of the refusal: the Guardians the definition of
 * the API names, which is also what its gateways were given.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianUsageDomainServiceTest {

    private final InMemoryGuardedApiPort apis = new InMemoryGuardedApiPort();
    private final GuardianUsageDomainService usage = new GuardianUsageDomainService(apis);

    /**
     * The failure the refusal exists for: withdrawing a Guardian an API runs would stop the traffic of an API nobody
     * touched, since the step stays in the definition the gateways execute.
     */
    @Test
    void should_refuse_while_an_api_runs_it() {
        apis.givenApi(
            aGuardedApi()
                .name("Support assistant")
                .internalFlow(Flow.builder().request(List.of(GuardianSteps.of(GUARDIAN_ID, "Credential leakage"))).build())
                .build()
        );

        assertThatThrownBy(() -> usage.ensureUnused(aGuardian().build(), RefusedChange.UNDEPLOYMENT))
            .as("a Guardian an API runs")
            .isInstanceOf(GuardianInUseException.class)
            .hasMessageContaining("1 API runs it")
            .as("no name of an API the caller may not be allowed to read")
            .hasMessageNotContaining("Support assistant");
    }

    /** An API that turned the Guardian off no longer holds it back: it stopped running it there and then. */
    @Test
    void should_let_through_an_api_that_turned_it_off() {
        apis.givenApi(aGuardedApi().name("Support assistant").internalFlow(null).build());

        assertThatCode(() -> usage.ensureUnused(aGuardian().build(), RefusedChange.UNDEPLOYMENT))
            .as("a Guardian no API names any more")
            .doesNotThrowAnyException();
    }

    @Test
    void should_let_through_a_guardian_no_api_runs() {
        assertThatCode(() -> usage.ensureUnused(aGuardian().build(), RefusedChange.DELETION))
            .as("a Guardian no API runs")
            .doesNotThrowAnyException();
    }
}
