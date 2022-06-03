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

import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.reactivex.Single;
import java.io.Serializable;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class PolicyStepTest {

    private DummyPolicyStep dummyPolicyStep;

    @BeforeEach
    public void setUp() {
        dummyPolicyStep = new DummyPolicyStep("policy", ExecutionPhase.REQUEST, "api");
    }

    @Test
    public void shouldIgnoreStartWhenAlreadyRunning() {
        assertThat(dummyPolicyStep.stopwatch.isRunning()).isFalse();
        dummyPolicyStep.starTimeWatch();
        assertThat(dummyPolicyStep.stopwatch.isRunning()).isTrue();
        dummyPolicyStep.starTimeWatch();
        assertThat(dummyPolicyStep.stopwatch.isRunning()).isTrue();
    }

    @Test
    public void shouldIgnoreStopWhenAlreadyStopped() {
        dummyPolicyStep.starTimeWatch();
        assertThat(dummyPolicyStep.stopwatch.isRunning()).isTrue();
        dummyPolicyStep.stopTimeWatch();
        assertThat(dummyPolicyStep.stopwatch.isRunning()).isFalse();
        dummyPolicyStep.stopTimeWatch();
        assertThat(dummyPolicyStep.stopwatch.isRunning()).isFalse();
    }

    @Test
    public void shouldSaveStateAndCleanIt() {
        // Assert initial state
        assertThat(dummyPolicyStep.stopwatch.isRunning()).isFalse();
        assertThat(dummyPolicyStep.getInputState()).isNull();

        // Assert state on before call
        dummyPolicyStep.pre(null, null).test().assertResult();
        assertThat(dummyPolicyStep.getInputState()).isNotNull();
        assertThat(dummyPolicyStep.stopwatch.isRunning()).isTrue();

        // Assert state on after call
        dummyPolicyStep.post(null, null).test().assertResult();
        assertThat(dummyPolicyStep.stopwatch.isRunning()).isFalse();
        assertThat(dummyPolicyStep.getInputState()).isNull();
    }

    static class DummyPolicyStep extends PolicyStep<Object> {

        public DummyPolicyStep(final String policyId, final ExecutionPhase exedummyPolicyStepionPhase, final String flowPhase) {
            super(policyId, exedummyPolicyStepionPhase, flowPhase);
        }

        @Override
        protected Single<PolicyStepState> saveInputState(final Object source, final Map<String, Serializable> inputAttributes) {
            return Single.just(new PolicyStepState());
        }

        @Override
        protected Single<Map<String, Object>> computeDiff(
            final Object source,
            final PolicyStepState inputState,
            final Map<String, Serializable> outputAttributes
        ) {
            return Single.just(Map.of());
        }
    }
}
