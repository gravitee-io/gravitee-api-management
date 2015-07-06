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

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.policy.ResponsePolicyChain;
import io.gravitee.model.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ResponsePolicyChainBuilder extends AbstractPolicyChainBuilder<ResponsePolicyChain> {

    private final Logger LOGGER = LoggerFactory.getLogger(ResponsePolicyChainBuilder.class);

    @Override
    public ResponsePolicyChain newPolicyChain(Request request) {
        Set<io.gravitee.gateway.api.Policy> appliedPolicies = new HashSet<>();

        Map<String, Policy> definedPolicies = getApi().getPolicies();
        if (definedPolicies != null) {
            for (Map.Entry<String, Policy> entry : definedPolicies.entrySet()){
                Class<? extends io.gravitee.gateway.api.Policy> policy = getRegistry().getPolicy(entry.getKey());
                if (policy == null) {
                    LOGGER.error("Policy {} can't be found in registry. Unable to apply it fo request {}", entry.getKey(), request.id());
                } else {
                    try {
                        appliedPolicies.add(policy.newInstance());
                    } catch (InstantiationException | IllegalAccessException ex) {
                        LOGGER.error("Unable to create an instance of Policy class {}", policy.getName(), ex);
                    }
                }
            }
        }

        return new ResponsePolicyChain(appliedPolicies);
    }
}
