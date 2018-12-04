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
package io.gravitee.gateway.services.endpoint.discovery.factory.impl;

import com.google.common.base.Predicate;
import io.gravitee.discovery.api.ServiceDiscovery;
import io.gravitee.discovery.api.ServiceDiscoveryConfiguration;
import io.gravitee.gateway.services.endpoint.discovery.factory.ServiceDiscoveryConfigurationFactory;
import io.gravitee.gateway.services.endpoint.discovery.factory.ServiceDiscoveryFactory;
import io.gravitee.plugin.discovery.ServiceDiscoveryPlugin;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withParametersCount;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ServiceDiscoveryFactoryImpl implements ServiceDiscoveryFactory {

    private final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscoveryFactoryImpl.class);

    @Autowired
    private ServiceDiscoveryConfigurationFactory serviceDiscoveryConfigurationFactory;

    /**
     * Cache of constructor by service discovery
     */
    private Map<Class<?>, Constructor<?>> constructors = new HashMap<>();

    @Override
    public <T extends ServiceDiscovery> T create(ServiceDiscoveryPlugin sdPlugin,
                                                 String sdConfiguration) {
        LOGGER.debug("Create a new service discovery instance for {}", sdPlugin.serviceDiscovery().getName());

        T serviceDiscoveryInst = null;

        Constructor<T> constr = lookingForConstructor(sdPlugin.serviceDiscovery());

        if (constr != null) {
            try {
                if (constr.getParameterCount() > 0) {
                    ServiceDiscoveryConfiguration serviceDiscoveryConfiguration =
                            serviceDiscoveryConfigurationFactory.create(sdPlugin.configuration(), sdConfiguration);
                    serviceDiscoveryInst = constr.newInstance(serviceDiscoveryConfiguration);
                } else {
                    serviceDiscoveryInst = constr.newInstance();
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                LOGGER.error("Unable to instantiate service discovery {}", sdPlugin.serviceDiscovery().getName(), ex);
            }
        }

        return serviceDiscoveryInst;
    }

    private <T extends ServiceDiscovery> Constructor<T> lookingForConstructor(Class<T> serviceDiscoveryClass) {
        Constructor constructor = constructors.get(serviceDiscoveryClass);
        if (constructor == null) {
            LOGGER.debug("Looking for a constructor to inject service discovery configuration");

            Set<Constructor> serviceDiscoveryConstructors =
                    ReflectionUtils.getConstructors(serviceDiscoveryClass,
                            withModifier(Modifier.PUBLIC),
                            withParametersAssignableFrom(ServiceDiscoveryConfiguration.class),
                            withParametersCount(1));

            if (serviceDiscoveryConstructors.isEmpty()) {
                LOGGER.debug("No configuration can be injected for {} because there is no valid constructor. " +
                        "Using default empty constructor.", serviceDiscoveryClass.getName());
                try {
                    constructor = serviceDiscoveryClass.getConstructor();
                } catch (NoSuchMethodException nsme) {
                    LOGGER.error("Unable to find default empty constructor for {}", serviceDiscoveryClass.getName(), nsme);
                }
            } else if (serviceDiscoveryConstructors.size() == 1) {
                constructor = serviceDiscoveryConstructors.iterator().next();
            } else {
                LOGGER.info("Too much constructors to instantiate service discovery {}", serviceDiscoveryClass.getName());
            }

            constructors.put(serviceDiscoveryClass, constructor);
        }

        return constructor;
    }

    private static Predicate<Member> withParametersAssignableFrom(final Class... types) {
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
