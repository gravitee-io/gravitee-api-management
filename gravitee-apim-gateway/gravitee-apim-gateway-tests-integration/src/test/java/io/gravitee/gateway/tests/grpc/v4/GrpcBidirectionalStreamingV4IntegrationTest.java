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
package io.gravitee.gateway.tests.grpc.v4;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.gateway.grpc.manualflowcontrol.HelloReply;
import io.gravitee.gateway.grpc.manualflowcontrol.HelloRequest;
import io.gravitee.gateway.grpc.manualflowcontrol.StreamingGreeterGrpc;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import io.vertx.grpc.VertxServer;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/v4/grpc/streaming-greeter.json" })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class GrpcBidirectionalStreamingV4IntegrationTest extends AbstractGrpcV4GatewayTest {

    private static final int MESSAGE_COUNT = 3;

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder.jupiterModeEnabled(true).jupiterModeDefault("always");
    }

    @Override
    public void configureEntrypoints(final Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        super.configureEntrypoints(entrypoints);
    }

    @Test
    void should_request_and_get_response(VertxTestContext testContext) throws InterruptedException {
        StreamingGreeterGrpc.StreamingGreeterImplBase service = buildRPCService();

        VertxServer rpcServer = createRpcServer(service);

        // Prepare gRPC Client
        ManagedChannel channel = createManagedChannel();

        // Message count checkpoint
        Checkpoint messageCounter = testContext.checkpoint(MESSAGE_COUNT);

        // Start is asynchronous
        rpcServer.start(
            event -> {
                // Get a stub to use for interacting with the remote service
                StreamingGreeterGrpc.StreamingGreeterStub stub = StreamingGreeterGrpc.newStub(channel);

                // Call the remote service
                stub.sayHelloStreaming(
                    new ClientResponseObserver<HelloRequest, HelloReply>() {
                        private long timerID;
                        private ClientCallStreamObserver<HelloRequest> clientCallStreamObserver;

                        @Override
                        public void beforeStart(ClientCallStreamObserver<HelloRequest> clientCallStreamObserver) {
                            this.clientCallStreamObserver = clientCallStreamObserver;

                            // Adding a latency to simulate multi calls to grpc service
                            timerID =
                                vertx.setPeriodic(
                                    1000,
                                    periodic -> clientCallStreamObserver.onNext(HelloRequest.newBuilder().setName("You").build())
                                );
                        }

                        @Override
                        public void onNext(HelloReply helloReply) {
                            assertThat(helloReply.getMessage()).isEqualTo("Hello You");
                            messageCounter.flag();

                            if (testContext.completed()) {
                                vertx.cancelTimer(timerID);
                                clientCallStreamObserver.onCompleted();
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            testContext.failNow(throwable);
                        }

                        @Override
                        public void onCompleted() {
                            // TestContext should be completed thanks to the messageCounter
                            assertThat(testContext.completed()).isTrue();
                        }
                    }
                );
            }
        );

        assertThat(testContext.awaitCompletion(10, TimeUnit.SECONDS)).isTrue();
    }

    private static StreamingGreeterGrpc.StreamingGreeterImplBase buildRPCService() {
        return new StreamingGreeterGrpc.StreamingGreeterImplBase() {
            private int counter = MESSAGE_COUNT;

            @Override
            public StreamObserver<HelloRequest> sayHelloStreaming(StreamObserver<HelloReply> responseObserver) {
                return new StreamObserver<>() {
                    @Override
                    public void onNext(HelloRequest helloRequest) {
                        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build());

                        if (--counter == 0) {
                            responseObserver.onCompleted();
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {}

                    @Override
                    public void onCompleted() {}
                };
            }
        };
    }
}
