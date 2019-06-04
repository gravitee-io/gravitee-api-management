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
package io.gravitee.rest.api.service.plaftform.plugins;

import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.discovery.ServiceDiscoveryPlugin;
import io.gravitee.rest.api.model.platform.plugin.PluginEntity;
import io.gravitee.rest.api.service.ServiceDiscoveryService;
import io.gravitee.rest.api.service.exceptions.ServiceDiscoveryNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.TransactionalService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ServiceDiscoveryServiceImpl extends TransactionalService implements ServiceDiscoveryService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscoveryServiceImpl.class);

    @Autowired
    private ConfigurablePluginManager<ServiceDiscoveryPlugin> serviceDiscoveryPluginManager;

    @Override
    public Set<PluginEntity> findAll() {
        try {
            LOGGER.debug("List all service discovery plugins");
            final Collection<ServiceDiscoveryPlugin> plugins = serviceDiscoveryPluginManager.findAll();

            return plugins.stream()
                    .map(this::convert)
                    .collect(Collectors.toSet());
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to list all service discovery plugins", ex);
            throw new TechnicalManagementException("An error occurs while trying to list all service discovery plugins", ex);
        }
    }

    @Override
    public PluginEntity findById(String pluginId) {
        LOGGER.debug("Find service discovery plugin by ID: {}", pluginId);
        ServiceDiscoveryPlugin plugin = serviceDiscoveryPluginManager.get(pluginId);

        if (plugin == null) {
            throw new ServiceDiscoveryNotFoundException(pluginId);
        }

        return convert(plugin);
    }

    @Override
    public String getSchema(String pluginId) {
        try {
            LOGGER.debug("Find service discovery plugin schema by ID: {}", pluginId);
            return serviceDiscoveryPluginManager.getSchema(pluginId);
        } catch (IOException ioex) {
            LOGGER.error("An error occurs while trying to get service discovery plugin's schema for plugin {}", pluginId, ioex);
            throw new TechnicalManagementException("An error occurs while trying to get service discovery plugin's schema for plugin " + pluginId, ioex);
        }
    }

    private PluginEntity convert(Plugin plugin) {
        PluginEntity entity = new PluginEntity();

        entity.setId(plugin.id());
        entity.setDescription(plugin.manifest().description());
        entity.setName(plugin.manifest().name());
        entity.setVersion(plugin.manifest().version());

        return entity;
    }
}
