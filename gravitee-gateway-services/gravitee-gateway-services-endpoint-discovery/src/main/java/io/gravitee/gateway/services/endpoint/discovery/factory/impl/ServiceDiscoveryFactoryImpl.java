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

import io.gravitee.discovery.api.ServiceDiscovery;
import io.gravitee.gateway.services.endpoint.discovery.factory.ServiceDiscoveryConfigurationFactory;
import io.gravitee.gateway.services.endpoint.discovery.factory.ServiceDiscoveryFactory;
import io.gravitee.plugin.core.api.PluginContextFactory;
import io.gravitee.plugin.core.internal.AnnotationBasedPluginContextConfigurer;
import io.gravitee.plugin.discovery.ServiceDiscoveryPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ApplicationContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ServiceDiscoveryFactoryImpl implements ServiceDiscoveryFactory {

    private final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscoveryFactoryImpl.class);

    @Autowired
    private ServiceDiscoveryConfigurationFactory serviceDiscoveryConfigurationFactory;

    @Autowired
    private PluginContextFactory pluginContextFactory;

    @Override
    public <T extends ServiceDiscovery> T create(ServiceDiscoveryPlugin sdPlugin, String sdConfiguration) {
        LOGGER.debug("Create a new service discovery instance for {}", sdPlugin.serviceDiscovery().getName());

        ApplicationContext sdContext = pluginContextFactory.create(
            new AnnotationBasedPluginContextConfigurer(sdPlugin) {
                @Override
                public ClassLoader classLoader() {
                    return sdPlugin.serviceDiscovery().getClassLoader();
                }

                @Override
                public void registerBeans() {
                    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(sdPlugin.clazz());
                    if (sdPlugin.configuration() != null) {
                        builder.addConstructorArgValue(
                            serviceDiscoveryConfigurationFactory.create(sdPlugin.configuration(), sdConfiguration)
                        );
                    }

                    pluginContext.registerBeanDefinition(sdPlugin.clazz(), builder.getBeanDefinition());
                }
            }
        );

        try {
            return (T) sdContext.getBean(sdPlugin.clazz());
        } catch (Exception ex) {
            LOGGER.error("Unable to get service discovery {}", sdPlugin.serviceDiscovery().getName(), ex);
            return null;
        }
    }
}
