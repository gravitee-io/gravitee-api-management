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

import io.gravitee.gateway.core.policy.PolicyDescriptor;
import io.gravitee.gateway.core.policy.PolicyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyPropertyDescriptorLoader implements PolicyLoader {

    private final Logger LOGGER = LoggerFactory.getLogger(PolicyPropertyDescriptorLoader.class);

    private final static String DESCRIPTOR_PROPERTIES_FILE = "policy.properties";

    private final static String DESCRIPTOR_ID_PROPERTY = "id";
    private final static String DESCRIPTOR_NAME_PROPERTY = "name";
    private final static String DESCRIPTOR_VERSION_PROPERTY = "version";
    private final static String DESCRIPTOR_DESCRIPTION_PROPERTY = "description";
    private final static String DESCRIPTOR_CLASS_PROPERTY = "class";

    @Override
    public Collection<PolicyDescriptor> load() {
        List<PolicyDescriptor> definitions = new ArrayList<>();

        try {
            Enumeration<URL> descriptors = getDescriptors();
            while (descriptors.hasMoreElements()) {
                URL dUrl = descriptors.nextElement();
                LOGGER.info("\tFound policy descriptor at {}", dUrl.toString());

                Properties policyProperties = new Properties();
                policyProperties.load(dUrl.openStream());

                PolicyDescriptor definition = create(policyProperties);
                definitions.add(definition);
            }
        } catch (IOException ioe) {
            LOGGER.error("An error occurs while looking for policy descriptors", ioe);
        }

        return definitions;
    }

    public PolicyDescriptor create(Properties properties) {
        final String id = properties.getProperty(DESCRIPTOR_ID_PROPERTY);
        final String description = properties.getProperty(DESCRIPTOR_DESCRIPTION_PROPERTY);
        final String clazz = properties.getProperty(DESCRIPTOR_CLASS_PROPERTY);
        final String name = properties.getProperty(DESCRIPTOR_NAME_PROPERTY);
        final String version = properties.getProperty(DESCRIPTOR_VERSION_PROPERTY);

        return new PolicyDescriptor() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public String version() {
                return version;
            }

            @Override
            public String policy() {
                return clazz;
            }
        };
    }

    public Enumeration<URL> getDescriptors() throws IOException {
        ClassLoader currentClassLoader = PolicyPropertyDescriptorLoader.class.getClassLoader();
        LOGGER.info("\tLooking for policy descriptors from {}", currentClassLoader);

        return currentClassLoader.getResources(DESCRIPTOR_PROPERTIES_FILE);
    }

}
