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
import io.vertx.junit5.Checkpoint;
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
public class GrpcSecuredServerStreamingJupiterIntegrationTest extends AbstractGrpcGatewayTest {

    private static final int MESSAGE_COUNT = 3;
    private static final int STREAM_SLEEP_MILLIS = 10;

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder
            .jupiterModeEnabled(true)
            .jupiterModeDefault("always")
            .httpSecured(true)
            .httpAlpn(true)
            .httpSslKeystoreType("self-signed");
    }

    @Test
    void should_request_secured_server(VertxTestContext testContext) throws InterruptedException {
        StreamingGreeterGrpc.StreamingGreeterImplBase service = buildRPCService();

        VertxServer rpcServer = createRpcServer(service);

        // Prepare gRPC Client
        ManagedChannel channel = createSecuredManagedChannel(event -> event.setUseAlpn(true).setSsl(true).setTrustAll(true));

        Checkpoint messageCounter = testContext.checkpoint(MESSAGE_COUNT);

        // Start is asynchronous
        rpcServer.start(
            event -> {
                // Get a stub to use for interacting with the remote service
                StreamingGreeterGrpc.StreamingGreeterStub stub = StreamingGreeterGrpc.newStub(channel);

                // Call the remote service
                final StreamObserver<HelloRequest> requestStreamObserver = stub.sayHelloStreaming(
                    new StreamObserver<>() {
                        @Override
                        public void onNext(HelloReply helloReply) {
                            messageCounter.flag();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            testContext.failNow(throwable.getMessage());
                        }

                        @Override
                        public void onCompleted() {
                            // TestContext should be completed thanks to the messageCounter
                            assertThat(testContext.completed()).isTrue();
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
            public StreamObserver<HelloRequest> sayHelloStreaming(StreamObserver<HelloReply> responseObserver) {
                return new StreamObserver<>() {
                    @Override
                    public void onNext(HelloRequest helloRequest) {
                        for (int i = 0; i < MESSAGE_COUNT; i++) {
                            final HelloReply helloReply = HelloReply
                                .newBuilder()
                                .setMessage("Hello " + helloRequest.getName() + ", part: " + i)
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
