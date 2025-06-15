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
package io.gravitee.apim.integration.tests.http.attributes;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.gateway.tests.sdk.utils.HttpClientUtils.extractHeaders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.reactor.ReactableApi;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class AttributesIntegrationTest {

    private static final Pattern UUID_PATTERN = Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/http/attributes-to-headers.json" })
    class CheckingAttributes extends AbstractAttributesIntegrationTest {

        @Test
        void should_set_attributes(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok()));

            httpClient
                .rxRequest(HttpMethod.GET, "/attributes-to-headers")
                .flatMap(HttpClientRequest::rxSend)
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response))
                        .contains(
                            Map.entry(ExecutionContext.ATTR_APPLICATION, "1"),
                            Map.entry(ExecutionContext.ATTR_API, "attributes-to-headers"),
                            Map.entry(ExecutionContext.ATTR_CONTEXT_PATH, "/attributes-to-headers/"),
                            Map.entry(ExecutionContext.ATTR_PLAN, "default_plan"),
                            Map.entry(ExecutionContext.ATTR_SUBSCRIPTION_ID, "127.0.0.1")
                        );

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
    @DeployApi({ "/apis/http/attributes-to-headers.json" })
    class CheckingClientIdAttribute extends AbstractAttributesIntegrationTest {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            addApiKeyPlan(api);
        }

        @Test
        void should_generate_client_id_attribute_by_hashing_subscription_id_attribute_when_equals_to_remote_address(HttpClient httpClient)
            throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok()));

            httpClient
                .rxRequest(HttpMethod.GET, "/attributes-to-headers")
                .flatMap(HttpClientRequest::rxSend)
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);

                    assertThat(extractHeaders(response).get(ExecutionContext.ATTR_CLIENT_IDENTIFIER)).doesNotMatch(UUID_PATTERN);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }

        @Test
        void should_use_subscription_id_for_client_id_when_not_equal_to_remote_address(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok()));

            when(getBean(ApiKeyService.class).getByApiAndKey(any(), any())).thenReturn(Optional.of(apiKey));
            when(getBean(SubscriptionService.class).getByApiAndSecurityToken(eq(apiKey.getApi()), any(), eq(apiKey.getPlan())))
                .thenReturn(Optional.of(aSubscription()));

            httpClient
                .rxRequest(HttpMethod.GET, "/attributes-to-headers")
                .flatMap(httpClientRequest -> httpClientRequest.putHeader("X-Gravitee-Api-Key", apiKey.getKey()).rxSend())
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response)).contains(Map.entry(ExecutionContext.ATTR_CLIENT_IDENTIFIER, "subscription-id"));
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }

        @Test
        void should_generate_client_id_from_value_provided(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok()));

            httpClient
                .rxRequest(HttpMethod.GET, "/attributes-to-headers")
                .flatMap(httpClientRequest -> httpClientRequest.putHeader("X-Gravitee-Client-Identifier", "my-client-id").rxSend())
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response).get(ExecutionContext.ATTR_CLIENT_IDENTIFIER)).startsWith("my-client-id-");
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors();
        }

        @Test
        void should_use_client_id_provided(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok()));

            httpClient
                .rxRequest(HttpMethod.GET, "/attributes-to-headers")
                .flatMap(httpClientRequest ->
                    httpClientRequest
                        .putHeader("X-Gravitee-Transaction-Id", "1234")
                        .putHeader("X-Gravitee-Client-Identifier", "my-client-id-1234")
                        .rxSend()
                )
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(extractHeaders(response)).contains(Map.entry(ExecutionContext.ATTR_CLIENT_IDENTIFIER, "my-client-id-1234"));
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
    @DeployApi({ "/apis/http/mapped-path-attribute.json" })
    class CheckingMappedPathAttribute extends AbstractAttributesIntegrationTest {

        @Test
        void should_set_mapped_path_attribute(HttpClient httpClient) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.GET, "/mapped/test/mock")
                .flatMap(HttpClientRequest::rxSend)
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors()
                .assertValue(body -> {
                    assertThat(body.toString()).contains("/test/:testId");
                    return true;
                });
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/mapped-path-attribute.json" })
    class CheckingMappedPathAttributeV4 extends AbstractAttributesIntegrationTest {

        @Test
        void should_set_mapped_path_attribute(HttpClient httpClient) throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.GET, "/mapped/test/mock")
                .flatMap(HttpClientRequest::rxSend)
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete()
                .assertNoErrors()
                .assertValue(body -> {
                    assertThat(body.toString()).contains("/test/:testId");
                    return true;
                });
        }
    }
}
