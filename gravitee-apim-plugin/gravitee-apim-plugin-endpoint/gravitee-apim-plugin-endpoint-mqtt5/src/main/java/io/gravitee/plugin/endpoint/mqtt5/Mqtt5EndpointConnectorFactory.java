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
package io.gravitee.plugin.endpoint.mqtt5;

import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.ConnectorHelper;
import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnectorFactory;
import io.gravitee.gateway.jupiter.api.context.DeploymentContext;
import io.gravitee.gateway.jupiter.api.exception.PluginConfigurationException;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.plugin.endpoint.mqtt5.configuration.Mqtt5EndpointConnectorConfiguration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class Mqtt5EndpointConnectorFactory implements EndpointAsyncConnectorFactory {

    private final ConnectorHelper connectorHelper;

    @Override
    public Set<ConnectorMode> supportedModes() {
        return Mqtt5EndpointConnector.SUPPORTED_MODES;
    }

    @Override
    public Set<Qos> supportedQos() {
        return Mqtt5EndpointConnector.SUPPORTED_QOS;
    }

    @Override
    public Mqtt5EndpointConnector createConnector(final DeploymentContext deploymentContext, final String configuration) {
        try {
            return new Mqtt5EndpointConnector(connectorHelper.readConfiguration(Mqtt5EndpointConnectorConfiguration.class, configuration));
        } catch (PluginConfigurationException e) {
            log.error("Can't create connector cause no valid configuration", e);
            return null;
        }
    }
}
