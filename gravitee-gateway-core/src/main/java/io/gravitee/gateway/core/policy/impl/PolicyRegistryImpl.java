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

import io.gravitee.gateway.api.policy.Policy;
import io.gravitee.gateway.api.policy.PolicyChain;
import io.gravitee.gateway.api.policy.PolicyConfiguration;
import io.gravitee.gateway.api.policy.annotations.OnRequest;
import io.gravitee.gateway.api.policy.annotations.OnResponse;
import io.gravitee.gateway.core.policy.PolicyDefinition;
import io.gravitee.gateway.core.policy.PolicyRegistry;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.reflections.ReflectionUtils.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyRegistryImpl implements PolicyRegistry {

    private final Logger LOGGER = LoggerFactory.getLogger(PolicyRegistryImpl.class);

    private final Map<String, PolicyDefinition> policies = new HashMap<>();

    @Override
    public Collection<PolicyDefinition> policies() {
        return policies.values();
    }

    @Override
    public PolicyDefinition getPolicy(String name) {
        return policies.get(name);
    }

    public void initialize() {
        LOGGER.info("Initializing policy definitions...");

        Set<String> policyClasses = new HashSet<>(
                SpringFactoriesLoader.loadFactoryNames(Policy.class, this.getClass().getClassLoader()));

        LOGGER.info("\tFound {} {} definitions:", policyClasses.size(), Policy.class.getSimpleName());

        for(String policyClass : policyClasses) {
            try {
                final Class<?> instanceClass = ClassUtils.forName(policyClass, this.getClass().getClassLoader());
                Assert.isAssignable(Policy.class, instanceClass);

                final io.gravitee.gateway.core.policy.annotations.Policy annot =
                        instanceClass.getAnnotation(io.gravitee.gateway.core.policy.annotations.Policy.class);

                if (annot != null) {
                    final Method onRequestMethod = resolvePolicyMethod(instanceClass, OnRequest.class);
                    final Method onResponseMethod = resolvePolicyMethod(instanceClass, OnResponse.class);

                    if (onRequestMethod == null && onResponseMethod == null) {
                        LOGGER.error("No method annotated with @OnRequest or @OnResponse found, skip policy registration for {}", instanceClass.getName());
                    } else {

                        PolicyDefinition definition = new PolicyDefinition() {
                            @Override
                            public String name() {
                                return annot.name();
                            }

                            @Override
                            public String description() {
                                return annot.description();
                            }

                            @Override
                            public Class<Policy> policy() {
                                return (Class<Policy>) instanceClass;
                            }

                            @Override
                            public Class<PolicyConfiguration> configuration() {
                                return null;
                            }

                            @Override
                            public Method onRequestMethod() {
                                return onRequestMethod;
                            }

                            @Override
                            public Method onResponseMethod() {
                                return onResponseMethod;
                            }
                        };

                        LOGGER.info("\t\tRegister policy definition: {} ({})", definition.name(), instanceClass.getName());
                        policies.put(definition.name(), definition);
                    }
                } else {
                    LOGGER.warn("\t\tPolicy {} can't be registered since @Policy annotation is not present.", instanceClass.getName());
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate Policy: " + policyClasses, e);
            }
        }
    }

    private Method resolvePolicyMethod(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Set<Method> methods = ReflectionUtils.getMethods(
                clazz,
                withModifier(Modifier.PUBLIC),
                withAnyParameterAnnotation(annotationClass),
                withParametersAssignableTo(PolicyChain.class));

        if (methods.isEmpty()) {
            return null;
        }

        return methods.iterator().next();
    }
}
