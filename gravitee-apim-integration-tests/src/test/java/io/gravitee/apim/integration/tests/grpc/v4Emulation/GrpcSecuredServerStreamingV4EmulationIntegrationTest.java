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

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpcio.client.GrpcIoClient;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/grpc/streaming-greeter.json" })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class GrpcSecuredServerStreamingV4EmulationIntegrationTest extends GrpcServerStreamingV4EmulationIntegrationTest {

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder.httpSecured(true).httpAlpn(true).httpSslKeystoreType("self-signed");
    }

    public GrpcIoClient createGrpcClient() {
        return getGrpcClient(() -> GrpcIoClient.client(vertx, new HttpClientOptions().setUseAlpn(true).setSsl(true).setTrustAll(true)));
    }
}
