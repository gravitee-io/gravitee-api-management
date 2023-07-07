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
package io.gravitee.gateway.reactive.handlers.api.v4.processor.logging;

import static io.gravitee.gateway.reactive.handlers.api.v4.processor.logging.LogRequestProcessor.ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.AbstractV4ProcessorTest;
import io.gravitee.reporter.api.v4.log.Log;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
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
class LogRequestProcessorTest extends AbstractV4ProcessorTest {

    protected static final String REQUEST_ID = "requestId";
    private final LogRequestProcessor cut = LogRequestProcessor.instance();

    @Mock
    private AnalyticsContext analyticsContext;

    @Mock
    private LoggingContext loggingContext;

    @BeforeEach
    public void beforeEach() {
        lenient().when(analyticsContext.isEnabled()).thenReturn(true);
        lenient().when(analyticsContext.getLoggingContext()).thenReturn(loggingContext);
        lenient().when(analyticsContext.isLoggingEnabled()).thenReturn(true);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);
    }

    @Test
    void shouldNotLogWhenLoggingDisabled() {
        when(analyticsContext.isLoggingEnabled()).thenReturn(false);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verifyNoInteractions(mockMetrics);
        verifyNoInteractions(mockRequest);
    }

    @Test
    void shouldNotLogWhenLoggingConditionIsEvaluatedToFalse() {
        when(loggingContext.getCondition()).thenReturn("false");

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();
        verifyNoInteractions(mockMetrics);
        verifyNoInteractions(mockRequest);
    }

    @Test
    void shouldCreateLogWhenLoggingConditionIsEvaluatedToTrue() {
        final long timestamp = System.currentTimeMillis();

        when(loggingContext.getCondition()).thenReturn("true");
        when(loggingContext.entrypointRequest()).thenReturn(false);
        when(mockRequest.timestamp()).thenReturn(timestamp);
        when(mockRequest.id()).thenReturn(REQUEST_ID);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        ArgumentCaptor<Log> logCaptor = ArgumentCaptor.forClass(Log.class);
        verify(mockMetrics).setLog(logCaptor.capture());

        final Log log = logCaptor.getValue();
        assertNotNull(log);
        assertEquals(timestamp, log.getTimestamp());
        assertEquals(REQUEST_ID, log.getRequestId());
        assertNull(log.getEntrypointRequest());
    }

    @Test
    void shouldCreateLogWhenLoggingConditionIsNull() {
        when(loggingContext.getCondition()).thenReturn(null);
        when(loggingContext.entrypointRequest()).thenReturn(false);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(mockMetrics).setLog(any(Log.class));
    }

    @Test
    void shouldCreateLogWhenLoggingConditionIsEmpty() {
        when(loggingContext.getCondition()).thenReturn("");
        when(loggingContext.entrypointRequest()).thenReturn(false);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(mockMetrics).setLog(any(Log.class));
    }

    @Test
    void shouldSetClientRequestWhenEntrypointRequestLogEnabled() {
        when(loggingContext.entrypointRequest()).thenReturn(true);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        ArgumentCaptor<Log> logCaptor = ArgumentCaptor.forClass(Log.class);
        verify(mockMetrics).setLog(logCaptor.capture());
    }

    @Test
    void shouldReturnId() {
        assertEquals(ID, cut.getId());
    }
}
