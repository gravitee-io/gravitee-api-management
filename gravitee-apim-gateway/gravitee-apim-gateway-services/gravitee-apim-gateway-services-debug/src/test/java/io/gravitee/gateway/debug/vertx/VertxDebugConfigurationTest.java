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
package io.gravitee.gateway.debug.vertx;

import static io.gravitee.gateway.debug.vertx.VertxDebugHttpClientConfiguration.MAX_CONNECTION_TIMEOUT;
import static io.gravitee.gateway.debug.vertx.VertxDebugHttpClientConfiguration.MAX_REQUEST_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.vertx.server.http.VertxHttpServer;
import io.gravitee.node.vertx.server.http.VertxHttpServerFactory;
import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxDebugConfigurationTest {

    @Mock
    private VertxHttpServerFactory debugHttpServerFactory;

    @Captor
    private ArgumentCaptor<VertxHttpServerOptions> optionsCaptor;

    private MockEnvironment environment;

    private final VertxDebugConfiguration cut = new VertxDebugConfiguration();

    @BeforeEach
    void init() {
        environment = new MockEnvironment();
    }

    @Test
    void should_create_debug_server_from_server_list() {
        final VertxHttpServer vertxServer1 = mock(VertxHttpServer.class);

        when(debugHttpServerFactory.create(any(VertxHttpServerOptions.class))).thenReturn(vertxServer1);

        environment.setProperty("servers[0].type", "http");
        environment.setProperty("servers[1].type", "http");

        final VertxHttpServer debugServer = cut.debugServer(debugHttpServerFactory, environment);

        assertThat(debugServer).isEqualTo(vertxServer1);
    }

    @Test
    void should_create_debug_server_from_server_list_with_default_8482_port_when_not_provided() {
        final VertxHttpServer vertxServer1 = mock(VertxHttpServer.class);

        when(debugHttpServerFactory.create(any(VertxHttpServerOptions.class))).thenReturn(vertxServer1);

        environment.setProperty("servers[0].type", "http");

        cut.debugServer(debugHttpServerFactory, environment);

        verify(debugHttpServerFactory).create(optionsCaptor.capture());

        assertThat(optionsCaptor.getValue().getPort()).isEqualTo(8482);
        assertThat(optionsCaptor.getValue().isHaProxyProtocol()).isFalse();
    }

    @Test
    void should_create_debug_server_manager_from_server_list_with_default_specified_debug_port() {
        final VertxHttpServer vertxServer1 = mock(VertxHttpServer.class);

        when(debugHttpServerFactory.create(any(VertxHttpServerOptions.class))).thenReturn(vertxServer1);

        environment.setProperty("servers[0].type", "http");
        environment.setProperty("debug.port", "1234");

        cut.debugServer(debugHttpServerFactory, environment);

        verify(debugHttpServerFactory).create(optionsCaptor.capture());

        assertThat(optionsCaptor.getValue().getPort()).isEqualTo(1234);
        assertThat(optionsCaptor.getValue().isHaProxyProtocol()).isFalse();
    }

    @Test
    void should_create_debug_server_manager_from_single_http_server() {
        final VertxHttpServer vertxServer = mock(VertxHttpServer.class);

        when(debugHttpServerFactory.create(any(VertxHttpServerOptions.class))).thenReturn(vertxServer);

        final VertxHttpServer debugServer = cut.debugServer(debugHttpServerFactory, environment);

        assertThat(debugServer).isEqualTo(vertxServer);
    }

    @Test
    void should_create_debug_server_manager_from_single_http_server_with_default_8482_port_when_not_provided() {
        final VertxHttpServer vertxServer = mock(VertxHttpServer.class);

        when(debugHttpServerFactory.create(any(VertxHttpServerOptions.class))).thenReturn(vertxServer);

        cut.debugServer(debugHttpServerFactory, environment);

        verify(debugHttpServerFactory).create(optionsCaptor.capture());

        assertThat(optionsCaptor.getValue().getPort()).isEqualTo(8482);
        assertThat(optionsCaptor.getValue().isHaProxyProtocol()).isFalse();
    }

    @Test
    void should_create_debug_server_manager_from_single_http_server_with_default_specified_debug_port() {
        final VertxHttpServer vertxServer = mock(VertxHttpServer.class);

        environment.setProperty("debug.port", "5678");

        when(debugHttpServerFactory.create(any(VertxHttpServerOptions.class))).thenReturn(vertxServer);

        cut.debugServer(debugHttpServerFactory, environment);

        verify(debugHttpServerFactory).create(optionsCaptor.capture());

        assertThat(optionsCaptor.getValue().getPort()).isEqualTo(5678);
        assertThat(optionsCaptor.getValue().isHaProxyProtocol()).isFalse();
    }

    @Test
    void should_create_debug_client_configuration_from_debug_server_options_and_default_timeouts() {
        final VertxHttpServerOptions debugServerOptions = VertxHttpServerOptions.builder()
            .port(1234)
            .host("somewhere.com")
            .secured(true)
            .openssl(true)
            .compressionSupported(true)
            .alpn(true)
            .build();

        final VertxHttpServer debugServer = mock(VertxHttpServer.class);
        when(debugServer.options()).thenReturn(debugServerOptions);

        final VertxDebugHttpClientConfiguration debugClientConfiguration = cut.debugHttpClientConfiguration(debugServer, environment);

        assertThat(debugClientConfiguration).isNotNull();
        assertThat(debugClientConfiguration.getPort()).isEqualTo(debugServerOptions.getPort());
        assertThat(debugClientConfiguration.getHost()).isEqualTo(debugServerOptions.getHost());
        assertThat(debugClientConfiguration.isSecured()).isEqualTo(debugServerOptions.isSecured());
        assertThat(debugClientConfiguration.isOpenssl()).isEqualTo(debugServerOptions.isOpenssl());
        assertThat(debugClientConfiguration.isCompressionSupported()).isEqualTo(debugServerOptions.isCompressionSupported());
        assertThat(debugClientConfiguration.isAlpn()).isEqualTo(debugServerOptions.isAlpn());
        assertThat(debugClientConfiguration.getConnectTimeout()).isEqualTo(5000);
        assertThat(debugClientConfiguration.getRequestTimeout()).isEqualTo(30000);
    }

    @Test
    void should_create_debug_client_configuration_from_debug_server_options_and_debug_timeouts() {
        environment.setProperty("debug.timeout.connect", "1234");
        environment.setProperty("debug.timeout.request", "4567");

        final VertxHttpServer debugServer = mock(VertxHttpServer.class);
        when(debugServer.options()).thenReturn(VertxHttpServerOptions.builder().build());

        final VertxDebugHttpClientConfiguration debugClientConfiguration = cut.debugHttpClientConfiguration(debugServer, environment);

        assertThat(debugClientConfiguration).isNotNull();
        assertThat(debugClientConfiguration.getConnectTimeout()).isEqualTo(1234);
        assertThat(debugClientConfiguration.getRequestTimeout()).isEqualTo(4567);
    }

    @Test
    void should_create_debug_client_configuration_from_debug_server_options_and_max_debug_timeouts() {
        environment.setProperty("debug.timeout.connect", "12340");
        environment.setProperty("debug.timeout.request", "45670");

        final VertxHttpServer debugServer = mock(VertxHttpServer.class);
        when(debugServer.options()).thenReturn(VertxHttpServerOptions.builder().build());

        final VertxDebugHttpClientConfiguration debugClientConfiguration = cut.debugHttpClientConfiguration(debugServer, environment);

        assertThat(debugClientConfiguration).isNotNull();
        assertThat(debugClientConfiguration.getConnectTimeout()).isEqualTo(MAX_CONNECTION_TIMEOUT);
        assertThat(debugClientConfiguration.getRequestTimeout()).isEqualTo(MAX_REQUEST_TIMEOUT);
    }
}
