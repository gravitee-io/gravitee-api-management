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
package io.gravitee.gamma.rest.core.guardrail.domain_service;

import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.GUARDRAIL_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailInUseException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailInUseException.RefusedChange;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardedApiPort;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * What "an API runs this Guardrail" means, which is the whole point of the refusal: the Guardrails the definition of
 * the API names, which is also what its gateways were given.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardrailUsageDomainServiceTest {

    private final InMemoryGuardedApiPort apis = new InMemoryGuardedApiPort();
    private final GuardrailUsageDomainService usage = new GuardrailUsageDomainService(apis);

    /**
     * The failure the refusal exists for: withdrawing a Guardrail an API runs would stop the traffic of an API nobody
     * touched, since the step stays in the definition the gateways execute.
     */
    @Test
    void should_refuse_while_an_api_runs_it() {
        apis.givenApi(
            aGuardedApi()
                .name("Support assistant")
                .internalFlow(Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build())
                .build()
        );

        assertThatThrownBy(() -> usage.ensureUnused(aGuardrail().build(), RefusedChange.UNDEPLOYMENT))
            .as("a Guardrail an API runs")
            .isInstanceOf(GuardrailInUseException.class)
            .hasMessageContaining("1 API runs it")
            .as("no name of an API the caller may not be allowed to read")
            .hasMessageNotContaining("Support assistant");
    }

    /** An API that turned the Guardrail off no longer holds it back: it stopped running it there and then. */
    @Test
    void should_let_through_an_api_that_turned_it_off() {
        apis.givenApi(aGuardedApi().name("Support assistant").internalFlow(null).build());

        assertThatCode(() -> usage.ensureUnused(aGuardrail().build(), RefusedChange.UNDEPLOYMENT))
            .as("a Guardrail no API names any more")
            .doesNotThrowAnyException();
    }

    @Test
    void should_let_through_a_guardrail_no_api_runs() {
        assertThatCode(() -> usage.ensureUnused(aGuardrail().build(), RefusedChange.DELETION))
            .as("a Guardrail no API runs")
            .doesNotThrowAnyException();
    }
}
