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
package io.gravitee.plugin.endpoint.mock;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class MockEndpointConnectorFactoryTest {

    @Mock
    private DeploymentContext deploymentContext;

    private MockEndpointConnectorFactory cut;

    @BeforeEach
    void beforeEach() {
        cut = new MockEndpointConnectorFactory(new PluginConfigurationHelper(null, new ObjectMapper()));
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(cut.supportedApi()).isEqualTo(ApiType.MESSAGE);
    }

    @Test
    void shouldSupportSubscribeMode() {
        assertThat(cut.supportedModes()).contains(ConnectorMode.SUBSCRIBE);
    }

    @ParameterizedTest
    @ValueSource(strings = { "wrong", "", "  ", "{\"unknown-key\":\"value\"}" })
    void shouldNotCreateConnectorWithWrongConfiguration(String configuration) {
        MockEndpointConnector connector = cut.createConnector(deploymentContext, configuration);
        assertThat(connector).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"messageContent\":\"my message content\"}" })
    void shouldCreateConnectorWithRightConfiguration(String configuration) {
        MockEndpointConnector connector = cut.createConnector(deploymentContext, configuration);
        assertThat(connector).isNotNull();
        assertThat(connector.configuration).isNotNull();
        assertThat(connector.configuration.getMessageContent()).isNotNull();
        assertThat(connector.configuration.getMessageInterval()).isNotNull();
    }

    @Test
    void shouldCreateConnectorWithNullConfiguration() {
        MockEndpointConnector connector = cut.createConnector(deploymentContext, null);
        assertThat(connector).isNotNull();
        assertThat(connector.configuration).isNotNull();
        assertThat(connector.configuration.getMessageContent()).isNotNull();
        assertThat(connector.configuration.getMessageInterval()).isNotNull();
    }
}
