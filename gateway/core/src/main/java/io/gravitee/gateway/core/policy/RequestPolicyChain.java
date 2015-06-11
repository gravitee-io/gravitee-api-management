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
import io.gravitee.gateway.api.Request;

import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestPolicyChain extends AbstractPolicyChain<Request> {

    public RequestPolicyChain(Set<Policy> policies) {
        super(policies);
    }

    @Override
    public void apply(Request request) {
        if (iterator().hasNext()) {
            Policy first = iterator().next();
            first.apply(request, this);
        }
    }
}
