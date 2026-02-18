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
package io.gravitee.apim.integration.tests.http.failover;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.gateway.tests.sdk.utils.URLUtils.exchangePort;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.dynamicrouting.DynamicRoutingPolicy;
import io.gravitee.policy.dynamicrouting.configuration.DynamicRoutingPolicyConfiguration;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.NetServerOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import io.vertx.rxjava3.core.net.NetServer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class FailoverV3IntegrationTest {

    public static final String RESPONSE_FROM_BACKEND = "ok from backend";

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    class CircuitBreakingCases extends AbstractGatewayTest {

        final Function<HttpClient, Single<HttpClientResponse>> requestSupplier = client ->
            client.rxRequest(HttpMethod.GET, "/test").flatMap(HttpClientRequest::rxSend);

        @Test
        @DeployApi("/apis/http/failover/api-only-one-endpoint.json")
        void should_fail_and_open_circuit_after_five_failures(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // And Given the backend answers in 750ms
            wiremock.stubFor(get("/endpoint").willReturn(ok(RESPONSE_FROM_BACKEND).withFixedDelay(750)));

            // When requesting five times the API
            final List<Single<HttpClientResponse>> listOfParallelCalls = List.of(
                requestSupplier.apply(client),
                requestSupplier.apply(client),
                requestSupplier.apply(client),
                requestSupplier.apply(client),
                requestSupplier.apply(client)
            );
            final TestSubscriber<HttpClientResponse> test = Single.merge(listOfParallelCalls)
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete();
            for (int i = 0; i < listOfParallelCalls.size(); i++) {
                test.assertValueAt(i, response -> {
                    // Then the API response should be 502
                    assertThat(response.statusCode()).isEqualTo(502);
                    return true;
                });
            }
            // Then the backend should have been called 4 times (5 request with one initial attempt and two retries)
            wiremock.verify(15, getRequestedFor(urlPathEqualTo("/endpoint")));

            // When doing a new request to the API
            wiremock.resetRequests();
            requestSupplier
                .apply(client)
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response should be 502
                    assertThat(response.statusCode()).isEqualTo(502);
                    return true;
                });

            // Then circuit breaker should be OPEN, and do not propagate request to the backend
            wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    // Due to Failover implementation in V3, requests are not properly cancelled and it seems to make some test fail, that's why we force the order here.
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class OnlyOneEndpointInGroup extends AbstractGatewayTest {

        @Test
        @DeployApi("/apis/http/failover/api-only-one-endpoint.json")
        @Order(1)
        void should_not_retry_on_fast_call(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with one endpoint
            // And Given the backend answers immediately
            wiremock.stubFor(get("/endpoint").willReturn(ok(RESPONSE_FROM_BACKEND)));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the response body should be right
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND);
                    return true;
                });
            // Then the backend should have been called 1 time
            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint")));
        }

        @Test
        @DeployApi("/apis/http/failover/api-only-one-endpoint.json")
        @Order(2)
        void should_retry_and_fail_on_slow_call(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with one endpoint
            // And Given the backend answers in 750ms
            wiremock.stubFor(get("/endpoint").willReturn(ok(RESPONSE_FROM_BACKEND).withFixedDelay(750)));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response should be 502
                    assertThat(response.statusCode()).isEqualTo(502);
                    return true;
                });
            // We expect three calls: the first attempt + the 2 retries
            // Then the backend should have been called 3 times (one initial request + 2 attempts)
            wiremock.verify(3, getRequestedFor(urlPathEqualTo("/endpoint")));
        }

        @Test
        @DeployApi("/apis/http/failover/api-only-one-endpoint.json")
        @Order(5)
        void should_retry_and_fail_on_connection_exception(HttpClient client, Vertx vertx) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with one endpoint
            // And Given the backend throw an exception on connection
            final int port = wiremock.port();
            wiremock.stop();
            final NetServerOptions options = new NetServerOptions();
            options.setPort(port);
            final NetServer netServer = vertx.createNetServer(options);
            AtomicInteger connectionCount = new AtomicInteger(0);
            netServer.connectHandler(socket -> {
                connectionCount.incrementAndGet();
                throw new RuntimeException("Unable to connect to the remote server");
            });
            netServer.listen().subscribe();

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response should be 502
                    assertThat(response.statusCode()).isEqualTo(502);
                    return true;
                });
            // We expect three calls: the first attempt + the 2 retries
            // Then the backend should have been called 3 times (one initial request + 2 attempts)
            assertThat(connectionCount.get()).isEqualTo(3);
            netServer.rxClose().subscribe();
        }

        /**
         * This test ensures the retries are done with the body each time, as body is normally consumable only once.
         */
        @Test
        @DeployApi("/apis/http/failover/api-only-one-endpoint.json")
        @Order(4)
        void should_retry_and_fail_on_slow_call_posting_payload(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with one endpoint
            // And Given the backend answers in 750ms
            wiremock.stubFor(post("/endpoint").willReturn(ok(RESPONSE_FROM_BACKEND).withFixedDelay(750)));

            // When requesting the API
            client
                .rxRequest(HttpMethod.POST, "/test")
                .flatMap(request -> request.rxSend(Buffer.buffer("body")))
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response should be 502
                    assertThat(response.statusCode()).isEqualTo(502);
                    return true;
                });
            // Then the backend should have been called 3 times (one initial request + 2 attempts)
            wiremock.verify(3, postRequestedFor(urlPathEqualTo("/endpoint")).withRequestBody(equalTo("body")));
        }

        @Test
        @DeployApi("/apis/http/failover/api-only-one-endpoint.json")
        @Order(3)
        void should_success_on_first_retry(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with one endpoint
            // And Given the backend answers in 750ms the first time, and immediately the second time
            wiremock.stubFor(
                get("/endpoint")
                    .inScenario("Slow first call scenario")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(ok(RESPONSE_FROM_BACKEND).withFixedDelay(750))
                    .willSetStateTo("First Retry")
            );

            wiremock.stubFor(
                get("/endpoint")
                    .inScenario("Slow first call scenario")
                    .whenScenarioStateIs("First Retry")
                    .willReturn(ok(RESPONSE_FROM_BACKEND))
            );

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response body should be right
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND);
                    return true;
                });
            // Then the backend should have been called 2 times (one initial request + 1 attempt)
            wiremock.verify(2, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    class MultipleEndpointsInGroup extends AbstractGatewayTest {

        @Test
        @DeployApi("/apis/http/failover/api-three-endpoints.json")
        void should_not_retry_on_fast_call(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with 3 endpoints
            // And Given the backend answers immediately
            wiremock.stubFor(get("/endpoint-1").willReturn(ok(RESPONSE_FROM_BACKEND + " - 1")));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the response body should be right
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND + " - 1");
                    return true;
                });
            // Then the backend should have been called 1 time
            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint-1")));
        }

        @Test
        @DeployApi("/apis/http/failover/api-three-endpoints.json")
        void should_retry_and_fail_on_slow_call(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with 3 endpoints (round-robin load balancing)
            // And Given backend answers in 750ms whatever the endpoint
            wiremock.stubFor(get("/endpoint-1").willReturn(ok(RESPONSE_FROM_BACKEND).withFixedDelay(750)));
            wiremock.stubFor(get("/endpoint-2").willReturn(ok(RESPONSE_FROM_BACKEND).withFixedDelay(750)));
            wiremock.stubFor(get("/endpoint-3").willReturn(ok(RESPONSE_FROM_BACKEND).withFixedDelay(750)));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response should be 502
                    assertThat(response.statusCode()).isEqualTo(502);
                    return true;
                });
            // Then the backend should have been called 1 time per endpoint (thanks to load balancing)
            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint-1")));
            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint-2")));
            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint-3")));
        }

        @Test
        @DeployApi("/apis/http/failover/api-three-endpoints.json")
        void should_success_on_second_retry(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with 3 endpoints (round-robin load balancing)
            // And Given backend answers in 750ms on all endpoints, expect the third that answers immediately
            wiremock.stubFor(get("/endpoint-1").willReturn(ok(RESPONSE_FROM_BACKEND + " - 1").withFixedDelay(750)));
            wiremock.stubFor(get("/endpoint-2").willReturn(ok(RESPONSE_FROM_BACKEND + " - 2").withFixedDelay(750)));
            wiremock.stubFor(get("/endpoint-3").willReturn(ok(RESPONSE_FROM_BACKEND + " - 3")));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response body should be the one from third endpoint
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND + " - 3");
                    return true;
                });
            // Then the backend should have been called 1 time per endpoint (thanks to load balancing)
            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint-1")));
            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint-2")));
            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint-3")));
        }

        @Test
        @DeployApi("/apis/http/failover/api-three-endpoints.json")
        void should_return_single_content_length_header_from_final_retry_response(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with 3 endpoints (round-robin load balancing)
            // And Given backend answers in 750ms on first two endpoints and immediately on third endpoint
            final String responseFromFirstEndpoint = RESPONSE_FROM_BACKEND + " - first";
            final String responseFromSecondEndpoint = RESPONSE_FROM_BACKEND + " - second-second";
            final String responseFromThirdEndpoint = RESPONSE_FROM_BACKEND + " - third-final";
            final String expectedContentLength = String.valueOf(responseFromThirdEndpoint.getBytes(StandardCharsets.UTF_8).length);

            wiremock.stubFor(get("/endpoint-1").willReturn(ok(responseFromFirstEndpoint).withFixedDelay(750)));
            wiremock.stubFor(get("/endpoint-2").willReturn(ok(responseFromSecondEndpoint).withFixedDelay(750)));
            wiremock.stubFor(get("/endpoint-3").willReturn(ok(responseFromThirdEndpoint)));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200 with only the final response Content-Length value
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.headers().getAll("Content-Length")).containsExactly(expectedContentLength);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response body should be the one from third endpoint
                    assertThat(response).hasToString(responseFromThirdEndpoint);
                    return true;
                });

            // Then the backend should have been called 1 time per endpoint (thanks to load balancing)
            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint-1")));
            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint-2")));
            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint-3")));
        }

        @Test
        @DeployApi("/apis/http/failover/api-three-endpoints-query-params.json")
        void should_success_on_second_retry_with_endpoint_having_query_params(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with 3 endpoints (round-robin load balancing)
            // And Given backend answers in 750ms on all endpoints, expect the third that answers immediately
            wiremock.stubFor(get(urlPathEqualTo("/endpoint-1")).willReturn(ok(RESPONSE_FROM_BACKEND + " - 1").withFixedDelay(750)));
            wiremock.stubFor(get(urlPathEqualTo("/endpoint-2")).willReturn(ok(RESPONSE_FROM_BACKEND + " - 2").withFixedDelay(750)));
            wiremock.stubFor(get(urlPathEqualTo("/endpoint-3")).willReturn(ok(RESPONSE_FROM_BACKEND + " - 3")));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response body should be the one from third endpoint
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND + " - 3");
                    return true;
                });

            // Then the backend should have been called 1 time per endpoint (thanks to load balancing)
            wiremock.verify(getRequestedFor(urlEqualTo("/endpoint-1?e=1")));
            wiremock.verify(getRequestedFor(urlEqualTo("/endpoint-2?e=2")));
            wiremock.verify(getRequestedFor(urlEqualTo("/endpoint-3?e=3")));
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    class DynamicRoutingToEndpoint extends AbstractGatewayTest {

        protected final int wiremockPort = getAvailablePort();

        @Override
        protected void configureWireMock(WireMockConfiguration configuration) {
            configuration.port(wiremockPort);
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.put(
                "dynamic-routing",
                PolicyBuilder.build("dynamic-routing", DynamicRoutingPolicy.class, DynamicRoutingPolicyConfiguration.class)
            );
        }

        @Override
        public void configureApi(Api api) {
            api
                .getProxy()
                .getGroups()
                .stream()
                .filter(group -> group.getName().equals("second-group"))
                .flatMap(group -> group.getEndpoints().stream())
                .forEach(endpoint -> endpoint.setTarget(exchangePort(endpoint.getTarget(), wiremockPort)));
        }

        @Test
        @DeployApi("/apis/http/failover/api-second-group-routing-to-one-endpoint.json")
        void should_not_retry_on_fast_call(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and two endpoints groups
            // and a dynamic routing policy routing to a particular endpoint of second group
            // And Given the backend answers immediately for a endpoint of second group
            wiremock.stubFor(get("/second-group-endpoint-1/dynamic-param").willReturn(ok(RESPONSE_FROM_BACKEND)));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test/dynamic-route/dynamic-param")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the response body should be right
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND);
                    return true;
                });
            // Then the endpoint of second group should have been called 1 time
            wiremock.verify(getRequestedFor(urlPathEqualTo("/second-group-endpoint-1/dynamic-param")));
        }

        @Test
        @DeployApi("/apis/http/failover/api-second-group-routing-to-one-endpoint.json")
        void should_retry_and_fail_on_slow_call(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and two endpoints groups
            // and a dynamic routing policy routing to a particular endpoint of second group
            // And Given the backend answers in 750ms for a endpoint of second group
            wiremock.stubFor(get("/second-group-endpoint-1/dynamic-param").willReturn(ok(RESPONSE_FROM_BACKEND).withFixedDelay(750)));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test/dynamic-route/dynamic-param")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response should be 502
                    assertThat(response.statusCode()).isEqualTo(502);
                    return true;
                });
            // Then the endpoint of second group should have been called 3 times (retrying each time the same endpoint)
            wiremock.verify(3, getRequestedFor(urlPathEqualTo("/second-group-endpoint-1/dynamic-param")));
        }

        @Test
        @DeployApi("/apis/http/failover/api-second-group-routing-to-one-endpoint.json")
        void should_success_on_first_retry(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and two endpoints groups
            // and a dynamic routing policy routing to a particular endpoint of second group
            // And Given the backend answers in 750ms first time and immediately the second time
            wiremock.stubFor(
                get("/second-group-endpoint-1/dynamic-param")
                    .inScenario("Slow first call scenario")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(ok(RESPONSE_FROM_BACKEND).withFixedDelay(750))
                    .willSetStateTo("First Retry")
            );

            wiremock.stubFor(
                get("/second-group-endpoint-1/dynamic-param")
                    .inScenario("Slow first call scenario")
                    .whenScenarioStateIs("First Retry")
                    .willReturn(ok(RESPONSE_FROM_BACKEND))
            );

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test/dynamic-route/dynamic-param")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response body should be right
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND);
                    return true;
                });
            // Then the endpoint of second group should have been called 2 times
            wiremock.verify(2, getRequestedFor(urlPathEqualTo("/second-group-endpoint-1/dynamic-param")));
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    class DynamicRoutingToGroup extends AbstractGatewayTest {

        protected final int wiremockPort = getAvailablePort();

        @Override
        protected void configureWireMock(WireMockConfiguration configuration) {
            configuration.port(wiremockPort);
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.put(
                "dynamic-routing",
                PolicyBuilder.build("dynamic-routing", DynamicRoutingPolicy.class, DynamicRoutingPolicyConfiguration.class)
            );
        }

        @Override
        public void configureApi(Api api) {
            api
                .getProxy()
                .getGroups()
                .stream()
                .filter(group -> group.getName().equals("second-group"))
                .flatMap(group -> group.getEndpoints().stream())
                .forEach(endpoint -> endpoint.setTarget(exchangePort(endpoint.getTarget(), wiremockPort)));
        }

        @Test
        @DeployApi("/apis/http/failover/api-second-group-routing-to-a-endpoints-group.json")
        void should_not_retry_on_fast_call(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and two endpoints groups
            // and a dynamic routing policy routing to the second group
            // And Given the backend answers immediately
            wiremock.stubFor(get("/second-group-endpoint-1/dynamic-param").willReturn(ok(RESPONSE_FROM_BACKEND + " - 1")));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test/dynamic-route/dynamic-param")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response body should be right
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND + " - 1");
                    return true;
                });
            // Then the endpoint of second group should have been called 1 time
            wiremock.verify(getRequestedFor(urlPathEqualTo("/second-group-endpoint-1/dynamic-param")));
        }

        @Test
        @DeployApi("/apis/http/failover/api-second-group-routing-to-a-endpoints-group.json")
        void should_retry_and_fail_on_slow_call(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and two endpoints groups
            // and a dynamic routing policy routing to the second group
            // And Given the backends answer in 750ms
            wiremock.stubFor(
                get("/second-group-endpoint-1/dynamic-param").willReturn(ok(RESPONSE_FROM_BACKEND + " - 1").withFixedDelay(750))
            );
            wiremock.stubFor(
                get("/second-group-endpoint-2/dynamic-param").willReturn(ok(RESPONSE_FROM_BACKEND + " - 2").withFixedDelay(750))
            );
            wiremock.stubFor(
                get("/second-group-endpoint-3/dynamic-param").willReturn(ok(RESPONSE_FROM_BACKEND + " - 3").withFixedDelay(750))
            );

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test/dynamic-route/dynamic-param")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(502);
                    return true;
                });
            // Then the endpoints of second group should have been called 1 time each
            wiremock.verify(getRequestedFor(urlPathEqualTo("/second-group-endpoint-1/dynamic-param")));
            wiremock.verify(getRequestedFor(urlPathEqualTo("/second-group-endpoint-2/dynamic-param")));
            wiremock.verify(getRequestedFor(urlPathEqualTo("/second-group-endpoint-3/dynamic-param")));
        }

        @Test
        @DeployApi("/apis/http/failover/api-second-group-routing-to-a-endpoints-group.json")
        void should_success_on_second_retry(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and two endpoints groups
            // and a dynamic routing policy routing to the second group
            // And Given the backends answer in 750ms except the third that answers immediately
            wiremock.stubFor(
                get("/second-group-endpoint-1/dynamic-param").willReturn(ok(RESPONSE_FROM_BACKEND + " - 1").withFixedDelay(750))
            );
            wiremock.stubFor(
                get("/second-group-endpoint-2/dynamic-param").willReturn(ok(RESPONSE_FROM_BACKEND + " - 2").withFixedDelay(750))
            );
            wiremock.stubFor(get("/second-group-endpoint-3/dynamic-param").willReturn(ok(RESPONSE_FROM_BACKEND + " - 3")));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test/dynamic-route/dynamic-param")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response bodu should be right
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND + " - 3");
                    return true;
                });
            // Then the endpoints of second group should have been called 1 time each
            wiremock.verify(getRequestedFor(urlPathEqualTo("/second-group-endpoint-1/dynamic-param")));
            wiremock.verify(getRequestedFor(urlPathEqualTo("/second-group-endpoint-2/dynamic-param")));
            wiremock.verify(getRequestedFor(urlPathEqualTo("/second-group-endpoint-3/dynamic-param")));
        }
    }
}
