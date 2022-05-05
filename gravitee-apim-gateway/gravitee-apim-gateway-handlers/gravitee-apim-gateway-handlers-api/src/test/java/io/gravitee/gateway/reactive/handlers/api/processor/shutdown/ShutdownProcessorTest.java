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
package io.gravitee.gateway.reactive.handlers.api.processor.shutdown;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.handlers.api.processor.AbstractProcessorTest;
import io.gravitee.gateway.reactive.handlers.api.processor.forward.XForwardedPrefixProcessor;
import io.gravitee.node.api.Node;
import io.vertx.core.http.HttpHeaders;
import java.time.Month;
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
        shutdownProcessor = ShutdownProcessor.instance();
    }

    @Test
    public void shouldDoNothingWhenNodeStateIsStarted() {
        lenient().when(mockNode.lifecycleState()).thenReturn(Lifecycle.State.STARTED);
        shutdownProcessor.node(mockNode);
        shutdownProcessor.execute(ctx).test().assertResult();
        verifyNoInteractions(mockRequest);
        verifyNoInteractions(mockResponse);
    }

    @ParameterizedTest
    @EnumSource(value = Lifecycle.State.class, names = { "STARTED" }, mode = EnumSource.Mode.EXCLUDE)
    public void shouldAddConnectionHeaderWhenNodeStateIsNotStartedAndRequestHttp2(Lifecycle.State state) {
        lenient().when(mockNode.lifecycleState()).thenReturn(state);
        shutdownProcessor.node(mockNode);
        when(mockRequest.version()).thenReturn(HttpVersion.HTTP_2);
        shutdownProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).version();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeadersValues.CONNECTION_GO_AWAY);
    }

    @ParameterizedTest
    @EnumSource(value = Lifecycle.State.class, names = { "STARTED" }, mode = EnumSource.Mode.EXCLUDE)
    public void shouldAddConnectionHeaderWhenNodeStateIsNotStartedAndRequestHttp10(Lifecycle.State state) {
        lenient().when(mockNode.lifecycleState()).thenReturn(state);
        shutdownProcessor.node(mockNode);
        when(mockRequest.version()).thenReturn(HttpVersion.HTTP_1_0);
        shutdownProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).version();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeadersValues.CONNECTION_CLOSE);
    }

    @ParameterizedTest
    @EnumSource(value = Lifecycle.State.class, names = { "STARTED" }, mode = EnumSource.Mode.EXCLUDE)
    public void shouldAddConnectionHeaderWhenNodeStateIsNotStartedAndRequestHttp11(Lifecycle.State state) {
        lenient().when(mockNode.lifecycleState()).thenReturn(state);
        shutdownProcessor.node(mockNode);
        when(mockRequest.version()).thenReturn(HttpVersion.HTTP_1_1);
        shutdownProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).version();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeadersValues.CONNECTION_CLOSE);
    }
}
