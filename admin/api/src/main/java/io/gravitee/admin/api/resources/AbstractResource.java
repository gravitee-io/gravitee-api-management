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
package io.gravitee.admin.api.resources;

import io.gravitee.common.utils.PropertiesUtils;
import io.gravitee.gateway.api.Registry;
import io.gravitee.gateway.core.registry.FileRegistry;
import io.gravitee.gateway.registry.mongodb.MongoDBRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public abstract class AbstractResource {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private Registry registry;
    private String registryMode;

    public AbstractResource() {
        final Properties properties = new Properties();
        final String propertyPath = System.getProperty("gateway.conf.registry", "/etc/gravitee.io/conf/registry.properties");
        try {
            final InputStream input = new FileInputStream(propertyPath);
            properties.load(input);
            registryMode = PropertiesUtils.getProperty(properties, "gravitee.io.registry.mode");
        } catch (final IOException e) {
            LOGGER.error("No property configuration can be read from {}", propertyPath, e);
        }
    }

    protected Registry getRegistry() {
        if (registry == null) {
            switch (registryMode) {
                case "mongo":
                    registry = new MongoDBRegistry();
                    break;
                default:
                    registry = new FileRegistry();
                    break;
            }
        }
        return registry;
    }
}
