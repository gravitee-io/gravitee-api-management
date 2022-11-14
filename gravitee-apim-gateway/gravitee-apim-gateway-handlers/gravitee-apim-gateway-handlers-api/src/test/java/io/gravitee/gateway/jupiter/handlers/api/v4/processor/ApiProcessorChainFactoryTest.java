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
package io.gravitee.gateway.jupiter.handlers.api.v4.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.handlers.api.processor.cors.CorsPreflightRequestProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.cors.CorsSimpleRequestProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.error.SimpleFailureProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.forward.XForwardedPrefixProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.logging.LogRequestProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.logging.LogResponseProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.pathmapping.PathMappingProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.plan.PlanProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.shutdown.ShutdownProcessor;
import io.gravitee.gateway.jupiter.handlers.api.v4.Api;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error.SimpleFailureMessageProcessor;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error.template.ResponseTemplateBasedFailureMessageProcessor;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Map;
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

    private ApiProcessorChainFactory apiProcessorChainFactory;

    @BeforeEach
    public void beforeEach() {
        when(configuration.getProperty("services.tracing.enabled", Boolean.class, false)).thenReturn(false);
        when(configuration.getProperty("handlers.request.headers.x-forwarded-prefix", Boolean.class, false)).thenReturn(false);
        apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node);
    }

    @Test
    void shouldReturnEmptyBeforeHandleProcessorChain() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        apiModel.setListeners(List.of(new HttpListener()));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeHandle(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-before-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertNoValues();
    }

    @Test
    void shouldReturnEmptyBeforeHandleProcessorChainWhenLoggingModeNone() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        final HttpListener httpListener = new HttpListener();
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.NONE);
        httpListener.setLogging(logging);
        apiModel.setListeners(List.of(httpListener));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeHandle(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-before-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertNoValues();
    }

    @Test
    void shouldReturnBeforeHandleProcessorChainWithLogging() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        final HttpListener httpListener = new HttpListener();
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT);
        httpListener.setLogging(logging);
        apiModel.setListeners(List.of(httpListener));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeHandle(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-before-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(1).assertValueAt(0, processor -> processor instanceof LogRequestProcessor);
    }

    @Test
    void shouldReturnEmptyBeforeApiExecutionChainWithNoListener() {
        Api api = new Api(new io.gravitee.definition.model.v4.Api());
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(0);
    }

    @Test
    void shouldReturnEmptyBeforeApiExecutionProcessorChainWithNoHttpListener() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        apiModel.setListeners(List.of(new SubscriptionListener()));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(0);
    }

    @Test
    void shouldReturnCorsBeforeApiExecutionChainWithHttpListenerAndCors() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        Cors cors = new Cors();
        cors.setEnabled(true);
        httpListener.setCors(cors);
        apiModel.setListeners(List.of(httpListener));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(2)
            .assertValueAt(0, processor -> processor instanceof CorsPreflightRequestProcessor)
            .assertValueAt(1, processor -> processor instanceof PlanProcessor);
    }

    @Test
    void shouldReturnBeforeApiExecutionChainWithHttpListener() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT);
        httpListener.setLogging(logging);
        apiModel.setListeners(List.of(httpListener));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(1).assertValueAt(0, processor -> processor instanceof PlanProcessor);
    }

    @Test
    void shouldReturnXForwardedBeforeApiExecutionChainWithHttpListenerAndOverrideXForwarded() {
        when(configuration.getProperty("handlers.request.headers.x-forwarded-prefix", Boolean.class, false)).thenReturn(true);
        apiProcessorChainFactory = new ApiProcessorChainFactory(configuration, node);

        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        apiModel.setListeners(List.of(httpListener));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.beforeApiExecution(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-before-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(2)
            .assertValueAt(0, processor -> processor instanceof XForwardedPrefixProcessor)
            .assertValueAt(1, processor -> processor instanceof PlanProcessor);
    }

    @Test
    void shouldReturnEmptyAfterApiExecutionChainWithNoListener() {
        Api api = new Api(new io.gravitee.definition.model.v4.Api());
        ProcessorChain processorChain = apiProcessorChainFactory.afterApiExecution(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-after-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(1).assertValueAt(0, processor -> processor instanceof ShutdownProcessor);
    }

    @Test
    void shouldReturnShutdownAfterApiExecutionChainWithNoHttpListener() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        apiModel.setListeners(List.of(new SubscriptionListener()));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterApiExecution(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-after-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(1).assertValueAt(0, processor -> processor instanceof ShutdownProcessor);
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
        ProcessorChain processorChain = apiProcessorChainFactory.afterApiExecution(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-after-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(2)
            .assertValueAt(0, processor -> processor instanceof ShutdownProcessor)
            .assertValueAt(1, processor -> processor instanceof CorsSimpleRequestProcessor);
    }

    @Test
    void shouldReturnLoggingAfterApiExecutionChainWithHttpListener() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT);
        httpListener.setLogging(logging);
        apiModel.setListeners(List.of(httpListener));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterApiExecution(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-after-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(1).assertValueAt(0, processor -> processor instanceof ShutdownProcessor);
    }

    @Test
    void shouldReturnPathMappingsPatternAfterApiExecutionChainWithHttpListenerAndPathMappingsPattern() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        httpListener.setPathMappings(Set.of("/tot"));
        apiModel.setListeners(List.of(httpListener));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterApiExecution(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-after-api-execution");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(2)
            .assertValueAt(0, processor -> processor instanceof ShutdownProcessor)
            .assertValueAt(1, processor -> processor instanceof PathMappingProcessor);
    }

    @Test
    void shouldReturnAllAfterApiExecutionChainProcessorPlusSimpleFailureProcessor() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        HttpListener httpListener = new HttpListener();
        httpListener.setPathMappings(Set.of("/tot"));
        apiModel.setListeners(List.of(httpListener));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.onError(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-api-error");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(3)
            .assertValueAt(0, processor -> processor instanceof ShutdownProcessor)
            .assertValueAt(1, processor -> processor instanceof PathMappingProcessor)
            .assertValueAt(2, processor -> processor instanceof SimpleFailureProcessor);
    }

    @Test
    void shouldReturnSimpleFailureProcessorChainWithResponseTemplate() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.onMessage(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-api-message");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(1)
            .assertValueAt(0, processor -> processor instanceof SimpleFailureMessageProcessor);
    }

    @Test
    void shouldReturnResponseTemplateFailureProcessorChainWithResponseTemplate() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        apiModel.setResponseTemplates(Map.of("test", Map.of("test", new ResponseTemplate())));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.onMessage(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-api-message");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(1)
            .assertValueAt(0, processor -> processor instanceof ResponseTemplateBasedFailureMessageProcessor);
    }

    @Test
    void shouldReturnEmptyAfterHandleProcessorChain() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        apiModel.setListeners(List.of(new HttpListener()));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterHandle(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-after-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertNoValues();
    }

    @Test
    void shouldReturnEmptyAfterHandleProcessorChainWhenLoggingModeNone() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        final HttpListener httpListener = new HttpListener();
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.NONE);
        httpListener.setLogging(logging);
        apiModel.setListeners(List.of(httpListener));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterHandle(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-after-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertNoValues();
    }

    @Test
    void shouldReturnAfterHandleProcessorChainWithLogging() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        final HttpListener httpListener = new HttpListener();
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT);
        httpListener.setLogging(logging);
        apiModel.setListeners(List.of(httpListener));
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiProcessorChainFactory.afterHandle(api);
        assertThat(processorChain.getId()).isEqualTo("processor-chain-after-api-handle");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors.test().assertComplete().assertValueCount(1).assertValueAt(0, processor -> processor instanceof LogResponseProcessor);
    }

    private Flowable<Processor> extractProcessorChain(final ProcessorChain processorChain) {
        return (Flowable<Processor>) ReflectionTestUtils.getField(processorChain, ProcessorChain.class, "processors");
    }
}
