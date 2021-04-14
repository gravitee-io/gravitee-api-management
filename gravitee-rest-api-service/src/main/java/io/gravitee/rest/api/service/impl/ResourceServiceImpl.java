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
package io.gravitee.rest.api.service.impl;

import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.rest.api.model.platform.plugin.PluginEntity;
import io.gravitee.rest.api.service.ResourceService;
import io.gravitee.rest.api.service.exceptions.ResourceNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ResourceServiceImpl extends TransactionalService implements ResourceService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(ResourceServiceImpl.class);

    @Autowired
    private ConfigurablePluginManager<ResourcePlugin> resourcePluginManager;

    @Override
    public Set<PluginEntity> findAll() {
        try {
            LOGGER.debug("List all resources");
            final Collection<ResourcePlugin> resourceDefinitions = resourcePluginManager.findAll();

            return resourceDefinitions.stream().map(resourceDefinition -> convert(resourceDefinition)).collect(Collectors.toSet());
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to list all resources", ex);
            throw new TechnicalManagementException("An error occurs while trying to list all resources", ex);
        }
    }

    @Override
    public PluginEntity findById(String resource) {
        LOGGER.debug("Find resource by ID: {}", resource);
        ResourcePlugin resourceDefinition = resourcePluginManager.get(resource);

        if (resourceDefinition == null) {
            throw new ResourceNotFoundException(resource);
        }

        return convert(resourceDefinition);
    }

    @Override
    public String getSchema(String resource) {
        try {
            LOGGER.debug("Find resource schema by ID: {}", resource);
            return resourcePluginManager.getSchema(resource);
        } catch (IOException ioex) {
            LOGGER.error("An error occurs while trying to get resource's schema for resource {}", resource, ioex);
            throw new TechnicalManagementException("An error occurs while trying to get resource's schema for resource " + resource, ioex);
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
