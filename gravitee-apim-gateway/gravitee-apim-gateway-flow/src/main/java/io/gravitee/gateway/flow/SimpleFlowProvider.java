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
package io.gravitee.gateway.flow;

import io.gravitee.definition.model.flow.FlowV2;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.EmptyStreamableProcessor;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.core.processor.chain.DefaultStreamableProcessorChain;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.policy.impl.OrderedPolicyChain;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SimpleFlowProvider implements FlowProvider {

    private final StreamType streamType;

    private final FlowResolver flowResolver;

    private final PolicyChainFactory policyChainFactory;
    private final FlowPolicyResolverFactory flowPolicyResolverFactory;

    public SimpleFlowProvider(
        final StreamType streamType,
        final FlowResolver flowResolver,
        final PolicyChainFactory policyChainFactory,
        final FlowPolicyResolverFactory flowPolicyResolverFactory
    ) {
        this.streamType = streamType;
        this.flowResolver = flowResolver;
        this.policyChainFactory = policyChainFactory;
        this.flowPolicyResolverFactory = flowPolicyResolverFactory;
    }

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> provide(ExecutionContext context) {
        List<FlowV2> flows = flowResolver.resolve(context);

        if (flows != null && !flows.isEmpty()) {
            final List<StreamableProcessor<ExecutionContext, Buffer>> chain = new ArrayList<>(flows.size());

            for (FlowV2 flow : flows) {
                chain.add(
                    policyChainFactory.create(
                        flowPolicyResolverFactory.create(flow).resolve(streamType, context),
                        streamType,
                        context,
                        policies -> OrderedPolicyChain.create(policies, context)
                    )
                );
            }

            return new DefaultStreamableProcessorChain<>(chain);
        }

        return new EmptyStreamableProcessor<>();
    }
}
