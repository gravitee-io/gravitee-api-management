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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author GraviteeSource Team
 */
class MockEndpointConnectorFactoryTest {

    private MockEndpointConnectorFactory mockEndpointConnectorFactory;

    @BeforeEach
    void beforeEach() {
        mockEndpointConnectorFactory = new MockEndpointConnectorFactory();
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(mockEndpointConnectorFactory.supportedApi()).isEqualTo(ApiType.ASYNC);
    }

    @Test
    void shouldSupportSubscribeMode() {
        assertThat(mockEndpointConnectorFactory.supportedModes()).contains(ConnectorMode.SUBSCRIBE);
    }

    @ParameterizedTest
    @ValueSource(strings = { "wrong", "", "  ", "{\"unknown-key\":\"value\"}" })
    void shouldNotCreateConnectorWithWrongConfiguration(String configuration) {
        MockEndpointConnector connector = mockEndpointConnectorFactory.createConnector(configuration);
        assertThat(connector).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"messageContent\":\"my message content\"}" })
    void shouldCreateConnectorWithRightConfiguration(String configuration) {
        MockEndpointConnector connector = mockEndpointConnectorFactory.createConnector(configuration);
        assertThat(connector).isNotNull();
        assertThat(connector.configuration).isNotNull();
        assertThat(connector.configuration.getMessageContent()).isNotNull();
        assertThat(connector.configuration.getMessageInterval()).isNotNull();
    }

    @Test
    void shouldCreateConnectorWithNullConfiguration() {
        MockEndpointConnector connector = mockEndpointConnectorFactory.createConnector(null);
        assertThat(connector).isNotNull();
        assertThat(connector.configuration).isNotNull();
        assertThat(connector.configuration.getMessageContent()).isNotNull();
        assertThat(connector.configuration.getMessageInterval()).isNotNull();
    }
}
