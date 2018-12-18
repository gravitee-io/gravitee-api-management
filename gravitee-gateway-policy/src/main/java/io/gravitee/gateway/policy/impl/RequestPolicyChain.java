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
package io.gravitee.gateway.policy.impl;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyChainException;
import io.gravitee.gateway.policy.PolicyException;

import java.util.Iterator;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestPolicyChain extends StreamablePolicyChain {

    private RequestPolicyChain(final List<Policy> policies, final ExecutionContext executionContext) {
        super(policies, executionContext);
    }

    public static RequestPolicyChain create(List<Policy> policies, ExecutionContext executionContext) {
        return new RequestPolicyChain(policies, executionContext);
    }

    @Override
    protected void execute(Policy policy, Object... args) throws PolicyChainException {
        try {
            policy.onRequest(args);
        } catch (PolicyException pe) {
            throw new PolicyChainException(pe);
        }
    }

    @Override
    protected ReadWriteStream<Buffer> stream(Policy policy, Object... args) throws Exception {
        return policy.onRequestContent(args);
    }

    @Override
    public Iterator<Policy> iterator() {
        return policies.iterator();
    }
}
