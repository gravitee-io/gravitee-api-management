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
package io.gravitee.gateway.resource.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.resource.api.ResourceConfiguration;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ResourceConfigurationFactoryImpl implements ResourceConfigurationFactory {

    private final Logger LOGGER = LoggerFactory.getLogger(ResourceConfigurationFactoryImpl.class);

    private static final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public <T extends ResourceConfiguration> T create(Class<T> resourceConfigurationClass, String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            LOGGER.error("Unable to create a resource configuration from a null or empty configuration data");
            return null;
        }

        try {
            return mapper.readValue(configuration, resourceConfigurationClass);
        } catch (IOException ex) {
            LOGGER.error("Unable to instance resource configuration for {}", resourceConfigurationClass.getName(), ex);
        }

        return null;
    }
}
