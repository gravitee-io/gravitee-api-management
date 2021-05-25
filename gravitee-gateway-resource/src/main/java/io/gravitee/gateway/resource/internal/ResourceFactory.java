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

import static org.reflections.ReflectionUtils.*;

import com.google.common.base.Predicate;
import io.gravitee.resource.api.Resource;
import io.gravitee.resource.api.ResourceConfiguration;
import java.lang.reflect.*;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ResourceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceFactory.class);

    public Resource create(Class<? extends Resource> resourceClass, Map<Class<?>, Object> injectables) throws Exception {
        LOGGER.debug("Create a new resource instance for {}", resourceClass.getName());

        return createResource(resourceClass, injectables);
    }

    private Resource createResource(Class<? extends Resource> resourceClass, Map<Class<?>, Object> injectables) {
        Resource resourceInst = null;

        Constructor<? extends Resource> constr = lookingForConstructor(resourceClass);

        if (constr != null) {
            try {
                //    if (constr.getParameterCount() > 0) {
                //        resourceInst = constr.newInstance(injectables.get(policyMetadata.configuration()));
                //    } else {
                resourceInst = constr.newInstance();
                //    }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                LOGGER.error("Unable to instantiate resource {}", resourceClass, ex);
            }
        }

        if (resourceInst != null) {
            Set<Field> fields = lookingForInjectableFields(resourceClass);
            if (fields != null) {
                for (Field field : fields) {
                    boolean accessible = field.isAccessible();

                    Class<?> type = field.getType();
                    Optional<?> value = injectables.values().stream().filter(o -> type.isAssignableFrom(o.getClass())).findFirst();

                    if (value.isPresent()) {
                        LOGGER.debug("Inject value into field {} [{}] in {}", field.getName(), type.getName(), resourceClass);
                        try {
                            field.setAccessible(true);
                            field.set(resourceInst, value.get());
                        } catch (IllegalAccessException iae) {
                            LOGGER.error("Unable to set field value for {} in {}", field.getName(), resourceClass, iae);
                        } finally {
                            field.setAccessible(accessible);
                        }
                    }
                }
            }
        }

        return resourceInst;
    }

    private Constructor<? extends Resource> lookingForConstructor(Class<? extends Resource> resourceClass) {
        LOGGER.debug("Looking for a constructor to inject resource configuration");
        Constructor<? extends Resource> constructor = null;

        Set<Constructor> resourceConstructors = ReflectionUtils.getConstructors(
            resourceClass,
            withModifier(Modifier.PUBLIC),
            withParametersAssignableFrom(ResourceConfiguration.class),
            withParametersCount(1)
        );

        if (resourceConstructors.isEmpty()) {
            LOGGER.debug(
                "No configuration can be injected for {} because there is no valid constructor. " + "Using default empty constructor.",
                resourceClass.getName()
            );
            try {
                constructor = resourceClass.getConstructor();
            } catch (NoSuchMethodException nsme) {
                LOGGER.error("Unable to find default empty constructor for {}", resourceClass.getName(), nsme);
            }
        } else if (resourceConstructors.size() == 1) {
            constructor = resourceConstructors.iterator().next();
        } else {
            LOGGER.info("Too much constructors to instantiate resource {}", resourceClass.getName());
        }

        return constructor;
    }

    private Set<Field> lookingForInjectableFields(Class<?> resourceClass) {
        return ReflectionUtils.getAllFields(resourceClass, withAnnotation(Inject.class));
    }

    public static Predicate<Member> withParametersAssignableFrom(final Class... types) {
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
