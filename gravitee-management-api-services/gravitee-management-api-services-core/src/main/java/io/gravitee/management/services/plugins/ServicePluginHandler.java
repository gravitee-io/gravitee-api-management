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
package io.gravitee.management.services.plugins;

import io.gravitee.common.service.AbstractService;
import io.gravitee.management.services.ServiceManager;
import io.gravitee.plugin.core.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ServicePluginHandler implements PluginHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServicePluginHandler.class);

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private Environment environment;

    @Autowired
    private PluginContextFactory pluginContextFactory;

    @Autowired
    private PluginClassLoaderFactory pluginClassLoaderFactory;

    @Override
    public boolean canHandle(Plugin plugin) {
        return plugin.type() == PluginType.SERVICE;
    }

    @Override
    public void handle(Plugin plugin) {
        try {
            pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());

            LOGGER.info("Register a new service: {} [{}]", plugin.id(), plugin.clazz());
            ApplicationContext context = pluginContextFactory.create(plugin);
            serviceManager.register((AbstractService) context.getBean(plugin.clazz()));

        } catch (Exception iae) {
            LOGGER.error("Unexpected error while create reporter instance", iae);
            // Be sure that the context does not exist anymore.
            pluginContextFactory.remove(plugin);
        }
    }
}
