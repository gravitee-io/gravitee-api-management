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

import com.google.common.base.Predicate;
import io.gravitee.gateway.core.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.core.policy.PolicyFactory;
import io.gravitee.plugin.policy.PolicyDefinition;
import io.gravitee.policy.api.PolicyConfiguration;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withParametersCount;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyFactoryImpl implements PolicyFactory {

    private final Logger LOGGER = LoggerFactory.getLogger(PolicyFactoryImpl.class);

    @Autowired
    private PolicyConfigurationFactory policyConfigurationFactory;

    private Map<Class<?>, Constructor<?>> constructorCache = new HashMap<>();

    @Override
    public Object create(PolicyDefinition policyDefinition, String configuration) {
        Class<? extends PolicyConfiguration> policyConfigurationClazz = policyDefinition.configuration();
        Class<?> policyClass = policyDefinition.policy();

        LOGGER.debug("Create a new policy instance for {}", policyClass.getName());

        PolicyConfiguration policyConfiguration = null;
        if (policyConfigurationClazz != null) {
            if (configuration == null) {
                LOGGER.error("A configuration is required for policy {}, returning a null policy", policyDefinition.id());
                return null;
            } else {
                LOGGER.debug("Create policy configuration for policy {}", policyDefinition.id());
                policyConfiguration = policyConfigurationFactory.create(policyConfigurationClazz, configuration);
            }
        }

        return createPolicy(policyClass, policyConfiguration);
    }

    private Object createPolicy(Class<?> policyClass, Object ... args) {
        Constructor<?> constr = getConstructor(policyClass);

        if (constr != null) {
            try {
                if (constr.getParameterCount() > 0) {
                    return constr.newInstance(args);
                } else {
                    return constr.newInstance();
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                LOGGER.error("Unable to instantiate policy {}", policyClass.getName(), ex);
            }
        }

        return null;
    }

    private Constructor<?> getConstructor(Class<?> policyClass) {
        Constructor cons = constructorCache.get(policyClass);
        if (cons == null) {
            LOGGER.debug("Looking for a constructor to inject policy configuration");

            Set<Constructor> constructors =
                    ReflectionUtils.getConstructors(policyClass,
                            withModifier(Modifier.PUBLIC),
                            withParametersAssignableFrom(PolicyConfiguration.class),
                            withParametersCount(1));

            if (constructors.isEmpty()) {
                LOGGER.debug("No configuration can be injected for {} because there is no valid constructor. " +
                        "Using default empty constructor.", policyClass.getName());
                try {
                    cons = policyClass.getConstructor();
                } catch (NoSuchMethodException nsme) {
                    LOGGER.error("Unable to find default empty constructor for {}", policyClass.getName(), nsme);
                }
            } else if (constructors.size() == 1) {
                cons = constructors.iterator().next();
            } else {
                LOGGER.info("Too much constructors to instantiate policy {}", policyClass.getName());
            }

            if (cons != null) {
                // Put reference in cache
                constructorCache.put(policyClass, cons);
            }
        }

        return cons;
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

    public static Predicate<Member> withParametersAssignableFrom(final Class... types) {
        return new Predicate<Member>() {
            public boolean apply(@Nullable Member input) {
                if (input != null) {
                    Class<?>[] parameterTypes = parameterTypes(input);
                    if (parameterTypes.length == types.length) {
                        for (int i = 0; i < parameterTypes.length; i++) {
                            if (!types[i].isAssignableFrom(parameterTypes[i]) ||
                                    (parameterTypes[i] == Object.class && types[i] != Object.class)) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private static Class[] parameterTypes(Member member) {
        return member != null ?
                member.getClass() == Method.class ? ((Method) member).getParameterTypes() :
                        member.getClass() == Constructor.class ? ((Constructor) member).getParameterTypes() : null : null;
    }
}
