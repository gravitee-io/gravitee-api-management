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
package io.gravitee.gateway.standalone.grpc;

import io.gravitee.gateway.grpc.manualflowcontrol.HelloReply;
import io.gravitee.gateway.grpc.manualflowcontrol.HelloRequest;
import io.gravitee.gateway.grpc.manualflowcontrol.StreamingGreeterGrpc;
import io.gravitee.gateway.standalone.AbstractGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.grpc.ManagedChannel;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.grpc.GrpcBidiExchange;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/grpc/streaming-greeter.json")
public class GrpcStreamingGreeterTest extends AbstractGatewayTest {

    @Rule
    public final TestRule chain = RuleChain.outerRule(new ApiDeployer(this));

    @Test
    public void simple_grpc_request() throws InterruptedException {
        Vertx vertx = Vertx.vertx(new VertxOptions().setPreferNativeTransport(true));

        // Prepare gRPC Server
        StreamingGreeterGrpc.StreamingGreeterVertxImplBase service = new StreamingGreeterGrpc.StreamingGreeterVertxImplBase() {

            private int counter = 3;

            @Override
            public void sayHelloStreaming(GrpcBidiExchange<io.gravitee.gateway.grpc.manualflowcontrol.HelloRequest, io.gravitee.gateway.grpc.manualflowcontrol.HelloReply> exchange) {
                exchange.handler(event -> {
                    exchange.write(HelloReply.newBuilder().setMessage("Hello " + event.getName()).build());

                    if (--counter == 0) {
                        exchange.end();
                    }
                }).endHandler(event -> {
                });
            }
        };

        VertxServer rpcServer = VertxServerBuilder
                .forAddress(vertx, "localhost", 50051)
                .addService(service)
                .build();

        // Wait for result
        CountDownLatch latch = new CountDownLatch(1);

        // Prepare gRPC Client
        ManagedChannel channel = VertxChannelBuilder
                .forAddress(vertx, "localhost", 8082)
                .usePlaintext(true)
                .build();

        // Start is asynchronous
        rpcServer.start(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                // Get a stub to use for interacting with the remote service
                StreamingGreeterGrpc.StreamingGreeterVertxStub stub = StreamingGreeterGrpc.newVertxStub(channel);

                // Call the remote service
                stub.sayHelloStreaming(new Handler<GrpcBidiExchange<HelloReply, io.gravitee.gateway.grpc.manualflowcontrol.HelloRequest>>() {

                    private int counter = 3;

                    @Override
                    public void handle(GrpcBidiExchange<HelloReply, io.gravitee.gateway.grpc.manualflowcontrol.HelloRequest> event) {
                        // Adding a latency to simulate multi calls to grpc service
                        long id = vertx.setPeriodic(1000, periodic -> event.write(HelloRequest.newBuilder().setName("David").build()));

                        event.handler(reply -> {
                            counter--;

                            if (counter == 0) {
                                vertx.cancelTimer(id);
                                event.end();
                            }
                        }).endHandler(event1 -> latch.countDown());
                    }
                });
            }
        });

        Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
        rpcServer.shutdown(event -> channel.shutdownNow());
    }
}
