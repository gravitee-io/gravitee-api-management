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

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.gateway.tests.sdk.utils.HttpClientUtils.extractHeaders;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.integration.tests.fake.AddHeaderPolicy;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.reactor.ReactableApi;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class CorsV4EmulationIntegrationTest {

    @Nested
    @GatewayTest
    @DeployApi(
        {
            "/apis/http/cors-running-policies.json",
            "/apis/http/cors-not-running-policies.json",
            "/apis/http/cors-with-response-template.json",
        }
    )
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
                    assertThat(extractHeaders(response)).contains(
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

        @ParameterizedTest
        @ValueSource(strings = { "/api-cors-running-policies", "/api-cors-not-running-policies" })
        void should_return_400_status_when_preflight_request_is_invalid_and_policies_configured_to_run(String path, HttpClient httpClient)
            throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.OPTIONS, path)
                .flatMap(httpClientRequest ->
                    httpClientRequest.putHeader("Origin", "https://unknown.com").putHeader("Access-Control-Request-Method", "GET").rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(400);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }

        @Test
        void should_return_apply_response_template_when_preflight_request_is_invalid(HttpClient httpClient) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.OPTIONS, "/api-cors-with-response-template")
                .flatMap(httpClientRequest ->
                    httpClientRequest.putHeader("Origin", "https://unknown.com").putHeader("Access-Control-Request-Method", "GET").rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(412);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors()
                .assertValue(body -> {
                    assertThat(body.toString()).contains("Custom CORS error message");
                    return true;
                });
        }
    }

    @Nested
    @GatewayTest
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

        @ParameterizedTest
        @ValueSource(strings = { "/api-cors-running-policies", "/api-cors-not-running-policies" })
        void should_not_apply_policy_on_preflight_request_when_invalid(String path, HttpClient httpClient) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.OPTIONS, path)
                .flatMap(httpClientRequest ->
                    httpClientRequest.putHeader("Origin", "https://unknown.com").putHeader("Access-Control-Request-Method", "GET").rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(400);
                    assertThat(extractHeaders(response)).doesNotContainKeys(AddHeaderPolicy.HEADER_NAME);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/http/cors-running-policies.json", "/apis/http/cors-not-running-policies.json" })
    class CheckingRejection extends AbstractCorsIntegrationTest {

        @Test
        void should_reject_preflight_request_when_forbidden_origin(HttpClient httpClient) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.OPTIONS, "/api-cors-running-policies")
                .flatMap(httpClientRequest ->
                    httpClientRequest.putHeader("Origin", "https://unknown.com").putHeader("Access-Control-Request-Method", "GET").rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(extractHeaders(response)).doesNotContainKeys(
                        "Access-Control-Allow-Origin",
                        "Access-Control-Allow-Methods",
                        "Access-Control-Allow-Headers"
                    );

                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }

        @Test
        void should_reject_preflight_request_when_forbidden_header(HttpClient httpClient) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.OPTIONS, "/api-cors-running-policies")
                .flatMap(httpClientRequest ->
                    httpClientRequest
                        .putHeader("Origin", "https://mydomain.com")
                        .putHeader("Access-Control-Request-Method", "GET")
                        .putHeader("Access-Control-Request-Headers", "x-gravitee-test, x-gravitee-dev")
                        .rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(extractHeaders(response)).doesNotContainKeys("Access-Control-Allow-Methods", "Access-Control-Allow-Headers");

                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }

        @Test
        void should_reject_preflight_request_when_forbidden_method(HttpClient httpClient) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.OPTIONS, "/api-cors-running-policies")
                .flatMap(httpClientRequest ->
                    httpClientRequest
                        .putHeader("Origin", "https://mydomain.com")
                        .putHeader("Access-Control-Request-Method", "PUT")
                        .putHeader("Access-Control-Request-Headers", "x-gravitee-test")
                        .rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(extractHeaders(response)).doesNotContainKeys("Access-Control-Allow-Methods", "Access-Control-Allow-Headers");

                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/http/cors-running-policies.json" })
    class CheckingSecurityChainSkip extends AbstractCorsIntegrationTest {

        protected ApiKey apiKey;

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);

            apiKey = anApiKey(api);
            if (api.getDefinition() instanceof Api) {
                ((Api) api.getDefinition()).setPlans(
                    List.of(
                        Plan.builder()
                            .id("plan-id")
                            .api(api.getId())
                            .security("API_KEY")
                            .status("PUBLISHED")
                            .securityDefinition("{\"propagateApiKey\":true}")
                            .build()
                    )
                );
            }
        }

        @Override
        public void configureApi(Api api) {
            super.configureApi(api);
        }

        @Test
        void should_skip_security_on_preflight_request_when_valid(HttpClient httpClient) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.OPTIONS, "/api-cors-running-policies")
                .flatMap(httpClientRequest ->
                    httpClientRequest.putHeader("Origin", "https://mydomain.com").putHeader("Access-Control-Request-Method", "GET").rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }

        @Test
        void should_skip_security_on_preflight_request_when_invalid(HttpClient httpClient) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.OPTIONS, "/api-cors-running-policies")
                .flatMap(httpClientRequest ->
                    httpClientRequest.putHeader("Origin", "https://unknown.com").putHeader("Access-Control-Request-Method", "GET").rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isNotEqualTo(401);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }

        @Test
        void should_apply_security_chain_on_regular_request(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok()));

            httpClient
                .rxRequest(HttpMethod.GET, "/api-cors-running-policies")
                .flatMap(httpClientRequest ->
                    httpClientRequest.putHeader("Origin", "https://mydomain.com").putHeader("Access-Control-Request-Method", "GET").rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(401);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }

        private ApiKey anApiKey(ReactableApi<?> api) {
            final ApiKey apiKey = new ApiKey();
            apiKey.setApi(api.getId());
            apiKey.setApplication("application-id");
            apiKey.setSubscription("subscription-id");
            apiKey.setPlan("plan-id");
            apiKey.setKey("apiKeyValue");
            return apiKey;
        }
    }
}
