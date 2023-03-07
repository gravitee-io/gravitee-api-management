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
package io.gravitee.plugin.entrypoint.sse;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.gateway.reactive.api.qos.Qos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SseEntrypointConnectorFactoryTest {

    private SseEntrypointConnectorFactory sseEntrypointConnectorFactory;

    @Mock
    private DeploymentContext deploymentContext;

    @BeforeEach
    void beforeEach() {
        sseEntrypointConnectorFactory = new SseEntrypointConnectorFactory(new PluginConfigurationHelper(null, new ObjectMapper()));
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(sseEntrypointConnectorFactory.supportedApi()).isEqualTo(ApiType.MESSAGE);
    }

    @Test
    void shouldSupportSubscribeMode() {
        assertThat(sseEntrypointConnectorFactory.supportedModes()).contains(ConnectorMode.SUBSCRIBE);
    }

    @Test
    void shouldSupportQos() {
        assertThat(sseEntrypointConnectorFactory.supportedQos()).containsOnly(Qos.NONE, Qos.AUTO);
    }

    @Test
    void shouldSupportListenerType() {
        assertThat(sseEntrypointConnectorFactory.supportedListenerType()).isEqualTo(ListenerType.HTTP);
    }

    @ParameterizedTest
    @ValueSource(strings = { "wrong", "", "  ", "{\"unknown-key\":\"value\"}" })
    void shouldNotCreateConnectorWithWrongConfiguration(String configuration) {
        SseEntrypointConnector connector = sseEntrypointConnectorFactory.createConnector(deploymentContext, Qos.NONE, configuration);
        assertThat(connector).isNull();
    }

    @Test
    void shouldCreateConnectorWithNullConfiguration() {
        SseEntrypointConnector connector = sseEntrypointConnectorFactory.createConnector(deploymentContext, Qos.NONE, null);
        assertThat(connector).isNotNull();
    }
}
