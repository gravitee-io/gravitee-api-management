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
package io.gravitee.gateway.handlers.api.policy;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.policy.*;
import io.gravitee.gateway.policy.impl.PolicyChain;
import io.gravitee.gateway.policy.impl.PolicyImpl;
import io.gravitee.gateway.policy.impl.RequestPolicyChain;
import io.gravitee.gateway.policy.impl.ResponsePolicyChain;
import io.gravitee.policy.api.PolicyConfiguration;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class AbstractPolicyChainResolver implements PolicyChainResolver {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected Api api;

    @Autowired
    private PolicyManager policyManager;

    @Autowired
    private PolicyFactory policyFactory;

    @Autowired
    private PolicyConfigurationFactory policyConfigurationFactory;

    @Override
    public PolicyChain resolve(StreamType streamType, Request request, Response response, ExecutionContext executionContext) {
        // Calculate the list of policies to apply under this policy chain
        List<Policy> policies = calculate(streamType, request, response, executionContext);

        if (policies.isEmpty()) {
            return new NoOpPolicyChain(policies, executionContext);
        }

        return (streamType == StreamType.ON_REQUEST) ?
                RequestPolicyChain.create(policies, executionContext) :
                ResponsePolicyChain.create(policies, executionContext);
    }

    protected Policy create(StreamType streamType, String policy, String configuration) {
        PolicyMetadata policyMetadata = policyManager.get(policy);

        if ((streamType == StreamType.ON_REQUEST &&
                        (policyMetadata.method(OnRequest.class) != null || policyMetadata.method(OnRequestContent.class) != null)) ||
                        (streamType == StreamType.ON_RESPONSE && (
                                policyMetadata.method(OnResponse.class) != null || policyMetadata.method(OnResponseContent.class) != null))) {
            PolicyConfiguration policyConfiguration = policyConfigurationFactory.create(
                    policyMetadata.configuration(), configuration);

            // TODO: this should be done only if policy is injectable
            Map<Class<?>, Object> injectables = new HashMap<>(2);
            injectables.put(policyMetadata.configuration(), policyConfiguration);
            if (policyMetadata.context() != null) {
                injectables.put(policyMetadata.context().getClass(), policyMetadata.context());
            }

            Object policyInst = policyFactory.create(policyMetadata, injectables);

            LOGGER.debug("Policy {} has been added to the policy chain", policyMetadata.id());
            return PolicyImpl
                    .target(policyInst)
                    .definition(policyMetadata)
                    .build();
        }

        return null;
    }

    public Api getApi() {
        return api;
    }

    protected abstract List<Policy> calculate(StreamType streamType, Request request, Response response,
                                              ExecutionContext executionContext);
}
