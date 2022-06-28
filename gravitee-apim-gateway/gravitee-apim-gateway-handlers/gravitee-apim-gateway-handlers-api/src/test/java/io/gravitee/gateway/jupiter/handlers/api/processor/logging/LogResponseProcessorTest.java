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

import static io.gravitee.gateway.jupiter.handlers.api.processor.logging.LogResponseProcessor.ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.core.logging.LoggingContext;
import io.gravitee.gateway.jupiter.handlers.api.processor.AbstractProcessorTest;
import io.gravitee.reporter.api.log.Log;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class LogResponseProcessorTest extends AbstractProcessorTest {

    private final LogResponseProcessor cut = LogResponseProcessor.instance();

    @Mock
    private LoggingContext loggingContext;

    @Test
    void shouldNotLogWhenNoLog() {
        when(mockMetrics.getLog()).thenReturn(null);
        ctx.setInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE, loggingContext);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();
    }

    @Test
    void shouldNotLogWhenNotClientMode() {
        final Log log = new Log(System.currentTimeMillis());

        when(mockMetrics.getLog()).thenReturn(log);
        when(loggingContext.clientMode()).thenReturn(false);
        ctx.setInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE, loggingContext);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        assertNull(log.getClientResponse());
    }

    @Test
    void shouldSetClientResponseWhenClientMode() {
        final Log log = new Log(System.currentTimeMillis());
        when(mockMetrics.getLog()).thenReturn(log);
        when(loggingContext.clientMode()).thenReturn(true);
        ctx.setInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE, loggingContext);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertComplete();

        assertNotNull(log.getClientResponse());
    }

    @Test
    void shouldReturnId() {
        assertEquals(ID, cut.getId());
    }
}
