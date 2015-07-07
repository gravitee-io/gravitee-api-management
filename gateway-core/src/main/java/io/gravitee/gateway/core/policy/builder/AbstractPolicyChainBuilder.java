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
import io.gravitee.gateway.api.PolicyChain;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.policy.PolicyBuilder;
import io.gravitee.gateway.core.policy.PolicyChainBuilder;
import io.gravitee.gateway.core.policy.PolicyDefinition;
import io.gravitee.gateway.core.policy.PolicyRegistry;
import io.gravitee.model.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractPolicyChainBuilder<T extends PolicyChain> implements PolicyChainBuilder<T, Request> {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractPolicyChainBuilder.class);

    @Autowired
    private Api api;

    @Autowired
    private PolicyRegistry policyRegistry;

    @Autowired
    private PolicyBuilder policyBuilder;

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public PolicyRegistry getPolicyRegistry() {
        return policyRegistry;
    }

    public void setPolicyRegistry(PolicyRegistry policyRegistry) {
        this.policyRegistry = policyRegistry;
    }

    public PolicyBuilder getPolicyBuilder() {
        return policyBuilder;
    }

    public void setPolicyBuilder(PolicyBuilder policyBuilder) {
        this.policyBuilder = policyBuilder;
    }

    protected Set<Policy> calculatePolicies(Request request) {
        Set<io.gravitee.gateway.api.Policy> policies = new HashSet<>();

        Map<String, io.gravitee.model.Policy> definedPolicies = getApi().getPolicies();
        if (definedPolicies != null) {
            definedPolicies.entrySet().stream().filter(entry -> entry.getValue() != null).forEach(entry -> {
                PolicyDefinition policyDefinition = getPolicyRegistry().getPolicy(entry.getKey());
                if (policyDefinition == null) {
                    LOGGER.error("Policy {} can't be found in policyRegistry. Unable to apply it fo request {}", entry.getKey(), request.id());
                } else {
                    Policy policy =
                            createPolicy(policyDefinition, entry.getValue().getConfiguration());

                    if (policy != null) {
                        LOGGER.debug("Policy {} has been added to the flow for request {}", policyDefinition.name(), request.id());
                        policies.add(policy);
                    }
                }
            });
        }

        return policies;
    }

    private Policy createPolicy(PolicyDefinition policyDefinition, String policyConfiguration) {
        return policyBuilder.build(policyDefinition, policyConfiguration);
    }
}
