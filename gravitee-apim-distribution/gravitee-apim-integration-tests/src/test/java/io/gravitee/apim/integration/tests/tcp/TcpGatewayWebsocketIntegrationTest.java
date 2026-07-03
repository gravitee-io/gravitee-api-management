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
package io.gravitee.apim.integration.tests.tcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.apim.gateway.tests.sdk.AbstractWebsocketGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.http.VertxWebSocketClientFactory;
import io.gravitee.node.vertx.client.ssl.SslOptions;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.tcp.proxy.TcpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.tcp.proxy.TcpProxyEntrypointConnectorFactory;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
class TcpGatewayWebsocketIntegrationTest extends AbstractWebsocketGatewayTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("tcp-proxy", EntrypointBuilder.build("tcp-proxy", TcpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("tcp-proxy", EndpointBuilder.build("tcp-proxy", TcpProxyEndpointConnectorFactory.class));
    }

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        // enables the TCP proxy
        gatewayConfigurationBuilder.enableTcpGateway().set("tcp.ssl.keystore.type", "self-signed");
    }

    @Test
    @DeployApi({ "/apis/v4/tcp/api-websocket.json" })
    @SneakyThrows
    void should_call_websocket_backend_via_TCP_port(VertxTestContext testContext, GatewayDynamicConfig.TcpConfig tcpConfig) {
        var serverConnected = testContext.checkpoint();
        var serverMessageSent = testContext.checkpoint();
        var serverMessageChecked = testContext.checkpoint();

        // simple websocket answering PONG when sent "PING"
        websocketServerHandler = serverWebSocket -> {
            serverConnected.flag();
            serverWebSocket.exceptionHandler(testContext::failNow);
            serverWebSocket.frameHandler(frame -> {
                if (frame.isText()) {
                    testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PING"));
                    serverWebSocket
                        .writeTextMessage("PONG")
                        .doOnComplete(serverMessageSent::flag)
                        .doOnError(testContext::failNow)
                        .subscribe();
                }
            });
        };

        //Use EtcHostConfigurer to use a valid hostname for SNI as Netty is checking it with this method SslUtils.isValidHostNameForSNI
        try (var etcHostsConfigurer = new EtcHostsConfigurer()) {
            etcHostsConfigurer.addHost("127.0.0.1", "myapi.tcp.local");

            // Configure hosts
            AddressResolverOptions resolverOptions = new AddressResolverOptions()
                .setHostsPath(etcHostsConfigurer.generateHostsFile())
                .setOptResourceEnabled(true)
                .setCacheMinTimeToLive(0)
                .setQueryTimeout(5000);

            VertxOptions vertxOptions = new VertxOptions().setAddressResolverOptions(resolverOptions);

            Vertx clientVertx = Vertx.vertx(vertxOptions);
            try {
                VertxWebSocketClientFactory.VertxWebSocketClientFactoryBuilder builder = VertxWebSocketClientFactory.builder()
                    .vertx(clientVertx)
                    .nodeConfiguration(mock(Configuration.class))
                    .defaultTarget("wss://localhost:" + tcpConfig.tcpPort())
                    .sslOptions(SslOptions.builder().hostnameVerifier(false).trustAll(true).build());

                var webSocketClient = builder.build().createWebSocketClient();

                // Given that a websocket is deployed
                // When calling the TCP proxy with an Api pointing to the websocket
                // Then we should expect a normal bidirectional websocket call
                webSocketClient
                    // path is not used by the TCP api, so it can be whatever we want
                    .connect("myapi.tcp.local", "/a-random-path")
                    .doOnSuccess(webSocket -> {
                        webSocket.exceptionHandler(testContext::failNow);
                        webSocket.frameHandler(frame -> {
                            if (frame.isText()) {
                                testContext.verify(() -> assertThat(frame.textData()).isEqualTo("PONG"));
                                serverMessageChecked.flag();
                            }
                        });
                        webSocket.writeTextMessage("PING").doOnError(testContext::failNow).subscribe();
                    })
                    .doOnError(testContext::failNow)
                    .test()
                    .awaitDone(5, TimeUnit.SECONDS);

                // Wait for the full PING-PONG exchange to complete before closing the Vert.x instance.
                // awaitDone() only waits for the connect() Single to emit; the frame exchange is async.
                testContext.awaitCompletion(5, TimeUnit.SECONDS);
            } finally {
                clientVertx.close().blockingAwait();
            }
        }
    }
}
