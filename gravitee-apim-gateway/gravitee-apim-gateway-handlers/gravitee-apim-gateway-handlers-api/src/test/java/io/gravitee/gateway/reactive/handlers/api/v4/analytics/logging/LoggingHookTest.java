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
package io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.HttpRequestInternal;
import io.gravitee.gateway.reactive.core.context.HttpResponseInternal;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.request.LogEndpointRequest;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.response.LogEndpointResponse;
import io.gravitee.reporter.api.v4.log.Log;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class LoggingHookTest {

    @Mock
    private HttpExecutionContextInternal ctx;

    @Mock
    private HttpRequestInternal request;

    @Mock
    private HttpResponseInternal response;

    @Mock
    private AnalyticsContext analyticsContext;

    @Mock
    private LoggingContext loggingContext;

    @Mock
    private Metrics metrics;

    private final LoggingHook cut = new LoggingHook();

    @BeforeEach
    void init() {
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(ctx.metrics()).thenReturn(metrics);
        lenient().when(analyticsContext.isEnabled()).thenReturn(true);
        lenient().when(analyticsContext.isLoggingEnabled()).thenReturn(true);
        lenient().when(analyticsContext.getLoggingContext()).thenReturn(loggingContext);
        lenient().when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT)).thenReturn(analyticsContext);
    }

    @Test
    void should_not_log_endpoint_request_when_no_log() {
        when(metrics.getLog()).thenReturn(null);

        final TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();
    }

    @Test
    void should_not_log_endpoint_request_when_not_endpoint_request() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(false);

        final TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNull(log.getEntrypointRequest());
    }

    @Test
    void should_set_endpoint_request_when_endpoint_request() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(true);

        final TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNotNull(log.getEndpointRequest());
    }

    @Test
    void should_set_endpoint_request_header_when_endpoint_request_headers() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(true);
        when(loggingContext.endpointRequestHeaders()).thenReturn(true);
        when(request.headers()).thenReturn(HttpHeaders.create());

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNotNull(log.getEndpointRequest().getHeaders());
    }

    @Test
    void should_not_set_endpoint_request_header_when_endpoint_request_and_not_endpoint_request_headers() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(true);
        when(loggingContext.endpointRequestHeaders()).thenReturn(false);

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNull(log.getEndpointRequest().getHeaders());
    }

    @Test
    void should_set_endpoint_request_header_when_endpoint_request_and_interrupt() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(true);
        when(loggingContext.endpointRequestHeaders()).thenReturn(true);
        when(request.headers()).thenReturn(HttpHeaders.create());

        final TestObserver<Void> obs = cut.interruptWith("test", ctx, ExecutionPhase.REQUEST, new ExecutionFailure(500)).test();
        obs.assertComplete();

        assertNotNull(log.getEndpointRequest().getHeaders());
    }

    @Test
    void should_set_endpoint_request_method_when_endpoint_request() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(true);
        when(request.method()).thenReturn(HttpMethod.CONNECT);

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertEquals(HttpMethod.CONNECT, log.getEndpointRequest().getMethod());
    }

    @Test
    void should_set_endpoint_request_method_when_endpoint_request_and_interrupt() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(true);
        when(request.method()).thenReturn(HttpMethod.CONNECT);

        final TestObserver<Void> obs = cut.interruptWith("test", ctx, ExecutionPhase.REQUEST, new ExecutionFailure(500)).test();
        obs.assertComplete();

        assertEquals(HttpMethod.CONNECT, log.getEndpointRequest().getMethod());
    }

    @Test
    void should_not_log_endpoint_response_when_no_log() {
        when(metrics.getLog()).thenReturn(null);

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();
    }

    @Test
    void should_not_log_endpoint_response_when_not_endpoint_response() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointResponse()).thenReturn(false);

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNull(log.getEndpointResponse());
    }

    @Test
    void should_set_endpoint_response_when_endpoint_response() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointResponse()).thenReturn(true);

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNotNull(log.getEndpointResponse());
    }

    @Test
    void should_set_endpoint_response_when_endpoint_response_and_interrupt() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointResponse()).thenReturn(true);

        final TestObserver<Void> obs = cut.interrupt("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNotNull(log.getEndpointResponse());
    }

    @Test
    void should_set_endpoint_response_when_proxy_mode_and_interrupt_with() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointResponse()).thenReturn(true);

        final TestObserver<Void> obs = cut.interruptWith("test", ctx, ExecutionPhase.REQUEST, new ExecutionFailure(500)).test();
        obs.assertComplete();

        assertNotNull(log.getEndpointResponse());
    }

    @Test
    void should_return_id() {
        assertEquals("hook-logging", cut.id());
    }

    @Test
    void should_unwrap_log_headers_captor_in_post() {
        Log log = initLog();
        HttpHeaders delegateHeaders = HttpHeaders.create().set("X-Test-Header", "test-value");
        LogHeadersCaptor wrappedHeaders = new LogHeadersCaptor(delegateHeaders);
        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointResponse()).thenReturn(true);
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);
        when(response.headers()).thenReturn(wrappedHeaders);
        TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.RESPONSE).test();
        obs.assertComplete();
        verify(response).setHeaders(delegateHeaders);
    }

    @Test
    void should_wrap_headers_in_pre_when_request_headers_or_response_logging_enabled() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointResponse()).thenReturn(false);
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);

        TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();
        verify(response, never()).setHeaders(any());

        when(loggingContext.endpointResponse()).thenReturn(true);
        TestObserver<Void> obs2 = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs2.assertComplete();
        verify(response, times(1)).setHeaders(argThat(headers -> headers instanceof LogHeadersCaptor));
    }

    @Test
    void should_set_method_and_headers_in_post_when_endpoint_request_and_headers_enabled() {
        Log log = initLog();
        when(metrics.getLog()).thenReturn(log);
        HttpHeaders requestHeaders = HttpHeaders.create().set("X-Test-Header", "test-value");
        when(ctx.request().headers()).thenReturn(requestHeaders);
        when(ctx.request().method()).thenReturn(HttpMethod.GET);
        when(loggingContext.endpointRequest()).thenReturn(true);
        when(loggingContext.endpointRequestHeaders()).thenReturn(true);
        TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();
        assertThat(log.getEndpointRequest().getHeaders()).isEqualTo(requestHeaders);
    }

    @Test
    void should_wrap_headers_in_pre_when_only_endpoint_response_enabled() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(false);
        when(loggingContext.endpointResponse()).thenReturn(true);
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);

        TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        verify(response).setHeaders(argThat(headers -> headers instanceof LogHeadersCaptor));
    }

    @Test
    void should_wrap_headers_in_pre_when_both_endpoint_request_headers_and_response_enabled() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(false);
        when(loggingContext.endpointResponse()).thenReturn(true);
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);
        TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        verify(response).setHeaders(argThat(headers -> headers instanceof LogHeadersCaptor));
    }

    @Test
    void should_not_wrap_headers_in_pre_when_neither_endpoint_request_headers_nor_response_enabled() {
        Log log = initLog();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(false);
        when(loggingContext.endpointResponse()).thenReturn(false);
        TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();
        verify(response, never()).setHeaders(any());
    }

    private Log initLog() {
        return Log.builder()
            .timestamp(System.currentTimeMillis())
            .endpointRequest(new LogEndpointRequest(loggingContext, request))
            .endpointResponse(new LogEndpointResponse(loggingContext, response))
            .build();
    }
}
