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
package io.gravitee.gamma.rest.core.guardrail.model;

import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GuardrailRunsWhereTest {

    private final Guardrail guardrail = aGuardrail(d -> d.phase(Phase.REQUEST).apiTypes(Set.of(ApiType.LLM_PROXY))).build();

    @Test
    void should_run_where_a_guardrail_of_the_same_phase_and_api_types_runs() {
        var renamed = aGuardrail(d -> d.name("Renamed").phase(Phase.REQUEST).apiTypes(Set.of(ApiType.LLM_PROXY))).build();

        assertThat(renamed.runsWhere(guardrail)).as("same phase and API types").isTrue();
    }

    @Test
    void should_not_run_where_a_guardrail_of_another_phase_runs() {
        var moved = aGuardrail(d -> d.phase(Phase.RESPONSE).apiTypes(Set.of(ApiType.LLM_PROXY))).build();

        assertThat(moved.runsWhere(guardrail)).as("another phase").isFalse();
    }

    @Test
    void should_not_run_where_a_guardrail_of_other_api_types_runs() {
        var widened = aGuardrail(d -> d.phase(Phase.REQUEST).apiTypes(Set.of(ApiType.LLM_PROXY, ApiType.PROXY))).build();

        assertThat(widened.runsWhere(guardrail)).as("other API types").isFalse();
    }
}
