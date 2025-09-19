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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith({ MockitoExtension.class })
class TcpProxyEndpointConnectorFactoryTest {

    TcpProxyEndpointConnectorFactory cut;

    @Mock
    DeploymentContext deploymentContext;

    @BeforeEach
    void before() {
        this.cut = new TcpProxyEndpointConnectorFactory(new PluginConfigurationHelper(null, new ObjectMapper()));
    }

    @Test
    void should_create_most_simple_connector() {
        TcpProxyEndpointConnector connector = cut.createConnector(
            deploymentContext,
            """
            {
                "target": {
                    "host": "www.acme.com",
                    "port": 8080
                }
            }
            """,
            """
            {
                "tcp": {
                    "connectTimeout": 200,
                    "reconnectAttempts": 10,
                    "reconnectInterval": 500
                }
            }"""
        );
        assertThat(cut.supportedModes()).containsExactly(ConnectorMode.SOCKET);
        assertThat(cut.supportedApi()).isEqualTo(ApiType.PROXY);

        assertThat(connector).isNotNull();
        assertThat(connector.id()).isEqualTo("tcp-proxy");
        assertThat(connector.supportedApi()).isEqualTo(ApiType.PROXY);
        assertThat(connector.supportedModes()).containsExactly(ConnectorMode.SOCKET);

        assertThat(connector.getConfiguration()).isNotNull();
        assertThat(connector.getConfiguration().getTcpTarget()).isNotNull();
        assertThat(connector.getConfiguration().getTcpTarget().getHost()).isEqualTo("www.acme.com");
        assertThat(connector.getConfiguration().getTcpTarget().getPort()).isEqualTo(8080);
        assertThat(connector.getSharedConfiguration()).isNotNull();
        assertThat(connector.getSharedConfiguration().getTcpClientOptions()).isNotNull();
        assertThat(connector.getSharedConfiguration().getTcpClientOptions().getReconnectAttempts()).isEqualTo(10);
        assertThat(connector.getSharedConfiguration().getTcpClientOptions().getConnectTimeout()).isEqualTo(200);
        assertThat(connector.getSharedConfiguration().getTcpClientOptions().getReconnectInterval()).isEqualTo(500);
    }

    @Test
    void should_not_create_connector() {
        TcpProxyEndpointConnector connector = cut.createConnector(
            deploymentContext,
            """
            {
                "target": {
                    "url": "https://www.acme.com:4443"
                }
            }
            """,
            null
        );
        assertThat(connector).isNull();
    }
}
