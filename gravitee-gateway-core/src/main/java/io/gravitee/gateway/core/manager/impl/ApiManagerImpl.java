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
package io.gravitee.gateway.core.manager.impl;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.core.definition.ApiDefinition;
import io.gravitee.gateway.core.manager.ApiEvent;
import io.gravitee.gateway.core.manager.ApiManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiManagerImpl implements ApiManager {

    private final Logger logger = LoggerFactory.getLogger(ApiManagerImpl.class);

    @Autowired
    private EventManager eventManager;

    private final Map<String, ApiDefinition> apis = new HashMap<>();

    @Override
    public void add(ApiDefinition apiDefinition) {
        logger.debug("{} has been added", apiDefinition);

        try {
            // TODO: validate API definition
            apis.put(apiDefinition.getName(), apiDefinition);
            eventManager.publishEvent(ApiEvent.CREATE, apiDefinition);
        } catch (IllegalStateException ise) {
            logger.error("{} can not be added to reactor due to previous error", apiDefinition);
        }
    }

    @Override
    public void update(ApiDefinition apiDefinition) {
        logger.debug("{} has been updated", apiDefinition);
        ApiDefinition cachedApi = apis.get(apiDefinition.getName());

        // Update only if certain fields has been updated:
        // - Lifecycle
        // - TargetURL
        // - PublicURL

        try {
            // TODO: validate API definition
            apis.put(apiDefinition.getName(), apiDefinition);
            eventManager.publishEvent(ApiEvent.UPDATE, apiDefinition);
        } catch (IllegalStateException ise) {
            logger.error("{} can not be updated in reactor due to previous error", apiDefinition);
        }
    }

    @Override
    public void remove(String apiName) {
        logger.debug("{} has been removed", apiName);
        apis.remove(apiName);
        eventManager.publishEvent(ApiEvent.REMOVE, apis.get(apiName));
    }

    @Override
    public Map<String, ApiDefinition> apis() {
        return apis;
    }

/*
    public void enhance(Api api) {
        try {
            logger.debug("Trying to enhance {} with policy configurations", api);

            List<PolicyConfiguration> policies = apiRepository.findPoliciesByApi(api.getName());
            if (! policies.isEmpty()) {
                policies.stream().forEach(new Consumer<PolicyConfiguration>() {
                    @Override
                    public void accept(PolicyConfiguration policyConfiguration) {
                        String policy = policyConfiguration.getPolicy();

                        // Check that the policy is locally installed
                        PolicyDefinition policyDefinition = policyManager.getPolicyDefinition(policy);
                        if (policyDefinition == null) {
                            logger.error("Policy {} is not available for {}", policy, api);
                            throw new IllegalStateException("Policy " + policy + " is not available for " + api);
                        }

                        String configuration = policyConfiguration.getConfiguration();
                        if (configuration != null && ! configuration.isEmpty()) {
                            // TODO: Validate configuration against installed policy
                            io.gravitee.gateway.api.policy.PolicyConfiguration internalPolicyConfiguration = policyConfigurationFactory.create(policyDefinition.configuration(), configuration);
                            if (internalPolicyConfiguration == null) {
                                logger.error("Policy configuration for {} and {} can not be parsed", policy, api);
                                throw new IllegalStateException("Policy configuration for {} and {} can not be parsed" + api);
                            }
                        }
                    }
                });
            } else {
                logger.warn("No policy has been configured for {}, skipping enhancement...", api);
            }

        } catch (TechnicalException e) {
            logger.error("Unable to retrieve policy configurations for {} from repository", api, e);
            throw new IllegalStateException("Unable to retrieve policy configuration for " + api);
        }
    }
*/

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }
}
