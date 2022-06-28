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
package io.gravitee.gateway.jupiter.handlers.api.logging.request;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.core.logging.LoggingContext;
import io.gravitee.gateway.core.logging.utils.LoggingUtils;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class LogProxyRequestTest {

    protected static final String URI = "https://test.gravitee.io";
    protected static final String BODY_CONTENT = "Body content";

    @Mock
    protected LoggingContext loggingContext;

    @Mock
    protected Request request;

    @Mock
    private Metrics metrics;

    @BeforeEach
    void init() {
        when(request.metrics()).thenReturn(metrics);
        when(metrics.getEndpoint()).thenReturn(URI);
        ReflectionTestUtils.setField(LoggingUtils.class, "EXCLUDED_CONTENT_TYPES_PATTERN", null);
    }

    @Test
    void shouldLogMethodAndUriOnly() {
        final ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);

        when(request.method()).thenReturn(HttpMethod.POST);
        when(loggingContext.proxyRequestHeaders()).thenReturn(false);
        when(loggingContext.proxyRequestPayload()).thenReturn(false);
        when(request.chunks()).thenReturn(Flowable.empty()).thenAnswer(i -> chunksCaptor.getValue());

        final LogProxyRequest logRequest = new LogProxyRequest(loggingContext, request);
        verify(request).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> obs = chunksCaptor.getValue().test();
        obs.assertComplete();

        assertEquals(HttpMethod.POST, logRequest.getMethod());
        assertEquals(URI, logRequest.getUri());
        assertNull(logRequest.getHeaders());
        assertNull(logRequest.getBody());
    }

    @Test
    void shouldLogHeaders() {
        final ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        final HttpHeaders headers = new VertxHttpHeaders(new HeadersMultiMap());

        headers.set("X-Test1", "Value1");
        headers.set("X-Test2", "Value2");
        headers.set("X-Test3", List.of("Value3-a", "Value3-b"));

        when(request.headers()).thenReturn(headers);
        when(request.method()).thenReturn(HttpMethod.POST);
        when(loggingContext.proxyRequestHeaders()).thenReturn(true);
        when(loggingContext.proxyRequestPayload()).thenReturn(false);
        when(request.chunks()).thenReturn(Flowable.empty()).thenAnswer(i -> chunksCaptor.getValue());

        final LogProxyRequest logRequest = new LogProxyRequest(loggingContext, request);
        verify(request).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> obs = chunksCaptor.getValue().test();
        obs.assertComplete();

        assertNotSame(headers, logRequest.getHeaders());
        assertTrue(logRequest.getHeaders().deeplyEquals(headers));
        assertNull(logRequest.getBody());
    }

    @Test
    void shouldLogBody() {
        final Flowable<Buffer> body = Flowable.just(Buffer.buffer(BODY_CONTENT));
        final ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);

        when(request.chunks()).thenReturn(body).thenAnswer(i -> chunksCaptor.getValue());
        doNothing().when(request).chunks(chunksCaptor.capture());
        when(request.headers()).thenReturn(HttpHeaders.create());
        when(loggingContext.proxyRequestHeaders()).thenReturn(false);
        when(loggingContext.proxyRequestPayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(-1);

        final LogProxyRequest logRequest = new LogProxyRequest(loggingContext, request);
        verify(request, times(2)).chunks(any(Flowable.class));

        final TestSubscriber<Buffer> obs = chunksCaptor.getValue().test();
        obs.assertComplete();

        assertNull(logRequest.getHeaders());
        assertEquals(BODY_CONTENT, logRequest.getBody());
    }

    @Test
    void shouldNotLogBodyWhenContentTypeExcluded() {
        final Flowable<Buffer> body = Flowable.just(Buffer.buffer(BODY_CONTENT));
        final ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        final HttpHeaders headers = HttpHeaders.create();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");

        when(request.chunks()).thenReturn(body);
        doNothing().when(request).chunks(chunksCaptor.capture());
        when(request.headers()).thenReturn(headers);
        when(loggingContext.proxyRequestHeaders()).thenReturn(false);
        when(loggingContext.proxyRequestPayload()).thenReturn(true);
        when(loggingContext.getExcludedResponseTypes()).thenReturn("application/octet-stream");

        final LogProxyRequest logRequest = new LogProxyRequest(loggingContext, request);
        verify(request).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> obs = chunksCaptor.getValue().test();
        obs.assertComplete();

        assertNull(logRequest.getHeaders());
        assertNull(logRequest.getBody());
    }

    @Test
    void shouldLogPartialBodyWhenMaxPayloadSizeIsDefined() {
        final Flowable<Buffer> body = Flowable.just(Buffer.buffer(BODY_CONTENT));
        final int maxPayloadSize = 5;

        final ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        when(request.chunks()).thenReturn(body).thenAnswer(i -> chunksCaptor.getValue());
        doNothing().when(request).chunks(chunksCaptor.capture());
        when(request.headers()).thenReturn(HttpHeaders.create());
        when(loggingContext.proxyRequestHeaders()).thenReturn(false);
        when(loggingContext.proxyRequestPayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(maxPayloadSize);

        final LogProxyRequest logRequest = new LogProxyRequest(loggingContext, request);
        verify(request, times(2)).chunks(any(Flowable.class));

        final TestSubscriber<Buffer> obs = chunksCaptor.getAllValues().get(chunksCaptor.getAllValues().size() - 1).test();
        obs.assertComplete();

        assertNull(logRequest.getHeaders());
        assertEquals(BODY_CONTENT.substring(0, maxPayloadSize), logRequest.getBody());
    }
}
