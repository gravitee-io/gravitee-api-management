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
package io.gravitee.gateway.reactive.handlers.api.processor;

import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor.DEFAULT_CLIENT_IDENTIFIER_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.handlers.api.processor.cors.CorsPreflightRequestProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.cors.CorsSimpleRequestProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.error.SimpleFailureProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.forward.XForwardedPrefixProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.pathmapping.PathMappingProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.pathparameters.PathParametersProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.shutdown.ShutdownProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.transaction.TransactionPostProcessor;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.logging.LogInitProcessor;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.logging.LogRequestProcessor;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.logging.LogResponseProcessor;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApiProcessorChainFactoryTest {

    @Mock
    private Configuration configuration;

    @Mock
    private Node node;

    private ApiProcessorChainFactory apiProcessorChainFactory;

    @BeforeEach
    public void beforeEach() {
        when(configuration.getProperty("handlers.request.headers.x-forwarded-prefix", Boolean.class, false)).thenReturn(false);
        when(configuration.getProperty("handlers.request.client.header", String.class, DEFAULT_CLIENT_IDENTIFIER_HEADER))
            .thenReturn(DEFAULT_CLIENT_IDENTIFIER_HEADER);
        apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node);
    }

    @Test
    void shouldReturnEmptyBeforeHandleProcessorChain() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Proxy proxy = new Proxy();
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeHandle(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertNoValues();
    }

    @Test
    void shouldReturnEmptyBeforeHandleProcessorChainWhenLoggingModeNone() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.NONE);
        final Proxy proxy = new Proxy();
        proxy.setLogging(logging);
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeHandle(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertNoValues();
    }

    @Test
    void shouldReturnBeforeHandleProcessorChainWithLogging() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT);
        final Proxy proxy = new Proxy();
        proxy.setLogging(logging);
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeHandle(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(2)
            .assertValueAt(0, processor -> processor instanceof LogInitProcessor)
            .assertValueAt(1, processor -> processor instanceof LogRequestProcessor);
    }

    @Test
    void shouldReturnEmptyBeforeApiExecutionProcessorChain() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Proxy proxy = new Proxy();
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(1).assertValueAt(0, processor -> processor instanceof SubscriptionProcessor);
    }

    @Test
    void shouldReturnCorsBeforeSecurityChainWithCors() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Proxy proxy = new Proxy();
        Cors cors = new Cors();
        cors.setEnabled(true);
        proxy.setCors(cors);
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeSecurityChain(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-security-chain");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(1)
            .assertValueAt(0, processor -> processor instanceof CorsPreflightRequestProcessor);
    }

    @Test
    void shouldReturnXForwardedBeforeApiExecutionWithOverrideXForwarded() {
        when(configuration.getProperty("handlers.request.headers.x-forwarded-prefix", Boolean.class, false)).thenReturn(true);
        apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node);

        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Proxy proxy = new Proxy();
        apiModel.setProxy(proxy);

        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(2)
            .assertValueAt(0, processor -> processor instanceof XForwardedPrefixProcessor)
            .assertValueAt(1, processor -> processor instanceof SubscriptionProcessor);
    }

    @Test
    void shouldReturnPathParamProcessorBeforeApiExecution() {
        apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node);

        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Proxy proxy = new Proxy();
        apiModel.setProxy(proxy);
        Flow flow = new Flow();
        flow.setPathOperator(new PathOperator("/products/:productId", Operator.STARTS_WITH));
        apiModel.setFlows(List.of(flow));

        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(2)
            .assertValueAt(0, processor -> processor instanceof PathParametersProcessor)
            .assertValueAt(1, processor -> processor instanceof SubscriptionProcessor);
    }

    @Test
    void shouldReturnBeforeApiExecutionChain() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT);
        final Proxy proxy = new Proxy();
        proxy.setLogging(logging);
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(1).assertValueAt(0, processor -> processor instanceof SubscriptionProcessor);
    }

    @Test
    void shouldReturnPathMappingsPatternBeforeApiExecutionChainWithPathMappingsPattern() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        apiModel.setPathMappings(Map.of("/tot", Pattern.compile("")));
        final Proxy proxy = new Proxy();
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(2)
            .assertValueAt(0, processor -> processor instanceof SubscriptionProcessor)
            .assertValueAt(1, processor -> processor instanceof PathMappingProcessor);
    }

    @Test
    void shouldReturnCorsAfterApiExecutionChainWithAndCors() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Proxy proxy = new Proxy();
        Cors cors = new Cors();
        cors.setEnabled(true);
        proxy.setCors(cors);
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterApiExecution(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("after-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(3)
            .assertValueAt(0, processor -> processor instanceof ShutdownProcessor)
            .assertValueAt(1, processor -> processor instanceof TransactionPostProcessor)
            .assertValueAt(2, processor -> processor instanceof CorsSimpleRequestProcessor);
    }

    @Test
    void shouldReturnLoggingAfterApiExecutionChain() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT);
        final Proxy proxy = new Proxy();
        proxy.setLogging(logging);
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterApiExecution(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("after-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(2)
            .assertValueAt(0, processor -> processor instanceof ShutdownProcessor)
            .assertValueAt(1, processor -> processor instanceof TransactionPostProcessor);
    }

    @Test
    void shouldReturnAllAfterApiExecutionChainProcessorPlusSimpleFailureProcessor() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        apiModel.setPathMappings(Map.of("/tot", Pattern.compile("")));
        final Proxy proxy = new Proxy();
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.onError(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("api-error");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(3)
            .assertValueAt(0, processor -> processor instanceof ShutdownProcessor)
            .assertValueAt(1, processor -> processor instanceof TransactionPostProcessor)
            .assertValueAt(2, processor -> processor instanceof SimpleFailureProcessor);
    }

    @Test
    void shouldReturnEmptyAfterHandleProcessorChain() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Proxy proxy = new Proxy();
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterHandle(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("after-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertNoValues();
    }

    @Test
    void shouldReturnEmptyAfterHandleProcessorChainWhenLoggingModeNone() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.NONE);
        final Proxy proxy = new Proxy();
        proxy.setLogging(logging);
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterHandle(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("after-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertNoValues();
    }

    @Test
    void shouldReturnAfterHandleProcessorChainWithLogging() {
        io.gravitee.definition.model.Api apiModel = new io.gravitee.definition.model.Api();
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT);
        final Proxy proxy = new Proxy();
        proxy.setLogging(logging);
        apiModel.setProxy(proxy);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterHandle(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("after-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(1).assertValueAt(0, processor -> processor instanceof LogResponseProcessor);
    }

    private Flowable<Processor> extractProcessorChain(final ProcessorChain processorChain) {
        return (Flowable<Processor>) ReflectionTestUtils.getField(processorChain, ProcessorChain.class, "processors");
    }
}
