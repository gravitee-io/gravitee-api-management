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
package io.gravitee.plugin.entrypoint.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
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
        webhookEntrypointConnectorFactory = new WebhookEntrypointConnectorFactory();
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(webhookEntrypointConnectorFactory.supportedApi()).isEqualTo(ApiType.ASYNC);
    }

    @Test
    void shouldSupportSubscribeMode() {
        assertThat(webhookEntrypointConnectorFactory.supportedModes()).contains(ConnectorMode.PUBLISH);
    }

    @ParameterizedTest
    @ValueSource(strings = { "wrong", "", "  ", "{\"unknown-key\":\"value\"}" })
    void shouldNotCreateConnectorWithWrongConfiguration(String configuration) {
        WebhookEntrypointConnector connector = webhookEntrypointConnectorFactory.createConnector(configuration);
        assertThat(connector).isNull();
    }

    @Test
    void shouldCreateConnectorWithRightConfiguration() {
        WebhookEntrypointConnector connector = webhookEntrypointConnectorFactory.createConnector(
            "{\"type\": \"webhook\",\"callbackUrl\":\"http://localhost:8082/callback\",\"retry\":{\"attempts\": 3}, \"headers\":{\"header1\":\"value1\", \"header2\": \"value2\"}, \"authentication\":{\"type\": \"oauth2\", \"jwksEndpoint\": \"http://localhost:8082/jwks\", \"tokenEndpoint\": \"http://localhost:8082/token\", \"clientId\": \"my-clientId\", \"clientSecret\": \"my-clientSecret\", \"scopes\": [\"scope1\", \"scope2\"], \"secret\": \"my-secret\", \"username\": \"my-username\", \"password\": \"my-password\"}}"
        );
        assertThat(connector).isNotNull();
        assertThat(connector.configuration).isNotNull();
        assertThat(connector.configuration.getType()).isEqualTo("webhook");
        assertThat(connector.configuration.getCallbackUrl()).isEqualTo("http://localhost:8082/callback");
        assertThat(connector.configuration.getRetry()).isNotNull();
        assertThat(connector.configuration.getRetry().getAttempts()).isEqualTo(3);
        assertThat(connector.configuration.getHeaders()).isNotNull();
        assertThat(connector.configuration.getHeaders().get("header1")).isEqualTo("value1");
        assertThat(connector.configuration.getAuthentication()).isNotNull();
        assertThat(connector.configuration.getAuthentication().getType()).isEqualTo("oauth2");
        assertThat(connector.configuration.getAuthentication().getJwksEndpoint()).isEqualTo("http://localhost:8082/jwks");
        assertThat(connector.configuration.getAuthentication().getTokenEndpoint()).isEqualTo("http://localhost:8082/token");
        assertThat(connector.configuration.getAuthentication().getClientId()).isEqualTo("my-clientId");
        assertThat(connector.configuration.getAuthentication().getClientSecret()).isEqualTo("my-clientSecret");
        assertThat(connector.configuration.getAuthentication().getScopes()).isNotEmpty();
        assertThat(connector.configuration.getAuthentication().getScopes().get(0)).isEqualTo("scope1");
        assertThat(connector.configuration.getAuthentication().getSecret()).isEqualTo("my-secret");
        assertThat(connector.configuration.getAuthentication().getUsername()).isEqualTo("my-username");
        assertThat(connector.configuration.getAuthentication().getPassword()).isEqualTo("my-password");
    }

    @Test
    void shouldCreateConnectorWithNullConfiguration() {
        WebhookEntrypointConnector connector = webhookEntrypointConnectorFactory.createConnector(null);
        assertThat(connector).isNotNull();
        assertThat(connector.configuration).isNotNull();
        assertThat(connector.configuration.getCallbackUrl()).isNull();
        assertThat(connector.configuration.getRetry()).isNull();
        assertThat(connector.configuration.getHeaders()).isNull();
        assertThat(connector.configuration.getAuthentication()).isNull();
    }

    @Test
    void shouldCreateConnectorWithEmptyConfiguration() {
        WebhookEntrypointConnector connector = webhookEntrypointConnectorFactory.createConnector(null);
        assertThat(connector).isNotNull();
        assertThat(connector.configuration).isNotNull();
        assertThat(connector.configuration.getCallbackUrl()).isNull();
        assertThat(connector.configuration.getRetry()).isNull();
        assertThat(connector.configuration.getHeaders()).isNull();
        assertThat(connector.configuration.getAuthentication()).isNull();
    }
}
