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

import io.gravitee.gateway.grpc.helloworld.GreeterGrpc;
import io.gravitee.gateway.grpc.helloworld.HelloReply;
import io.gravitee.gateway.standalone.AbstractGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.grpc.VertxChannelBuilder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

/**
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/grpc/unknown-endpoint.json")
public class GrpcUnknownEndpointTest extends AbstractGatewayTest {

    @Rule
    public final TestRule chain = RuleChain.outerRule(new ApiDeployer(this));

    @Test
    public void simple_grpc_request() throws InterruptedException {
        Vertx vertx = Vertx.vertx();

        // Wait for result
        CountDownLatch latch = new CountDownLatch(1);

        // Prepare gRPC Client
        ManagedChannel channel = VertxChannelBuilder.forAddress(vertx, "localhost", 8082).usePlaintext().build();

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
                    Assert.fail();
                }

                @Override
                public void onError(Throwable throwable) {
                    Assert.assertNotNull(throwable);
                    Assert.assertEquals(StatusRuntimeException.class, throwable.getClass());
                    Assert.assertEquals(Status.Code.UNAVAILABLE, ((StatusRuntimeException) throwable).getStatus().getCode());

                    latch.countDown();
                }

                @Override
                public void onCompleted() {}
            }
        );

        Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
        channel.shutdownNow();
    }
}
