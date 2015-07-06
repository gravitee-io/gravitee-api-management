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
import io.gravitee.gateway.core.policy.PolicyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyRegistryImpl implements PolicyRegistry {

    protected final Logger LOGGER = LoggerFactory.getLogger(PolicyRegistryImpl.class);

    private Map<String, Class<? extends Policy>> policies;

    @Override
    public Map<String, Class<? extends Policy>> policies() {
        return policies;
    }

    @Override
    public Class<? extends Policy> getPolicy(String name) {
        return policies.get(name);
    }

    public void initialize() {
        LOGGER.info("Initializing policy definitions...");

        Set<String> policyClasses = new HashSet<>(
                SpringFactoriesLoader.loadFactoryNames(Policy.class, this.getClass().getClassLoader()));

        LOGGER.info("\tFound {} {} definitions:", policyClasses.size(), Policy.class.getSimpleName());
        policies = new HashMap<>(policyClasses.size());

        for(String policyClass : policyClasses) {
            try {
                Class<?> instanceClass = ClassUtils.forName(policyClass, this.getClass().getClassLoader());
                Assert.isAssignable(Policy.class, instanceClass);

                io.gravitee.gateway.core.policy.annotations.Policy annot =
                        instanceClass.getAnnotation(io.gravitee.gateway.core.policy.annotations.Policy.class);

                if (annot != null) {
                    String policyName = annot.name();
                    LOGGER.info("\t\tRegister policy definition: {} ({})", policyName, instanceClass.getName());
                    policies.put(policyName, (Class<? extends Policy>) instanceClass);
                } else {
                    LOGGER.warn("\t\tPolicy {} can't be registered since @Policy annotation is not present.", instanceClass.getName());
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate Policy: " + policyClasses, e);
            }
        }
    }
}
