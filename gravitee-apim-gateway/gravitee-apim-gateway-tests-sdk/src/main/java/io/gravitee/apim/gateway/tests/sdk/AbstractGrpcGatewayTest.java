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

import io.gravitee.definition.model.Api;
import io.gravitee.gateway.reactor.ReactableApi;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpcio.client.GrpcIoClient;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractGrpcGatewayTest extends AbstractGatewayTest {

    public static final String LOCALHOST = "localhost";

    protected Vertx vertx;
    protected int grpcServerPort;

    protected HttpServer vertxServer;
    private GrpcIoClient client;

    @BeforeEach
    public void setupVertx(io.vertx.rxjava3.core.Vertx v) {
        // vertx-grpc needs io.vertx.core.Vertx and do not support the rxjava3 implementation,
        // so we need a manual instantiation instead of relying on the VertxExtension
        vertx = v.getDelegate();
    }

    @AfterEach
    public void tearDown() {
        if (vertxServer != null) {
            vertxServer.close(event -> {});
        }
        if (client != null) {
            client.close();
        }

        vertx.close();
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        grpcServerPort = getAvailablePort();
        if (isLegacyApi(definitionClass)) {
            updateEndpointsPort((Api) api.getDefinition(), grpcServerPort);
        } else if (isV4Api(definitionClass)) {
            updateEndpointsPort((io.gravitee.definition.model.v4.Api) api.getDefinition(), grpcServerPort);
        }
    }

    protected HttpServer createHttpServer(GrpcServer grpcServer) {
        vertxServer = vertx.createHttpServer(new HttpServerOptions().setPort(grpcServerPort).setHost(LOCALHOST));
        vertxServer.requestHandler(grpcServer);
        return vertxServer;
    }

    protected SocketAddress gatewayAddress() {
        return SocketAddress.inetSocketAddress(gatewayPort(), LOCALHOST);
    }

    protected GrpcIoClient getGrpcClient(Supplier<GrpcIoClient> factory) {
        this.client = factory.get();
        return this.client;
    }

    public final GrpcIoClient getGrpcClient() {
        return getGrpcClient(() -> GrpcIoClient.client(vertx));
    }
}
