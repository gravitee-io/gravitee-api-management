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
import io.gravitee.gateway.core.processor.chain.StreamableProcessorChain;
import io.gravitee.gateway.core.processor.provider.ProcessorProvider;
import io.gravitee.gateway.core.processor.provider.ProcessorSupplier;
import io.gravitee.gateway.core.processor.provider.StreamableProcessorProviderChain;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyChainResolver;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyChainResolver;
import io.gravitee.gateway.handlers.api.processor.alert.AlertProcessorSupplier;
import io.gravitee.gateway.handlers.api.processor.cors.CorsSimpleRequestProcessor;
import io.gravitee.gateway.handlers.api.processor.pathmapping.PathMappingProcessor;
import io.gravitee.gateway.policy.PolicyChainResolver;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.plugin.alert.AlertEngineService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseProcessorChainFactory extends ApiProcessorChainFactory {

    @Autowired
    private AlertEngineService alertEngineService;

    private final List<ProcessorProvider<ExecutionContext, StreamableProcessor<ExecutionContext, Buffer>>> providers = new ArrayList<>();

    public void afterPropertiesSet() {
        PolicyChainResolver apiResponsePolicyResolver = new ApiPolicyChainResolver(StreamType.ON_RESPONSE);
        PolicyChainResolver planPolicyResolver = new PlanPolicyChainResolver(StreamType.ON_RESPONSE);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(apiResponsePolicyResolver);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(planPolicyResolver);
        providers.add(apiResponsePolicyResolver);
        providers.add(planPolicyResolver);

        if (api.getProxy().getCors() != null && api.getProxy().getCors().isEnabled()) {
            providers.add(new ProcessorSupplier<>(() ->
                    new StreamableProcessorDecorator<>(new CorsSimpleRequestProcessor(api.getProxy().getCors()))));
        }

        if (api.getPathMappings() != null && !api.getPathMappings().isEmpty()) {
            providers.add(new ProcessorSupplier<>(() ->
                    new StreamableProcessorDecorator<>(new PathMappingProcessor(api.getPathMappings()))));
        }

        if (alertEngineService != null) {
            AlertProcessorSupplier supplier = new AlertProcessorSupplier();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(supplier);
            providers.add(new ProcessorSupplier<>(() -> new StreamableProcessorDecorator<>(supplier.get())));
        }
    }

    @Override
    public StreamableProcessorChain<ExecutionContext, Buffer, StreamableProcessor<ExecutionContext, Buffer>> create() {
        return new StreamableProcessorProviderChain<>(providers);
    }
}
