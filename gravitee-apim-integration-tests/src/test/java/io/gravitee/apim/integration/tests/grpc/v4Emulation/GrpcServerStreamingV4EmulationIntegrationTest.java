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

import io.gravitee.apim.gateway.tests.sdk.AbstractGrpcGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.grpc.manualflowcontrol.HelloReply;
import io.gravitee.gateway.grpc.manualflowcontrol.HelloRequest;
import io.gravitee.gateway.grpc.manualflowcontrol.StreamingGreeterGrpc;
import io.gravitee.gateway.reactor.ReactableApi;
import io.vertx.core.http.HttpServer;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerResponse;
import io.vertx.grpcio.client.GrpcIoClient;
import io.vertx.grpcio.server.GrpcIoServer;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/grpc/streaming-greeter.json" })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class GrpcServerStreamingV4EmulationIntegrationTest extends AbstractGrpcGatewayTest {

    private static final int STREAM_MESSAGE_NUMBER = 3;
    private static final long STREAM_SLEEP_MILLIS = 10;

    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        super.configureApi(api, definitionClass);
        if (isLegacyApi(definitionClass)) {
            final Api definition = (Api) api.getDefinition();
            definition.setExecutionMode(ExecutionMode.V4_EMULATION_ENGINE);
        }
    }

    @Test
    void should_request_grpc_server(VertxTestContext testContext) {
        // start the backend
        GrpcIoServer grpcServer = GrpcIoServer.server(vertx);
        grpcServer.callHandler(
            StreamingGreeterGrpc.getSayHelloStreamingMethod(),
            request -> {
                GrpcServerResponse<HelloRequest, HelloReply> response = request.response();
                // end back three replies...
                request.handler(hello -> {
                    for (int i = 0; i < STREAM_MESSAGE_NUMBER; i++) {
                        response.write(HelloReply.newBuilder().setMessage("Hello " + hello.getName() + " part " + i).build());
                        try {
                            Thread.sleep(STREAM_SLEEP_MILLIS);
                        } catch (InterruptedException e) {
                            response.status(GrpcStatus.ABORTED);
                        }
                    }
                    //.. and end the stream
                    response.end();
                });
            }
        );

        // Message count checkpoint
        Checkpoint messageCounter = testContext.checkpoint(STREAM_MESSAGE_NUMBER);

        // create http server handled by gRPC
        HttpServer httpServer = createHttpServer(grpcServer);
        httpServer
            .listen()
            .andThen(handler -> {
                // dynamic client, and call the Gateway
                createGrpcClient()
                    .request(gatewayAddress(), StreamingGreeterGrpc.getSayHelloStreamingMethod())
                    .compose(request -> {
                        // send one request
                        request.end(HelloRequest.newBuilder().setName("You").build());
                        return request.response();
                    })
                    .onSuccess(response -> {
                        response.handler(helloReply -> {
                            // assert and count
                            assertThat(helloReply.getMessage()).startsWith("Hello You part");
                            messageCounter.flag();
                        });
                        response.exceptionHandler(err -> {
                            testContext.failNow(err.getMessage());
                        });
                        response.endHandler(v -> {
                            assertThat(testContext.completed()).isTrue();
                        });
                    })
                    .onComplete(response -> response.result().end())
                    .onFailure(testContext::failNow);
            });
    }

    protected GrpcIoClient createGrpcClient() {
        return getGrpcClient();
    }
}
