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
package io.gravitee.gateway.handlers.api.processor.shutdown;

import static org.mockito.Mockito.*;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.node.api.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ShutdownProcessorTest {

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private HttpHeaders headers;

    @Mock
    private ExecutionContext context;

    @Mock
    private Node node;

    private ShutdownProcessor cut;

    @Before
    public void before() {
        lenient().when(context.request()).thenReturn(request);
        lenient().when(context.response()).thenReturn(response);
        lenient().when(response.headers()).thenReturn(headers);

        cut = new ShutdownProcessor(node);
    }

    @Test
    public void shouldNotShutdownWhenNodeIsStarted() {
        when(node.lifecycleState()).thenReturn(Lifecycle.State.STARTED);

        cut.handler(
            context -> {
                verifyNoInteractions(request);
                verifyNoInteractions(response);
            }
        );

        cut.handle(context);
    }

    @Test
    public void shouldShutdown_Http1_0() {
        when(node.lifecycleState()).thenReturn(Lifecycle.State.STOPPING);
        when(request.version()).thenReturn(HttpVersion.HTTP_1_0);

        cut.handler(context -> verify(headers).set(io.vertx.core.http.HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE));
        cut.handle(context);
    }

    @Test
    public void shouldShutdown_Http1_1() {
        when(node.lifecycleState()).thenReturn(Lifecycle.State.STOPPING);
        when(request.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut.handler(context -> verify(headers).set(io.vertx.core.http.HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE));
        cut.handle(context);
    }

    @Test
    public void shouldShutdown_Http2() {
        when(node.lifecycleState()).thenReturn(Lifecycle.State.STOPPING);
        when(request.version()).thenReturn(HttpVersion.HTTP_2);

        cut.handler(context -> verify(headers).set(io.vertx.core.http.HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_GO_AWAY));
        cut.handle(context);
    }
}
