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
package io.gravitee.gateway.tests.grpc.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGrpcGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.gateway.grpc.manualflowcontrol.HelloReply;
import io.gravitee.gateway.grpc.manualflowcontrol.HelloRequest;
import io.gravitee.gateway.grpc.manualflowcontrol.StreamingGreeterGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.vertx.grpc.VertxServer;
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
public class GrpcServerStreamingJupiterIntegrationTest extends AbstractGrpcGatewayTest {

    private static final int STREAM_MESSAGE_NUMBER = 3;
    private static final long STREAM_SLEEP_MILLIS = 10;

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder.jupiterModeEnabled(true).jupiterModeDefault("always");
    }

    @Test
    void should_request_grpc_server(VertxTestContext testContext) throws InterruptedException {
        StreamingGreeterGrpc.StreamingGreeterImplBase service = buildRPCService();

        VertxServer rpcServer = createRpcServer(service);

        // Prepare gRPC Client
        ManagedChannel channel = createManagedChannel();

        // Start is asynchronous
        rpcServer.start(
            event -> {
                // Get a stub to use for interacting with the remote service
                StreamingGreeterGrpc.StreamingGreeterStub stub = StreamingGreeterGrpc.newStub(channel);

                // Call the remote service
                StreamObserver<HelloRequest> requestStreamObserver = stub.sayHelloStreaming(
                    new StreamObserver<>() {
                        @Override
                        public void onNext(HelloReply helloReply) {
                            assertThat(helloReply.getMessage()).startsWith("Hello You part");
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            testContext.failNow(throwable.getMessage());
                        }

                        @Override
                        public void onCompleted() {
                            testContext.completeNow();
                        }
                    }
                );

                requestStreamObserver.onNext(HelloRequest.newBuilder().setName("You").build());
            }
        );

        assertThat(testContext.awaitCompletion(10, TimeUnit.SECONDS)).isTrue();
    }

    private static StreamingGreeterGrpc.StreamingGreeterImplBase buildRPCService() {
        return new StreamingGreeterGrpc.StreamingGreeterImplBase() {
            @Override
            public StreamObserver<io.gravitee.gateway.grpc.manualflowcontrol.HelloRequest> sayHelloStreaming(
                StreamObserver<io.gravitee.gateway.grpc.manualflowcontrol.HelloReply> responseObserver
            ) {
                return new StreamObserver<>() {
                    @Override
                    public void onNext(io.gravitee.gateway.grpc.manualflowcontrol.HelloRequest helloRequest) {
                        for (int i = 0; i < STREAM_MESSAGE_NUMBER; i++) {
                            HelloReply helloReply = HelloReply
                                .newBuilder()
                                .setMessage("Hello " + helloRequest.getName() + " part " + i)
                                .build();
                            responseObserver.onNext(helloReply);

                            try {
                                Thread.sleep(STREAM_SLEEP_MILLIS);
                            } catch (InterruptedException e) {
                                responseObserver.onError(Status.ABORTED.asException());
                            }
                        }
                        responseObserver.onCompleted();
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
