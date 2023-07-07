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
package io.gravitee.rest.api.service.v4.impl;

import io.gravitee.plugin.apiservice.ApiServicePlugin;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.rest.api.model.v4.apiservice.ApiServicePluginEntity;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.impl.AbstractPluginService;
import io.gravitee.rest.api.service.v4.ApiServicePluginService;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiServicePluginServiceImpl
    extends AbstractPluginService<ApiServicePlugin<?, ?>, ApiServicePluginEntity>
    implements ApiServicePluginService {

    public ApiServicePluginServiceImpl(
        JsonSchemaService jsonSchemaService,
        ConfigurablePluginManager<ApiServicePlugin<?, ?>> pluginManager
    ) {
        super(jsonSchemaService, pluginManager);
    }

    @Override
    public String validateApiServiceConfiguration(String apiServicePluginId, String configuration) {
        ApiServicePluginEntity entity = this.findById(apiServicePluginId);
        return validateConfiguration(entity.getId(), configuration);
    }

    @Override
    public Set<ApiServicePluginEntity> findAll() {
        return super.list().stream().map(this::convert).collect(Collectors.toSet());
    }

    @Override
    public ApiServicePluginEntity findById(String plugin) {
        ApiServicePlugin apiSevicePlugin = super.get(plugin);
        return convert(apiSevicePlugin);
    }

    @Override
    protected ApiServicePluginEntity convert(Plugin plugin) {
        ApiServicePluginEntity entity = new ApiServicePluginEntity();
        entity.setId(plugin.id());
        entity.setDescription(plugin.manifest().description());
        entity.setName(plugin.manifest().name());
        entity.setVersion(plugin.manifest().version());
        entity.setCategory(plugin.manifest().category());
        entity.setDeployed(plugin.deployed());
        return entity;
    }
}
