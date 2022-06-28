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
package io.gravitee.gateway.jupiter.handlers.api.hook.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.core.logging.LoggingContext;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableRequestExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.handlers.api.logging.LogHeadersCaptor;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
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
    private MutableRequestExecutionContext ctx;

    @Mock
    private MutableRequest request;

    @Mock
    private MutableResponse response;

    @Mock
    private LoggingContext loggingContext;

    @Mock
    private Metrics metrics;

    private final LoggingHook cut = new LoggingHook();

    @BeforeEach
    void init() {
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(request.metrics()).thenReturn(metrics);
        lenient().when(ctx.getInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE)).thenReturn(loggingContext);
    }

    @Test
    void shouldNotLogProxyRequestWhenNoLog() {
        when(metrics.getLog()).thenReturn(null);

        final TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();
    }

    @Test
    void shouldNotLogProxyRequestWhenNotProxyMode() {
        final Log log = new Log(System.currentTimeMillis());

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.proxyMode()).thenReturn(false);

        final TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNull(log.getProxyRequest());
    }

    @Test
    void shouldSetProxyRequestWhenProxyMode() {
        final Log log = new Log(System.currentTimeMillis());

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.proxyMode()).thenReturn(true);
        when(request.chunks()).thenReturn(Flowable.empty());

        final TestObserver<Void> obs = cut.pre("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNotNull(log.getProxyRequest());
    }

    @Test
    void shouldNotLogProxyResponseWhenNoLog() {
        when(metrics.getLog()).thenReturn(null);

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();
    }

    @Test
    void shouldNotLogProxyResponseWhenNotProxyMode() {
        final Log log = new Log(System.currentTimeMillis());

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.proxyMode()).thenReturn(false);

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNull(log.getProxyResponse());
    }

    @Test
    void shouldSetProxyResponseWhenProxyMode() {
        final Log log = new Log(System.currentTimeMillis());

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.proxyMode()).thenReturn(true);
        when(response.headers()).thenReturn(new LogHeadersCaptor(HttpHeaders.create()));

        final TestObserver<Void> obs = cut.post("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNotNull(log.getProxyResponse());
    }

    @Test
    void shouldSetProxyResponseWhenProxyModeAndInterrupt() {
        final Log log = new Log(System.currentTimeMillis());

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.proxyMode()).thenReturn(true);
        when(response.headers()).thenReturn(new LogHeadersCaptor(HttpHeaders.create()));

        final TestObserver<Void> obs = cut.interrupt("test", ctx, ExecutionPhase.REQUEST).test();
        obs.assertComplete();

        assertNotNull(log.getProxyResponse());
    }

    @Test
    void shouldSetProxyResponseWhenProxyModeAndInterruptWith() {
        final Log log = new Log(System.currentTimeMillis());

        when(metrics.getLog()).thenReturn(log);
        when(loggingContext.proxyMode()).thenReturn(true);
        when(response.headers()).thenReturn(new LogHeadersCaptor(HttpHeaders.create()));

        final TestObserver<Void> obs = cut.interruptWith("test", ctx, ExecutionPhase.REQUEST, new ExecutionFailure(500)).test();
        obs.assertComplete();

        assertNotNull(log.getProxyResponse());
    }

    @Test
    void shouldReturnId() {
        assertEquals("hook-logging", cut.id());
    }
}
