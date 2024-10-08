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
package io.gravitee.gateway.reactive.handlers.api.adapter.invoker;

import static io.gravitee.common.http.HttpStatusCode.SERVICE_UNAVAILABLE_503;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.HttpResponse;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.reactivex.rxjava3.core.CompletableEmitter;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ConnectionHandlerAdapterTest {

    protected static final String MOCK_EXCEPTION_MESSAGE = "Mock exception";
    protected static final HttpHeaders MOCK_HTTP_HEADERS = HttpHeaders.create().add("X-Test1", "X-Value1").add("X-Test2", "X-Value2");
    protected static final HttpHeaders MOCK_HTTP_TRAILERS = HttpHeaders.create().add("X-Test3", "X-Value3").add("X-Test4", "X-Value4");

    @Mock
    private HttpPlainExecutionContext ctx;

    @Mock
    private CompletableEmitter nextEmitter;

    @Mock
    private ProxyConnection proxyConnection;

    @Mock
    private ProxyResponse proxyResponse;

    @Mock
    private HttpResponse response;

    @Mock
    private FlowableProxyResponse flowableProxyResponse;

    @Captor
    private ArgumentCaptor<Handler<ProxyResponse>> handlerCaptor;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    private ConnectionHandlerAdapter cut;

    @BeforeEach
    public void init() {
        cut = new ConnectionHandlerAdapter(ctx, nextEmitter);
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
        verify(nextEmitter).onComplete();
        assertTrue(responseHeaders.deeplyEquals(MOCK_HTTP_HEADERS));
    }

    @Test
    public void shouldSetResponseTrailers() {
        ReflectionTestUtils.setField(cut, "chunks", flowableProxyResponse);

        final HttpHeaders responseTrailers = HttpHeaders.create();

        cut.handle(proxyConnection);

        verify(proxyConnection).responseHandler(handlerCaptor.capture());

        when(ctx.response()).thenReturn(response);
        when(response.trailers()).thenReturn(responseTrailers);
        when(response.headers()).thenReturn(HttpHeaders.create());
        when(response.ended()).thenReturn(false);
        when(proxyResponse.connected()).thenReturn(true);
        when(proxyResponse.headers()).thenReturn(MOCK_HTTP_HEADERS);
        when(proxyResponse.trailers()).thenReturn(MOCK_HTTP_TRAILERS);

        when(flowableProxyResponse.initialize(ctx, proxyConnection, proxyResponse)).thenReturn(flowableProxyResponse);

        final Handler<ProxyResponse> proxyResponseHandler = handlerCaptor.getValue();
        proxyResponseHandler.handle(proxyResponse);

        verify(flowableProxyResponse).doOnComplete(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        assertTrue(responseTrailers.deeplyEquals(MOCK_HTTP_TRAILERS));
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

        verify(nextEmitter).tryOnError(exceptionCaptor.capture());

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

        verify(nextEmitter).tryOnError(exceptionCaptor.capture());

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
        verify(nextEmitter).onComplete();
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

        verify(nextEmitter).tryOnError(argThat(t -> t.getMessage().equals(MOCK_EXCEPTION_MESSAGE)));
    }
}
