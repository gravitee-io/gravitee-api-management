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
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.policy.impl.processor.PolicyChainProcessorFailure;
import io.gravitee.policy.api.PolicyResult;

/**
 * A 'direct' policy chain used to chain an empty policy collection.
 * It immediately returns a policy result when invoking its <code>doNext</code> method.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DirectPolicyChain extends NoOpPolicyChain {

    private final PolicyResult policyResult;

    public DirectPolicyChain(PolicyResult policyResult, ExecutionContext executionContext) {
        super(executionContext);
        this.policyResult = policyResult;
    }

    @Override
    public void doNext(Request request, Response response) {
        errorHandler.handle(new PolicyChainProcessorFailure(policyResult));
    }
}