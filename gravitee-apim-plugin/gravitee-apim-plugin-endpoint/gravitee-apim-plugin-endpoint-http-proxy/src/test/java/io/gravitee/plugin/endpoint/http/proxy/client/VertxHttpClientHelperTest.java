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
package io.gravitee.plugin.endpoint.http.proxy.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.definition.model.v4.http.HttpClientOptions;
import io.gravitee.definition.model.v4.http.HttpProxyOptions;
import io.gravitee.definition.model.v4.ssl.SslOptions;
import io.gravitee.gateway.jupiter.http.vertx.client.VertxHttpClient;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.vertx.core.http.RequestOptions;
import io.vertx.rxjava3.core.Vertx;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class VertxHttpClientHelperTest {

    protected static final String DEFAULT_TARGET = "http://localhost:8080/team";

    @Mock
    private VertxHttpClient.VertxHttpClientBuilder vertxHttpClientBuilder;

    @Test
    void shouldCallBuilderWithArguments() {
        try (MockedStatic<VertxHttpClient> vertxHttpClientMockedStatic = Mockito.mockStatic(VertxHttpClient.class)) {
            vertxHttpClientMockedStatic.when(VertxHttpClient::builder).thenReturn(vertxHttpClientBuilder);

            final HttpProxyEndpointConnectorConfiguration configuration = new HttpProxyEndpointConnectorConfiguration();
            final Vertx vertx = mock(Vertx.class);
            final Configuration nodeConfiguration = mock(Configuration.class);
            final HttpClientOptions httpOptions = new HttpClientOptions();
            final SslOptions sslOptions = new SslOptions();
            final HttpProxyOptions proxyOptions = new HttpProxyOptions();

            configuration.setHttpOptions(httpOptions);
            configuration.setSslOptions(sslOptions);
            configuration.setProxyOptions(proxyOptions);

            when(vertxHttpClientBuilder.vertx(vertx)).thenReturn(vertxHttpClientBuilder);
            when(vertxHttpClientBuilder.nodeConfiguration(nodeConfiguration)).thenReturn(vertxHttpClientBuilder);
            when(vertxHttpClientBuilder.defaultTarget(DEFAULT_TARGET)).thenReturn(vertxHttpClientBuilder);
            when(vertxHttpClientBuilder.httpOptions(httpOptions)).thenReturn(vertxHttpClientBuilder);
            when(vertxHttpClientBuilder.sslOptions(sslOptions)).thenReturn(vertxHttpClientBuilder);
            when(vertxHttpClientBuilder.proxyOptions(proxyOptions)).thenReturn(vertxHttpClientBuilder);

            final VertxHttpClient vertxHttpClient = mock(VertxHttpClient.class);
            when(vertxHttpClientBuilder.build()).thenReturn(vertxHttpClient);
            VertxHttpClientHelper.buildHttpClient(vertx, nodeConfiguration, configuration, DEFAULT_TARGET);

            verify(vertxHttpClient).createHttpClient();
        }
    }

    @Test
    void shouldConfigureAbsoluteUri() {
        final RequestOptions requestOptions = new RequestOptions();
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

        parameters.put("foo", List.of("bar"));
        VertxHttpClientHelper.configureAbsoluteUri(requestOptions, "http://api.gravitee.io/echo?foo=bar1&hello=gravitee", parameters);

        assertThat(requestOptions.getURI()).isEqualTo("/echo?foo=bar1&hello=gravitee&foo=bar");
        assertThat(requestOptions.getHost()).isEqualTo("api.gravitee.io");
        assertThat(requestOptions.getPort()).isEqualTo(80);
        assertThat(requestOptions.isSsl()).isFalse();
    }

    @Test
    void shouldConfigureAbsoluteUriWithSslEnabled() {
        final RequestOptions requestOptions = new RequestOptions();
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

        parameters.put("foo", List.of("bar"));
        VertxHttpClientHelper.configureAbsoluteUri(requestOptions, "https://api.gravitee.io/echo?foo=bar1&hello=gravitee", parameters);

        assertThat(requestOptions.getURI()).isEqualTo("/echo?foo=bar1&hello=gravitee&foo=bar");
        assertThat(requestOptions.getHost()).isEqualTo("api.gravitee.io");
        assertThat(requestOptions.getPort()).isEqualTo(443);
        assertThat(requestOptions.isSsl()).isTrue();
    }
}
