/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.debug.policy.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class PolicyStepFactoryTest {

    @Test
    public void shouldCreatePolicyRequestStep() {
        PolicyStep<?> policyStep = PolicyStepFactory.createPolicyStep("policyId", ExecutionPhase.REQUEST, "flow");

        assertThat(policyStep).isNotNull();
        assertThat(policyStep).isInstanceOf(PolicyRequestStep.class);
        assertThat(policyStep.getId()).isNotNull();
        assertThat(policyStep.getPolicyId()).isEqualTo("policyId");
        assertThat(policyStep.getExecutionPhase()).isEqualTo(ExecutionPhase.REQUEST);
        assertThat(policyStep.getFlowPhase()).isEqualTo("flow");
    }

    @Test
    public void shouldCreatePolicyResponseStep() {
        PolicyStep<?> policyStep = PolicyStepFactory.createPolicyStep("policyId", ExecutionPhase.RESPONSE, "flow");

        assertThat(policyStep).isNotNull();
        assertThat(policyStep).isInstanceOf(PolicyResponseStep.class);
        assertThat(policyStep.getId()).isNotNull();
        assertThat(policyStep.getPolicyId()).isEqualTo("policyId");
        assertThat(policyStep.getExecutionPhase()).isEqualTo(ExecutionPhase.RESPONSE);
        assertThat(policyStep.getFlowPhase()).isEqualTo("flow");
    }
}
