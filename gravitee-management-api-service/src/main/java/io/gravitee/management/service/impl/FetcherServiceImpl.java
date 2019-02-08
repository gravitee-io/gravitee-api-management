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
package io.gravitee.management.service.impl;

import io.gravitee.fetcher.api.FilesFetcher;
import io.gravitee.management.model.FetcherEntity;
import io.gravitee.management.model.PluginEntity;
import io.gravitee.management.service.FetcherService;
import io.gravitee.management.service.exceptions.FetcherNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class FetcherServiceImpl extends TransactionalService implements FetcherService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(FetcherServiceImpl.class);

    @Autowired
    private ConfigurablePluginManager<FetcherPlugin> fetcherPluginManager;


    @Override
    public Set<FetcherEntity> findAll() {
        return findAll(false);
    }

    @Override
    public Set<FetcherEntity> findAll(boolean onlyFilesFetchers) {
        try {
            LOGGER.debug("List all fetchers");
            List<FetcherPlugin> fetcherDefinitions = new ArrayList<>(fetcherPluginManager.findAll());
            if (onlyFilesFetchers) {
                Class<?> filesFetcherClass = FilesFetcher.class;
                fetcherDefinitions = fetcherDefinitions.stream()
                        .filter(fetcherPlugin ->
                                filesFetcherClass.isAssignableFrom(fetcherPlugin.fetcher()))
                        .collect(Collectors.toList());
            }

            return fetcherDefinitions.stream()
                    .map(fetcherDefinition -> convert(fetcherDefinition, false))
                    .collect(Collectors.toSet());
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to list all fetchers", ex);
            throw new TechnicalManagementException("An error occurs while trying to list all fetchers", ex);
        }
    }

    @Override
    public FetcherEntity findById(String fetcher) {
        LOGGER.debug("Find fetcher by ID: {}", fetcher);
        FetcherPlugin fetcherDefinition = fetcherPluginManager.get(fetcher);

        if (fetcherDefinition == null) {
            throw new FetcherNotFoundException(fetcher);
        }

        return convert(fetcherDefinition, true);
    }

    @Override
    public String getSchema(String fetcher) {
        try {
            LOGGER.debug("Find fetcher schema by ID: {}", fetcher);
            return fetcherPluginManager.getSchema(fetcher);
        } catch (IOException ioex) {
            LOGGER.error("An error occurs while trying to get fetcher's schema for fetcher {}", fetcher, ioex);
            throw new TechnicalManagementException("An error occurs while trying to get fetcher's schema for fetcher " + fetcher, ioex);
        }
    }

    private FetcherEntity convert(FetcherPlugin fetcherPlugin, boolean withPlugin) {
        FetcherEntity entity = new FetcherEntity();

        entity.setId(fetcherPlugin.id());
        entity.setDescription(fetcherPlugin.manifest().description());
        entity.setName(fetcherPlugin.manifest().name());
        entity.setVersion(fetcherPlugin.manifest().version());

        if (withPlugin) {
            // Plugin information
            Plugin plugin = fetcherPlugin;
            PluginEntity pluginEntity = new PluginEntity();

            pluginEntity.setPlugin(plugin.clazz());
            pluginEntity.setPath(plugin.path().toString());
            pluginEntity.setType(plugin.type().toString().toLowerCase());
            pluginEntity.setDependencies(plugin.dependencies());

            entity.setPlugin(pluginEntity);
        }

        return entity;
    }
}
