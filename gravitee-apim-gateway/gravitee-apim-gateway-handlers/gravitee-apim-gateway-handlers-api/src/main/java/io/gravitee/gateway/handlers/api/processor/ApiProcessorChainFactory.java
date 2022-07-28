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
import io.gravitee.gateway.core.processor.Processor;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.core.processor.chain.ProcessorChainFactory;
import io.gravitee.gateway.core.processor.chain.StreamableProcessorChain;
import io.gravitee.gateway.core.processor.provider.ProcessorProvider;
import io.gravitee.gateway.core.processor.provider.StreamableProcessorProviderChain;
import io.gravitee.gateway.core.processor.provider.StreamableProcessorSupplier;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.handlers.api.definition.Api;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class ApiProcessorChainFactory
    implements ProcessorChainFactory<StreamableProcessorChain<ExecutionContext, Buffer, StreamableProcessor<ExecutionContext, Buffer>>> {

    protected final Api api;

    protected final PolicyChainFactory policyChainFactory;

    private final List<ProcessorProvider<ExecutionContext, StreamableProcessor<ExecutionContext, Buffer>>> providers = new ArrayList<>();

    protected ApiProcessorChainFactory(final Api api, final PolicyChainFactory policyChainFactory) {
        this.api = api;
        this.policyChainFactory = policyChainFactory;
    }

    protected void add(ProcessorProvider<ExecutionContext, StreamableProcessor<ExecutionContext, Buffer>> provider) {
        this.providers.add(provider);
    }

    protected void addAll(List<ProcessorProvider<ExecutionContext, StreamableProcessor<ExecutionContext, Buffer>>> providers) {
        this.providers.addAll(providers);
    }

    protected void add(Supplier<Processor<ExecutionContext>> supplier) {
        add(new StreamableProcessorSupplier<>(supplier));
    }

    @Override
    public StreamableProcessorChain<ExecutionContext, Buffer, StreamableProcessor<ExecutionContext, Buffer>> create() {
        return new StreamableProcessorProviderChain<>(providers);
    }
}
