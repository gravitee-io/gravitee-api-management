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
import java.util.ListIterator;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponsePolicyChain extends StreamablePolicyChain {

    private ResponsePolicyChain(final List<Policy> policies, final ExecutionContext executionContext) {
        super(policies, executionContext);
    }

    public static ResponsePolicyChain create(List<Policy> policies, ExecutionContext executionContext) {
        return new ResponsePolicyChain(policies, executionContext);
    }

    @Override
    protected void execute(Policy policy, Object... args) throws PolicyChainException {
        try {
            policy.onResponse(args);
        } catch (PolicyException pe) {
            throw new PolicyChainException(pe);
        }
    }

    @Override
    protected ReadWriteStream<Buffer> stream(Policy policy, Object... args) throws Exception {
        return policy.onResponseContent(args);
    }

    @Override
    public Iterator<Policy> iterator() {
        final ListIterator<Policy> listIterator = policies.listIterator(policies.size());
        return new Iterator<Policy>() {
            @Override
            public boolean hasNext() { return listIterator.hasPrevious(); }

            @Override
            public Policy next() { return listIterator.previous(); }

            @Override
            public void remove() { listIterator.remove(); }
        };
    }
}
