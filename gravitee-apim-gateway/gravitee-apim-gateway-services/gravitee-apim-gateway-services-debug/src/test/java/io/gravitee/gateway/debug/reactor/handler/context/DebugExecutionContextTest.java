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
package io.gravitee.gateway.debug.reactor.handler.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugResponseStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class DebugExecutionContextTest {

    @Mock
    private MutableExecutionContext executionContext;

    @Mock
    private DebugResponseStep debugStep;

    private DebugExecutionContext cut;

    @BeforeEach
    public void setUp() {
        Request request = mock(Request.class);
        when(request.headers()).thenReturn(HttpHeaders.create());
        when(executionContext.request()).thenReturn(request);
        cut = new DebugExecutionContext(executionContext);
    }

    @Test
    public void shouldAddNoTransformationStep() {
        cut.saveNoTransformationDebugStep(debugStep);
        assertThat(cut.getDebugSteps()).hasSize(1);
        assertThat(cut.getDebugSteps()).contains(debugStep);
        verify(debugStep, times(1)).noTransformation();
    }

    @Test
    public void shouldNotAddAStepTwice() {
        cut.beforePolicyExecution(debugStep);
        assertThat(cut.getDebugSteps()).hasSize(1);
        assertThat(cut.getDebugSteps()).contains(debugStep);
        cut.beforePolicyExecution(debugStep);
        assertThat(cut.getDebugSteps()).hasSize(1);
        assertThat(cut.getDebugSteps()).contains(debugStep);
        verify(debugStep, atMostOnce()).before(any(), any());
    }

    @Test
    public void shouldNotEndAStepTwice() {
        doCallRealMethod().when(debugStep).ended();
        when(debugStep.isEnded()).thenCallRealMethod();

        assertThat(debugStep.isEnded()).isFalse();

        cut.afterPolicyExecution(debugStep);
        assertThat(debugStep.isEnded()).isTrue();
        cut.afterPolicyExecution(debugStep);
        assertThat(debugStep.isEnded()).isTrue();
        verify(debugStep, atMostOnce()).after(any(), any(), any(), any());
    }
}
