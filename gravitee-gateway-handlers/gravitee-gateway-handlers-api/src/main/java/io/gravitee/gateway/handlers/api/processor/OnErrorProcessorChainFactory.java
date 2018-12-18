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
package io.gravitee.gateway.handlers.api.processor;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.core.processor.StreamableProcessorDecorator;
import io.gravitee.gateway.core.processor.chain.DefaultStreamableProcessorChain;
import io.gravitee.gateway.core.processor.chain.StreamableProcessorChain;
import io.gravitee.gateway.handlers.api.processor.cors.CorsSimpleRequestProcessor;

import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OnErrorProcessorChainFactory extends ApiProcessorChainFactory {

    private List<StreamableProcessor<ExecutionContext, Buffer>> processors;

    public void afterPropertiesSet() {
        if (api.getProxy().getCors() != null && api.getProxy().getCors().isEnabled()) {
            processors = Collections.singletonList(
                    new StreamableProcessorDecorator<>(new CorsSimpleRequestProcessor(api.getProxy().getCors())));
        } else {
            processors = Collections.emptyList();
        }
    }

    @Override
    public StreamableProcessorChain<ExecutionContext, Buffer, StreamableProcessor<ExecutionContext, Buffer>> create() {
        return new DefaultStreamableProcessorChain<>(processors);
    }
}