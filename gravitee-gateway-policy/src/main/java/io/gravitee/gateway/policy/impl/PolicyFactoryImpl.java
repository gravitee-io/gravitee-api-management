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

import com.google.common.base.Predicate;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.policy.api.PolicyConfiguration;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.reflections.ReflectionUtils.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyFactoryImpl implements PolicyFactory {

    private final Logger LOGGER = LoggerFactory.getLogger(PolicyFactoryImpl.class);

    /**
     * Cache of constructor by policy
     */
    private Map<Class<?>, Constructor<?>> constructors = new HashMap<>();

    /**
     * Cache of injectable fields by policy
     */
    private Map<Class<?>, Set<Field>> injectableFields = new HashMap<>();

    @Override
    public Object create(PolicyMetadata policyMetadata, Map<Class<?>, Object> injectables) {
        Class<?> policyClass = policyMetadata.policy();
        LOGGER.debug("Create a new policy instance for {}", policyClass.getName());

        return createPolicy(policyMetadata, injectables);
    }

    private Object createPolicy(PolicyMetadata policyMetadata, Map<Class<?>, Object> injectables) {
        Object policyInst = null;

        Constructor<?> constr = lookingForConstructor(policyMetadata.policy());

        if (constr != null) {
            try {
                if (constr.getParameterCount() > 0 && injectables != null) {
                    policyInst = constr.newInstance(injectables.get(policyMetadata.configuration()));
                } else {
                    policyInst = constr.newInstance();
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                LOGGER.error("Unable to instantiate policy {}", policyMetadata.policy().getName(), ex);
            }
        }

        if (policyInst != null && injectables != null) {
            Set<Field> fields = lookingForInjectableFields(policyMetadata.policy());
            if (fields != null) {
                for (Field field : fields) {
                    boolean accessible = field.isAccessible();

                    Class<?> type = field.getType();
                    Object value = injectables.get(type);
                    LOGGER.debug("Inject value into field {} [{}] in {}", field.getName(), type.getName(),
                            policyMetadata.policy());
                    try {
                        field.setAccessible(true);
                        field.set(policyInst, value);
                    } catch (IllegalAccessException iae) {
                        LOGGER.error("Unable to set field value for {} in {}", field.getName(),
                                policyMetadata.policy(), iae);
                    } finally {
                        field.setAccessible(accessible);
                    }
                }
            }
        }

        return policyInst;
    }

    private Constructor<?> lookingForConstructor(Class<?> policyClass) {
        Constructor constructor = constructors.get(policyClass);
        if (constructor == null) {
            LOGGER.debug("Looking for a constructor to inject policy configuration");

            Set<Constructor> policyConstructors =
                    ReflectionUtils.getConstructors(policyClass,
                            withModifier(Modifier.PUBLIC),
                            withParametersAssignableFrom(PolicyConfiguration.class),
                            withParametersCount(1));

            if (policyConstructors.isEmpty()) {
                LOGGER.debug("No configuration can be injected for {} because there is no valid constructor. " +
                        "Using default empty constructor.", policyClass.getName());
                try {
                    constructor = policyClass.getConstructor();
                } catch (NoSuchMethodException nsme) {
                    LOGGER.error("Unable to find default empty constructor for {}", policyClass.getName(), nsme);
                }
            } else if (policyConstructors.size() == 1) {
                constructor = policyConstructors.iterator().next();
            } else {
                LOGGER.info("Too much constructors to instantiate policy {}", policyClass.getName());
            }

            constructors.put(policyClass, constructor);
        }

        return constructor;
    }

    private Set<Field> lookingForInjectableFields(Class<?> policyClass) {
        return injectableFields.computeIfAbsent(policyClass,
                aClass -> ReflectionUtils.getAllFields(policyClass, withAnnotation(Inject.class)));
    }

    public static Predicate<Member> withParametersAssignableFrom(final Class... types) {
        return input -> {
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
        };
    }

    private static Class[] parameterTypes(Member member) {
        return member != null ?
                member.getClass() == Method.class ? ((Method) member).getParameterTypes() :
                        member.getClass() == Constructor.class ? ((Constructor) member).getParameterTypes() : null : null;
    }
}
