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
package io.gravitee.gateway.flow;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.policy.AbstractPolicyChainProvider;
import io.gravitee.gateway.policy.StreamType;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SimpleFlowPolicyChainProvider extends AbstractPolicyChainProvider {

    private final FlowProvider flowProvider;

    public SimpleFlowPolicyChainProvider(
        final StreamType streamType,
        final FlowResolver flowResolver,
        final PolicyChainFactory policyChainFactory,
        final FlowPolicyResolverFactory flowPolicyResolverFactory
    ) {
        this.flowProvider = new SimpleFlowProvider(streamType, flowResolver, policyChainFactory, flowPolicyResolverFactory);
    }

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> provide(ExecutionContext context) {
        return flowProvider.provide(context);
    }
}
