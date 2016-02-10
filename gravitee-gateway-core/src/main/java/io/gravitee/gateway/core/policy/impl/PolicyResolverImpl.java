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

import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Rule;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.policy.*;
import io.gravitee.plugin.policy.PolicyDefinition;
import io.gravitee.plugin.policy.PolicyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyResolverImpl implements PolicyResolver {

    private final Logger LOGGER = LoggerFactory.getLogger(PolicyResolverImpl.class);

    @Autowired
    private PolicyManager policyManager;

    @Autowired
    private Api api;

    @Autowired
    private PolicyFactory policyFactory;

    @Override
    public List<Policy> resolve(StreamType streamType, Request request, List<Rule> rules) {
        List<Policy> policies = new ArrayList<>();

        rules.stream().filter(rule -> rule.getMethods().contains(request.method())).forEach(rule -> {
            PolicyDefinition policyDefinition = policyManager.getPolicyDefinition(rule.getPolicy().getName());
            if (policyDefinition == null) {
                LOGGER.error("Policy {} can't be found in registry. Unable to apply it for request {}",
                        rule.getPolicy().getName(), request.id());
            } else if (
                    (streamType == StreamType.REQUEST && policyDefinition.onRequestMethod() != null) ||
                            (streamType == StreamType.RESPONSE && policyDefinition.onResponseMethod() != null)){

                Object policyInst = policyFactory.create(policyDefinition, rule.getPolicy().getConfiguration());

                if (policyInst != null) {
                    LOGGER.debug("Policy {} has been added to the chain for request {}", policyDefinition.id(), request.id());
                    policies.add(new PolicyImpl(policyInst, policyDefinition.onRequestMethod(), policyDefinition.onResponseMethod()));
                }
            }
        });

        return policies;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }


}
