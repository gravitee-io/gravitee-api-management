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
package io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.context.HttpResponse;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.LogHeadersCaptor;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class LogEndpointResponseTest {

    protected static final String BODY_CONTENT = "Body content";

    @Mock
    protected LoggingContext loggingContext;

    @Mock
    protected HttpResponse response;

    @Test
    void shouldLogMethodAndUriOnly() {
        when(response.status()).thenReturn(HttpStatusCode.OK_200);
        when(loggingContext.endpointResponseHeaders()).thenReturn(false);
        when(loggingContext.endpointResponsePayload()).thenReturn(false);

        final LogEndpointResponse logResponse = new LogEndpointResponse(loggingContext, response);
        logResponse.capture();

        assertThat(logResponse.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        assertNull(logResponse.getHeaders());
        assertNull(logResponse.getBody());
    }

    @Test
    void shouldLogHeaders() {
        final HttpHeaders headers = new VertxHttpHeaders(new HeadersMultiMap());

        headers.set("X-Test1", "Value1");
        headers.set("X-Test2", "Value2");
        headers.set("X-Test3", List.of("Value3-a", "Value3-b"));

        when(response.headers()).thenReturn(headers);
        when(response.status()).thenReturn(HttpStatusCode.OK_200);
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);
        when(loggingContext.endpointResponsePayload()).thenReturn(false);

        final LogEndpointResponse logResponse = new LogEndpointResponse(loggingContext, response);
        logResponse.capture();

        assertNotSame(headers, logResponse.getHeaders());
        assertThat(logResponse.getHeaders().deeplyEquals(headers)).isTrue();
        assertNull(logResponse.getBody());
    }

    @Test
    void shouldLogNoHeadersWhenUsingLogHeadersCaptorAndNoHeadersHasChanged() {
        final VertxHttpHeaders existingHeaders = new VertxHttpHeaders(new HeadersMultiMap());
        existingHeaders.set("X-Test1", "Value1");
        existingHeaders.set("X-Test2", "Value2");
        existingHeaders.set("X-Test3", List.of("Value3-a", "Value3-b"));

        final HttpHeaders headers = new LogHeadersCaptor(existingHeaders);

        when(response.headers()).thenReturn(headers);
        when(response.status()).thenReturn(HttpStatusCode.OK_200);
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);
        when(loggingContext.endpointResponsePayload()).thenReturn(false);

        final LogEndpointResponse logResponse = new LogEndpointResponse(loggingContext, response);
        logResponse.capture();

        assertNotSame(headers, logResponse.getHeaders());
        assertThat(logResponse.getHeaders().deeplyEquals(HttpHeaders.create())).isTrue();
        assertNull(logResponse.getBody());
    }

    @Test
    void shouldLogBody() {
        final Flowable<Buffer> body = Flowable.just(Buffer.buffer(BODY_CONTENT));
        final ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);

        when(response.chunks()).thenReturn(body);
        when(response.headers()).thenReturn(HttpHeaders.create());
        when(loggingContext.endpointResponseHeaders()).thenReturn(false);
        when(loggingContext.endpointResponsePayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(-1);
        when(loggingContext.isBodyLoggable()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(any())).thenReturn(true);

        final LogEndpointResponse logResponse = new LogEndpointResponse(loggingContext, response);
        logResponse.capture();
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> obs = chunksCaptor.getValue().test();
        obs.assertComplete();

        assertNull(logResponse.getHeaders());
        assertThat(logResponse.getBody()).isEqualTo(BODY_CONTENT);
    }

    @Test
    void shouldNotLogBodyWhenContentTypeExcluded() {
        final HttpHeaders headers = HttpHeaders.create();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");

        when(response.headers()).thenReturn(headers);
        when(loggingContext.endpointResponseHeaders()).thenReturn(false);
        when(loggingContext.endpointResponsePayload()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable("application/octet-stream")).thenReturn(false);

        final LogEndpointResponse logResponse = new LogEndpointResponse(loggingContext, response);
        logResponse.capture();
        verify(response, times(0)).chunks(any(Flowable.class));

        assertNull(logResponse.getHeaders());
        assertNull(logResponse.getBody());
    }

    @Test
    void shouldLogPartialBodyWhenMaxPayloadSizeIsDefined() {
        final Flowable<Buffer> body = Flowable.just(Buffer.buffer(BODY_CONTENT));
        final int maxPayloadSize = 5;

        final ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        when(response.chunks()).thenReturn(body);
        when(response.headers()).thenReturn(HttpHeaders.create());
        when(loggingContext.endpointResponseHeaders()).thenReturn(false);
        when(loggingContext.endpointResponsePayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(maxPayloadSize);
        when(loggingContext.isBodyLoggable()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(any())).thenReturn(true);

        final LogEndpointResponse logResponse = new LogEndpointResponse(loggingContext, response);
        logResponse.capture();
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> obs = chunksCaptor.getValue().test();
        obs.assertComplete();

        assertNull(logResponse.getHeaders());
        assertThat(logResponse.getBody()).isEqualTo(BODY_CONTENT.substring(0, maxPayloadSize));
    }

    @Test
    void shouldLogBodyNotCapturedWhenBodyIsNotLoggable() {
        when(response.headers()).thenReturn(HttpHeaders.create());
        when(loggingContext.endpointResponseHeaders()).thenReturn(false);
        when(loggingContext.endpointResponsePayload()).thenReturn(true);
        when(loggingContext.isBodyLoggable()).thenReturn(false);
        when(loggingContext.isContentTypeLoggable(any())).thenReturn(true);

        final LogEndpointResponse logResponse = new LogEndpointResponse(loggingContext, response);
        logResponse.capture();
        verify(response, times(0)).chunks(any(Flowable.class));

        assertNull(logResponse.getHeaders());
        assertThat(logResponse.getBody()).isEqualTo("BODY NOT CAPTURED");
    }
}
