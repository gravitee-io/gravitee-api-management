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
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.policy.impl.PolicyChain;
import io.gravitee.gateway.policy.impl.RequestPolicyChain;
import io.gravitee.gateway.policy.impl.ResponsePolicyChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractPolicyChainResolver implements PolicyChainResolver {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PolicyManager policyManager;

    @Override
    public PolicyChain resolve(StreamType streamType, Request request, Response response, ExecutionContext executionContext) {
        // Calculate the list of policies to apply under this policy chain
        List<Policy> policies = calculate(streamType, request, response, executionContext);

        if (policies.isEmpty()) {
            return new NoOpPolicyChain(executionContext);
        }

        return (streamType == StreamType.ON_REQUEST) ?
                RequestPolicyChain.create(policies, executionContext) :
                ResponsePolicyChain.create(policies, executionContext);
    }

    protected Policy create(StreamType streamType, String policy, String configuration) {
        return policyManager.create(streamType, policy, configuration);
    }

    protected abstract List<Policy> calculate(StreamType streamType, Request request, Response response,
                                              ExecutionContext executionContext);

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> provide(ExecutionContext context) {
        return resolve(StreamType.ON_REQUEST, context.request(), context.response(), context);
    }

    public void setPolicyManager(PolicyManager policyManager) {
        this.policyManager = policyManager;
    }
}
