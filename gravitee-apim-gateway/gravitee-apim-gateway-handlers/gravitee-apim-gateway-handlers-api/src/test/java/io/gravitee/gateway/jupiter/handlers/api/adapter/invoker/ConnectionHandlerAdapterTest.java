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
package io.gravitee.gateway.jupiter.handlers.api.adapter.invoker;

import static io.gravitee.common.http.HttpStatusCode.BAD_GATEWAY_502;
import static io.gravitee.common.http.HttpStatusCode.SERVICE_UNAVAILABLE_503;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.gravitee.gateway.jupiter.api.context.HttpResponse;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionFailureException;
import io.reactivex.CompletableEmitter;
import io.reactivex.subscribers.TestSubscriber;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockSettings;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ConnectionHandlerAdapterTest {

    protected static final String MOCK_EXCEPTION_MESSAGE = "Mock exception";
    protected static final HttpHeaders MOCK_HTTP_HEADERS = HttpHeaders.create().add("X-Test1", "X-Value1").add("X-Test2", "X-Value2");

    @Mock
    private HttpExecutionContext ctx;

    @Mock
    private CompletableEmitter nexEmitter;

    @Mock
    private ProxyConnection proxyConnection;

    @Mock
    private ProxyResponse proxyResponse;

    @Mock
    private HttpResponse response;

    @Captor
    private ArgumentCaptor<Handler<ProxyResponse>> handlerCaptor;

    private ConnectionHandlerAdapter cut;

    @BeforeEach
    public void init() {
        cut = new ConnectionHandlerAdapter(ctx, nexEmitter);
    }

    @Test
    void shouldSetResponseStatus() {
        final HttpHeaders responseHeaders = HttpHeaders.create();

        cut.handle(proxyConnection);

        verify(proxyConnection).responseHandler(handlerCaptor.capture());

        when(ctx.response()).thenReturn(response);
        when(response.headers()).thenReturn(responseHeaders);
        when(response.ended()).thenReturn(false);
        when(proxyResponse.connected()).thenReturn(true);
        when(proxyResponse.status()).thenReturn(200);
        when(proxyResponse.headers()).thenReturn(MOCK_HTTP_HEADERS);

        final Handler<ProxyResponse> proxyResponseHandler = handlerCaptor.getValue();
        proxyResponseHandler.handle(proxyResponse);

        verify(response).status(200);
        verify(nexEmitter).onComplete();
        assertTrue(responseHeaders.deeplyEquals(MOCK_HTTP_HEADERS));
    }

    @Test
    void shouldErrorWithInterruptionFailureExceptionWhenProxyResponseNotConnected() {
        cut.handle(proxyConnection);
        verify(proxyConnection).responseHandler(handlerCaptor.capture());

        when(proxyResponse.connected()).thenReturn(false);
        when(proxyResponse.status()).thenReturn(SERVICE_UNAVAILABLE_503);

        final Handler<ProxyResponse> proxyResponseHandler = handlerCaptor.getValue();
        proxyResponseHandler.handle(proxyResponse);

        final TestSubscriber<Buffer> obs = cut.getChunks().test();
        obs.assertComplete();
        obs.assertNoValues();

        final ArgumentCaptor<InterruptionFailureException> exceptionCaptor = ArgumentCaptor.forClass(InterruptionFailureException.class);

        verify(nexEmitter).tryOnError(exceptionCaptor.capture());

        final ExecutionFailure executionFailure = exceptionCaptor.getValue().getExecutionFailure();
        assertEquals(SERVICE_UNAVAILABLE_503, executionFailure.statusCode());
        verifyNoInteractions(response);
    }

    @Test
    void shouldErrorWithInterruptionFailureExceptionWhenProxyResponseIsAProcessorFailure() {
        final ProxyResponse proxyResponse = mock(ProxyResponse.class, withSettings().extraInterfaces(ProcessorFailure.class));
        final ProcessorFailure processorFailure = (ProcessorFailure) proxyResponse;

        when(processorFailure.statusCode()).thenReturn(SERVICE_UNAVAILABLE_503);
        when(processorFailure.key()).thenReturn("UNABLE_TO_CONNECT");
        when(processorFailure.message()).thenReturn("Unable to connect");
        when(processorFailure.contentType()).thenReturn("text/plain");
        final Map<String, Object> parameters = Map.of();
        when(processorFailure.parameters()).thenReturn(parameters);

        cut.handle(proxyConnection);
        verify(proxyConnection).responseHandler(handlerCaptor.capture());

        when(proxyResponse.connected()).thenReturn(false);

        final Handler<ProxyResponse> proxyResponseHandler = handlerCaptor.getValue();
        proxyResponseHandler.handle(proxyResponse);

        final TestSubscriber<Buffer> obs = cut.getChunks().test();
        obs.assertComplete();
        obs.assertNoValues();

        final ArgumentCaptor<InterruptionFailureException> exceptionCaptor = ArgumentCaptor.forClass(InterruptionFailureException.class);

        verify(nexEmitter).tryOnError(exceptionCaptor.capture());

        final ExecutionFailure executionFailure = exceptionCaptor.getValue().getExecutionFailure();
        assertEquals(SERVICE_UNAVAILABLE_503, executionFailure.statusCode());
        assertEquals("UNABLE_TO_CONNECT", executionFailure.key());
        assertEquals("Unable to connect", executionFailure.message());
        assertEquals("text/plain", executionFailure.contentType());
        assertEquals(parameters, executionFailure.parameters());
        verifyNoInteractions(response);
    }

    @Test
    void shouldNotSetResponseStatusWhenProxyResponseEnded() {
        final HttpHeaders responseHeaders = HttpHeaders.create();

        cut.handle(proxyConnection);

        verify(proxyConnection).responseHandler(handlerCaptor.capture());
        when(ctx.response()).thenReturn(response);
        when(proxyResponse.connected()).thenReturn(true);
        when(response.ended()).thenReturn(true);

        final Handler<ProxyResponse> proxyResponseHandler = handlerCaptor.getValue();
        proxyResponseHandler.handle(proxyResponse);

        verify(response, times(0)).status(anyInt());
        assertTrue(responseHeaders.isEmpty());
        verify(nexEmitter).onComplete();
    }

    @Test
    void shouldErrorWhenExceptionOccurs() {
        cut.handle(proxyConnection);

        verify(proxyConnection).responseHandler(handlerCaptor.capture());

        when(ctx.response()).thenReturn(response);
        when(proxyResponse.connected()).thenReturn(true);
        when(response.ended()).thenReturn(false);
        when(response.status(anyInt())).thenThrow(new RuntimeException(MOCK_EXCEPTION_MESSAGE));

        final Handler<ProxyResponse> proxyResponseHandler = handlerCaptor.getValue();
        proxyResponseHandler.handle(proxyResponse);

        verify(nexEmitter).tryOnError(argThat(t -> t.getMessage().equals(MOCK_EXCEPTION_MESSAGE)));
    }
}
