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
package io.gravitee.gateway.jupiter.reactor.processor.shutdown;

import static org.mockito.Mockito.*;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.jupiter.reactor.processor.AbstractProcessorTest;
import io.gravitee.node.api.Node;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class ShutdownProcessorTest extends AbstractProcessorTest {

    private ShutdownProcessor shutdownProcessor;

    @Mock
    private Node mockNode;

    @BeforeEach
    public void beforeEach() {
        Mockito.clearInvocations(mockNode);
        shutdownProcessor = new ShutdownProcessor(mockNode);
    }

    @Test
    public void shouldDoNothingWhenNodeStateIsStarted() {
        lenient().when(mockNode.lifecycleState()).thenReturn(Lifecycle.State.STARTED);
        shutdownProcessor.execute(ctx).test().assertResult();
        Mockito.verifyNoInteractions(mockRequest);
        Mockito.verifyNoInteractions(mockResponse);
    }

    @ParameterizedTest
    @EnumSource(value = Lifecycle.State.class, names = { "STARTED" }, mode = EnumSource.Mode.EXCLUDE)
    public void shouldAddConnectionHeaderWhenNodeStateIsNotStartedAndRequestHttp2(Lifecycle.State state) {
        lenient().when(mockNode.lifecycleState()).thenReturn(state);
        when(mockRequest.version()).thenReturn(HttpVersion.HTTP_2);
        shutdownProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).version();
        AssertionsForClassTypes
            .assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION))
            .isEqualTo(HttpHeadersValues.CONNECTION_GO_AWAY);
    }

    @ParameterizedTest
    @EnumSource(value = Lifecycle.State.class, names = { "STARTED" }, mode = EnumSource.Mode.EXCLUDE)
    public void shouldAddConnectionHeaderWhenNodeStateIsNotStartedAndRequestHttp10(Lifecycle.State state) {
        lenient().when(mockNode.lifecycleState()).thenReturn(state);
        when(mockRequest.version()).thenReturn(HttpVersion.HTTP_1_0);
        shutdownProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).version();
        AssertionsForClassTypes
            .assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION))
            .isEqualTo(HttpHeadersValues.CONNECTION_CLOSE);
    }

    @ParameterizedTest
    @EnumSource(value = Lifecycle.State.class, names = { "STARTED" }, mode = EnumSource.Mode.EXCLUDE)
    public void shouldAddConnectionHeaderWhenNodeStateIsNotStartedAndRequestHttp11(Lifecycle.State state) {
        lenient().when(mockNode.lifecycleState()).thenReturn(state);
        when(mockRequest.version()).thenReturn(HttpVersion.HTTP_1_1);
        shutdownProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).version();
        AssertionsForClassTypes
            .assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION))
            .isEqualTo(HttpHeadersValues.CONNECTION_CLOSE);
    }
}
