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
package io.gravitee.plugin.entrypoint.websocket;

import static io.gravitee.plugin.entrypoint.websocket.WebSocketEntrypointConnector.SUPPORTED_QOS;

import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.ConnectorHelper;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnectorFactory;
import io.gravitee.gateway.jupiter.api.context.DeploymentContext;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.plugin.entrypoint.websocket.configuration.WebSocketEntrypointConnectorConfiguration;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@AllArgsConstructor
public class WebSocketEntrypointConnectorFactory implements EntrypointAsyncConnectorFactory<WebSocketEntrypointConnector> {

    private final ConnectorHelper connectorHelper;

    @Override
    public Set<ConnectorMode> supportedModes() {
        return WebSocketEntrypointConnector.SUPPORTED_MODES;
    }

    @Override
    public Set<Qos> supportedQos() {
        return SUPPORTED_QOS;
    }

    @Override
    public WebSocketEntrypointConnector createConnector(
        final DeploymentContext deploymentContext,
        final Qos qos,
        final String configuration
    ) {
        try {
            return new WebSocketEntrypointConnector(
                qos,
                connectorHelper.readConfiguration(WebSocketEntrypointConnectorConfiguration.class, configuration)
            );
        } catch (Exception e) {
            log.error("Can't create connector cause no valid configuration", e);
            return null;
        }
    }
}
