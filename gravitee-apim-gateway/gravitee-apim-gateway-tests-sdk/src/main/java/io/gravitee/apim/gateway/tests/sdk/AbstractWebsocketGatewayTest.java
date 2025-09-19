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
package io.gravitee.apim.gateway.tests.sdk;

import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.reactor.ReactableApi;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ParameterContext;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractWebsocketGatewayTest extends AbstractGatewayTest {

    protected HttpServer httpServer;
    protected int websocketPort;

    protected Handler<ServerWebSocket> websocketServerHandler;
    private Disposable serverDispose;

    @BeforeAll
    public void startWebsocketServer(Vertx vertx, VertxTestContext context) {
        final HttpServerOptions httpServerOptions = new HttpServerOptions();
        final int serverPort = getAvailablePort();
        httpServerOptions.setPort(serverPort);
        serverDispose = vertx
            .createHttpServer(httpServerOptions)
            .webSocketHandler(serverWebSocket -> {
                if (null != websocketServerHandler) {
                    websocketServerHandler.handle(serverWebSocket);
                }
            })
            .listen(serverPort)
            .subscribe(
                server -> {
                    httpServer = server;
                    context.completeNow();
                },
                context::failNow
            );
    }

    @AfterAll
    public void stopServers() {
        if (null != serverDispose) {
            serverDispose.dispose();
        }
        if (null != httpServer) {
            httpServer.close().subscribe();
        }
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        websocketPort = httpServer.actualPort();
        if (isLegacyApi(definitionClass)) {
            updateEndpointsPort((Api) api.getDefinition(), websocketPort);
        } else if (isV4Api(definitionClass)) {
            updateEndpointsPort((io.gravitee.definition.model.v4.Api) api.getDefinition(), websocketPort);
        }
    }

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        gatewayConfigurationBuilder.set("http.websocket.enabled", true);
        gatewayConfigurationBuilder.set("vertx.disableWebsockets", false);
    }

    @Override
    protected void configureHttpClient(
        HttpClientOptions options,
        GatewayDynamicConfig.Config gatewayConfig,
        ParameterContext parameterContext
    ) {
        options.setDefaultPort(gatewayConfig.httpPort()).setDefaultHost("localhost");
    }

    @BeforeEach
    public void beforeEachWebsocketTest(Vertx vertx) {
        wiremock.stop();
    }
}
