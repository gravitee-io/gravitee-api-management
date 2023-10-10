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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ListenerType;
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
@ExtendWith(MockitoExtension.class)
class TcpProxyEntrypointConnectorFactoryTest {

    @Mock
    DeploymentContext deploymentContext;

    TcpProxyEntrypointConnectorFactory factory;

    @BeforeEach
    void setup() {
        factory = new TcpProxyEntrypointConnectorFactory(new PluginConfigurationHelper(null, new ObjectMapper()));
    }

    @Test
    void should_have_a_connector() throws InterruptedException {
        var connector = factory.createConnector(deploymentContext, "{}");
        assertThat(connector.id()).isEqualTo("tcp-proxy");
        assertThat(connector.supportedListenerType()).isEqualTo(ListenerType.TCP);
        assertThat(connector.supportedModes()).containsExactly(ConnectorMode.SOCKET);
        assertThat(connector.matches(null)).isTrue();
        assertThat(connector.configuration()).isNotNull();
        connector.handleRequest(null).test().await().assertComplete();
    }

    @Test
    void should_have_a_connector_no_config() {
        var connector = factory.createConnector(deploymentContext, null);
        assertThat(connector.configuration()).isNotNull();
    }

    @Test
    void should_fail_on_bad_config() {
        var connector = factory.createConnector(deploymentContext, "{\"message\": \"test\"}");
        assertThat(connector).isNull();
    }
}
