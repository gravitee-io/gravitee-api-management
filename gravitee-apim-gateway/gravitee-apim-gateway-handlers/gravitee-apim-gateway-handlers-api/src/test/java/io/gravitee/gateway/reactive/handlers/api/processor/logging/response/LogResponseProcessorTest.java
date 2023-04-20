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
package io.gravitee.gateway.reactive.handlers.api.processor.logging.response;

import static io.gravitee.gateway.reactive.handlers.api.processor.logging.response.LogResponseProcessor.ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.reactive.handlers.api.processor.AbstractProcessorTest;
import io.gravitee.gateway.reactive.handlers.api.processor.logging.response.LogResponseProcessor;
import io.gravitee.reporter.api.v4.log.Log;
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
class LogResponseProcessorTest extends AbstractProcessorTest {

    private final LogResponseProcessor cut = LogResponseProcessor.instance();

    @Mock
    private AnalyticsContext analyticsContext;

    @Mock
    private LoggingContext loggingContext;

    @BeforeEach
    public void beforeEach() {
        lenient().when(analyticsContext.isEnabled()).thenReturn(true);
        lenient().when(analyticsContext.getLoggingContext()).thenReturn(loggingContext);
        spyCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT, analyticsContext);
        spyCtx.metrics(mockMetrics);
    }

    @Test
    void shouldNotLogWhenNoLog() {
        spyCtx.metrics().setLog(null);

        final TestObserver<Void> obs = cut.execute(spyCtx).test();
        obs.assertComplete();
        assertThat(spyCtx.metrics().getLog()).isNull();
    }

    @Test
    void shouldNotLogWhenEntrypointResponseLogNotEnable() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();
        when(mockMetrics.getLog()).thenReturn(log);
        when(loggingContext.entrypointResponse()).thenReturn(false);

        final TestObserver<Void> obs = cut.execute(spyCtx).test();
        obs.assertComplete();

        assertNull(log.getEntrypointResponse());
    }

    @Test
    void shouldSetClientResponseWhenEntrypointResponseLogEnabled() {
        Log log = Log.builder().timestamp(System.currentTimeMillis()).build();
        when(mockMetrics.getLog()).thenReturn(log);
        when(loggingContext.entrypointResponse()).thenReturn(true);

        final TestObserver<Void> obs = cut.execute(spyCtx).test();
        obs.assertComplete();

        assertNotNull(log.getEntrypointResponse());
    }

    @Test
    void shouldReturnId() {
        assertEquals(ID, cut.getId());
    }
}
