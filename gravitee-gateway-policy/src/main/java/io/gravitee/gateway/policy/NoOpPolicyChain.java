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
package io.gravitee.gateway.policy;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.policy.impl.PolicyChain;

import java.util.Collections;
import java.util.Iterator;

/**
 * A no-op policy chain used to chain an empty policy collection.
 * It immediately returns a successful result when invoking its <code>doNext</code> method.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpPolicyChain extends PolicyChain {

    public NoOpPolicyChain(ExecutionContext executionContext) {
        super(Collections.emptyList(), executionContext);
    }

    @Override
    protected void execute(Policy policy, Object... args) throws PolicyChainException {
        // Nothing to do
    }

    @Override
    protected Iterator<Policy> iterator() {
        return Collections.emptyIterator();
    }
}
