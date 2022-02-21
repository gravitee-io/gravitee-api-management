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
package io.gravitee.gateway.debug.reactor.handler.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugResponseStep;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DebugExecutionContextTest {

    @Mock
    private MutableExecutionContext executionContext;

    @Mock
    private DebugResponseStep debugStep;

    private DebugExecutionContext cut;

    @Before
    public void setUp() {
        Request request = mock(Request.class);
        when(request.headers()).thenReturn(HttpHeaders.create());
        when(executionContext.request()).thenReturn(request);
        cut = new DebugExecutionContext(executionContext);
    }

    @Test
    public void shouldNotAddAStepTwice() {
        doNothing().when(debugStep).before(any(), any());
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
        doNothing().when(debugStep).after(any(), any(), any(), any());
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
