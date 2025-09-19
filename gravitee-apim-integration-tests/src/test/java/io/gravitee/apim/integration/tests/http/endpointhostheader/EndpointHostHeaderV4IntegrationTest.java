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
package io.gravitee.apim.integration.tests.http.endpointhostheader;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Gravitee Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EndpointHostHeaderV4IntegrationTest {

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/endpointhostheader/api-with-propagateClientHost-endpoint-option.json" })
    class WithPropagateClientHostOption extends AbstractEndpointHostHeaderIntegrationTest {

        @DisplayName("Should receive 200 - and call endpoint with client call Host header")
        @Test
        void should_return_200_and_call_endpoint_with_client_host_header(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(
                get("/endpoint").willReturn(
                    ok("response from backend").withBody("Host header was: {{request.headers.Host}}").withTransformers("response-template")
                )
            );

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(httpClientRequest -> httpClientRequest.putHeader("Host", "my.api.com").rxSend())
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertValue(body -> {
                    assertThat(body.toString()).isEqualTo("Host header was: my.api.com");
                    return true;
                })
                .assertNoErrors();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api.json" })
    class WithoutPropagateClientHostOption extends AbstractEndpointHostHeaderIntegrationTest {

        @DisplayName("Should receive 200 - and call endpoint with target Host header")
        @Test
        void should_return_200_and_call_endpoint_with_target_host_header(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(
                get("/endpoint").willReturn(
                    ok("response from backend").withBody("Host header was: {{request.headers.Host}}").withTransformers("response-template")
                )
            );

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(httpClientRequest -> httpClientRequest.putHeader("Host", "my.api.com").rxSend())
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertValue(body -> {
                    assertThat(body.toString()).isEqualTo("Host header was: localhost:" + wiremock.port());
                    return true;
                })
                .assertNoErrors();
        }
    }
}
