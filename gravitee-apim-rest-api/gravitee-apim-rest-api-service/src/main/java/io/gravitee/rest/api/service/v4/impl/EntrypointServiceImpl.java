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
package io.gravitee.rest.api.service.v4.impl;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.gateway.jupiter.api.entrypoint.EntrypointConnectorFactory;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.entrypoint.EntrypointPlugin;
import io.gravitee.plugin.entrypoint.EntrypointPluginManager;
import io.gravitee.rest.api.model.v4.entrypoint.EntrypointPluginEntity;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.impl.AbstractPluginService;
import io.gravitee.rest.api.service.v4.EntrypointService;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("EntrypointServiceImplV4")
public class EntrypointServiceImpl extends AbstractPluginService<EntrypointPlugin, EntrypointPluginEntity> implements EntrypointService {

    private final JsonSchemaService jsonSchemaService;

    public EntrypointServiceImpl(final JsonSchemaService jsonSchemaService) {
        this.jsonSchemaService = jsonSchemaService;
    }

    @Override
    public Set<EntrypointPluginEntity> findAll() {
        return super.list().stream().map(this::convert).collect(Collectors.toSet());
    }

    @Override
    public EntrypointPluginEntity findById(String entrypointId) {
        EntrypointPlugin resourceDefinition = super.get(entrypointId);
        return convert(resourceDefinition);
    }

    @Override
    protected EntrypointPluginEntity convert(Plugin plugin) {
        EntrypointPluginEntity entity = new EntrypointPluginEntity();

        entity.setId(plugin.id());
        entity.setDescription(plugin.manifest().description());
        entity.setName(plugin.manifest().name());
        entity.setVersion(plugin.manifest().version());
        EntrypointConnectorFactory<?> connectorFactory = ((EntrypointPluginManager) pluginManager).getFactoryById(plugin.id());

        entity.setSupportedApiType(ApiType.fromLabel(connectorFactory.supportedApi().getLabel()));
        entity.setSupportedModes(
            connectorFactory
                .supportedModes()
                .stream()
                .map(connectorMode -> ConnectorMode.fromLabel(connectorMode.getLabel()))
                .collect(Collectors.toSet())
        );

        return entity;
    }

    @Override
    public Set<EntrypointPluginEntity> findBySupportedApi(final ApiType apiType) {
        return super
            .list()
            .stream()
            .filter(plugin ->
                ((EntrypointPluginManager) pluginManager).getFactoryById(plugin.id())
                    .supportedApi()
                    .equals(io.gravitee.gateway.jupiter.api.ApiType.fromLabel(apiType.getLabel()))
            )
            .map(this::convert)
            .collect(Collectors.toSet());
    }

    @Override
    public Set<EntrypointPluginEntity> findByConnectorMode(final ConnectorMode connectorMode) {
        return super
            .list()
            .stream()
            .filter(plugin ->
                ((EntrypointPluginManager) pluginManager).getFactoryById(plugin.id())
                    .supportedModes()
                    .contains(io.gravitee.gateway.jupiter.api.ConnectorMode.fromLabel(connectorMode.getLabel()))
            )
            .map(this::convert)
            .collect(Collectors.toSet());
    }

    @Override
    public String validateEntrypointConfiguration(final String entrypointId, final String configuration) {
        EntrypointPluginEntity entrypointPluginEntity = this.findById(entrypointId);
        validateConfiguration(entrypointPluginEntity, configuration);
        return configuration;
    }

    private String validateConfiguration(EntrypointPluginEntity plugin, String configuration) {
        if (plugin != null && configuration != null) {
            String schema = getSchema(plugin.getId());
            return jsonSchemaService.validate(schema, configuration);
        }
        return configuration;
    }
}
