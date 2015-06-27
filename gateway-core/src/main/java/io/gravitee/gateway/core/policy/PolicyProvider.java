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
package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.Policy;
import io.gravitee.gateway.core.policy.impl.AccessControlPolicy;
import io.gravitee.gateway.core.policy.impl.RateLimitPolicy;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO: Policy provider should be replace by getting policies from the API configuration
 * This policy provider comes only with "default" policies.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyProvider {

    private final Set<Policy> policies = new HashSet();

    {
        policies.add(new AccessControlPolicy());
        policies.add(new RateLimitPolicy());
    }

    public Set<Policy> getPolicies() {
        return policies;
    }
}
