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
package io.gravitee.plugin.entrypoint.http.get;

import static io.gravitee.plugin.entrypoint.http.get.HttpGetEntrypointConnector.SUPPORTED_QOS;

import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.ConnectorHelper;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnectorFactory;
import io.gravitee.gateway.jupiter.api.context.DeploymentContext;
import io.gravitee.gateway.jupiter.api.exception.PluginConfigurationException;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.plugin.entrypoint.http.get.configuration.HttpGetEntrypointConnectorConfiguration;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@AllArgsConstructor
public class HttpGetEntrypointConnectorFactory implements EntrypointAsyncConnectorFactory<HttpGetEntrypointConnector> {

    private ConnectorHelper connectorHelper;

    @Override
    public Set<ConnectorMode> supportedModes() {
        return HttpGetEntrypointConnector.SUPPORTED_MODES;
    }

    @Override
    public Set<Qos> supportedQos() {
        return SUPPORTED_QOS;
    }

    @Override
    public HttpGetEntrypointConnector createConnector(DeploymentContext deploymentContext, final Qos qos, final String configuration) {
        try {
            return new HttpGetEntrypointConnector(
                qos,
                connectorHelper.readConfiguration(HttpGetEntrypointConnectorConfiguration.class, configuration)
            );
        } catch (PluginConfigurationException e) {
            log.error("Can't create connector cause no valid configuration", e);
            return null;
        }
    }
}
