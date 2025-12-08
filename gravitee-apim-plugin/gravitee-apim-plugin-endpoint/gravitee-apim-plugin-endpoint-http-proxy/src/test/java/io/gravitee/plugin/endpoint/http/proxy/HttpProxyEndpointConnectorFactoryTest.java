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
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.plugin.configurations.http.HttpClientOptions;
import io.gravitee.plugin.configurations.http.HttpProxyOptions;
import io.gravitee.plugin.configurations.ssl.SslOptions;
import io.reactivex.rxjava3.core.Maybe;
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
    private TemplateContext templateContext;

    @Mock
    private DeploymentContext deploymentContext;

    @BeforeEach
    void beforeEach() {
        lenient().when(deploymentContext.getTemplateEngine()).thenReturn(templateEngine);
        lenient().when(templateEngine.getTemplateContext()).thenReturn(templateContext);
        lenient()
            .when(templateEngine.eval(anyString(), any()))
            .thenAnswer(i -> Maybe.just(i.getArgument(0)));
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
        verify(templateEngine).eval("https://localhost:8082/echo?foo=bar", String.class);
        verify(templateEngine).eval("localhost", String.class);
        verify(templateEngine).eval("user", String.class);
        verify(templateEngine).eval("pwd", String.class);
        verify(templateEngine).eval("Value1", String.class);
        verify(templateEngine).eval("Value2", String.class);
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
        assertThat(connector.sharedConfiguration.getProxyOptions())
            .isNotNull()
            .usingRecursiveComparison()
            .isEqualTo(new HttpProxyOptions());
        assertThat(connector.sharedConfiguration.getSslOptions()).isNotNull().usingRecursiveComparison().isEqualTo(new SslOptions());
    }

    static final String CONFIG = "{\n" + "  \"target\": \"https://localhost:8082/echo?foo=bar\"\n" + "}";

    static final String SHARED_CONFIG = """
        {
               "http": {
                   "keepAlive": true,
                   "followRedirects": false,
                   "readTimeout": 10000,
                   "idleTimeout": 60000,
                   "keepAliveTimeout": 30000,
                   "connectTimeout": 5000,
                   "propagateClientAcceptEncoding": true,
                   "useCompression": false,
                   "maxConcurrentConnections": 20,
                   "version": "HTTP_1_1",
                   "pipelining": false,
                   "clearTextUpgrade": true
               },
               "proxy": {
                   "enabled": false,
                   "useSystemProxy": false,
                   "host": "localhost",
                   "port": 8080,
                   "username": "user",
                   "password": "pwd",
                   "type": "HTTP"
               },
               "headers": [
                   {
                       "name": "X-Header1",
                       "value": "Value1"
                   },
                   {
                       "name": "X-Header2",
                       "value": "Value2"
                   }
               ],
               "ssl": {
                   "keyStore": {
                       "type": "PKCS12",
                       "content": "MIIG/Tsbfs1Cgn....",
                       "password": "truststore-secret"
                   },
                   "hostnameVerifier": false,
                   "trustStore": {
                       "type": "PKCS12",
                       "content": "MIIG/gIBAzCCBrcGCS....",
                       "password": "truststore-secret"
                   },
                   "trustAll": false
               }
        }""";
}
