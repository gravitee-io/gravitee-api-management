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

import io.gravitee.gateway.reactive.api.connector.ConnectorFactory;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import java.io.IOException;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component("EndpointConnectorPluginServiceImplV4")
public class EndpointConnectorPluginServiceImpl
    extends AbstractConnectorPluginService<EndpointConnectorPlugin<?, ?>>
    implements EndpointConnectorPluginService {

    public EndpointConnectorPluginServiceImpl(
        JsonSchemaService jsonSchemaService,
        ConfigurablePluginManager<EndpointConnectorPlugin<?, ?>> pluginManager
    ) {
        super(jsonSchemaService, pluginManager);
    }

    @Override
    protected ConnectorFactory<?> getConnectorFactory(final String connectorId) {
        return ((EndpointConnectorPluginManager) pluginManager).getFactoryById(connectorId, true);
    }

    @Override
    public String getSharedConfigurationSchema(final String connectorId) {
        try {
            logger.debug("Find endpoint shared configuration configuration schema by ID: {}", connectorId);
            return ((EndpointConnectorPluginManager) pluginManager).getSharedConfigurationSchema(connectorId, true);
        } catch (IOException ioex) {
            logger.error("An error occurs while trying to get endpoint shared configuration schema for plugin {}", connectorId, ioex);
            throw new TechnicalManagementException(
                "An error occurs while trying to get endpoint shared configuration schema for plugin " + connectorId,
                ioex
            );
        }
    }

    @Override
    public String validateSharedConfiguration(ConnectorPluginEntity endpointConnector, String configuration) {
        return validatePluginConfigurationAgainstSchema(endpointConnector.getId(), configuration, this::getSharedConfigurationSchema);
    }
}
