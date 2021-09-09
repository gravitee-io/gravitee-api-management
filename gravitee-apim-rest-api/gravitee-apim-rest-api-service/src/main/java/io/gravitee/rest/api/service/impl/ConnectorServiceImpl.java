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

import io.gravitee.plugin.connector.ConnectorPlugin;
import io.gravitee.plugin.connector.ConnectorPluginManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.rest.api.model.ConnectorPluginEntity;
import io.gravitee.rest.api.service.ConnectorService;
import io.gravitee.rest.api.service.JsonSchemaService;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ConnectorServiceImpl extends AbstractPluginService<ConnectorPlugin, ConnectorPluginEntity> implements ConnectorService {

    @Autowired
    private JsonSchemaService jsonSchemaService;

    @Override
    public Set<ConnectorPluginEntity> findAll() {
        return super.list().stream().map(this::convert).collect(Collectors.toSet());
    }

    @Override
    public ConnectorPluginEntity findById(String connector) {
        ConnectorPlugin resourceDefinition = super.get(connector);
        return convert(resourceDefinition);
    }

    @Override
    protected ConnectorPluginEntity convert(Plugin plugin) {
        ConnectorPluginEntity entity = new ConnectorPluginEntity();

        entity.setId(plugin.id());
        entity.setDescription(plugin.manifest().description());
        entity.setName(plugin.manifest().name());
        entity.setVersion(plugin.manifest().version());

        entity.setSupportedTypes(((ConnectorPluginManager) pluginManager).getConnector(plugin.id()).supportedTypes());

        return entity;
    }

    @Override
    public Optional<ConnectorPluginEntity> findBySupportedType(String type) {
        return super
            .list()
            .stream()
            .filter(plugin -> ((ConnectorPluginManager) pluginManager).getConnector(plugin.id()).supportedTypes().contains(type))
            .map(this::convert)
            .findAny();
    }

    @Override
    public String validateConnectorConfiguration(String type, String configuration) {
        Optional<ConnectorPluginEntity> candidate = this.findBySupportedType(type);
        if (candidate.isPresent()) {
            return validateConfiguration(candidate.get(), configuration);
        }
        return configuration;
    }

    private String validateConfiguration(ConnectorPluginEntity plugin, String configuration) {
        if (plugin != null && configuration != null) {
            String schema = getSchema(plugin.getId());
            return jsonSchemaService.validate(schema, configuration);
        }
        return configuration;
    }
}
