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
package io.gravitee.apim.integration.tests.grpc.v4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.gateway.grpc.manualflowcontrol.HelloReply;
import io.gravitee.gateway.grpc.manualflowcontrol.HelloRequest;
import io.gravitee.gateway.grpc.manualflowcontrol.StreamingGreeterGrpc;
import io.vertx.core.http.HttpServer;
import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerResponse;
import io.vertx.grpcio.server.GrpcIoServer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

    private static final int REQUEST_COUNT = 3;
    private static final int REPLIES = 2;

    private static final int TOTAL_REPLY = REQUEST_COUNT * REPLIES;

    @Test
    void should_request_and_get_response() {
        // to manage when to stop the test
        AtomicInteger replyCount = new AtomicInteger();

        // Backend service
        GrpcIoServer grpcServer = GrpcIoServer.server(vertx);
        grpcServer.callHandler(
            StreamingGreeterGrpc.getSayHelloStreamingMethod(),
            request -> {
                GrpcServerResponse<HelloRequest, HelloReply> response = request.response();
                request.handler(hello -> {
                    // send back 3 messages on each request
                    for (int i = 0; i < REPLIES; i++) {
                        response.write(HelloReply.newBuilder().setMessage("Reply %d to %s".formatted(i, hello.getName())).build());
                        try {
                            // not ideal, but enough to handle backpressure
                            Thread.sleep(STREAM_SLEEP_MILLIS);
                        } catch (InterruptedException e) {
                            response.status(GrpcStatus.ABORTED);
                        }
                    }
                    // stop the stream when enough request wher sent
                    if (replyCount.incrementAndGet() == TOTAL_REPLY) {
                        response.end();
                    }
                });
            }
        );

        // Need a new Vertx to avoid side effects with connection pools from previous tests
        AtomicLong timerId = new AtomicLong();

        // Create http server handled by gRPC
        HttpServer httpServer = createHttpServer(grpcServer);
        httpServer
            .listen()
            .andThen(handler -> {
                // capture progress data
                List<String> replies = new ArrayList<>();
                AtomicBoolean done = new AtomicBoolean();

                // call the remote service
                getGrpcClient()
                    .request(gatewayAddress(), StreamingGreeterGrpc.getSayHelloStreamingMethod())
                    .onSuccess(request -> {
                        AtomicInteger i = new AtomicInteger();
                        // request each 100ms, with a new message
                        long id = vertx.setPeriodic(
                            100,
                            ignore -> {
                                request.write(HelloRequest.newBuilder().setName("Request " + i.getAndIncrement()).build());
                            }
                        );
                        timerId.set(id);
                    })
                    .compose(GrpcClientRequest::response)
                    .onSuccess(response -> {
                        response.handler(reply -> {
                            // collect data
                            replies.add(reply.getMessage());
                            replyCount.incrementAndGet();
                        });
                    })
                    .onComplete(response -> {
                        // end gracefully
                        response.result().end();
                        vertx.cancelTimer(timerId.get());
                        done.set(true);
                    });

                await()
                    .atMost(30, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        assertThat(replies)
                            // we might have more, so checking at least what is planned
                            .containsAnyElementsOf(
                                List.of(
                                    "Reply 0 to Request 0",
                                    "Reply 1 to Request 0",
                                    "Reply 0 to Request 1",
                                    "Reply 1 to Request 1",
                                    "Reply 0 to Request 2",
                                    "Reply 1 to Request 2"
                                )
                            );
                        assertThat(done).isTrue();
                    });
            });
    }
}
