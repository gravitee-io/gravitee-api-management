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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.request.LogEndpointRequest;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.response.LogEndpointResponse;
import io.gravitee.reporter.api.common.Request;
import io.gravitee.reporter.api.v4.log.Log;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class LoggingHookTest {

    @Mock
    private MutableExecutionContext ctx;

    @Mock
    private MutableRequest request;

    @Mock
    private MutableResponse response;

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
    void shouldNotLogEndpointRequestWhenNoLog() {
        when(metrics.getLog()).thenReturn(null);

        final TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();
    }

    @Test
    void shouldNotLogEndpointRequestWhenNotEndpointRequest() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(false);

        final TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNull(log.getEntrypointRequest());
    }

    @Test
    void shouldSetEndpointRequestWhenEndpointRequest() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(true);
        when(request.chunks()).thenReturn(Flowable.empty());

        log.setEndpointRequest(new LogEndpointRequest(loggingContext, ctx));

        final TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNotNull(log.getEndpointRequest());
    }

    @Test
    void shouldSetEndpointRequestHeaderWhenEndpointRequestHeaders() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();
        log.setEndpointRequest(new Request());

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequestHeaders()).thenReturn(true);
        when(request.headers()).thenReturn(HttpHeaders.create());

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNotNull(log.getEndpointRequest().getHeaders());
    }

    @Test
    void shouldNotSetEndpointRequestHeaderWhenEndpointRequestAndNotEndpointRequestHeaders() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();
        log.setEndpointRequest(new Request());

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequestHeaders()).thenReturn(false);

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNull(log.getEndpointRequest().getHeaders());
    }

    @Test
    void shouldSetEndpointRequestHeaderWhenEndpointRequestAndInterrupt() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();
        log.setEndpointRequest(new Request());

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequestHeaders()).thenReturn(true);
        when(request.headers()).thenReturn(HttpHeaders.create());

        final TestObserver<Void> obs = cut.interruptWith("test", ctx, ExecutionPhase.REQUEST, new ExecutionFailure(500)).test();
        obs.assertComplete();

        assertNotNull(log.getEndpointRequest().getHeaders());
    }

    @Test
    void shouldSetEndpointRequestMethodWhenEndpointRequest() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();
        Request endpointRequest = new Request();
        endpointRequest.setMethod(HttpMethod.GET);
        log.setEndpointRequest(endpointRequest);

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(true);
        when(request.method()).thenReturn(HttpMethod.CONNECT);

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertEquals(HttpMethod.CONNECT, log.getEndpointRequest().getMethod());
    }

    @Test
    void shouldSetEndpointRequestMethodWhenEndpointRequestAndInterrupt() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();
        Request endpointRequest = new Request();
        endpointRequest.setMethod(HttpMethod.GET);
        log.setEndpointRequest(endpointRequest);

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(true);
        when(request.method()).thenReturn(HttpMethod.CONNECT);

        final TestObserver<Void> obs = cut.interruptWith("test", ctx, ExecutionPhase.REQUEST, new ExecutionFailure(500)).test();
        obs.assertComplete();

        assertEquals(HttpMethod.CONNECT, log.getEndpointRequest().getMethod());
    }

    @Test
    void shouldNotLogEndpointResponseWhenNoLog() {
        when(metrics.getLog()).thenReturn(null);

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();
    }

    @Test
    void shouldNotLogEndpointResponseWhenNotEndpointResponse() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointResponse()).thenReturn(false);

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNull(log.getEndpointResponse());
    }

    @Test
    void shouldSetEndpointResponseWhenEndpointResponse() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();
        log.setEndpointResponse(new LogEndpointResponse(loggingContext, response));

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointResponse()).thenReturn(true);
        when(response.headers()).thenReturn(new LogHeadersCaptor(HttpHeaders.create()));

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNotNull(log.getEndpointResponse());
    }

    @Test
    void shouldSetEndpointResponseWhenEndpointResponseAndInterrupt() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();
        log.setEndpointResponse(new LogEndpointResponse(loggingContext, response));

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointResponse()).thenReturn(true);
        when(response.headers()).thenReturn(new LogHeadersCaptor(HttpHeaders.create()));

        final TestObserver<Void> obs = cut.interrupt("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNotNull(log.getEndpointResponse());
    }

    @Test
    void shouldSetEndpointResponseWhenProxyModeAndInterruptWith() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();
        log.setEndpointResponse(new LogEndpointResponse(loggingContext, response));

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointResponse()).thenReturn(true);
        when(response.headers()).thenReturn(new LogHeadersCaptor(HttpHeaders.create()));

        final TestObserver<Void> obs = cut.interruptWith("test", ctx, ExecutionPhase.REQUEST, new ExecutionFailure(500)).test();
        obs.assertComplete();

        assertNotNull(log.getEndpointResponse());
    }

    @Test
    void shouldReturnId() {
        assertEquals("hook-logging", cut.id());
    }

    @Test
    void shouldUnwrapLogHeadersCaptorInPost() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();
        LogEndpointResponse endpointResponse = new LogEndpointResponse(loggingContext, response);
        log.setEndpointResponse(endpointResponse);
        HttpHeaders delegateHeaders = HttpHeaders.create();
        LogHeadersCaptor wrappedHeaders = new LogHeadersCaptor(delegateHeaders);
        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointResponse()).thenReturn(true);
        when(response.headers()).thenReturn(wrappedHeaders);
        TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.RESPONSE).test();
        obs.assertComplete();
        verify(response).setHeaders(delegateHeaders);
    }

    @Test
    void shouldWrapHeadersInPreWhenRequestHeadersOrResponseLoggingEnabled() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();

        when(metrics.getLog()).thenReturn(log);
        when(ctx.response()).thenReturn(response);
        when(loggingContext.endpointRequest()).thenReturn(false);
        when(loggingContext.endpointResponseHeaders()).thenReturn(false);

        TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();
        verify(response, never()).setHeaders(any());

        when(loggingContext.endpointRequest()).thenReturn(false);
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);
        TestObserver<Void> obs2 = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs2.assertComplete();
        verify(response, times(1)).setHeaders(argThat(headers -> headers instanceof LogHeadersCaptor));
    }

    @Test
    void shouldSetMethodAndHeadersInPostWhenEndpointRequestAndHeadersEnabled() {
        LogEndpointRequest logEndpointRequest = mock(LogEndpointRequest.class);
        Log log = Log.builder().timestamp(System.currentTimeMillis()).endpointRequest(logEndpointRequest).build();
        when(metrics.getLog()).thenReturn(log);
        HttpHeaders mockRequestHeaders = mock(HttpHeaders.class);
        when(ctx.request().headers()).thenReturn(mockRequestHeaders);
        when(ctx.request().method()).thenReturn(HttpMethod.GET);
        when(loggingContext.endpointRequest()).thenReturn(true);
        when(loggingContext.endpointRequestHeaders()).thenReturn(true);
        TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();
        verify(logEndpointRequest).setHeaders(eq(mockRequestHeaders));
    }

    @Test
    void shouldWrapHeadersInPreWhenOnlyEndpointResponseEnabled() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();

        when(metrics.getLog()).thenReturn(log);
        when(ctx.response()).thenReturn(response);
        when(loggingContext.endpointRequest()).thenReturn(false);
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);

        TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        verify(response).setHeaders(argThat(headers -> headers instanceof LogHeadersCaptor));
    }

    @Test
    void shouldWrapHeadersInPreWhenBothEndpointRequestHeadersAndResponseEnabled() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();

        when(metrics.getLog()).thenReturn(log);
        when(ctx.response()).thenReturn(response);
        when(loggingContext.endpointRequest()).thenReturn(false);
        when(loggingContext.endpointResponseHeaders()).thenReturn(true);
        TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        verify(response).setHeaders(argThat(headers -> headers instanceof LogHeadersCaptor));
    }

    @Test
    void shouldNotWrapHeadersInPreWhenNeitherEndpointRequestHeadersNorResponseEnabled() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.endpointRequest()).thenReturn(false);
        TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();
        verify(response, never()).setHeaders(any());
    }
}
