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

import io.gravitee.gateway.api.Policy;
import io.gravitee.gateway.api.PolicyConfiguration;
import io.gravitee.gateway.core.policy.PolicyBuilder;
import io.gravitee.gateway.core.policy.PolicyDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyBuilderImpl implements PolicyBuilder {

    protected final Logger LOGGER = LoggerFactory.getLogger(PolicyBuilderImpl.class);

    @Override
    public Policy build(PolicyDefinition policyDefinition, String configuration) {
        Class<? extends PolicyConfiguration> policyConfigurationClazz = policyDefinition.configuration();
        Class<? extends Policy> policyClass = policyDefinition.policy();

        Policy policy = null;

        try {
            if (policyClass != null) {
                PolicyConfiguration policyConfiguration = null;
                if (policyConfigurationClazz != null) {
                    if (configuration == null) {
                        LOGGER.error("A configuration is required for policy {}, returning a null policy.", policyDefinition.name());
                        return null;
                    } else {
                        LOGGER.debug("Initializing policy configuration for policy {}", policyDefinition.name());
                        policyConfiguration = null;
                    }
                }

                policy = createInstance(policyClass);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        return policy;
    }

    private <T> T createInstance(Class<T> clazz) throws IllegalAccessException, InstantiationException {
        return clazz.newInstance();
    }
}
