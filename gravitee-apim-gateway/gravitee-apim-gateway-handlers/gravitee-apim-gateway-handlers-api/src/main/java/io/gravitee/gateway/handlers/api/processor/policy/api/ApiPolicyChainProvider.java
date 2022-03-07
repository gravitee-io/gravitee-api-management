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
package io.gravitee.gateway.handlers.api.processor.policy.api;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.flow.policy.PolicyResolver;
import io.gravitee.gateway.handlers.api.processor.policy.AbstractPolicyChainProvider;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.StreamType;
import java.util.List;

/**
 * A policy chain provider based on the policy configuration at the API level.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPolicyChainProvider extends AbstractPolicyChainProvider {

    private final StreamType streamType;

    private final PolicyChainFactory policyChainFactory;

    public ApiPolicyChainProvider(
        final StreamType streamType,
        final PolicyResolver policyResolver,
        final PolicyChainFactory policyChainFactory
    ) {
        super(policyResolver);
        this.streamType = streamType;
        this.policyChainFactory = policyChainFactory;
    }

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> provide(ExecutionContext context) {
        // Calculate the list of policies to apply under this policy chain
        List<PolicyMetadata> policies = policyResolver.resolve(streamType, context);

        return policyChainFactory.create(policies, streamType, context);
    }
}
