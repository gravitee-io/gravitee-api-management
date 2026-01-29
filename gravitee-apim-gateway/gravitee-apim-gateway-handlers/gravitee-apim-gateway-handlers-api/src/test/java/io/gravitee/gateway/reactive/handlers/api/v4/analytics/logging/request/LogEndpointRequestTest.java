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
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.HttpRequestInternal;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
class LogEndpointRequestTest {

    protected static final String URI = "https://test.gravitee.io";
    protected static final String BODY_CONTENT = "Body content";

    @Mock
    protected LoggingContext loggingContext;

    @Mock
    protected HttpRequestInternal request;

    @Mock
    private HttpExecutionContextInternal ctx;

    @Captor
    private ArgumentCaptor<Flowable<Buffer>> chunksCaptor;

    @Captor
    private ArgumentCaptor<FlowableTransformer<Buffer, Buffer>> buffersInterceptorCaptor;

    private LogEndpointRequest cut;

    @BeforeEach
    void init() {
        final Metrics metrics = new Metrics();
        metrics.setEndpoint(URI);
        when(request.method()).thenReturn(HttpMethod.POST);
        lenient().doNothing().when(request).registerBuffersInterceptor(buffersInterceptorCaptor.capture());
        when(ctx.request()).thenReturn(request);
        when(ctx.metrics()).thenReturn(metrics);

        cut = new LogEndpointRequest(loggingContext, request);
    }

    @Test
    void should_log_method_and_uri_only() {
        when(loggingContext.endpointRequestHeaders()).thenReturn(false);
        when(loggingContext.endpointRequestPayload()).thenReturn(false);

        cut.setupCapture(ctx);
        triggerRequestToBackend(null, false);

        assertThat(cut.getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(cut.getUri()).isEqualTo(URI);
        assertNull(cut.getHeaders());
        assertNull(cut.getBody());
    }

    @Test
    void should_log_headers() {
        final HttpHeaders existingHeaders = new VertxHttpHeaders(HeadersMultiMap.httpHeaders());
        existingHeaders.set("X-Test1", "Value1");
        existingHeaders.set("X-Test2", "Value2");
        existingHeaders.set("X-Test3", List.of("Value3-a", "Value3-b"));

        initializeHeaders(existingHeaders);
        when(loggingContext.endpointRequestHeaders()).thenReturn(true);
        when(loggingContext.endpointRequestPayload()).thenReturn(false);

        cut.setupCapture(ctx);
        triggerRequestToBackend(null, false);

        assertNotSame(existingHeaders, cut.getHeaders());
        assertThat(cut.getHeaders().deeplyEquals(existingHeaders)).isTrue();
        assertNull(cut.getBody());
    }

    @Test
    void should_log_headers_including_all_headers_added_by_the_endpoint() {
        final HttpHeaders backendHeaders = HttpHeaders.create().set("X-To-Backend1", "Backend1").set("X-To-Backend2", "Backend2");
        final VertxHttpHeaders existingHeaders = new VertxHttpHeaders(HeadersMultiMap.httpHeaders());
        existingHeaders.set("X-Test1", "Value1");
        existingHeaders.set("X-Test2", "Value2");
        existingHeaders.set("X-Test3", List.of("Value3-a", "Value3-b"));

        initializeHeaders(existingHeaders);
        when(loggingContext.endpointRequestHeaders()).thenReturn(true);
        when(loggingContext.endpointRequestPayload()).thenReturn(false);

        cut.setupCapture(ctx);
        triggerRequestToBackend(backendHeaders, false);

        final HttpHeaders allHeaders = HttpHeaders.create(existingHeaders);
        backendHeaders.forEach(e -> allHeaders.set(e.getKey(), e.getValue()));

        assertNotSame(existingHeaders, cut.getHeaders());
        assertThat(cut.getHeaders().deeplyEquals(allHeaders)).isTrue();
        assertNull(cut.getBody());
    }

    @Test
    void should_log_body_only() {
        initializeHeaders(HttpHeaders.create());
        when(loggingContext.endpointRequestHeaders()).thenReturn(false);
        when(loggingContext.endpointRequestPayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(-1);
        when(loggingContext.isBodyLoggable()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(any(), any())).thenReturn(true);

        cut.setupCapture(ctx);
        triggerRequestToBackend(HttpHeaders.create().set("X-To-Backend1", "Backend1"), true);

        assertNull(cut.getHeaders());
        assertThat(cut.getBody()).isEqualTo(BODY_CONTENT);
    }

    @Test
    void should_log_body_and_headers() {
        final HttpHeaders backendHeaders = HttpHeaders.create().set("X-To-Backend1", "Backend1");

        initializeHeaders(HttpHeaders.create());
        when(loggingContext.endpointRequestHeaders()).thenReturn(true);
        when(loggingContext.endpointRequestPayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(-1);
        when(loggingContext.isBodyLoggable()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(any(), any())).thenReturn(true);

        cut.setupCapture(ctx);
        triggerRequestToBackend(backendHeaders, true);

        assertThat(cut.getHeaders().deeplyEquals(backendHeaders)).isTrue();
        assertThat(cut.getBody()).isEqualTo(BODY_CONTENT);
    }

    @Test
    void should_not_log_body_when_content_type_is_excluded() {
        final HttpHeaders initialHeaders = HttpHeaders.create();
        initialHeaders.set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");

        initializeHeaders(initialHeaders);
        when(loggingContext.endpointRequestHeaders()).thenReturn(false);
        when(loggingContext.endpointRequestPayload()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(eq("application/octet-stream"), any())).thenReturn(false);

        cut.setupCapture(ctx);
        triggerRequestToBackend(HttpHeaders.create(), false);

        assertNull(cut.getHeaders());
        assertNull(cut.getBody());
    }

    @Test
    void should_log_partial_pody_when_max_payload_size_is_defined() {
        final int maxPayloadSize = 5;

        initializeHeaders(HttpHeaders.create());
        when(loggingContext.endpointRequestHeaders()).thenReturn(false);
        when(loggingContext.endpointRequestPayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(maxPayloadSize);
        when(loggingContext.isBodyLoggable()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(any(), any())).thenReturn(true);

        cut.setupCapture(ctx);
        triggerRequestToBackend(HttpHeaders.create(), true);

        assertNull(cut.getHeaders());
        assertThat(cut.getBody()).isEqualTo(BODY_CONTENT.substring(0, maxPayloadSize));
    }

    @Test
    void should_log_body_not_captured_when_body_is_not_loggable() {
        initializeHeaders(HttpHeaders.create());
        when(loggingContext.endpointRequestHeaders()).thenReturn(false);
        when(loggingContext.endpointRequestPayload()).thenReturn(true);
        when(loggingContext.isBodyLoggable()).thenReturn(false);
        when(loggingContext.isContentTypeLoggable(any(), any())).thenReturn(true);

        cut.setupCapture(ctx);
        triggerRequestToBackend(HttpHeaders.create(), true);

        assertNull(cut.getHeaders());
        assertThat(cut.getBody()).isEqualTo("BODY NOT CAPTURED");
    }

    private void triggerRequestToBackend(HttpHeaders backendHeaders, boolean expectCaptureBody) {
        if (backendHeaders != null) {
            backendHeaders.forEach(e -> request.headers().set(e.getKey(), e.getValue()));
        }

        if (expectCaptureBody) {
            Flowable.fromPublisher(
                buffersInterceptorCaptor.getValue().apply(Flowable.just(Buffer.buffer(LogEndpointRequestTest.BODY_CONTENT)))
            )
                .test()
                .assertComplete();
        }
        cut.finalizeCapture(ctx);
    }

    private void initializeHeaders(HttpHeaders initialHeaders) {
        when(request.headers()).thenReturn(initialHeaders);
    }
}
