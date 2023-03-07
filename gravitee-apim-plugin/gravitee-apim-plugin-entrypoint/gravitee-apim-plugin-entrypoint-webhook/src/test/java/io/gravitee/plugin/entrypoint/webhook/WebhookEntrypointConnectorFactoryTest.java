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
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.spel.SpelTemplateEngine;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.gateway.reactive.api.qos.Qos;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class WebhookEntrypointConnectorFactoryTest {

    @Mock
    private DeploymentContext deploymentContext;

    private WebhookEntrypointConnectorFactory webhookEntrypointConnectorFactory;

    @BeforeEach
    void beforeEach() {
        webhookEntrypointConnectorFactory = new WebhookEntrypointConnectorFactory(new PluginConfigurationHelper(null, new ObjectMapper()));
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(webhookEntrypointConnectorFactory.supportedApi()).isEqualTo(ApiType.MESSAGE);
    }

    @Test
    void shouldSupportSubscribeMode() {
        assertThat(webhookEntrypointConnectorFactory.supportedModes()).contains(ConnectorMode.SUBSCRIBE);
    }

    @Test
    void shouldSupportQos() {
        assertThat(webhookEntrypointConnectorFactory.supportedQos()).containsOnly(Qos.NONE, Qos.AUTO, Qos.AT_MOST_ONCE, Qos.AT_LEAST_ONCE);
    }

    @Test
    void shouldSupportListenerType() {
        assertThat(webhookEntrypointConnectorFactory.supportedListenerType()).isEqualTo(ListenerType.SUBSCRIPTION);
    }

    @ParameterizedTest
    @ValueSource(strings = { "wrong", "", "  ", "{\"unknown-key\":\"value\"}" })
    void shouldNotCreateConnectorWithWrongConfiguration(String configuration) {
        WebhookEntrypointConnector connector = webhookEntrypointConnectorFactory.createConnector(
            deploymentContext,
            Qos.NONE,
            configuration
        );
        assertThat(connector).isNull();
    }

    @Test
    void shouldCreateConnectorWithRightConfiguration() {
        WebhookEntrypointConnector connector = webhookEntrypointConnectorFactory.createConnector(deploymentContext, Qos.NONE, "{}");
        assertThat(connector).isNotNull();
        assertThat(connector.configuration).isNotNull();
    }

    @Test
    void shouldCreateConnectorWithNullConfiguration() {
        WebhookEntrypointConnector connector = webhookEntrypointConnectorFactory.createConnector(deploymentContext, Qos.NONE, null);
        assertThat(connector).isNotNull();
        assertThat(connector.configuration).isNotNull();
    }

    @Test
    void shouldCreateConnectorWithStaticProxyConfiguration() throws Exception {
        when(deploymentContext.getTemplateEngine()).thenReturn(TemplateEngine.templateEngine());

        try (final var in = this.getClass().getResourceAsStream("/config/static-proxy.json")) {
            WebhookEntrypointConnector connector = webhookEntrypointConnectorFactory.createConnector(
                deploymentContext,
                Qos.NONE,
                new String(in.readAllBytes())
            );
            assertThat(connector).isNotNull();
            assertThat(connector.configuration).isNotNull();

            final var config = connector.configuration;
            assertThat(config).isNotNull();
            assertThat(config.getProxyOptions()).isNotNull();
            assertThat(config.getProxyOptions().getHost()).isNotNull().isEqualTo("proxy.host");
            assertThat(config.getProxyOptions().getUsername()).isNotNull().isEqualTo("proxy-user");
            assertThat(config.getProxyOptions().getPassword()).isNotNull().isEqualTo("password");
        }
    }

    @Test
    void shouldCreateConnectorWithDynamicProxyConfiguration() throws Exception {
        final TemplateEngine engine = TemplateEngine.templateEngine();
        engine
            .getTemplateContext()
            .setVariable("attributes", Map.of("host", "evaluated-host", "username", "evaluated-user", "password", "evalpass"));
        when(deploymentContext.getTemplateEngine()).thenReturn(engine);

        try (final var in = this.getClass().getResourceAsStream("/config/dynamic-proxy.json")) {
            WebhookEntrypointConnector connector = webhookEntrypointConnectorFactory.createConnector(
                deploymentContext,
                Qos.NONE,
                new String(in.readAllBytes())
            );
            assertThat(connector).isNotNull();
            assertThat(connector.configuration).isNotNull();

            final var config = connector.configuration;
            assertThat(config).isNotNull();
            assertThat(config.getProxyOptions()).isNotNull();
            assertThat(config.getProxyOptions().getHost()).isNotNull().isEqualTo("evaluated-host");
            assertThat(config.getProxyOptions().getUsername()).isNotNull().isEqualTo("evaluated-user");
            assertThat(config.getProxyOptions().getPassword()).isNotNull().isEqualTo("evalpass");
        }
    }
}
