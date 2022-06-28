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
package io.gravitee.gateway.jupiter.handlers.api.processor.logging;

import static io.gravitee.gateway.jupiter.handlers.api.processor.logging.LogRequestProcessor.ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.core.logging.LoggingContext;
import io.gravitee.gateway.jupiter.handlers.api.processor.AbstractProcessorTest;
import io.gravitee.reporter.api.log.Log;
import io.reactivex.observers.TestObserver;
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
class LogRequestProcessorTest extends AbstractProcessorTest {

    protected static final String REQUEST_ID = "requestId";
    private final LogRequestProcessor cut = LogRequestProcessor.instance();

    @Mock
    private LoggingContext loggingContext;

    @Test
    void shouldNotLogWhenNoLoggingContext() {
        ctx.setInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE, null);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verifyNoInteractions(mockMetrics);
        verifyNoInteractions(mockRequest);
        verifyNoInteractions(mockResponse);
    }

    @Test
    void shouldNotLogWhenLoggingConditionIsEvaluatedToFalse() {
        when(loggingContext.getCondition()).thenReturn("false");
        ctx.setInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE, loggingContext);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verifyNoInteractions(mockMetrics);
        verifyNoInteractions(mockRequest);
    }

    @Test
    void shouldCreateLogWhenLoggingConditionIsEvaluatedToTrue() {
        final long timestamp = System.currentTimeMillis();

        when(loggingContext.getCondition()).thenReturn("true");
        when(loggingContext.clientMode()).thenReturn(false);
        when(mockRequest.timestamp()).thenReturn(timestamp);
        when(mockRequest.id()).thenReturn(REQUEST_ID);
        ctx.setInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE, loggingContext);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        ArgumentCaptor<Log> logCaptor = ArgumentCaptor.forClass(Log.class);
        verify(mockMetrics).setLog(logCaptor.capture());

        final Log log = logCaptor.getValue();
        assertNotNull(log);
        assertEquals(timestamp, log.getTimestamp());
        assertEquals(REQUEST_ID, log.getRequestId());
        assertNull(log.getClientRequest());
    }

    @Test
    void shouldCreateLogWhenLoggingConditionIsNull() {
        when(loggingContext.getCondition()).thenReturn(null);
        when(loggingContext.clientMode()).thenReturn(false);
        ctx.setInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE, loggingContext);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(mockMetrics).setLog(any(Log.class));
    }

    @Test
    void shouldCreateLogWhenLoggingConditionIsEmpty() {
        when(loggingContext.getCondition()).thenReturn("");
        when(loggingContext.clientMode()).thenReturn(false);
        ctx.setInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE, loggingContext);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        verify(mockMetrics).setLog(any(Log.class));
    }

    @Test
    void shouldSetClientRequestWhenClientMode() {
        when(loggingContext.clientMode()).thenReturn(true);
        ctx.setInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE, loggingContext);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        ArgumentCaptor<Log> logCaptor = ArgumentCaptor.forClass(Log.class);
        verify(mockMetrics).setLog(logCaptor.capture());

        final Log log = logCaptor.getValue();
        assertNotNull(log);
        assertNotNull(log.getClientRequest());
    }

    @Test
    void shouldReturnId() {
        assertEquals(ID, cut.getId());
    }
}
