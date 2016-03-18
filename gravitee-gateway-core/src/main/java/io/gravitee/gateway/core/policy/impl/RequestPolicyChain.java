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
package io.gravitee.gateway.core.policy.impl;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.policy.Policy;
import io.gravitee.policy.api.PolicyResult;

import java.util.Iterator;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestPolicyChain extends AbstractPolicyChain {

    private static final PolicyResult SUCCESS_POLICY_CHAIN = new SuccessPolicyResult();

    private RequestPolicyChain(final List<Policy> policies, final ExecutionContext executionContext) {
        super(policies, executionContext);
    }

    public static RequestPolicyChain create(List<Policy> policies, ExecutionContext executionContext) {
        return new RequestPolicyChain(policies, executionContext);
    }

    @Override
    public void doNext(Request request, Response response) {
        if (iterator.hasNext()) {
            super.doNext(request, response);
        } else {
            resultHandler.handle(SUCCESS_POLICY_CHAIN);
        }
    }

    @Override
    protected void execute(Policy policy, Object... args) throws Exception {
        policy.onRequest(args);
    }

    @Override
    public Iterator<Policy> iterator() {
        return policies.iterator();
    }
}
