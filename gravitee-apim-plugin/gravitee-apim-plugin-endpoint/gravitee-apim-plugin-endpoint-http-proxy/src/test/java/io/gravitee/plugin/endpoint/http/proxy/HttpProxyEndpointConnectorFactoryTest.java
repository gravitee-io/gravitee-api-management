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
package io.gravitee.plugin.endpoint.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpClientOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HttpProxyEndpointConnectorFactoryTest {

    private HttpProxyEndpointConnectorFactory cut;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private DeploymentContext deploymentContext;

    @BeforeEach
    void beforeEach() {
        lenient().when(deploymentContext.getTemplateEngine()).thenReturn(templateEngine);
        lenient().when(templateEngine.convert(anyString())).thenAnswer(i -> i.getArgument(0));
        cut = new HttpProxyEndpointConnectorFactory(new PluginConfigurationHelper(null, new ObjectMapper()));
    }

    @Test
    void shouldSupportSyncApi() {
        assertThat(cut.supportedApi()).isEqualTo(ApiType.PROXY);
    }

    @Test
    void shouldSupportRequestResponseModes() {
        assertThat(cut.supportedModes()).contains(ConnectorMode.REQUEST_RESPONSE);
    }

    @ParameterizedTest
    @ValueSource(strings = { "wrong", "", "  ", "{\"unknown-key\":\"value\"}" })
    void shouldNotCreateConnectorWithWrongConfiguration(String configuration) {
        HttpProxyEndpointConnector connector = cut.createConnector(deploymentContext, configuration, SHARED_CONFIG);
        assertThat(connector).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "wrong", "", "  ", "{\"unknown-key\":\"value\"}" })
    void shouldNotCreateConnectorWithWrongSharedConfiguration(String sharedConfiguration) {
        HttpProxyEndpointConnector connector = cut.createConnector(deploymentContext, CONFIG, sharedConfiguration);
        assertThat(connector).isNull();
    }

    @Test
    void shouldCreateConnectorWithRightConfiguration() {
        HttpProxyEndpointConnector connector = cut.createConnector(deploymentContext, CONFIG, SHARED_CONFIG);
        assertThat(connector).isNotNull();
        assertThat(connector.configuration).isNotNull();
        verify(templateEngine).convert("https://localhost:8082/echo?foo=bar");
        verify(templateEngine).convert("localhost");
        verify(templateEngine).convert("user");
        verify(templateEngine).convert("pwd");
        verify(templateEngine).convert("Value1");
        verify(templateEngine).convert("Value2");
    }

    @Test
    void shouldNotCreateConnectorWithNullConfiguration() {
        HttpProxyEndpointConnector connector = cut.createConnector(deploymentContext, null, null);
        assertThat(connector).isNull();
    }

    @Test
    void shouldCreateConnectorWithMinimalConfiguration() {
        HttpProxyEndpointConnector connector = cut.createConnector(
            deploymentContext,
            "{\"target\": \"https://localhost:8082/echo?foo=bar\"}",
            null
        );
        assertThat(connector).isNotNull();
        assertThat(connector.configuration.getTarget()).isEqualTo("https://localhost:8082/echo?foo=bar");
        assertThat(connector.sharedConfiguration.getHeaders()).isNull();
        assertThat(connector.sharedConfiguration.getHttpOptions())
            .isNotNull()
            .usingRecursiveComparison()
            .isEqualTo(new HttpClientOptions());
        assertThat(connector.sharedConfiguration.getProxyOptions()).isNull();
        assertThat(connector.sharedConfiguration.getSslOptions()).isNull();
    }

    static final String CONFIG = "{\n" + "  \"target\": \"https://localhost:8082/echo?foo=bar\"\n" + "}";

    static final String SHARED_CONFIG =
        "{\n" +
        "       \"http\": {\n" +
        "           \"keepAlive\": true,\n" +
        "           \"followRedirects\": false,\n" +
        "           \"readTimeout\": 10000,\n" +
        "           \"idleTimeout\": 60000,\n" +
        "           \"keepAliveTimeout\": 30000,\n" +
        "           \"connectTimeout\": 5000,\n" +
        "           \"propagateClientAcceptEncoding\": true,\n" +
        "           \"useCompression\": false,\n" +
        "           \"maxConcurrentConnections\": 100,\n" +
        "           \"version\": \"HTTP_1_1\",\n" +
        "           \"pipelining\": false,\n" +
        "           \"clearTextUpgrade\": true\n" +
        "       },\n" +
        "       \"proxy\": {\n" +
        "           \"enabled\": false,\n" +
        "           \"useSystemProxy\": false,\n" +
        "           \"host\": \"localhost\",\n" +
        "           \"port\": 8080,\n" +
        "           \"username\": \"user\",\n" +
        "           \"password\": \"pwd\",\n" +
        "           \"type\": \"HTTP\"\n" +
        "       },\n" +
        "       \"headers\": [\n" +
        "           {\n" +
        "               \"name\": \"X-Header1\",\n" +
        "               \"value\": \"Value1\"\n" +
        "           },\n" +
        "           {\n" +
        "               \"name\": \"X-Header2\",\n" +
        "               \"value\": \"Value2\"\n" +
        "           }\n" +
        "       ],\n" +
        "       \"ssl\": {\n" +
        "           \"keyStore\": {\n" +
        "               \"type\": \"PKCS12\",\n" +
        "               \"content\": \"MIIG/Tsbfs1Cgn....\",\n" +
        "               \"password\": \"truststore-secret\"\n" +
        "           },\n" +
        "           \"hostnameVerifier\": false,\n" +
        "           \"trustStore\": {\n" +
        "               \"type\": \"PKCS12\",\n" +
        "               \"content\": \"MIIG/gIBAzCCBrcGCS....\",\n" +
        "               \"password\": \"truststore-secret\"\n" +
        "           },\n" +
        "           \"trustAll\": false\n" +
        "       }\n" +
        "}";
}
