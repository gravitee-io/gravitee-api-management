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

import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.handlers.api.v4.Api;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error.SimpleFailureMessageProcessor;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error.template.ResponseTemplateBasedFailureMessageProcessor;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApiMessageProcessorChainFactoryTest {

    private ApiMessageProcessorChainFactory apiMessageProcessorChainFactory;

    @BeforeEach
    public void beforeEach() {
        apiMessageProcessorChainFactory = new ApiMessageProcessorChainFactory();
    }

    @Test
    void shouldReturnSimpleFailureProcessorChainWithResponseTemplate() {
        io.gravitee.definition.model.v4.Api apiModel = new io.gravitee.definition.model.v4.Api();
        Api api = new Api(apiModel);
        ProcessorChain processorChain = apiMessageProcessorChainFactory.messageProcessorChain(api);
        assertThat(processorChain.getId()).isEqualTo("message-processor-chain-api");
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
        ProcessorChain processorChain = apiMessageProcessorChainFactory.messageProcessorChain(api);
        assertThat(processorChain.getId()).isEqualTo("message-processor-chain-api");
        Flowable<Processor> processors = extractProcessorChain(processorChain);
        processors
            .test()
            .assertComplete()
            .assertValueCount(1)
            .assertValueAt(0, processor -> processor instanceof ResponseTemplateBasedFailureMessageProcessor);
    }

    private Flowable<Processor> extractProcessorChain(final ProcessorChain processorChain) {
        return (Flowable<Processor>) ReflectionTestUtils.getField(processorChain, ProcessorChain.class, "processors");
    }
}
