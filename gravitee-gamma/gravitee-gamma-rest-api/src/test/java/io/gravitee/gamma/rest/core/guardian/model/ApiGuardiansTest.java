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
package io.gravitee.gamma.rest.core.guardian.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiGuardiansTest {

    @Test
    void should_list_the_guardians_of_each_phase_in_execution_order() {
        var internalFlow = Flow.builder()
            .request(List.of(GuardianSteps.of("guardian-1", "One"), aPolicy(), GuardianSteps.of("guardian-2", "Two")))
            .response(List.of(GuardianSteps.of("guardian-3", "Three")))
            .build();

        assertThat(ApiGuardians.of(internalFlow).list())
            .as("enabled guardians")
            .containsExactly(
                new ApiGuardians.Enabled("guardian-1", Phase.REQUEST, 1),
                new ApiGuardians.Enabled("guardian-2", Phase.REQUEST, 2),
                new ApiGuardians.Enabled("guardian-3", Phase.RESPONSE, 1)
            );
    }

    @Test
    void should_list_no_guardian_when_the_api_has_no_internal_flow() {
        assertThat(ApiGuardians.of(null).list()).as("enabled guardians").isEmpty();
    }

    private static Step aPolicy() {
        return Step.builder().name("Rate limit").policy("rate-limit").build();
    }
}
