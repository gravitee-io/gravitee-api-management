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
 * The API deployed here is an ordinary HTTP proxy: its target is a plain {@code http://} backend and its endpoint
 * configuration says nothing about HTTP/2. What protocol the gateway uses to reach that backend is nevertheless
 * decided by the caller, because the endpoint connector classifies a request as gRPC on its {@code Content-Type}
 * alone.
 *
 * <p>This documents today's rule. A deliberate decision to stop trusting the request header - requiring the endpoint
 * to declare gRPC through its target scheme or its configured HTTP version - will make this test fail, which is the
 * point: the change should be visible rather than silent.
 */
@GatewayTest
@DeployApi({ "/apis/v4/http/api.json" })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GrpcContentTypeOnHttpTargetV4IntegrationTest extends AbstractHttpTargetGrpcV4GatewayTest {

    @Test
    void the_content_type_sent_by_the_caller_decides_the_protocol_used_to_reach_the_backend(HttpClient httpClient)
        throws InterruptedException {
        wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

        callBackend(httpClient, null);
        callBackend(httpClient, GRPC_CONTENT_TYPE);

        List<LoggedRequest> backendRequests = wiremock.findAll(postRequestedFor(urlPathEqualTo("/endpoint")));
        assertThat(backendRequests).hasSize(2);
        assertThat(backendRequests.get(0).getProtocol()).isEqualTo("HTTP/1.1");
        assertThat(backendRequests.get(1).getProtocol()).isEqualTo("HTTP/2.0");
    }
}
