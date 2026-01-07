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
package io.gravitee.plugin.endpoint.tcp.proxy;

import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.connector.endpoint.BaseEndpointConnectorFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.plugin.endpoint.tcp.proxy.configuration.TcpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.tcp.proxy.configuration.TcpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.plugin.endpoint.tcp.proxy.configuration.TcpProxyEndpointConnectorSharedConfigurationEvaluator;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@CustomLog
public class TcpProxyEndpointConnectorFactory implements BaseEndpointConnectorFactory<TcpProxyEndpointConnector> {

    private final PluginConfigurationHelper connectorFactoryHelper;

    @Override
    public ApiType supportedApi() {
        return ApiType.PROXY;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return TcpProxyEndpointConnector.SUPPORTED_MODES;
    }

    @Override
    public TcpProxyEndpointConnector createConnector(
        DeploymentContext deploymentContext,
        String configuration,
        String sharedConfiguration
    ) {
        try {
            TcpProxyEndpointConnectorSharedConfigurationEvaluator configurationEvaluator =
                new TcpProxyEndpointConnectorSharedConfigurationEvaluator(
                    connectorFactoryHelper.readConfiguration(TcpProxyEndpointConnectorSharedConfiguration.class, sharedConfiguration)
                );
            return new TcpProxyEndpointConnector(
                connectorFactoryHelper.readConfiguration(TcpProxyEndpointConnectorConfiguration.class, configuration),
                configurationEvaluator.evalNow(deploymentContext)
            );
        } catch (Exception e) {
            log.error("Can't create TCP endpoint connector because no valid configuration", e);
            return null;
        }
    }
}
