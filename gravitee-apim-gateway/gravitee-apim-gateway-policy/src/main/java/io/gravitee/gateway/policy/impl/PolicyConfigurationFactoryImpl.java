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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.policy.api.PolicyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyConfigurationFactoryImpl implements PolicyConfigurationFactory {

    private final Logger LOGGER = LoggerFactory.getLogger(PolicyConfigurationFactoryImpl.class);

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public <T extends PolicyConfiguration> T create(Class<T> policyConfigurationClass, String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            LOGGER.debug("Unable to create a Policy configuration from a null or empty configuration data");
            return null;
        }

        try {
            return mapper.readValue(configuration, policyConfigurationClass);
        } catch (IOException ex) {
            LOGGER.error("Unable to instance Policy configuration for {}", policyConfigurationClass.getName(), ex);
        }

        return null;
    }
}
