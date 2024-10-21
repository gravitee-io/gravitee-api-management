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
package io.gravitee.gateway.reactive.handlers.api.v4.processor;

import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor.DEFAULT_CLIENT_IDENTIFIER_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
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
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.logging.LogInitProcessor;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.logging.LogRequestProcessor;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.logging.LogResponseProcessor;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApiProcessorChainFactoryTest {

    @Mock
    private Configuration configuration;

    @Mock
    private Node node;

    @Mock
    private ReporterService reporterService;

    private ApiProcessorChainFactory apiProcessorChainFactory;

    @BeforeEach
    public void beforeEach() {
        when(configuration.getProperty("handlers.request.headers.x-forwarded-prefix", Boolean.class, false)).thenReturn(false);
        when(configuration.getProperty("handlers.request.client.header", String.class, DEFAULT_CLIENT_IDENTIFIER_HEADER))
            .thenReturn(DEFAULT_CLIENT_IDENTIFIER_HEADER);
        apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node, reporterService);
    }

    @Test
    void shouldReturnEmptyBeforeHandleProcessorChain() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        apiModel.setListeners(List.of(new HttpListener()));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeHandle(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertNoValues();
    }

    @Test
    void shouldReturnEmptyBeforeHandleProcessorChainWhenLoggingModeNone() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        final Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(false).endpoint(false).build());
        analytics.setLogging(logging);
        apiModel.setAnalytics(analytics);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeHandle(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertNoValues();
    }

    @Test
    void shouldReturnBeforeHandleProcessorChainWithLogging() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        final Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(false).build());
        analytics.setLogging(logging);
        apiModel.setAnalytics(analytics);
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
    void shouldReturnEmptyBeforeApiExecutionChainWithNoListener() {
        Api api = new Api(new io.gravitee.definition.model.v4.Api());
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(0);
    }

    @Test
    void shouldReturnEmptyBeforeApiExecutionProcessorChainWithNoHttpListener() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        apiModel.setListeners(List.of(new SubscriptionListener()));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(1);
    }

    @Test
    void shouldReturnCorsBeforeSecurityChainWithHttpListenerAndCors() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        Cors cors = new Cors();
        cors.setEnabled(true);
        httpListener.setCors(cors);
        apiModel.setListeners(List.of(httpListener));
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
    void shouldReturnXForwardedBeforeApiExecutionChainWithHttpListenerAndOverrideXForwarded() {
        when(configuration.getProperty("handlers.request.headers.x-forwarded-prefix", Boolean.class, false)).thenReturn(true);
        apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node, reporterService);

        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        apiModel.setListeners(List.of(httpListener));
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
        apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node, reporterService);

        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        apiModel.setListeners(List.of(httpListener));
        Flow flow = new Flow();
        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setPath("/products/:productId");
        httpSelector.setPathOperator(Operator.STARTS_WITH);
        httpSelector.setMethods(Set.of());
        flow.setSelectors(List.of(httpSelector));
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
    void shouldReturnBeforeApiExecutionChainWithHttpListener() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        apiModel.setListeners(List.of(httpListener));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(1).assertValueAt(0, processor -> processor instanceof SubscriptionProcessor);
    }

    @Test
    void shouldReturnPathMappingsPatternBeforeApiExecutionChainWithHttpListenerAndPathMappingsPattern() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        httpListener.setPathMappings(Set.of("/test"));
        apiModel.setListeners(List.of(httpListener));
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
    void shouldReturnEmptyAfterApiExecutionChainWithNoListener() {
        Api api = new Api(new io.gravitee.definition.model.v4.Api());
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
    void shouldReturnShutdownAfterApiExecutionChainWithNoHttpListener() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        apiModel.setListeners(List.of(new SubscriptionListener()));
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
    void shouldReturnCorsAfterApiExecutionChainWithHttpListenerAndCors() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        Cors cors = new Cors();
        cors.setEnabled(true);
        httpListener.setCors(cors);
        apiModel.setListeners(List.of(httpListener));
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
    void shouldReturnShutdownAfterApiExecutionChainWithHttpListener() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
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
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        httpListener.setPathMappings(Set.of("/tot"));
        apiModel.setListeners(List.of(httpListener));
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
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        apiModel.setListeners(List.of(new HttpListener()));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterHandle(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("after-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertNoValues();
    }

    @Test
    void shouldReturnEmptyAfterHandleProcessorChainWhenLoggingModeNone() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(false).endpoint(false).build());
        analytics.setLogging(logging);
        apiModel.setAnalytics(analytics);
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterHandle(api, TracingContext.noop());
        assertThat(processorChain.getId()).isEqualTo("after-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertNoValues();
    }

    @Test
    void shouldReturnAfterHandleProcessorChainWithLogging() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(false).build());
        analytics.setLogging(logging);
        apiModel.setAnalytics(analytics);
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
