/*
 * *
 *  * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.gravitee.plugin.endpoint.database.proxy;

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.connector.endpoint.sync.EndpointSyncConnectorFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.plugin.endpoint.database.proxy.configuration.DatabaseProxyEndpointConnectorConfiguration;
import java.util.Set;
import io.gravitee.plugin.endpoint.database.proxy.configuration.DatabaseProxyEndpointConnectorSharedConfiguration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class DatabaseProxyEndpointConnectorFactory implements EndpointSyncConnectorFactory<DatabaseProxyEndpointConnector> {

    private final PluginConfigurationHelper connectorFactoryHelper;

    @Override
    public Set<ConnectorMode> supportedModes() {
        return DatabaseProxyEndpointConnector.SUPPORTED_MODES;
    }

    @Override
    public DatabaseProxyEndpointConnector createConnector(
        final DeploymentContext deploymentContext,
        final String configuration,
        final String sharedConfiguration
    ) {
        try {
            return new DatabaseProxyEndpointConnector(
                eval(
                    deploymentContext,
                    connectorFactoryHelper.readConfiguration(DatabaseProxyEndpointConnectorConfiguration.class, configuration)
                ),
                eval(
                    deploymentContext,
                    connectorFactoryHelper.readConfiguration(DatabaseProxyEndpointConnectorSharedConfiguration.class, sharedConfiguration)
                )
            );
        } catch (Exception e) {
            log.error("Can't create connector because no valid configuration", e);
            return null;
        }
    }

    private DatabaseProxyEndpointConnectorConfiguration eval(
        final DeploymentContext deploymentContext,
        final DatabaseProxyEndpointConnectorConfiguration configuration
    ) {
        // Todo put something here to allow dynamic re-eval?
        return configuration;
    }

    private DatabaseProxyEndpointConnectorSharedConfiguration eval(
        final DeploymentContext deploymentContext,
        final DatabaseProxyEndpointConnectorSharedConfiguration groupConfiguration
    ) {
        // Todo put something here to allow dynamic re-eval?
        return groupConfiguration;
    }

    private String eval(TemplateEngine templateEngine, String value) {
        if (value != null && !value.isEmpty()) {
            return templateEngine.convert(value);
        }
        return value;
    }
}
