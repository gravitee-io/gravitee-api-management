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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class Mqtt5EndpointConnectorFactoryTest {

    private Mqtt5EndpointConnectorFactory mqtt5EndpointConnectorFactory;
    private DeploymentContext deploymentContext;

    @BeforeEach
    void beforeEach() {
        deploymentContext = mock(DeploymentContext.class);
        mqtt5EndpointConnectorFactory = new Mqtt5EndpointConnectorFactory(new PluginConfigurationHelper(null, new ObjectMapper()));
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(mqtt5EndpointConnectorFactory.supportedApi()).isEqualTo(ApiType.MESSAGE);
    }

    @Test
    void shouldSupportSubscribePublishMode() {
        assertThat(mqtt5EndpointConnectorFactory.supportedModes()).contains(ConnectorMode.SUBSCRIBE, ConnectorMode.PUBLISH);
    }

    @ParameterizedTest
    @ValueSource(strings = { "wrong", "", "  ", "{\"unknown-key\":\"value\"}" })
    void shouldNotCreateConnectorWithWrongConfiguration(String configuration) {
        Mqtt5EndpointConnector connector = mqtt5EndpointConnectorFactory.createConnector(deploymentContext, configuration);
        assertThat(connector).isNull();
    }

    @Test
    void shouldCreateConnectorWithRightConfiguration() {
        Mqtt5EndpointConnector connector = mqtt5EndpointConnectorFactory.createConnector(
            deploymentContext,
            "{\"serverHost\":\"localhost\",\"serverPort\":\"1234\", \"consumer\":{\"topic\":\"test/topic\"}, \"producer\":{\"topic\":\"test/topic\"}}"
        );
        assertThat(connector).isNotNull();
        assertThat(connector.configuration()).isNotNull();
        assertThat(connector.configuration().getServerHost()).isEqualTo("localhost");
        assertThat(connector.configuration().getServerPort()).isEqualTo(1234);
        assertThat(connector.configuration().getConsumer()).isNotNull();
        assertThat(connector.configuration().getConsumer().isEnabled()).isFalse();
        assertThat(connector.configuration().getConsumer().getTopic()).isEqualTo("test/topic");
        assertThat(connector.configuration().getProducer()).isNotNull();
        assertThat(connector.configuration().getProducer().isEnabled()).isFalse();
        assertThat(connector.configuration().getProducer().getTopic()).isEqualTo("test/topic");
    }

    @Test
    void shouldCreateConnectorWithEmptyConfiguration() {
        Mqtt5EndpointConnector connector = mqtt5EndpointConnectorFactory.createConnector(deploymentContext, "{}");
        assertThat(connector).isNotNull();
        assertThat(connector.configuration()).isNotNull();
        assertThat(connector.configuration().getServerHost()).isNull();
        assertThat(connector.configuration().getServerPort()).isNull();
        assertThat(connector.configuration().getConsumer()).isNotNull();
        assertThat(connector.configuration().getProducer()).isNotNull();
    }

    @Test
    void shouldCreateConnectorWithNullConfiguration() {
        Mqtt5EndpointConnector connector = mqtt5EndpointConnectorFactory.createConnector(deploymentContext, null);
        assertThat(connector).isNotNull();
        assertThat(connector.configuration()).isNotNull();
        assertThat(connector.configuration().getServerHost()).isNull();
        assertThat(connector.configuration().getServerPort()).isNull();
        assertThat(connector.configuration().getConsumer()).isNotNull();
        assertThat(connector.configuration().getProducer()).isNotNull();
    }
}
