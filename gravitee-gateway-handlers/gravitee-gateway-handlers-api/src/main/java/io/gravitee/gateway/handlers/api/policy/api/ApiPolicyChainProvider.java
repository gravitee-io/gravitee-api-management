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
package io.gravitee.gateway.handlers.api.policy.api;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.policy.*;
import io.gravitee.gateway.policy.impl.RequestPolicyChain;
import io.gravitee.gateway.policy.impl.ResponsePolicyChain;

import java.util.List;

/**
 * A policy chain provider based on the policy configuration at the API level.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPolicyChainProvider extends AbstractPolicyChainProvider {

    private final StreamType streamType;

    public ApiPolicyChainProvider(final StreamType streamType, final PolicyResolver policyResolver) {
        super(policyResolver);
        this.streamType = streamType;
    }

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> provide(ExecutionContext context) {
        // Calculate the list of policies to apply under this policy chain
        List<Policy> policies = policyResolver.resolve(streamType, context);

        if (policies.isEmpty()) {
            return new NoOpPolicyChain(context);
        }

        return (streamType == StreamType.ON_REQUEST) ?
                RequestPolicyChain.create(policies, context) :
                ResponsePolicyChain.create(policies, context);
    }
}
