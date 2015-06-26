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
package io.gravitee.gateway.core.policy.builder;

import io.gravitee.gateway.api.Policy;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.policy.PolicyChainBuilder;
import io.gravitee.gateway.core.policy.RequestPolicyChain;
import io.gravitee.gateway.core.policy.impl.AccessControlPolicy;
import io.gravitee.gateway.core.policy.impl.RateLimitPolicy;

import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestPolicyChainBuilder implements PolicyChainBuilder<RequestPolicyChain, Request> {

    @Override
    public RequestPolicyChain newPolicyChain(Request request) {
        Set<Policy> policies = policies();

        return new RequestPolicyChain(policies);
    }

    private Set<Policy> policies() {
        Set<Policy> policies = new HashSet<>();

        policies.add(new AccessControlPolicy());
        policies.add(new RateLimitPolicy());

        return policies;
    }
}
