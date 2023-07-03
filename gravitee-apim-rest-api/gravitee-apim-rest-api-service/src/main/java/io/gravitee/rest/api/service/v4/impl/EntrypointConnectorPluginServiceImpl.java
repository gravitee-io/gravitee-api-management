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

import static java.util.Optional.ofNullable;

import io.gravitee.gateway.reactive.api.connector.ConnectorFactory;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import java.io.IOException;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("EntrypointPluginServiceImplV4")
public class EntrypointConnectorPluginServiceImpl
    extends AbstractConnectorPluginService<EntrypointConnectorPlugin<?, ?>>
    implements EntrypointConnectorPluginService {

    public EntrypointConnectorPluginServiceImpl(
        JsonSchemaService jsonSchemaService,
        ConfigurablePluginManager<EntrypointConnectorPlugin<?, ?>> pluginManager
    ) {
        super(jsonSchemaService, pluginManager);
    }

    @Override
    protected ConnectorFactory<?> getConnectorFactory(final String connectorId) {
        return ((EntrypointConnectorPluginManager) pluginManager).getFactoryById(connectorId, true);
    }

    @Override
    public String getSubscriptionSchema(final String connectorId, final SchemaDisplayFormat schemaDisplayFormat) {
        if (schemaDisplayFormat == SchemaDisplayFormat.GV_SCHEMA_FORM) {
            try {
                logger.debug("Find entrypoint subscription schema for format {} by ID: {}", schemaDisplayFormat, connectorId);
                String schema = pluginManager.getSchema(connectorId, "subscriptions/display-gv-schema-form", true);
                if (schema != null) {
                    return schema;
                }
                logger.debug("No specific schema-form exists for this display format. Fall back on default schema-form.");
            } catch (IOException ioex) {
                logger.debug(
                    "Error while getting specific specific schema-form for this display format. Fall back on default schema-form."
                );
            }
        }
        try {
            logger.debug("Find entrypoint subscription schema by ID: {}", connectorId);
            return ((EntrypointConnectorPluginManager) pluginManager).getSubscriptionSchema(connectorId, true);
        } catch (IOException ioex) {
            logger.error("An error occurs while trying to get entrypoint subscription schema for plugin {}", connectorId, ioex);
            throw new TechnicalManagementException(
                "An error occurs while trying to get entrypoint subscription schema for plugin " + connectorId,
                ioex
            );
        }
    }

    @Override
    public String validateEntrypointSubscriptionConfiguration(String entrypointId, String configuration) {
        // Only to check if plugin exists
        findById(entrypointId);
        return validatePluginConfigurationAgainstSchema(entrypointId, ofNullable(configuration).orElse("{}"), this::getSubscriptionSchema);
    }
}
