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

import io.gravitee.gateway.jupiter.api.hook.ProcessorHook;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.handlers.api.v4.Api;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error.SimpleFailureMessageProcessor;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error.template.ResponseTemplateBasedFailureMessageProcessor;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiMessageProcessorChainFactory {

    private final List<ProcessorHook> processorHooks = new ArrayList<>();

    public ProcessorChain messageProcessorChain(final Api api) {
        List<Processor> postProcessorList = new ArrayList<>();

        if (api.getDefinition().getResponseTemplates() != null && !api.getDefinition().getResponseTemplates().isEmpty()) {
            postProcessorList.add(ResponseTemplateBasedFailureMessageProcessor.instance());
        } else {
            postProcessorList.add(SimpleFailureMessageProcessor.instance());
        }

        ProcessorChain processorChain = new ProcessorChain("message-processor-chain-api", postProcessorList);
        processorChain.addHooks(processorHooks);
        return processorChain;
    }
}
