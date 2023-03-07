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
package io.gravitee.plugin.entrypoint.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.exception.PluginConfigurationException;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.plugin.entrypoint.websocket.configuration.WebSocketEntrypointConnectorConfiguration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class WebSocketEntrypointConnectorFactoryTest {

    protected static final String CONNECTOR_CONFIGURATION = "configuration";
    protected static final String MOCK_EXCEPTION = "Mock exception";

    @Mock
    private PluginConfigurationHelper pluginConfigurationHelper;

    @Mock
    private DeploymentContext deploymentContext;

    private WebSocketEntrypointConnectorFactory cut;

    @BeforeEach
    void init() {
        cut = new WebSocketEntrypointConnectorFactory(pluginConfigurationHelper);
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(cut.supportedApi()).isEqualTo(ApiType.MESSAGE);
    }

    @Test
    void shouldSupportPublishAndSubscribeModes() {
        assertThat(cut.supportedModes()).isEqualTo(Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE));
    }

    @Test
    void shouldSupportQos() {
        assertThat(cut.supportedQos()).containsOnly(Qos.NONE, Qos.AUTO);
    }

    @Test
    void shouldSupportListenerType() {
        assertThat(cut.supportedListenerType()).isEqualTo(ListenerType.HTTP);
    }

    @Test
    void shouldCreateConnector() throws PluginConfigurationException {
        when(pluginConfigurationHelper.readConfiguration(WebSocketEntrypointConnectorConfiguration.class, CONNECTOR_CONFIGURATION))
            .thenReturn(new WebSocketEntrypointConnectorConfiguration());

        final WebSocketEntrypointConnector connector = cut.createConnector(deploymentContext, Qos.NONE, CONNECTOR_CONFIGURATION);

        assertThat(connector).isNotNull();
    }

    @Test
    void shouldCreateNullConnectorWhenExceptionOccursGettingConfiguration() throws PluginConfigurationException {
        when(pluginConfigurationHelper.readConfiguration(WebSocketEntrypointConnectorConfiguration.class, CONNECTOR_CONFIGURATION))
            .thenThrow(new RuntimeException(MOCK_EXCEPTION));

        final WebSocketEntrypointConnector connector = cut.createConnector(deploymentContext, Qos.NONE, CONNECTOR_CONFIGURATION);

        assertThat(connector).isNull();
    }
}
