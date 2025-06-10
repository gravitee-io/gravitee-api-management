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
package io.gravitee.apim.integration.tests.http.cors;

import static io.gravitee.apim.gateway.tests.sdk.utils.HttpClientUtils.extractHeaders;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.integration.tests.fake.AddHeaderPolicy;
import io.gravitee.definition.model.ExecutionMode;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class CorsV3IntegrationTest extends CorsV4EmulationIntegrationTest {

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi({ "/apis/http/cors-running-policies.json", "/apis/http/cors-not-running-policies.json" })
    class CheckingResponseStatus extends AbstractCorsIntegrationTest {

        @ParameterizedTest
        @ValueSource(strings = { "/api-cors-running-policies", "/api-cors-not-running-policies" })
        void should_return_200_status_when_preflight_request_is_valid(String path, HttpClient httpClient) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.OPTIONS, path)
                .flatMap(httpClientRequest ->
                    httpClientRequest.putHeader("Origin", "https://mydomain.com").putHeader("Access-Control-Request-Method", "GET").rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response))
                        .contains(
                            Map.entry("Access-Control-Allow-Origin", "https://mydomain.com"),
                            Map.entry("Access-Control-Allow-Methods", "POST, GET"),
                            Map.entry("Access-Control-Allow-Headers", "x-gravitee-test")
                        );
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }

        // ⚠️with v3 engine the status is 200 when the preflight request is invalid and policies are configured to run
        // This behavior changes in V4 Emulation Engine and V4
        @ParameterizedTest
        @CsvSource({ "/api-cors-running-policies,200", "/api-cors-not-running-policies,400" })
        void should_return_200_status_when_preflight_request_is_invalid_and_policies_configured_to_run(
            String path,
            Integer expectedStatus,
            HttpClient httpClient
        ) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.OPTIONS, path)
                .flatMap(httpClientRequest ->
                    httpClientRequest.putHeader("Origin", "https://unknown.com").putHeader("Access-Control-Request-Method", "GET").rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(expectedStatus);
                    assertThat(extractHeaders(response))
                        .doesNotContainKeys("Access-Control-Allow-Origin", "Access-Control-Allow-Methods", "Access-Control-Allow-Headers");
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi({ "/apis/http/cors-running-policies.json", "/apis/http/cors-not-running-policies.json" })
    class CheckingPoliciesExecution extends AbstractCorsIntegrationTest {

        @Test
        void should_apply_policy_on_preflight_request_when_valid(HttpClient httpClient) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.OPTIONS, "/api-cors-running-policies")
                .flatMap(httpClientRequest ->
                    httpClientRequest.putHeader("Origin", "https://mydomain.com").putHeader("Access-Control-Request-Method", "GET").rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response)).contains(Map.entry(AddHeaderPolicy.HEADER_NAME, AddHeaderPolicy.RESPONSE_HEADER));
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }

        // ⚠️with v3 engine policies are executed when the preflight request is invalid
        // This behavior changes in V4 Emulation Engine and V4
        @Test
        void should_apply_policy_on_preflight_request_when_invalid(HttpClient httpClient) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.OPTIONS, "/api-cors-running-policies")
                .flatMap(httpClientRequest ->
                    httpClientRequest.putHeader("Origin", "https://unknown.com").putHeader("Access-Control-Request-Method", "GET").rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response)).contains(Map.entry(AddHeaderPolicy.HEADER_NAME, AddHeaderPolicy.RESPONSE_HEADER));
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi({ "/apis/http/cors-running-policies.json", "/apis/http/cors-not-running-policies.json" })
    class CheckingRejection extends CorsV4EmulationIntegrationTest.CheckingRejection {}

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi({ "/apis/http/cors-running-policies.json" })
    class CheckingSecurityChainSkip extends CorsV4EmulationIntegrationTest.CheckingSecurityChainSkip {}
}
