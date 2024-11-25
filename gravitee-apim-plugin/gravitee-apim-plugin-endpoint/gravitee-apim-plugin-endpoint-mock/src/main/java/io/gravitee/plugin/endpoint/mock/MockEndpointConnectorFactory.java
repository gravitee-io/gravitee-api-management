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
package io.gravitee.plugin.endpoint.mock;

import static io.gravitee.plugin.endpoint.mock.MockEndpointConnector.SUPPORTED_QOS;

import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.connector.endpoint.async.HttpEndpointAsyncConnectorFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.exception.PluginConfigurationException;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.plugin.endpoint.mock.configuration.MockEndpointConnectorConfiguration;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author GraviteeSource Team
 */
@Slf4j
@AllArgsConstructor
public class MockEndpointConnectorFactory implements HttpEndpointAsyncConnectorFactory<MockEndpointConnector> {

    private PluginConfigurationHelper pluginConfigurationHelper;

    @Override
    public Set<ConnectorMode> supportedModes() {
        return MockEndpointConnector.SUPPORTED_MODES;
    }

    @Override
    public Set<Qos> supportedQos() {
        return SUPPORTED_QOS;
    }

    @Override
    public MockEndpointConnector createConnector(
        final DeploymentContext deploymentContext,
        final String configuration,
        final String sharedConfiguration
    ) {
        try {
            return new MockEndpointConnector(
                pluginConfigurationHelper.readConfiguration(MockEndpointConnectorConfiguration.class, configuration)
            );
        } catch (PluginConfigurationException e) {
            log.error("Can't create connector cause no valid configuration", e);
            return null;
        }
    }
}
