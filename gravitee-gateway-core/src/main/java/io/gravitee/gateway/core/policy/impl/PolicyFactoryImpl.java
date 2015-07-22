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
import io.gravitee.gateway.core.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.core.policy.PolicyDefinition;
import io.gravitee.gateway.core.policy.PolicyFactory;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Set;

import static org.reflections.ReflectionUtils.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyFactoryImpl implements PolicyFactory {

    private final Logger LOGGER = LoggerFactory.getLogger(PolicyFactoryImpl.class);

    @Autowired
    private PolicyConfigurationFactory policyConfigurationFactory;

    @Override
    public Object create(PolicyDefinition policyDefinition, String configuration) {
        Class<? extends PolicyConfiguration> policyConfigurationClazz = policyDefinition.configuration();
        Class<?> policyClass = policyDefinition.policy();

        try {
            Object policyInst = null;

            LOGGER.info("Trying to create a new instance of policy {}", policyClass.getName());

            if (policyClass != null) {
                PolicyConfiguration policyConfiguration = null;
                if (policyConfigurationClazz != null) {
                    if (configuration == null) {
                        LOGGER.error("A configuration is required for policy {}, returning a null policy.", policyDefinition.name());
                        return null;
                    } else {
                        LOGGER.debug("Create policy configuration for policy {}", policyDefinition.name());
                        policyConfiguration = policyConfigurationFactory.create(policyConfigurationClazz, configuration);
                    }
                }

                LOGGER.info("Looking for an injectable configuration using policy constructor.");
                Set<Constructor> constructors =
                        ReflectionUtils.getConstructors(policyClass,
                                withModifier(Modifier.PUBLIC),
                                withParametersAssignableTo(PolicyConfiguration.class),
                                withParametersCount(1));

                Constructor<?> constr = null;

                if (constructors.isEmpty()) {
                    LOGGER.warn("No configuration can be injected for {} because there is no valid constructor, using default constructor.", policyClass.getName());
                    policyInst = createInstance(policyClass);
                } else if (constructors.size() == 1) {
                    constr = constructors.iterator().next();
                    policyInst = constr.newInstance(policyConfiguration);
                } else {
                    LOGGER.warn("Too much constructor to instantiate policy {}", policyClass.getName());
                }
            }

            return policyInst;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
            LOGGER.error("Unable to instantiate Policy {}", policyDefinition.policy().getName(), ex);
        }

        return null;
    }

    private <T> T createInstance(Class<T> clazz) throws IllegalAccessException, InstantiationException {
        return clazz.newInstance();
    }

    public PolicyConfigurationFactory getPolicyConfigurationFactory() {
        return policyConfigurationFactory;
    }

    public void setPolicyConfigurationFactory(PolicyConfigurationFactory policyConfigurationFactory) {
        this.policyConfigurationFactory = policyConfigurationFactory;
    }
}
