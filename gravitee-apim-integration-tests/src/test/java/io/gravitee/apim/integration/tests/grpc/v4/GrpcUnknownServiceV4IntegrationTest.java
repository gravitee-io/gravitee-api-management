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
import static org.assertj.core.api.Assertions.fail;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.gateway.grpc.manualflowcontrol.HelloReply;
import io.gravitee.gateway.grpc.manualflowcontrol.HelloRequest;
import io.gravitee.gateway.grpc.manualflowcontrol.StreamingGreeterGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.vertx.grpc.client.GrpcClientChannel;
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
@DeployApi({ "/apis/v4/grpc/invalid-path.json" })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class GrpcUnknownServiceV4IntegrationTest extends AbstractGrpcV4GatewayTest {

    @Test
    void should_request_and_not_get_response(VertxTestContext testContext) throws InterruptedException {
        // Get a stub to use for interacting with the remote service
        GrpcClientChannel channel = new GrpcClientChannel(getGrpcClient(), gatewayAddress());
        StreamingGreeterGrpc.StreamingGreeterStub stub = StreamingGreeterGrpc.newStub(channel);

        // Call the remote service, only to get a proper exception
        StreamObserver<HelloRequest> requestStreamObserver = stub.sayHelloStreaming(
            new StreamObserver<>() {
                @Override
                public void onNext(HelloReply helloReply) {
                    testContext.failNow("Should not receive a reply");
                }

                @Override
                public void onError(Throwable throwable) {
                    assertThat(throwable).isNotNull().isInstanceOf(StatusRuntimeException.class);
                    final StatusRuntimeException exception = (StatusRuntimeException) throwable;

                    assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNKNOWN);
                    testContext.completeNow();
                }

                @Override
                public void onCompleted() {
                    testContext.failNow("Should not complete");
                }
            }
        );

        requestStreamObserver.onNext(HelloRequest.newBuilder().setName("You").build());

        assertThat(testContext.awaitCompletion(10, TimeUnit.SECONDS)).isTrue();
    }
}
