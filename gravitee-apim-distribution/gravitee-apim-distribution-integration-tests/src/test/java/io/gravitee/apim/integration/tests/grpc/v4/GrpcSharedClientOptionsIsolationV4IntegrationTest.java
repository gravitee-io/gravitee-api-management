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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * The gRPC client must be built from a copy of the endpoint's shared options. While it was forcing HTTP/2 on the
 * shared instance itself, one gRPC-flagged request moved every later request of the same endpoint to HTTP/2 as well,
 * for the lifetime of the node - so a single caller could change the protocol used to reach the backend for everybody.
 */
@GatewayTest
@DeployApi({ "/apis/v4/http/api.json" })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GrpcSharedClientOptionsIsolationV4IntegrationTest extends AbstractHttpTargetGrpcV4GatewayTest {

    @Test
    void a_grpc_request_does_not_move_the_following_requests_to_http_2(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

        callBackend(httpClient, GRPC_CONTENT_TYPE);
        callBackend(httpClient, null);

        List<LoggedRequest> backendRequests = wiremock.findAll(postRequestedFor(urlPathEqualTo("/endpoint")));
        assertThat(backendRequests).hasSize(2);
        assertThat(backendRequests.get(0).getProtocol()).isEqualTo("HTTP/2.0");
        assertThat(backendRequests.get(1).getProtocol()).isEqualTo("HTTP/1.1");
    }
}
