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
package io.gravitee.gateway.standalone.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.server.ServerManager;
import io.gravitee.node.vertx.server.VertxServer;
import io.gravitee.node.vertx.server.VertxServerFactory;
import io.gravitee.node.vertx.server.VertxServerOptions;
import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
import io.gravitee.node.vertx.server.tcp.VertxTcpServerOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
class VertxReactorConfigurationTest {

    @Mock
    private VertxServerFactory<VertxServer<?, VertxServerOptions>, VertxServerOptions> serverFactory;

    @Captor
    private ArgumentCaptor<VertxServerOptions> optionsCaptor;

    private MockEnvironment environment;

    private final VertxReactorConfiguration cut = new VertxReactorConfiguration();

    @BeforeEach
    void init() {
        environment = new MockEnvironment();
    }

    @Test
    void should_create_server_manager_from_server_list() {
        final VertxServer vertxServer1 = mock(VertxServer.class);
        when(vertxServer1.id()).thenReturn("1");
        final VertxServer vertxServer2 = mock(VertxServer.class);
        when(vertxServer2.id()).thenReturn("2");

        when(serverFactory.create(any(VertxServerOptions.class))).thenReturn(vertxServer1).thenReturn(vertxServer2);

        environment.setProperty("servers[0].type", "http");
        environment.setProperty("servers[1].type", "http");

        final ServerManager serverManager = cut.serverManager(serverFactory, environment);

        assertThat(serverManager.servers()).isNotNull();
        assertThat(serverManager.servers()).containsExactlyInAnyOrder(vertxServer1, vertxServer2);
    }

    @Test
    void should_create_server_manager_from_server_list_with_default_8082_port_when_not_provided() {
        final VertxServer vertxServer1 = mock(VertxServer.class);

        when(serverFactory.create(any(VertxServerOptions.class))).thenReturn(vertxServer1);

        environment.setProperty("servers[0].type", "http");

        cut.serverManager(serverFactory, environment);

        verify(serverFactory).create(optionsCaptor.capture());

        assertThat(optionsCaptor.getValue().getPort()).isEqualTo(8082);
    }

    @Test
    void should_create_server_manager_from_server_list_with_default_specified_port() {
        final VertxServer vertxServer1 = mock(VertxServer.class);

        when(serverFactory.create(any(VertxServerOptions.class))).thenReturn(vertxServer1);

        environment.setProperty("servers[0].type", "http");
        environment.setProperty("servers[0].port", "1234");

        cut.serverManager(serverFactory, environment);

        verify(serverFactory).create(optionsCaptor.capture());

        assertThat(optionsCaptor.getValue().getPort()).isEqualTo(1234);
    }

    @Test
    void should_create_server_manager_from_single_http_server() {
        final VertxServer vertxServer = mock(VertxServer.class);

        when(serverFactory.create(any(VertxServerOptions.class))).thenReturn(vertxServer);

        final ServerManager serverManager = cut.serverManager(serverFactory, environment);

        assertThat(serverManager.servers()).isNotNull();
        assertThat(serverManager.servers()).containsExactly(vertxServer);
    }

    @Test
    void should_create_server_manager_from_single_http_server_with_default_8082_port_when_not_provided() {
        final VertxServer vertxServer = mock(VertxServer.class);

        when(serverFactory.create(any(VertxServerOptions.class))).thenReturn(vertxServer);

        cut.serverManager(serverFactory, environment);

        verify(serverFactory).create(optionsCaptor.capture());

        assertThat(optionsCaptor.getValue().getPort()).isEqualTo(8082);
    }

    @Test
    void should_create_server_manager_from_single_http_server_with_default_specified_port() {
        final VertxServer vertxServer = mock(VertxServer.class);

        environment.setProperty("http.port", "5678");

        when(serverFactory.create(any(VertxServerOptions.class))).thenReturn(vertxServer);

        cut.serverManager(serverFactory, environment);

        verify(serverFactory).create(optionsCaptor.capture());

        assertThat(optionsCaptor.getValue().getPort()).isEqualTo(5678);
    }

    @Test
    void should_create_http_and_tcp_server() {
        final VertxServer httpVertxServer = mock(VertxServer.class);
        when(httpVertxServer.id()).thenReturn("http");

        final VertxServer tcpVertxServer = mock(VertxServer.class);
        when(tcpVertxServer.id()).thenReturn("tcp");

        environment.setProperty("tcp.enabled", "true");
        environment.setProperty("tcp.secured", "true");
        environment.setProperty("tcp.ssl.sni", "true");

        when(serverFactory.create(any(VertxHttpServerOptions.class))).thenReturn(httpVertxServer);
        when(serverFactory.create(any(VertxTcpServerOptions.class))).thenReturn(tcpVertxServer);

        final ServerManager serverManager = cut.serverManager(serverFactory, environment);

        assertThat(serverManager.servers()).isNotNull();
        assertThat(serverManager.servers()).containsExactlyInAnyOrder(httpVertxServer, tcpVertxServer);
    }

    @ParameterizedTest
    @CsvSource({ "true,false", "false,false", "false,true" })
    void should_fail_creating_default_tcp_server(String secured, String sni) {
        // there is always a http server
        final VertxServer httpVertxServer = mock(VertxServer.class);
        when(httpVertxServer.id()).thenReturn("http");
        when(serverFactory.create(any(VertxHttpServerOptions.class))).thenReturn(httpVertxServer);

        environment.setProperty("tcp.enabled", "true");
        environment.setProperty("tcp.secured", secured);
        environment.setProperty("tcp.ssl.sni", sni);

        assertThatCode(() -> cut.serverManager(serverFactory, environment))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SNI");
    }

    @ParameterizedTest
    @CsvSource({ "true,false", "false,false", "false,true" })
    void should_fail_creating_multi_tcp_server(String secured, String sni) {
        environment.setProperty("servers[0].type", "tcp");
        environment.setProperty("servers[0].secured", secured);
        environment.setProperty("servers[0].ssl.sni", sni);

        assertThatCode(() -> cut.serverManager(serverFactory, environment))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SNI");
    }
}
