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

import io.gravitee.fetcher.api.FilesFetcher;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.rest.api.model.FetcherEntity;
import io.gravitee.rest.api.model.PluginEntity;
import io.gravitee.rest.api.service.FetcherService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class FetcherServiceImpl extends AbstractPluginService<FetcherPlugin, FetcherEntity> implements FetcherService {

    @Override
    public Set<FetcherEntity> findAll() {
        return findAll(false);
    }

    @Override
    public Set<FetcherEntity> findAll(boolean onlyFilesFetchers) {
        try {
            Set<FetcherPlugin> fetcherDefinitions = super.list();

            if (onlyFilesFetchers) {
                Class<?> filesFetcherClass = FilesFetcher.class;
                fetcherDefinitions =
                    fetcherDefinitions
                        .stream()
                        .filter(fetcherPlugin -> filesFetcherClass.isAssignableFrom(fetcherPlugin.fetcher()))
                        .collect(Collectors.toSet());
            }

            return fetcherDefinitions.stream().map(fetcherDefinition -> convert(fetcherDefinition, false)).collect(Collectors.toSet());
        } catch (Exception ex) {
            logger.error("An error occurs while trying to list all fetchers", ex);
            throw new TechnicalManagementException("An error occurs while trying to list all fetchers", ex);
        }
    }

    @Override
    public FetcherEntity findById(String fetcher) {
        FetcherPlugin fetcherPlugin = super.get(fetcher);
        return convert(fetcherPlugin, true);
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
            // TODO: check the purpose of this
            //entity.setPlugin(pluginEntity);
        }

        return entity;
    }
}
