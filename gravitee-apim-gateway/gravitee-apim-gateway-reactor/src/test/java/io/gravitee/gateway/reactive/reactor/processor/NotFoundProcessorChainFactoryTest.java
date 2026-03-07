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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.reactor.processor.metrics.MetricsProcessor;
import io.gravitee.gateway.reactive.reactor.processor.notfound.NotFoundProcessor;
import io.gravitee.gateway.reactive.reactor.processor.reporter.ReporterProcessor;
import io.gravitee.gateway.reactive.reactor.processor.responsetime.ResponseTimeProcessor;
import io.gravitee.gateway.reactive.reactor.processor.transaction.TransactionPreProcessor;
import io.gravitee.gateway.reactive.reactor.processor.transaction.TransactionPreProcessorFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotFoundProcessorChainFactoryTest {

    private TransactionPreProcessorFactory transactionPreProcessorFactory = new TransactionPreProcessorFactory(null, null);

    @Mock
    private ReporterService reporterService;

    @Mock
    private MutableRequest request;

    @Mock
    private MutableResponse response;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private ComponentProvider componentProvider;

    @Mock
    private Node node;

    @BeforeEach
    void setUp() {
        lenient().when(componentProvider.getComponent(Node.class)).thenReturn(node);
    }

    @Test
    void should_have_notFoundProcessors() {
        NotFoundProcessorChainFactory notFoundProcessorChainFactory = new NotFoundProcessorChainFactory(
            transactionPreProcessorFactory,
            new StandardEnvironment(),
            reporterService,
            false,
            gatewayConfiguration,
            null
        );
        List<Processor> processors = notFoundProcessorChainFactory.buildProcessorChain();

        assertEquals(5, processors.size());
        assertTrue(processors.get(0) instanceof TransactionPreProcessor);
        assertTrue(processors.get(1) instanceof MetricsProcessor);
        assertTrue(processors.get(2) instanceof NotFoundProcessor);
        assertTrue(processors.get(3) instanceof ResponseTimeProcessor);
        assertTrue(processors.get(4) instanceof ReporterProcessor);
    }

    @Test
    void should_execute_not_found_processor_chain() {
        // Given
        DefaultExecutionContext notFoundRequestContext = new DefaultExecutionContext(request, response).componentProvider(
            componentProvider
        );

        NotFoundProcessorChainFactory notFoundProcessorChainFactory = new NotFoundProcessorChainFactory(
            transactionPreProcessorFactory,
            new StandardEnvironment(),
            reporterService,
            true,
            gatewayConfiguration,
            null
        );
        ProcessorChain processorChain = notFoundProcessorChainFactory.processorChain();

        when(response.end(notFoundRequestContext)).thenReturn(Completable.complete());
        when(response.headers()).thenReturn(HttpHeaders.create());
        when(request.id()).thenReturn("requestId");
        when(request.headers()).thenReturn(HttpHeaders.create());

        when(request.transactionId(any())).thenAnswer(answer -> {
            when(request.transactionId()).thenReturn(answer.getArgument(0));
            return request;
        });

        when(gatewayConfiguration.tenant()).thenReturn(Optional.of("TENANT"));
        when(gatewayConfiguration.zone()).thenReturn(Optional.of("ZONE"));

        // When
        processorChain.execute(notFoundRequestContext, ExecutionPhase.RESPONSE).test().assertResult();

        // Then
        verify(response).status(HttpStatusCode.NOT_FOUND_404);
        verify(response).end(notFoundRequestContext);
        verify(reporterService).report(any(Reportable.class));
        Metrics metrics = notFoundRequestContext.metrics();
        assertTrue(metrics.getGatewayResponseTimeMs() > 0);
        assertTrue(metrics.getGatewayLatencyMs() > 0);
        assertThat(metrics.getApiId()).isEqualTo("1");
        assertThat(metrics.getApplicationId()).isEqualTo("1");
        assertThat(metrics.getTenant()).isEqualTo("TENANT");
        assertThat(metrics.getZone()).isEqualTo("ZONE");
        assertThat(metrics.getRequestId()).isNotNull();
        assertThat(metrics.getTransactionId()).isNotNull();
    }
}
