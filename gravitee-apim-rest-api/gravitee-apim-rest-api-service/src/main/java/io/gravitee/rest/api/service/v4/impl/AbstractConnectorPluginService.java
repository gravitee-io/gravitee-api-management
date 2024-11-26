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

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorFeature;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.gateway.reactive.api.connector.ConnectorFactory;
import io.gravitee.gateway.reactive.api.connector.endpoint.async.HttpEndpointAsyncConnectorFactory;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnectorFactory;
import io.gravitee.gateway.reactive.api.connector.entrypoint.async.HttpEntrypointAsyncConnectorFactory;
import io.gravitee.plugin.core.api.ConfigurablePlugin;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.impl.AbstractPluginService;
import io.gravitee.rest.api.service.v4.ConnectorPluginService;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractConnectorPluginService<T extends ConfigurablePlugin<?>>
    extends AbstractPluginService<T, ConnectorPluginEntity>
    implements ConnectorPluginService {

    protected AbstractConnectorPluginService(final JsonSchemaService jsonSchemaService, final ConfigurablePluginManager<T> pluginManager) {
        super(jsonSchemaService, pluginManager);
    }

    @Override
    public Set<ConnectorPluginEntity> findAll() {
        // Need to preserve list order in Set
        return super.list().stream().map(this::convert).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public ConnectorPluginEntity findById(String entrypointPluginId) {
        T resourceDefinition = super.get(entrypointPluginId);
        return convert(resourceDefinition);
    }

    protected abstract ConnectorFactory<?> getConnectorFactory(final String connectorId);

    @Override
    protected ConnectorPluginEntity convert(Plugin plugin) {
        ConnectorPluginEntity entity = new ConnectorPluginEntity();

        entity.setId(plugin.id());
        entity.setDescription(plugin.manifest().description());
        entity.setName(plugin.manifest().name());
        entity.setIcon(getIcon(plugin.id()));
        entity.setVersion(plugin.manifest().version());
        entity.setDeployed(plugin.deployed());
        entity.setFeature(plugin.manifest().feature());
        ConnectorFactory<?> connectorFactory = getConnectorFactory(plugin.id());

        if (connectorFactory.supportedApi() != null) {
            entity.setSupportedApiType(ApiType.fromLabel(connectorFactory.supportedApi().getLabel()));
        }
        if (connectorFactory.supportedApi() == io.gravitee.gateway.reactive.api.ApiType.MESSAGE) {
            Set<io.gravitee.gateway.reactive.api.qos.Qos> supportedQos = extractSupportedQos(connectorFactory);
            if (supportedQos != null) {
                entity.setSupportedQos(
                    supportedQos
                        .stream()
                        .map(qos -> Qos.fromLabel(qos.getLabel()))
                        .sorted()
                        .collect(Collectors.toCollection(LinkedHashSet::new))
                );
            }
        }
        if (connectorFactory.supportedModes() != null) {
            entity.setSupportedModes(
                connectorFactory
                    .supportedModes()
                    .stream()
                    .map(connectorMode -> ConnectorMode.fromLabel(connectorMode.getLabel()))
                    .collect(Collectors.toSet())
            );
        }
        if (connectorFactory.supportedApi() != null) {
            entity.setSupportedApiType(ApiType.fromLabel(connectorFactory.supportedApi().getLabel()));
        }

        if (connectorFactory instanceof EntrypointConnectorFactory) {
            EntrypointConnectorFactory entrypointConnectorFactory = (EntrypointConnectorFactory) connectorFactory;
            if (entrypointConnectorFactory.supportedListenerType() != null) {
                entity.setSupportedListenerType(ListenerType.fromLabel(entrypointConnectorFactory.supportedListenerType().getLabel()));
            }
        }

        if (
            plugin.manifest().properties() != null &&
            plugin.manifest().properties().get("features") != null &&
            !plugin.manifest().properties().get("features").isEmpty()
        ) {
            entity.setAvailableFeatures(
                Arrays
                    .stream(plugin.manifest().properties().get("features").split(","))
                    .map(ConnectorFeature::fromLabel)
                    .collect(Collectors.toSet())
            );
        } else {
            entity.setAvailableFeatures(Collections.emptySet());
        }

        return entity;
    }

    private static Set<io.gravitee.gateway.reactive.api.qos.Qos> extractSupportedQos(ConnectorFactory<?> connectorFactory) {
        Set<io.gravitee.gateway.reactive.api.qos.Qos> supportedQos = null;
        if (connectorFactory instanceof HttpEntrypointAsyncConnectorFactory<?> httpEntrypointAsyncConnectorFactory) {
            supportedQos = httpEntrypointAsyncConnectorFactory.supportedQos();
        } else if (connectorFactory instanceof HttpEndpointAsyncConnectorFactory<?> httpEndpointAsyncConnectorFactory) {
            supportedQos = httpEndpointAsyncConnectorFactory.supportedQos();
        }
        return supportedQos;
    }

    @Override
    public Set<ConnectorPluginEntity> findBySupportedApi(final ApiType apiType) {
        return super
            .list()
            .stream()
            .filter(plugin ->
                ((EntrypointConnectorPluginManager) pluginManager).getFactoryById(plugin.id(), true)
                    .supportedApi()
                    .equals(io.gravitee.gateway.reactive.api.ApiType.fromLabel(apiType.getLabel()))
            )
            .map(this::convert)
            .collect(Collectors.toSet());
    }

    @Override
    public Set<ConnectorPluginEntity> findByConnectorMode(final ConnectorMode connectorMode) {
        return super
            .list()
            .stream()
            .filter(plugin ->
                ((EntrypointConnectorPluginManager) pluginManager).getFactoryById(plugin.id(), true)
                    .supportedModes()
                    .contains(io.gravitee.gateway.reactive.api.ConnectorMode.fromLabel(connectorMode.getLabel()))
            )
            .map(this::convert)
            .collect(Collectors.toSet());
    }

    @Override
    public String validateConnectorConfiguration(final String connectorPluginId, final String configuration) {
        ConnectorPluginEntity connectorPluginEntity = this.findById(connectorPluginId);
        return validateConfiguration(connectorPluginEntity.getId(), configuration);
    }

    @Override
    public String validateConnectorConfiguration(final ConnectorPluginEntity connectorPluginEntity, final String configuration) {
        return validateConfiguration(connectorPluginEntity.getId(), configuration);
    }

    /**
     * Gets the schema belonging to the plugin and validate given configuration
     * @param pluginId is the plugin for which validation is needed
     * @param configuration is the configuration to validate against the plugin's schema
     * @param schemaProvider is a function returning the schema for a given plugin id
     * @return the validated configuration
     */
    protected String validatePluginConfigurationAgainstSchema(String pluginId, String configuration, UnaryOperator<String> schemaProvider) {
        if (pluginId != null && configuration != null) {
            String schema = schemaProvider.apply(pluginId);
            return jsonSchemaService.validate(schema, configuration);
        }
        return configuration;
    }
}
