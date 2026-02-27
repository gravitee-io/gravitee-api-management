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
package io.gravitee.gateway.reactive.reactor.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.core.connection.ConnectionDrainManager;
import io.gravitee.gateway.reactive.core.connection.DefaultConnectionDrainManager;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.reactor.processor.alert.AlertProcessor;
import io.gravitee.gateway.reactive.reactor.processor.connection.ConnectionDrainProcessor;
import io.gravitee.gateway.reactive.reactor.processor.forward.XForwardProcessor;
import io.gravitee.gateway.reactive.reactor.processor.metrics.MetricsProcessor;
import io.gravitee.gateway.reactive.reactor.processor.reporter.ReporterProcessor;
import io.gravitee.gateway.reactive.reactor.processor.responsetime.ResponseTimeProcessor;
import io.gravitee.gateway.reactive.reactor.processor.tracing.TraceContextProcessor;
import io.gravitee.gateway.reactive.reactor.processor.transaction.TransactionPreProcessor;
import io.gravitee.gateway.reactive.reactor.processor.transaction.TransactionPreProcessorFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultPlatformProcessorChainFactoryTest {

    @Mock
    private TransactionPreProcessorFactory transactionHandlerFactory;

    @Mock
    private ReporterService reporterService;

    @Mock
    private AlertEventProducer eventProducer;

    @Mock
    private Node node;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    private ConnectionDrainManager connectionDrainManager;

    @BeforeEach
    public void setup() {
        lenient().when(transactionHandlerFactory.create()).thenReturn(mock(TransactionPreProcessor.class));
        connectionDrainManager = new DefaultConnectionDrainManager();
    }

    @Nested
    class PreProcessors {

        @Test
        void should_have_preProcessors_with_TraceContextProcessor_when_traceContext_is_enabled() {
            DefaultPlatformProcessorChainFactory platformProcessorChainFactory = new DefaultPlatformProcessorChainFactory(
                transactionHandlerFactory,
                true,
                true,
                reporterService,
                eventProducer,
                node,
                "8080",
                gatewayConfiguration,
                connectionDrainManager,
                null
            );
            List<Processor> processors = platformProcessorChainFactory.buildPreProcessorList();

            assertEquals(5, processors.size());
            assertInstanceOf(ConnectionDrainProcessor.class, processors.get(0));
            assertInstanceOf(TransactionPreProcessor.class, processors.get(1));
            assertInstanceOf(MetricsProcessor.class, processors.get(2));
            assertInstanceOf(XForwardProcessor.class, processors.get(3));
            assertInstanceOf(TraceContextProcessor.class, processors.get(4));
        }

        @Test
        void should_not_have_preProcessors_with_TraceContextProcessor_when_traceContext_is_not_enabled() {
            DefaultPlatformProcessorChainFactory platformProcessorChainFactory = new DefaultPlatformProcessorChainFactory(
                transactionHandlerFactory,
                false,
                true,
                reporterService,
                eventProducer,
                node,
                "8080",
                gatewayConfiguration,
                connectionDrainManager,
                null
            );
            List<Processor> processors = platformProcessorChainFactory.buildPreProcessorList();

            assertEquals(4, processors.size());
            assertInstanceOf(ConnectionDrainProcessor.class, processors.get(0));
            assertInstanceOf(TransactionPreProcessor.class, processors.get(1));
            assertInstanceOf(MetricsProcessor.class, processors.get(2));
            assertInstanceOf(XForwardProcessor.class, processors.get(3));
        }
    }

    @Nested
    class PostProcessors {

        @Test
        void should_not_have_postProcessors_with_AlertProcessors_when_EventProducer_is_empty() {
            DefaultPlatformProcessorChainFactory platformProcessorChainFactory = new DefaultPlatformProcessorChainFactory(
                transactionHandlerFactory,
                false,
                true,
                reporterService,
                eventProducer,
                node,
                "8080",
                gatewayConfiguration,
                connectionDrainManager,
                null
            );
            when(eventProducer.isEmpty()).thenReturn(true);

            List<Processor> processors = platformProcessorChainFactory.buildPostProcessorList();

            assertEquals(2, processors.size());
            assertTrue(processors.get(0) instanceof ResponseTimeProcessor);
            assertTrue(processors.get(1) instanceof ReporterProcessor);
        }

        @Test
        void should_have_postProcessors_with_AlertProcessors_when_EventProducer_is_not_empty() {
            DefaultPlatformProcessorChainFactory platformProcessorChainFactory = new DefaultPlatformProcessorChainFactory(
                transactionHandlerFactory,
                false,
                true,
                reporterService,
                eventProducer,
                node,
                "8080",
                gatewayConfiguration,
                connectionDrainManager,
                null
            );
            when(eventProducer.isEmpty()).thenReturn(false);

            List<Processor> processors = platformProcessorChainFactory.buildPostProcessorList();

            assertEquals(3, processors.size());
            assertTrue(processors.get(0) instanceof ResponseTimeProcessor);
            assertTrue(processors.get(1) instanceof ReporterProcessor);
            assertTrue(processors.get(2) instanceof AlertProcessor);
        }
    }
}
