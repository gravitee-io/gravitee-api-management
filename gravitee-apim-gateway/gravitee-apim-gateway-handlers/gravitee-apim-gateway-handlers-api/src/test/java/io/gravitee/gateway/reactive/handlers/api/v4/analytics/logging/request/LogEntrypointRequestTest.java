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
package io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.HttpRequestInternal;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class LogEntrypointRequestTest {

    protected static final String URI = "/test";
    protected static final String BODY_CONTENT = "Body content";

    @Mock
    protected LoggingContext loggingContext;

    @Mock
    protected HttpRequestInternal request;

    @Mock
    private HttpExecutionContextInternal ctx;

    @Captor
    private ArgumentCaptor<Flowable<Buffer>> chunksCaptor;

    @Test
    void should_log_method_and_uri_only() {
        when(request.method()).thenReturn(HttpMethod.POST);
        when(request.uri()).thenReturn(URI);

        when(loggingContext.entrypointRequestHeaders()).thenReturn(false);
        when(loggingContext.entrypointRequestPayload()).thenReturn(false);

        final var logRequest = new LogEntrypointRequest(loggingContext, request);
        logRequest.capture(ctx);

        assertThat(logRequest.getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(logRequest.getUri()).isEqualTo(URI);
        assertNull(logRequest.getHeaders());
        assertNull(logRequest.getBody());
    }

    @Test
    void should_log_headers() {
        final HttpHeaders headers = new VertxHttpHeaders(HeadersMultiMap.httpHeaders());

        headers.set("X-Test1", "Value1");
        headers.set("X-Test2", "Value2");
        headers.set("X-Test3", List.of("Value3-a", "Value3-b"));

        when(request.headers()).thenReturn(headers);
        when(loggingContext.entrypointRequestHeaders()).thenReturn(true);
        when(loggingContext.entrypointRequestPayload()).thenReturn(false);

        final var logRequest = new LogEntrypointRequest(loggingContext, request);
        logRequest.capture(ctx);

        assertNotSame(headers, logRequest.getHeaders());
        assertThat(logRequest.getHeaders().deeplyEquals(headers)).isTrue();
        assertNull(logRequest.getBody());
    }

    @Test
    void should_log_body() {
        final Flowable<Buffer> body = Flowable.just(Buffer.buffer(BODY_CONTENT));

        when(request.chunks()).thenReturn(body);
        when(request.headers()).thenReturn(HttpHeaders.create());
        when(loggingContext.entrypointRequestHeaders()).thenReturn(false);
        when(loggingContext.entrypointRequestPayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(-1);
        when(loggingContext.isBodyLoggable()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(any(), any())).thenReturn(true);

        final var logRequest = new LogEntrypointRequest(loggingContext, request);
        logRequest.capture(ctx);
        verify(request).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> obs = chunksCaptor.getValue().test();
        obs.assertComplete();

        assertNull(logRequest.getHeaders());
        assertThat(logRequest.getBody()).isEqualTo(BODY_CONTENT);
    }

    @Test
    void should_not_log_body_when_content_type_excluded() {
        final HttpHeaders headers = HttpHeaders.create();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        when(request.headers()).thenReturn(headers);
        when(loggingContext.entrypointRequestHeaders()).thenReturn(false);
        when(loggingContext.entrypointRequestPayload()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(eq("application/octet-stream"), any())).thenReturn(false);

        final var logRequest = new LogEntrypointRequest(loggingContext, request);
        logRequest.capture(ctx);
        verify(request, times(0)).chunks(any(Flowable.class));

        assertNull(logRequest.getHeaders());
        assertNull(logRequest.getBody());
    }

    @Test
    void should_log_partial_body_when_max_payload_size_is_defined() {
        final Flowable<Buffer> body = Flowable.just(Buffer.buffer(BODY_CONTENT));
        final int maxPayloadSize = 5;

        when(request.chunks()).thenReturn(body);
        when(request.headers()).thenReturn(HttpHeaders.create());
        when(loggingContext.entrypointRequestHeaders()).thenReturn(false);
        when(loggingContext.entrypointRequestPayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(maxPayloadSize);
        when(loggingContext.isBodyLoggable()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(any(), any())).thenReturn(true);

        final var logRequest = new LogEntrypointRequest(loggingContext, request);
        logRequest.capture(ctx);
        verify(request).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> obs = chunksCaptor.getValue().test();
        obs.assertComplete();

        assertNull(logRequest.getHeaders());
        assertThat(logRequest.getBody()).isEqualTo(BODY_CONTENT.substring(0, maxPayloadSize));
    }

    @Test
    void should_log_body_not_capture_when_body_not_loggable() {
        when(request.headers()).thenReturn(HttpHeaders.create());
        when(loggingContext.entrypointRequestHeaders()).thenReturn(false);
        when(loggingContext.entrypointRequestPayload()).thenReturn(true);
        when(loggingContext.isBodyLoggable()).thenReturn(false);
        when(loggingContext.isContentTypeLoggable(any(), any())).thenReturn(true);

        final var logRequest = new LogEntrypointRequest(loggingContext, request);
        logRequest.capture(ctx);
        verify(request, times(0)).chunks(any(Flowable.class));

        assertNull(logRequest.getHeaders());
        assertThat(logRequest.getBody()).isEqualTo("BODY NOT CAPTURED");
    }
}
