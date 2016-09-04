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
package io.gravitee.gateway.policy.impl;

import io.gravitee.definition.model.Rule;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.policy.*;
import io.gravitee.policy.api.PolicyConfiguration;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyResolverImpl implements PolicyResolver {

    private final Logger LOGGER = LoggerFactory.getLogger(PolicyResolverImpl.class);

    @Autowired
    private PolicyManager policyManager;

    @Autowired
    private PolicyFactory policyFactory;

    @Autowired
    private PolicyConfigurationFactory policyConfigurationFactory;

    @Override
    public List<Policy> resolve(StreamType streamType, Request request, List<Rule> rules) {
        List<Policy> policies = new ArrayList<>();

        rules.stream().filter(rule -> rule.isEnabled() && rule.getMethods().contains(request.method())).forEach(rule -> {
            PolicyMetadata policyMetadata = policyManager.get(rule.getPolicy().getName());
            if (policyMetadata == null) {
                LOGGER.error("Policy {} can't be found in registry. Unable to apply it for request {}",
                        rule.getPolicy().getName(), request.id());
            } else if (
                    ((streamType == StreamType.ON_REQUEST &&
                            (policyMetadata.method(OnRequest.class) != null || policyMetadata.method(OnRequestContent.class) != null)) ||
                            (streamType == StreamType.ON_RESPONSE && (
                                    policyMetadata.method(OnResponse.class) != null || policyMetadata.method(OnResponseContent.class) != null)))) {

                PolicyConfiguration policyConfiguration = policyConfigurationFactory.create(
                        policyMetadata.configuration(), rule.getPolicy().getConfiguration());

                // TODO: this should be done only if policy is injectable
                Map<Class<?>, Object> injectables = new HashMap<>(2);
                injectables.put(policyMetadata.configuration(), policyConfiguration);
                if (policyMetadata.context() != null) {
                    injectables.put(policyMetadata.context().getClass(), policyMetadata.context());
                }

                Object policyInst = policyFactory.create(policyMetadata, injectables);

                if (policyInst != null) {
                    LOGGER.debug("Policy {} has been added to the chain for request {}", policyMetadata.id(), request.id());
                    policies.add(PolicyImpl
                            .target(policyInst)
                            .definition(policyMetadata)
                            .build());
                }
            }
        });

        return policies;
    }
}
