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

import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withParametersCount;

import com.google.common.base.Predicate;
import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.policy.api.PolicyConfiguration;
import java.lang.reflect.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyPluginFactoryImpl implements PolicyPluginFactory {

    private final Logger LOGGER = LoggerFactory.getLogger(PolicyPluginFactoryImpl.class);

    /**
     * Cache of constructor by policy
     */
    private final ConcurrentMap<Class<?>, Constructor<?>> constructors = new ConcurrentHashMap<>();

    @Override
    public <T> T create(Class<T> policyClass, PolicyConfiguration policyConfiguration) {
        LOGGER.debug("Create a new policy instance for {}", policyClass.getName());

        //TODO: could we reuse policy instance ?
        return createInstance(policyClass, policyConfiguration);
    }

    @Override
    public void cleanup(PolicyManifest policyManifest) {
        if (policyManifest != null) {
            constructors.remove(policyManifest.policy());
        }
    }

    @Override
    public void clearCache() {
        constructors.clear();
    }

    private <T> T createInstance(Class<T> policyClass, PolicyConfiguration policyConfiguration) {
        T policyInst = null;

        Constructor<T> constr = lookingForConstructor(policyClass);

        if (constr != null) {
            try {
                if (constr.getParameterCount() > 0) {
                    policyInst = constr.newInstance(policyConfiguration);
                } else {
                    policyInst = constr.newInstance();
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                LOGGER.error("Unable to instantiate policy {}", policyClass.getName(), ex);
            }
        }

        return policyInst;
    }

    private <T> Constructor<T> lookingForConstructor(Class<T> policyClass) {
        return (Constructor<T>) constructors.computeIfAbsent(
            policyClass,
            aClass -> {
                LOGGER.debug("Looking for a constructor to inject policy configuration");

                Set<Constructor> policyConstructors = ReflectionUtils.getConstructors(
                    policyClass,
                    withModifier(Modifier.PUBLIC),
                    withParametersAssignableFrom(PolicyConfiguration.class),
                    withParametersCount(1)
                );

                if (policyConstructors.isEmpty()) {
                    LOGGER.debug(
                        "No configuration can be injected for {} because there is no valid constructor. " +
                        "Using default empty constructor.",
                        policyClass.getName()
                    );
                    try {
                        return policyClass.getConstructor();
                    } catch (NoSuchMethodException nsme) {
                        LOGGER.error("Unable to find default empty constructor for {}", policyClass.getName(), nsme);
                    }
                } else if (policyConstructors.size() == 1) {
                    return policyConstructors.iterator().next();
                } else {
                    LOGGER.info("Too much constructors to instantiate policy {}", policyClass.getName());
                }

                return null;
            }
        );
    }

    private static Predicate<Member> withParametersAssignableFrom(final Class... types) {
        return input -> {
            if (input != null) {
                Class<?>[] parameterTypes = parameterTypes(input);
                if (parameterTypes.length == types.length) {
                    for (int i = 0; i < parameterTypes.length; i++) {
                        if (
                            !types[i].isAssignableFrom(parameterTypes[i]) || (parameterTypes[i] == Object.class && types[i] != Object.class)
                        ) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        };
    }

    private static Class[] parameterTypes(Member member) {
        return member != null
            ? member.getClass() == Method.class
                ? ((Method) member).getParameterTypes()
                : member.getClass() == Constructor.class ? ((Constructor) member).getParameterTypes() : null
            : null;
    }
}
