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
package io.gravitee.apim.gateway.tests.sdk;

import io.gravitee.definition.model.Api;
import io.gravitee.gateway.reactor.ReactableApi;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.ClientOptionsBase;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractGrpcGatewayTest extends AbstractGatewayTest {

    public static final String LOCALHOST = "localhost";

    protected Vertx vertx;
    protected int grpcServerPort;

    protected VertxServer vertxServer;
    protected ManagedChannel managedChannel;

    @BeforeEach
    public void setup(io.vertx.rxjava3.core.Vertx v) {
        // vertx-grpc needs io.vertx.core.Vertx and do not support the rxjava3 implementation,
        // so we need a manual instantiation instead of relying on the VertxExtension
        vertx = v.getDelegate();
    }

    @AfterEach
    public void tearDown() {
        if (vertxServer != null) {
            vertxServer.shutdown(
                event -> {
                    if (managedChannel != null) {
                        managedChannel.shutdownNow();
                    }
                }
            );
        }

        if (managedChannel != null && !managedChannel.isShutdown()) {
            managedChannel.shutdownNow();
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

    protected VertxServer createRpcServer(BindableService service) {
        vertxServer = VertxServerBuilder.forAddress(vertx, LOCALHOST, grpcServerPort).addService(service).build();
        return vertxServer;
    }

    protected ManagedChannel createSecuredManagedChannel(Handler<ClientOptionsBase> clientOptionBase) {
        managedChannel = VertxChannelBuilder.forAddress(vertx, LOCALHOST, gatewayPort()).useSsl(clientOptionBase).build();
        return managedChannel;
    }

    protected ManagedChannel createManagedChannel() {
        managedChannel = VertxChannelBuilder.forAddress(vertx, LOCALHOST, gatewayPort()).usePlaintext().build();
        return managedChannel;
    }
}
