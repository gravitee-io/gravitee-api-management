/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import io.gravitee.plugin.core.api.ConfigurablePlugin;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginMoreInformation;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.PluginService;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractPluginService<T extends ConfigurablePlugin, E extends PlatformPluginEntity>
    extends TransactionalService
    implements PluginService<E> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final JsonSchemaService jsonSchemaService;

    protected ConfigurablePluginManager<T> pluginManager;

    public AbstractPluginService(JsonSchemaService jsonSchemaService, ConfigurablePluginManager<T> pluginManager) {
        this.jsonSchemaService = jsonSchemaService;
        this.pluginManager = pluginManager;
    }

    protected Set<T> list() {
        try {
            logger.debug("List all plugins");
            final Collection<T> plugins = pluginManager.findAll(true);

            return new HashSet<>(plugins);
        } catch (Exception ex) {
            logger.error("An error occurs while trying to list all policies", ex);
            throw new TechnicalManagementException("An error occurs while trying to list all policies", ex);
        }
    }

    protected T get(String pluginId) {
        logger.debug("Find plugin by ID: {}", pluginId);
        T plugin = pluginManager.get(pluginId, true);

        if (plugin == null) {
            throw new PluginNotFoundException(pluginId);
        }

        return plugin;
    }

    @Override
    public String getSchema(String pluginId) {
        try {
            logger.debug("Find plugin schema by ID: {}", pluginId);
            return pluginManager.getSchema(pluginId, true);
        } catch (IOException ioex) {
            logger.error("An error occurs while trying to get plugin schema for plugin {}", pluginId, ioex);
            throw new TechnicalManagementException("An error occurs while trying to get plugin schema for plugin " + pluginId, ioex);
        }
    }

    @Override
    public String getIcon(String pluginId) {
        try {
            logger.debug("Find plugin icon by ID: {}", pluginId);
            return pluginManager.getIcon(pluginId, true);
        } catch (IOException ioex) {
            logger.error("An error occurs while trying to get plugin icon for plugin {}", pluginId, ioex);
            throw new TechnicalManagementException("An error occurs while trying to get plugin icon for plugin " + pluginId, ioex);
        }
    }

    @Override
    public String getDocumentation(String pluginId) {
        try {
            logger.debug("Find plugin documentation by ID: {}", pluginId);
            return pluginManager.getDocumentation(pluginId, true);
        } catch (IOException ioex) {
            logger.error("An error occurs while trying to get plugin documentation for plugin {}", pluginId, ioex);
            throw new TechnicalManagementException("An error occurs while trying to get plugin documentation for plugin " + pluginId, ioex);
        }
    }

    protected PlatformPluginEntity convert(Plugin plugin) {
        PlatformPluginEntity entity = new PlatformPluginEntity();

        entity.setId(plugin.id());
        entity.setDescription(plugin.manifest().description());
        entity.setName(plugin.manifest().name());
        entity.setCategory(plugin.manifest().category());
        entity.setVersion(plugin.manifest().version());
        entity.setFeature(plugin.manifest().feature());
        entity.setDeployed(plugin.deployed());

        return entity;
    }

    protected String validateConfiguration(String pluginId, String configuration) {
        if (pluginId != null && configuration != null) {
            String schema = getSchema(pluginId);
            return jsonSchemaService.validate(schema, configuration);
        }
        return configuration;
    }

    @Override
    public PluginMoreInformation getMoreInformation(String pluginId) {
        try {
            logger.debug("Find plugin more information by ID: {}", pluginId);
            return pluginManager.getMoreInformation(pluginId, true);
        } catch (IOException ioex) {
            logger.error("An error occurs while trying to get plugin more information for plugin {}", pluginId, ioex);
            throw new TechnicalManagementException(
                "An error occurs while trying to get plugin more information for plugin " + pluginId,
                ioex
            );
        }
    }
}
