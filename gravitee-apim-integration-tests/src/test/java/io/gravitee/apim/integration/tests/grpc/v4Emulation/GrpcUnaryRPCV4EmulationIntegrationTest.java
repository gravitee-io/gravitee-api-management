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
package io.gravitee.apim.integration.tests.grpc.v4Emulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.gravitee.apim.gateway.tests.sdk.AbstractGrpcGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.grpc.helloworld.GreeterGrpc;
import io.gravitee.gateway.grpc.helloworld.HelloReply;
import io.gravitee.gateway.grpc.helloworld.HelloRequest;
import io.gravitee.gateway.reactor.ReactableApi;
import io.vertx.core.http.HttpServer;
import io.vertx.grpc.common.GrpcReadStream;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerResponse;
import io.vertx.grpcio.server.GrpcIoServer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/grpc/hello-world.json" })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class GrpcUnaryRPCV4EmulationIntegrationTest extends AbstractGrpcGatewayTest {

    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        super.configureApi(api, definitionClass);
        if (isLegacyApi(definitionClass)) {
            final Api definition = (Api) api.getDefinition();
            definition.setExecutionMode(ExecutionMode.V4_EMULATION_ENGINE);
        }
    }

    @Test
    void should_request_and_get_response() {
        // create the backend
        GrpcIoServer grpcServer = GrpcIoServer.server(vertx);
        grpcServer.callHandler(
            GreeterGrpc.getSayHelloMethod(),
            request -> {
                request.handler(hello -> {
                    // just a simple response
                    GrpcServerResponse<HelloRequest, HelloReply> response = request.response();
                    HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + hello.getName()).build();
                    response.end(reply);
                });
            }
        );

        // prep for test
        CountDownLatch latch = new CountDownLatch(1);

        // Create the backend HTTP Server handling gRPC
        HttpServer httpServer = createHttpServer(grpcServer);
        httpServer
            .listen()
            .andThen(handler -> {
                // call the service through the gateway
                getGrpcClient()
                    .request(gatewayAddress(), GreeterGrpc.getSayHelloMethod())
                    .compose(request -> {
                        request.end(HelloRequest.newBuilder().setName("You").build());
                        return request.response().compose(GrpcReadStream::last);
                    })
                    .onSuccess(helloReply -> {
                        assertThat(helloReply).isNotNull();
                        assertThat(helloReply.getMessage()).isEqualTo("Hello You");
                        latch.countDown();
                    })
                    .onFailure(failure -> {
                        failure.printStackTrace();
                        fail(failure.getMessage());
                    });

                await()
                    .atMost(30, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        assertThat(latch.getCount()).isZero();
                    });
            });
    }
}
