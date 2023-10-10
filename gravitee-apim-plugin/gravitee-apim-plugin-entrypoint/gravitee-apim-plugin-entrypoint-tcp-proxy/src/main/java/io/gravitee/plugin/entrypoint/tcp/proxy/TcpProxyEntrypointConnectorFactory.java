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
package io.gravitee.plugin.entrypoint.tcp.proxy;

import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnectorFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.exception.PluginConfigurationException;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.plugin.entrypoint.tcp.proxy.configuration.TcpProxyEntrypointConnectorConfiguration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class TcpProxyEntrypointConnectorFactory implements EntrypointConnectorFactory<TcpProxyEntrypointConnector> {

    private final PluginConfigurationHelper connectorFactoryHelper;

    @Override
    public ApiType supportedApi() {
        return ApiType.PROXY;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return TcpProxyEntrypointConnector.SUPPORTED_MODES;
    }

    @Override
    public ListenerType supportedListenerType() {
        return TcpProxyEntrypointConnector.SUPPORTED_LISTENER_TYPE;
    }

    @Override
    public TcpProxyEntrypointConnector createConnector(DeploymentContext deploymentContext, String configuration) {
        try {
            return new TcpProxyEntrypointConnector(
                connectorFactoryHelper.readConfiguration(TcpProxyEntrypointConnectorConfiguration.class, configuration)
            );
        } catch (PluginConfigurationException e) {
            log.error("Can't create TCP entrypoint connector cause no valid configuration", e);
            return null;
        }
    }
}
