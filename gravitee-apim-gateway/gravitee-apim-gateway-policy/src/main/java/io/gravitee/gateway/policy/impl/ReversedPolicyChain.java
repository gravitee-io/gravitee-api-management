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
import io.gravitee.gateway.policy.Policy;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A specific {@link io.gravitee.policy.api.PolicyChain} which is used to execute policies in their reverse order.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReversedPolicyChain extends StreamablePolicyChain {

    private ReversedPolicyChain(final List<Policy> policies, final ExecutionContext executionContext) {
        super(policies, executionContext);
    }

    public static ReversedPolicyChain create(final List<Policy> policies, final ExecutionContext executionContext) {
        return new ReversedPolicyChain(policies, executionContext);
    }

    @Override
    public Iterator<Policy> iterator() {
        final ListIterator<Policy> listIterator = policies.listIterator(policies.size());
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return listIterator.hasPrevious();
            }

            @Override
            public Policy next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return listIterator.previous();
            }

            @Override
            public void remove() {
                listIterator.remove();
            }
        };
    }
}
