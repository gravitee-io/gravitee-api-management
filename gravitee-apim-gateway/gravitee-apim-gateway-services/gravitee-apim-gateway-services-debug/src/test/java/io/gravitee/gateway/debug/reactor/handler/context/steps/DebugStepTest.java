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
package io.gravitee.gateway.debug.reactor.handler.context.steps;

import static org.assertj.core.api.Assertions.*;

import com.google.common.base.Stopwatch;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.debug.reactor.handler.context.DebugScope;
import io.gravitee.gateway.policy.StreamType;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugStepTest {

    private DebugTestingStep cut;

    @Before
    public void setUp() {
        cut = new DebugTestingStep("policy", StreamType.ON_REQUEST, "uid", DebugScope.ON_REQUEST);
    }

    @Test
    public void shouldNotThrowExceptionIfStartWhenRunning() {
        assertThat(cut.getStopwatch().isRunning()).isFalse();
        cut.start();
        assertThat(cut.getStopwatch().isRunning()).isTrue();
        cut.start();
        assertThat(cut.getStopwatch().isRunning()).isTrue();
    }

    @Test
    public void shouldNotThrowExceptionIfStopWhenNotRunning() {
        cut.start();
        assertThat(cut.getStopwatch().isRunning()).isTrue();
        cut.stop();
        assertThat(cut.getStopwatch().isRunning()).isFalse();
        cut.stop();
        assertThat(cut.getStopwatch().isRunning()).isFalse();
    }

    @Test
    public void shouldStartStopAndEmptyPolicyInputContentOnBeforeThenAfter() {
        // Assert initial state
        assertThat(cut.getStopwatch().isRunning()).isFalse();
        assertThat(cut.getPolicyInputContent()).isNotNull();

        // Assert state on before call
        cut.before(null, null);
        assertThat(cut.getPolicyInputContent()).isNotNull();
        assertThat(cut.getStopwatch().isRunning()).isTrue();

        // Assert state on after call
        cut.after(null, null, null, null);
        assertThat(cut.getStopwatch().isRunning()).isFalse();
        assertThat(cut.getPolicyInputContent()).isNull();
    }

    static class DebugTestingStep extends DebugStep<Object> {

        public DebugTestingStep(String policyId, StreamType streamType, String uuid, DebugScope debugScope) {
            super(policyId, streamType, uuid, debugScope);
        }

        @Override
        protected void snapshotInputData(Object source, Map<String, Object> attributes) {}

        @Override
        protected void generateDiffMap(Object source, Map<String, Object> attributes, Buffer inputBuffer, Buffer outputBuffer) {}

        public DebugStepContent getPolicyInputContent() {
            return this.policyInputContent;
        }

        public Stopwatch getStopwatch() {
            return this.stopwatch;
        }
    }
}
