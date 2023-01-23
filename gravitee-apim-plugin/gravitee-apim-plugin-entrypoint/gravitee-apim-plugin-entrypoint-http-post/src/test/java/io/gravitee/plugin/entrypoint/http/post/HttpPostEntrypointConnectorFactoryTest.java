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
package io.gravitee.plugin.entrypoint.http.post;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.ConnectorHelper;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.core.context.DefaultDeploymentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class HttpPostEntrypointConnectorFactoryTest {

    private HttpPostEntrypointConnectorFactory httpPostEntrypointConnectorFactory;

    @BeforeEach
    void beforeEach() {
        httpPostEntrypointConnectorFactory = new HttpPostEntrypointConnectorFactory(new ConnectorHelper(null, new ObjectMapper()));
    }

    @Test
    void shouldSupportSubscribeMode() {
        assertThat(httpPostEntrypointConnectorFactory.supportedModes()).containsOnly(ConnectorMode.PUBLISH);
    }

    @Test
    void shouldSupportQos() {
        assertThat(httpPostEntrypointConnectorFactory.supportedQos()).containsOnly(Qos.NONE, Qos.AUTO);
    }

    @Test
    void shouldSupportListenerType() {
        assertThat(httpPostEntrypointConnectorFactory.supportedListenerType()).isEqualTo(ListenerType.HTTP);
    }

    @ParameterizedTest
    @ValueSource(strings = { "wrong", "", "  " })
    void shouldCreateConnectorWithWrongConfiguration(String configuration) {
        HttpPostEntrypointConnector connector = httpPostEntrypointConnectorFactory.createConnector(
            new DefaultDeploymentContext(),
            Qos.NONE,
            configuration
        );
        assertThat(connector).isNull();
    }

    @Test
    void shouldCreateConnectorWithNullConfiguration() {
        HttpPostEntrypointConnector connector = httpPostEntrypointConnectorFactory.createConnector(
            new DefaultDeploymentContext(),
            Qos.NONE,
            null
        );
        assertThat(connector).isNotNull();
    }
}
