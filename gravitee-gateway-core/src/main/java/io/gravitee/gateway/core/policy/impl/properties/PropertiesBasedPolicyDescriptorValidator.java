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
package io.gravitee.gateway.core.policy.impl.properties;

import io.gravitee.gateway.core.policy.PolicyDescriptorValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PropertiesBasedPolicyDescriptorValidator implements PolicyDescriptorValidator {

    protected final Logger LOGGER = LoggerFactory.getLogger(PropertiesBasedPolicyDescriptorValidator.class);

    private final Properties properties;

    private final static String [] DESCRIPTOR_PROPERTIES = new String [] {
            PolicyDescriptorProperties.DESCRIPTOR_ID_PROPERTY,
            PolicyDescriptorProperties.DESCRIPTOR_DESCRIPTION_PROPERTY,
            PolicyDescriptorProperties.DESCRIPTOR_CLASS_PROPERTY,
            PolicyDescriptorProperties.DESCRIPTOR_NAME_PROPERTY,
            PolicyDescriptorProperties.DESCRIPTOR_VERSION_PROPERTY
    };

    public PropertiesBasedPolicyDescriptorValidator(Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean validate() {
        for(String key: DESCRIPTOR_PROPERTIES) {
            if (! validate(key)) {
                LOGGER.error("The property {} is not valid", key);
                return false;
            }
        }

        return true;
    }

    private boolean validate(String key) {
        final String value = properties.getProperty(key);
        return (value != null && !value.isEmpty());
    }
}
