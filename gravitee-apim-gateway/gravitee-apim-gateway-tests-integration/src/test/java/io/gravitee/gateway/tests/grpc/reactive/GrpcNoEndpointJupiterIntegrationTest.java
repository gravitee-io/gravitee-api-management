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
import io.gravitee.gateway.grpc.helloworld.GreeterGrpc;
import io.gravitee.gateway.grpc.helloworld.HelloReply;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
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
@DeployApi({ "/apis/grpc/no-endpoint.json" })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class GrpcNoEndpointJupiterIntegrationTest extends AbstractGrpcGatewayTest {

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder.jupiterModeEnabled(true).jupiterModeDefault("always");
    }

    @Test
    void should_have_request_in_error_when_no_endpoint(VertxTestContext testContext) throws InterruptedException {
        // Prepare gRPC Client
        ManagedChannel channel = createManagedChannel();

        // Get a stub to use for interacting with the remote service
        GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);

        io.gravitee.gateway.grpc.helloworld.HelloRequest request = io.gravitee.gateway.grpc.helloworld.HelloRequest
            .newBuilder()
            .setName("David")
            .build();

        // Call the remote service
        stub.sayHello(
            request,
            new StreamObserver<>() {
                @Override
                public void onNext(HelloReply helloReply) {
                    testContext.failNow("Should not reply");
                }

                @Override
                public void onError(Throwable throwable) {
                    assertThat(throwable).isNotNull().isInstanceOf(StatusRuntimeException.class);
                    assertThat(((StatusRuntimeException) throwable).getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);

                    testContext.completeNow();
                }

                @Override
                public void onCompleted() {
                    testContext.failNow("Should not complete");
                }
            }
        );

        assertThat(testContext.awaitCompletion(10, TimeUnit.SECONDS)).isTrue();
    }
}
