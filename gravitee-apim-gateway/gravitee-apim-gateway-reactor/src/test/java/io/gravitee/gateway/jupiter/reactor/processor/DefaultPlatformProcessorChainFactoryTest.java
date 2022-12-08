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
package io.gravitee.gateway.jupiter.reactor.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.jupiter.reactor.processor.alert.AlertProcessor;
import io.gravitee.gateway.jupiter.reactor.processor.forward.XForwardForProcessor;
import io.gravitee.gateway.jupiter.reactor.processor.metrics.MetricsProcessor;
import io.gravitee.gateway.jupiter.reactor.processor.reporter.ReporterProcessor;
import io.gravitee.gateway.jupiter.reactor.processor.responsetime.ResponseTimeProcessor;
import io.gravitee.gateway.jupiter.reactor.processor.tracing.TraceContextProcessor;
import io.gravitee.gateway.jupiter.reactor.processor.transaction.TransactionProcessor;
import io.gravitee.gateway.jupiter.reactor.processor.transaction.TransactionProcessorFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultPlatformProcessorChainFactoryTest {

    @Mock
    private TransactionProcessorFactory transactionHandlerFactory;

    @Mock
    private ReporterService reporterService;

    @Mock
    private AlertEventProducer eventProducer;

    @Mock
    private Node node;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @BeforeEach
    public void setup() {
        lenient().when(transactionHandlerFactory.create()).thenReturn(mock(TransactionProcessor.class));
    }

    @Test
    @DisplayName("Should have 4 pre processors when trace context is enabled")
    public void shouldHave3PreProcessorsWhenTraceContextEnabled() {
        DefaultPlatformProcessorChainFactory platformProcessorChainFactory = new DefaultPlatformProcessorChainFactory(
            transactionHandlerFactory,
            true,
            reporterService,
            eventProducer,
            node,
            "8080",
            false,
            gatewayConfiguration
        );
        List<Processor> processors = platformProcessorChainFactory.buildPreProcessorList();

        assertEquals(4, processors.size());
        assertTrue(processors.get(0) instanceof MetricsProcessor);
        assertTrue(processors.get(1) instanceof XForwardForProcessor);
        assertTrue(processors.get(2) instanceof TraceContextProcessor);
        assertTrue(processors.get(3) instanceof TransactionProcessor);
    }

    @Test
    @DisplayName("Should have 3 pre processors when trace context is not enabled")
    public void shouldHave2PreProcessorsWhenTraceContextNotEnabled() {
        DefaultPlatformProcessorChainFactory platformProcessorChainFactory = new DefaultPlatformProcessorChainFactory(
            transactionHandlerFactory,
            false,
            reporterService,
            eventProducer,
            node,
            "8080",
            false,
            gatewayConfiguration
        );
        List<Processor> processors = platformProcessorChainFactory.buildPreProcessorList();

        assertEquals(3, processors.size());
        assertTrue(processors.get(0) instanceof MetricsProcessor);
        assertTrue(processors.get(1) instanceof XForwardForProcessor);
        assertTrue(processors.get(2) instanceof TransactionProcessor);
    }

    @Test
    @DisplayName("Should have 2 post processors when event producer is empty")
    public void shouldHave2PostProcessorsWhenEmptyEventProducer() {
        DefaultPlatformProcessorChainFactory platformProcessorChainFactory = new DefaultPlatformProcessorChainFactory(
            transactionHandlerFactory,
            false,
            reporterService,
            eventProducer,
            node,
            "8080",
            false,
            gatewayConfiguration
        );
        when(eventProducer.isEmpty()).thenReturn(true);

        List<Processor> processors = platformProcessorChainFactory.buildPostProcessorList();

        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof ResponseTimeProcessor);
        assertTrue(processors.get(1) instanceof ReporterProcessor);
    }

    @Test
    @DisplayName("Should have 3 post processors when event producer is not empty")
    public void shouldHave2PostProcessorsWhenNotEmptyEventProducer() {
        DefaultPlatformProcessorChainFactory platformProcessorChainFactory = new DefaultPlatformProcessorChainFactory(
            transactionHandlerFactory,
            false,
            reporterService,
            eventProducer,
            node,
            "8080",
            false,
            gatewayConfiguration
        );
        when(eventProducer.isEmpty()).thenReturn(false);

        List<Processor> processors = platformProcessorChainFactory.buildPostProcessorList();

        assertEquals(3, processors.size());
        assertTrue(processors.get(0) instanceof ResponseTimeProcessor);
        assertTrue(processors.get(1) instanceof ReporterProcessor);
        assertTrue(processors.get(2) instanceof AlertProcessor);
    }
}
