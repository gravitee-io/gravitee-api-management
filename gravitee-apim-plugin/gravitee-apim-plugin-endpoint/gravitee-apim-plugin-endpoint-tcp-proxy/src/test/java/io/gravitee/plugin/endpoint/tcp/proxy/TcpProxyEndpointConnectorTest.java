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
package io.gravitee.plugin.endpoint.tcp.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.tcp.VertxTcpRequest;
import io.gravitee.gateway.reactive.tcp.VertxTcpResponse;
import io.gravitee.node.api.configuration.Configuration;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.net.NetServer;
import io.vertx.rxjava3.core.net.NetSocket;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith({ MockitoExtension.class })
class TcpProxyEndpointConnectorTest {

    TcpProxyEndpointConnectorFactory factory;

    @Mock
    DeploymentContext deploymentContext;

    @Mock
    private NetSocket proxySocket;

    private NetServer server;
    private Vertx vertx;

    @BeforeEach
    void before() {
        this.vertx = Vertx.vertx();
        this.factory = new TcpProxyEndpointConnectorFactory(new PluginConfigurationHelper(null, new ObjectMapper()));
        this.server = vertx
            .createNetServer()
            .connectHandler(socket -> {
                socket.rxWrite("hello!").subscribe();
            })
            .rxListen()
            .blockingGet();
    }

    @Test
    void should_connect_to_backend() {
        TcpProxyEndpointConnector connector = factory.createConnector(
            deploymentContext,
            """
            {
                "target": {
                    "host": "localhost",
                    "port": %d
                }
            }
            """.formatted(server.actualPort()),
            null
        );

        when(proxySocket.toFlowable()).thenReturn(Flowable.empty());
        VertxTcpRequest request = new VertxTcpRequest(proxySocket, new UUID());
        VertxTcpResponse response = new VertxTcpResponse(request);
        DefaultExecutionContext context = createContext(vertx, request, response);

        connector.connect(context).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

        response
            .chunks()
            .test()
            .awaitCount(1)
            .assertValue(buffer -> {
                assertThat(buffer).hasToString("hello!");
                return true;
            });
    }

    @AfterEach
    void after() {
        if (server != null) {
            server.close();
        }
    }

    private static DefaultExecutionContext createContext(Vertx vertx, VertxTcpRequest request, VertxTcpResponse response) {
        DefaultExecutionContext context = new DefaultExecutionContext(request, response);
        CustomComponentProvider componentProvider = new CustomComponentProvider();
        componentProvider.add(Vertx.class, vertx);
        componentProvider.add(Configuration.class, mock(Configuration.class));
        context.componentProvider(componentProvider);
        return context;
    }
}
