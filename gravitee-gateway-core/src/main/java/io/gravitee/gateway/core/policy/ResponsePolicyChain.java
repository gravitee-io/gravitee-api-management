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

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.policy.PolicyChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ListIterator;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ResponsePolicyChain implements PolicyChain {

    private final Logger LOGGER = LoggerFactory.getLogger(ResponsePolicyChain.class);

    private final List<ExecutablePolicy> policies;

    public ResponsePolicyChain(final List<ExecutablePolicy> policies) {
        this.policies = policies;
    }

    @Override
    public void doNext(final Request request, final Response response) {
        if (iterator().hasPrevious()) {
            ExecutablePolicy policy = iterator().previous();
            if (policy.getPolicyDefinition().onResponseMethod() != null) {
                try {
                    policy.onResponse(request, response, this);
                } catch (Exception ex) {
                    LOGGER.error("Unexpected error while running onResponse on policy", ex);
                }
            }
        }
    }

    @Override
    public void doError(Throwable throwable) {

    }

    public ListIterator<ExecutablePolicy> iterator() {
        return policies.listIterator(policies.size());
    }
}
