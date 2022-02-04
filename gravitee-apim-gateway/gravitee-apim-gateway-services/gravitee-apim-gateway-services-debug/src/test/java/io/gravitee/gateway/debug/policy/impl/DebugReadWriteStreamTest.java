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
package io.gravitee.gateway.debug.policy.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContext;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStep;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DebugReadWriteStreamTest {

    @Mock
    private ReadWriteStream<Buffer> delegate;

    @Mock
    private DebugStep<?> debugStep;

    @Mock
    private DebugExecutionContext context;

    @Mock
    private Handler<Buffer> bodyHandler;

    @Mock
    private Handler<Void> endHandler;

    @Captor
    private ArgumentCaptor<Buffer> inputBufferCaptor;

    @Captor
    private ArgumentCaptor<Buffer> outputBufferCaptor;

    @Captor
    private ArgumentCaptor<Handler<Buffer>> delegateBodyHandlerCaptor;

    @Captor
    private ArgumentCaptor<Handler<Void>> delegateEndHandlerCaptor;

    private DebugReadWriteStream cut;

    @Before
    public void setUp() {
        cut = new DebugReadWriteStream(context, delegate, debugStep);
        cut.bodyHandler(bodyHandler);
        cut.endHandler(endHandler);
    }

    @Test
    public void shouldDoTheDebugActions() {
        // Write the input buffer for policy
        cut.write(Buffer.buffer("Input string"));

        // Capture the bodyHandler used by delegate and handle output value
        verify(delegate, times(1)).bodyHandler(delegateBodyHandlerCaptor.capture());
        final Buffer outputBuffer = Buffer.buffer("Output string");
        delegateBodyHandlerCaptor.getValue().handle(outputBuffer);

        // Capture the endHandler used by delegate to call context.afterPolicy()
        verify(delegate, times(1)).endHandler(delegateEndHandlerCaptor.capture());
        delegateEndHandlerCaptor.getValue().handle(null);

        cut.end();

        verify(bodyHandler, times(1)).handle(outputBuffer);
        verify(endHandler, times(1)).handle(null);
        verify(context, times(1)).beforePolicyExecution(eq(debugStep));
        verify(context, times(1)).afterPolicyExecution(eq(debugStep), inputBufferCaptor.capture(), outputBufferCaptor.capture());

        assertThat(inputBufferCaptor.getValue().toString()).isEqualTo("Input string");
        assertThat(outputBufferCaptor.getValue().toString()).isEqualTo("Output string");
    }
}
