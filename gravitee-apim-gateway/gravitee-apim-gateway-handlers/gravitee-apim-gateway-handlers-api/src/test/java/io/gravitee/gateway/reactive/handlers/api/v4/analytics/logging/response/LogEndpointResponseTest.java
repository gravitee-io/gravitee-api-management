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

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.HttpResponseInternal;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
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
class LogEndpointResponseTest {

    protected static final String BODY_CONTENT = "Body content";

    @Mock
    protected LoggingContext loggingContext;

    @Mock
    protected HttpResponseInternal response;

    @Mock
    private HttpExecutionContextInternal ctx;

    @Captor
    private ArgumentCaptor<Flowable<Buffer>> chunksCaptor;

    @Captor
    private ArgumentCaptor<FlowableTransformer<Buffer, Buffer>> buffersInterceptorCaptor;

    private LogEndpointResponse cut;

    @BeforeEach
    void init() {
        doNothing().when(response).registerBuffersInterceptor(buffersInterceptorCaptor.capture());
        cut = new LogEndpointResponse(loggingContext, response);
    }

    @Test
    void should_log_method_and_uri_only() {
        initializeHeaders(HttpHeaders.create());
        when(response.status()).thenReturn(OK_200);
        when(loggingContext.endpointResponseHeaders()).thenReturn(false);
        when(loggingContext.endpointResponsePayload()).thenReturn(false);

        cut.setupCapture(ctx);
        triggerResponseFromBackend(null);

        assertThat(cut.getStatus()).isEqualTo(OK_200);
        assertNull(cut.getHeaders());
        assertNull(cut.getBody());
    }

    @Test
    void should_log_headers() {
        final HttpHeaders backendHeaders = new VertxHttpHeaders(new HeadersMultiMap());
        backendHeaders.set("X-Test1", "Value1");
        backendHeaders.set("X-Test2", "Value2");
        backendHeaders.set("X-Test3", List.of("Value3-a", "Value3-b"));

        initializeHeaders(HttpHeaders.create());
        when(response.status()).thenReturn(OK_200);
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);
        when(loggingContext.endpointResponsePayload()).thenReturn(false);

        cut.setupCapture(ctx);
        triggerResponseFromBackend(backendHeaders);

        assertNotSame(backendHeaders, cut.getHeaders());
        assertThat(cut.getHeaders().deeplyEquals(backendHeaders)).isTrue();
        assertNull(cut.getBody());
    }

    @Test
    void should_log_no_headers_when_using_log_headers_captor_and_no_headers_has_been_return_by_the_backend() {
        final VertxHttpHeaders existingHeaders = new VertxHttpHeaders(new HeadersMultiMap());
        existingHeaders.set("X-Test1", "Value1");
        existingHeaders.set("X-Test2", "Value2");
        existingHeaders.set("X-Test3", List.of("Value3-a", "Value3-b"));

        initializeHeaders(existingHeaders);
        when(response.status()).thenReturn(OK_200);
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);
        when(loggingContext.endpointResponsePayload()).thenReturn(false);

        cut.setupCapture(ctx);
        triggerResponseFromBackend(null);

        assertNotSame(existingHeaders, cut.getHeaders());
        assertThat(cut.getHeaders().deeplyEquals(HttpHeaders.create())).isTrue();
        assertNull(cut.getBody());
    }

    @Test
    void should_log_only_headers_returned_by_the_backend_and_ignore_headers_set_by_the_gateway() {
        final HttpHeaders backendHeaders = HttpHeaders.create().set("X-From-Backend1", "Backend1").set("X-From-Backend2", "Backend2");
        final VertxHttpHeaders existingHeaders = new VertxHttpHeaders(new HeadersMultiMap());
        existingHeaders.set("X-Test1", "Value1");
        existingHeaders.set("X-Test2", "Value2");
        existingHeaders.set("X-Test3", List.of("Value3-a", "Value3-b"));

        initializeHeaders(existingHeaders);
        when(response.status()).thenReturn(OK_200);
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);
        when(loggingContext.endpointResponsePayload()).thenReturn(false);

        cut.setupCapture(ctx);
        triggerResponseFromBackend(backendHeaders);

        assertNotSame(existingHeaders, cut.getHeaders());
        assertThat(cut.getHeaders().deeplyEquals(backendHeaders)).isTrue();
        assertNull(cut.getBody());
    }

    @Test
    void should_log_body_only() {
        initializeHeaders(HttpHeaders.create());
        when(loggingContext.endpointResponseHeaders()).thenReturn(false);
        when(loggingContext.endpointResponsePayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(-1);
        when(loggingContext.isBodyLoggable()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(any(), any())).thenReturn(true);

        cut.setupCapture(ctx);
        triggerResponseFromBackend(HttpHeaders.create().set("X-From-Backend1", "Backend1"));

        assertNull(cut.getHeaders());
        assertThat(cut.getBody()).isEqualTo(BODY_CONTENT);
    }

    @Test
    void should_log_body_and_headers() {
        final HttpHeaders backendHeaders = HttpHeaders.create().set("X-From-Backend1", "Backend1");

        initializeHeaders(HttpHeaders.create());
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);
        when(loggingContext.endpointResponsePayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(-1);
        when(loggingContext.isBodyLoggable()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(any(), any())).thenReturn(true);

        cut.setupCapture(ctx);
        triggerResponseFromBackend(backendHeaders);

        assertThat(cut.getHeaders().deeplyEquals(backendHeaders)).isTrue();
        assertThat(cut.getBody()).isEqualTo(BODY_CONTENT);
    }

    @Test
    void should_not_log_body_when_content_type_is_excluded() {
        final HttpHeaders backendHeaders = HttpHeaders.create();
        backendHeaders.set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");

        initializeHeaders(HttpHeaders.create());
        when(loggingContext.endpointResponseHeaders()).thenReturn(false);
        when(loggingContext.endpointResponsePayload()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(eq("application/octet-stream"), any())).thenReturn(false);

        cut.setupCapture(ctx);
        triggerResponseFromBackend(backendHeaders);

        assertNull(cut.getHeaders());
        assertNull(cut.getBody());
    }

    @Test
    void should_log_partial_body_when_max_payload_size_is_defined() {
        final int maxPayloadSize = 5;

        initializeHeaders(HttpHeaders.create());
        when(loggingContext.endpointResponseHeaders()).thenReturn(false);
        when(loggingContext.endpointResponsePayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(maxPayloadSize);
        when(loggingContext.isBodyLoggable()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(any(), any())).thenReturn(true);

        cut.setupCapture(ctx);
        triggerResponseFromBackend(HttpHeaders.create());

        assertNull(cut.getHeaders());
        assertThat(cut.getBody()).isEqualTo(BODY_CONTENT.substring(0, maxPayloadSize));
    }

    @Test
    void should_log_partial_body_when_error_occurs_during_body_transfer() {
        initializeHeaders(HttpHeaders.create());
        when(loggingContext.endpointResponseHeaders()).thenReturn(false);
        when(loggingContext.endpointResponsePayload()).thenReturn(true);
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(-1);
        when(loggingContext.isBodyLoggable()).thenReturn(true);
        when(loggingContext.isContentTypeLoggable(any(), any())).thenReturn(true);

        cut.setupCapture(ctx);
        triggerResponseFromBackendWithError();

        assertNull(cut.getHeaders());
        // The last character is missing due to the error before completion.
        assertThat(cut.getBody()).isEqualTo(BODY_CONTENT.substring(0, BODY_CONTENT.length() - 1));
    }

    @Test
    void should_log_body_not_captured_when_body_is_not_loggable() {
        initializeHeaders(HttpHeaders.create());
        when(loggingContext.endpointResponseHeaders()).thenReturn(false);
        when(loggingContext.endpointResponsePayload()).thenReturn(true);
        when(loggingContext.isBodyLoggable()).thenReturn(false);
        when(loggingContext.isContentTypeLoggable(any(), any())).thenReturn(true);

        cut.setupCapture(ctx);
        triggerResponseFromBackend(HttpHeaders.create());

        assertNull(cut.getHeaders());
        assertThat(cut.getBody()).isEqualTo("BODY NOT CAPTURED");
    }

    private void triggerResponseFromBackend(HttpHeaders backendHeaders) {
        if (backendHeaders != null) {
            backendHeaders.forEach(e -> response.headers().set(e.getKey(), e.getValue()));
        }

        Flowable.fromPublisher(
            buffersInterceptorCaptor.getValue().apply(Flowable.just(Buffer.buffer(LogEndpointResponseTest.BODY_CONTENT)))
        )
            .test()
            .assertComplete();

        cut.finalizeCapture(ctx);
    }

    private void triggerResponseFromBackendWithError() {
        List<Buffer> buffers = BODY_CONTENT.chars()
            .mapToObj(i -> Buffer.buffer(String.valueOf((char) i)))
            .collect(Collectors.toCollection(ArrayList::new));

        // Remove last buffer to simulate error before complete.
        buffers.removeLast();

        Flowable.fromPublisher(
            buffersInterceptorCaptor
                .getValue()
                .apply(Flowable.fromIterable(buffers).concatWith(Flowable.error(new RuntimeException("error"))))
        )
            .test()
            .assertError(RuntimeException.class);

        cut.finalizeCapture(ctx);
    }

    private void initializeHeaders(HttpHeaders initialHeaders) {
        AtomicReference<HttpHeaders> headersRef = new AtomicReference<>(initialHeaders);
        when(response.headers()).thenAnswer(invocation -> headersRef.get());
        when(response.setHeaders(any(HttpHeaders.class))).thenAnswer(invocation -> {
            headersRef.set(invocation.getArgument(0));
            return response;
        });
    }
}
