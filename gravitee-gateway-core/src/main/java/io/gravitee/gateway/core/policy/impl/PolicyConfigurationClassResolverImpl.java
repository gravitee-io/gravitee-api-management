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

import io.gravitee.gateway.api.policy.PolicyConfiguration;
import io.gravitee.gateway.core.policy.PolicyConfigurationClassResolver;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyConfigurationClassResolverImpl implements PolicyConfigurationClassResolver {

    protected final Logger LOGGER = LoggerFactory.getLogger(PolicyConfigurationClassResolverImpl.class);

    @Override
    public Class<? extends PolicyConfiguration> resolvePolicyConfigurationClass(Class<?> policyClass) {
        LOGGER.info("Looking for a policy configuration class for plugin {} in package {}", policyClass.getName(), policyClass.getPackage().getName());

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addClassLoader(policyClass.getClassLoader())
                .setUrls(ClasspathHelper.forClass(policyClass, policyClass.getClassLoader()))
                .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner())
                .filterInputsBy(new FilterBuilder().includePackage(policyClass.getPackage().getName())));

        Set<Class<? extends PolicyConfiguration>> policyConfigurations =
                reflections.getSubTypesOf(PolicyConfiguration.class);

        Class<? extends PolicyConfiguration> policyConfiguration = null;

        if (policyConfigurations.isEmpty()) {
            LOGGER.info("No policy configuration class defined for policy {}", policyClass.getName());
        } else {
            policyConfiguration = policyConfigurations.iterator().next();
            LOGGER.info("Policy configuration class found for plugin {} : {}", policyClass.getName(), policyConfiguration.getName());
        }

        return policyConfiguration;
    }
}
