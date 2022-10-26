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
package io.gravitee.plugin.entrypoint.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.ConnectorHelper;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class WebhookEntrypointConnectorFactoryTest {

    private WebhookEntrypointConnectorFactory webhookEntrypointConnectorFactory;

    @BeforeEach
    void beforeEach() {
        webhookEntrypointConnectorFactory = new WebhookEntrypointConnectorFactory(new ConnectorHelper(null, new ObjectMapper()));
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(webhookEntrypointConnectorFactory.supportedApi()).isEqualTo(ApiType.ASYNC);
    }

    @Test
    void shouldSupportSubscribeMode() {
        assertThat(webhookEntrypointConnectorFactory.supportedModes()).contains(ConnectorMode.SUBSCRIBE);
    }

    @Test
    void shouldSupportQos() {
        assertThat(webhookEntrypointConnectorFactory.supportedQos())
            .containsOnly(Qos.NONE, Qos.BALANCED, Qos.AT_BEST, Qos.AT_MOST_ONCE, Qos.AT_LEAST_ONCE);
    }

    @ParameterizedTest
    @ValueSource(strings = { "wrong", "", "  ", "{\"unknown-key\":\"value\"}" })
    void shouldNotCreateConnectorWithWrongConfiguration(String configuration) {
        WebhookEntrypointConnector connector = webhookEntrypointConnectorFactory.createConnector(Qos.NONE, configuration);
        assertThat(connector).isNull();
    }

    @Test
    void shouldCreateConnectorWithRightConfiguration() {
        WebhookEntrypointConnector connector = webhookEntrypointConnectorFactory.createConnector(Qos.NONE, "{}");
        assertThat(connector).isNotNull();
        assertThat(connector.configuration).isNotNull();
    }

    @Test
    void shouldCreateConnectorWithNullConfiguration() {
        WebhookEntrypointConnector connector = webhookEntrypointConnectorFactory.createConnector(Qos.NONE, null);
        assertThat(connector).isNotNull();
        assertThat(connector.configuration).isNotNull();
    }
}
