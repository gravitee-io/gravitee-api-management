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

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_APIKEY_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.createSubscription;
import static io.gravitee.gateway.reactive.api.policy.SecurityToken.TokenType.API_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.ConnectionErrorHttpProxyEndpointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.ConnectionErrorMockEndpointConnector;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.ConnectionErrorMockEndpointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.ConnectionLatencyMockEndpointConnector;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.ConnectionLatencyMockEndpointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.apikey.ApiKeyPolicy;
import io.gravitee.policy.apikey.ApiKeyPolicyInitializer;
import io.gravitee.policy.apikey.configuration.ApiKeyPolicyConfiguration;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import io.vertx.rxjava3.core.http.HttpServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.stubbing.OngoingStubbing;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class FailoverV4IntegrationTest extends FailoverV4EmulationIntegrationTest {

    @Nested
    @GatewayTest
    class ProxyApiErrorOnConnection extends AbstractGatewayTest {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent(
                "http-proxy",
                EndpointBuilder.build("http-proxy", ConnectionErrorHttpProxyEndpointConnectorFactory.class)
            );
        }

        @Test
        @DeployApi("/apis/v4/http/failover/connectionfailure/api-proxy-only-one-endpoint-no-fail-connect.json")
        void should_not_retry_on_fast_call(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with one endpoint
            // And Given the backend answers immediately with no connection error
            wiremock.stubFor(get("/endpoint").willReturn(ok(RESPONSE_FROM_BACKEND)));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    // Then the connection has been attempted once
                    assertThat(response.getHeader(ConnectionLatencyMockEndpointConnector.MOCK_CONNECTION_ATTEMPTS)).isEqualTo("1");
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
        @DeployApi("/apis/v4/http/failover/connectionfailure/api-proxy-only-one-endpoint-fail-each-connect.json")
        void should_retry_and_fail_on_error_connection(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with one endpoint
            // And Given the endpoint systematically fail to connect to the backend
            wiremock.stubFor(get("/endpoint").willReturn(ok(RESPONSE_FROM_BACKEND)));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(502);
                    // Then the connection should have been attempted three times
                    assertThat(response.getHeader(ConnectionLatencyMockEndpointConnector.MOCK_CONNECTION_ATTEMPTS)).isEqualTo("3");
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete();
            // Then the backend should have not been called
            wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
        }

        @Test
        @DeployApi("/apis/v4/http/failover/connectionfailure/api-proxy-only-one-endpoint-fail-first-connect.json")
        void should_success_on_first_retry(HttpClient client) {
            // Given an API with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 5 before opening the circuit breaker
            // and only one group with one endpoint
            // And Given the endpoint systematically fail to connect to the backend
            wiremock.stubFor(get("/endpoint").willReturn(ok(RESPONSE_FROM_BACKEND)));

            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    // Then the connection should have been attempted twice
                    assertThat(response.getHeader(ConnectionLatencyMockEndpointConnector.MOCK_CONNECTION_ATTEMPTS)).isEqualTo("2");
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
            // Then the backend should have been called once
            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    @Nested
    @GatewayTest
    class MessageApiErrorOnConnection extends AbstractGatewayTest {

        @Override
        public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
            reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", ConnectionErrorMockEndpointConnectorFactory.class));
        }

        @Test
        @DeployApi("/apis/v4/http/failover/connectionfailure/api-message-only-one-endpoint-no-fail-connect.json")
        void should_not_retry_on_fast_call(HttpClient client) {
            // Given a Message API, with no latency on endpoint connection
            // and with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 2
            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMapPublisher(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    // Then the connection must have been tried once
                    assertThat(response.getHeader(ConnectionLatencyMockEndpointConnector.MOCK_CONNECTION_ATTEMPTS)).isEqualTo("1");
                    return response.rxBody().flatMapPublisher(FailoverV4IntegrationTest::extractPlainTextMessages);
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValueCount(5);
        }

        @Test
        @DeployApi("/apis/v4/http/failover/connectionfailure/api-message-only-one-endpoint-fail-each-connect.json")
        void should_retry_and_fail_on_error_connection(HttpClient client) {
            // Given a Message API, with latency of 750ms on each endpoint connection
            // and with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 2
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
                    // Then the connection must have been tried 3 times
                    assertThat(response.getHeader(ConnectionErrorMockEndpointConnector.MOCK_CONNECTION_ATTEMPTS)).isEqualTo("3");
                    return true;
                });
        }

        @Test
        @DeployApi("/apis/v4/http/failover/connectionfailure/api-message-only-one-endpoint-fail-first-connect.json")
        void should_success_on_first_retry(HttpClient client) {
            // Given a Message API, with latency of 750ms on first endpoint connection
            // and with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 2
            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMapPublisher(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    // Then the connection must have been tried twice
                    assertThat(response.getHeader(ConnectionErrorMockEndpointConnector.MOCK_CONNECTION_ATTEMPTS)).isEqualTo("2");
                    return response.rxBody().flatMapPublisher(FailoverV4IntegrationTest::extractPlainTextMessages);
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValueCount(5);
        }
    }

    @Nested
    @GatewayTest
    class MessageApiLatencyOnConnection extends AbstractGatewayTest {

        @Override
        public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
            reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", ConnectionLatencyMockEndpointConnectorFactory.class));
        }

        @Test
        @DeployApi("/apis/v4/http/failover/connectionfailure/api-message-only-one-endpoint-no-latency.json")
        void should_not_retry_on_fast_call(HttpClient client) {
            // Given a Message API, with no latency on endpoint connection
            // and with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 2
            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMapPublisher(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    // Then the connection must have been tried once
                    assertThat(response.getHeader(ConnectionLatencyMockEndpointConnector.MOCK_CONNECTION_ATTEMPTS)).isEqualTo("1");
                    return response.rxBody().flatMapPublisher(FailoverV4IntegrationTest::extractPlainTextMessages);
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValueCount(5);
        }

        @Test
        @DeployApi("/apis/v4/http/failover/connectionfailure/api-message-only-one-endpoint-latency-each-connect.json")
        void should_retry_and_fail_on_slow_call(HttpClient client) {
            // Given a Message API, with latency of 750ms on each endpoint connection
            // and with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 2
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
                    // Then the connection must have been tried 3 times
                    assertThat(response.getHeader(ConnectionLatencyMockEndpointConnector.MOCK_CONNECTION_ATTEMPTS)).isEqualTo("3");
                    return true;
                });
        }

        @Test
        @DeployApi("/apis/v4/http/failover/connectionfailure/api-message-only-one-endpoint-latency-first-connect.json")
        void should_success_on_first_retry(HttpClient client) {
            // Given a Message API, with latency of 750ms on first endpoint connection
            // and with failover configured with 2 maxRetries, a slowCallDuration of 500ms and a maxFailures of 2
            // When requesting the API
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMapPublisher(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    // Then the connection must have been tried twice
                    assertThat(response.getHeader(ConnectionLatencyMockEndpointConnector.MOCK_CONNECTION_ATTEMPTS)).isEqualTo("2");
                    return response.rxBody().flatMapPublisher(FailoverV4IntegrationTest::extractPlainTextMessages);
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValueCount(5);
        }
    }

    @Nested
    @GatewayTest
    class PerSubscription extends AbstractGatewayTest {

        final Function<HttpClient, Single<HttpClientResponse>> requestSupplier = client ->
            client.rxRequest(HttpMethod.GET, "/test").flatMap(HttpClientRequest::rxSend);

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        @Override
        public void configurePolicies(final Map<String, PolicyPlugin> policies) {
            policies.put(
                "api-key",
                PolicyBuilder.build("api-key", ApiKeyPolicy.class, ApiKeyPolicyConfiguration.class, ApiKeyPolicyInitializer.class)
            );
        }

        @Test
        @DeployApi("/apis/v4/http/failover/api-one-circuit-breaker-per-subscription.json")
        void should_fail_and_open_circuit_after_five_failures_and_remain_closed_for_other_subscriber(HttpClient client) {
            // Given an API with failover configured with one maxRetries, a slowCallDuration of 500ms and a maxFailures of 2 before opening the circuit breaker
            // And Given the backend answers in 750ms
            wiremock.stubFor(
                get("/endpoint")
                    .inScenario("Keyless calls")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(ok(RESPONSE_FROM_BACKEND).withFixedDelay(750))
            );

            // When requesting two times the API
            final List<Single<HttpClientResponse>> listOfParallelCalls = List.of(
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
            // Then the backend should have been called 4 times
            wiremock.verify(4, getRequestedFor(urlPathEqualTo("/endpoint")));

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

            // Given the backend now answer normally
            wiremock.stubFor(
                get("/endpoint").inScenario("Api-Key calls").whenScenarioStateIs("API-KEY").willReturn(ok(RESPONSE_FROM_BACKEND))
            );
            wiremock.resetRequests();
            wiremock.setScenarioState("Api-Key calls", "API-KEY");

            // When requesting the backend as a different subscriber (with a subscription to a plan)
            final ApiKey apiKey = createApiKey("api-one-circuit-breaker-per-subscription");
            when(getBean(ApiKeyService.class).getByApiAndKey(any(), any())).thenReturn(Optional.of(apiKey));
            whenSearchingSubscription(apiKey).thenReturn(Optional.of(createSubscription(apiKey.getApi(), PLAN_APIKEY_ID, false)));

            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(request -> request.putHeader("X-Gravitee-Api-Key", apiKey.getKey()).rxSend())
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // Then the API response should be 200
                    assertThat(response.statusCode()).isEqualTo(200);
                    return true;
                });

            // Then the circuit breaker should be CLOSE and the call should be done as expected
            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
        }

        private ApiKey createApiKey(final String apiId) {
            final ApiKey apiKey = new ApiKey();
            apiKey.setApi(apiId);
            apiKey.setApplication("application-id");
            apiKey.setSubscription("subscription-id");
            apiKey.setPlan("plan-apikey-id");
            apiKey.setKey("apiKeyValue");
            return apiKey;
        }

        private OngoingStubbing<Optional<Subscription>> whenSearchingSubscription(ApiKey apiKey) {
            return when(
                getBean(SubscriptionService.class).getByApiAndSecurityToken(
                    eq(apiKey.getApi()),
                    argThat(
                        securityToken ->
                            securityToken.getTokenType().equals(API_KEY.name()) && securityToken.getTokenValue().equals(apiKey.getKey())
                    ),
                    eq(apiKey.getPlan())
                )
            );
        }
    }

    @Nested
    @GatewayTest
    class CircuitBreakingCases extends FailoverV4EmulationIntegrationTest.CircuitBreakingCases {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-only-one-endpoint.json")
        @Test
        void should_fail_and_open_circuit_after_five_failures(HttpClient client) {
            super.should_fail_and_open_circuit_after_five_failures(client);
        }
    }

    @Nested
    //Need to add test order since last test (should_retry_and_fail_on_connection_exception) is stopping wiremock which is messing with the other tests
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @GatewayTest
    class OnlyOneEndpointInGroup extends FailoverV4EmulationIntegrationTest.OnlyOneEndpointInGroup {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-only-one-endpoint.json")
        @Order(1)
        @Test
        void should_retry_and_fail_on_slow_call_posting_payload(HttpClient client) {
            super.should_retry_and_fail_on_slow_call_posting_payload(client);
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-only-one-endpoint.json")
        @Order(2)
        @Test
        void should_not_retry_on_fast_call(HttpClient client) {
            super.should_not_retry_on_fast_call(client);
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-only-one-endpoint.json")
        @Order(3)
        @Test
        void should_retry_and_fail_on_slow_call(HttpClient client) {
            super.should_retry_and_fail_on_slow_call(client);
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-only-one-endpoint.json")
        @Order(5)
        @Test
        void should_retry_and_fail_on_connection_exception(HttpClient client, Vertx vertx) {
            super.should_retry_and_fail_on_connection_exception(client, vertx);
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-only-one-endpoint.json")
        @Order(4)
        @Test
        void should_success_on_first_retry(HttpClient client) {
            super.should_success_on_first_retry(client);
        }
    }

    @Nested
    @GatewayTest
    class MultipleEndpointsInGroup extends FailoverV4EmulationIntegrationTest.MultipleEndpointsInGroup {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-three-endpoints.json")
        @Test
        void should_retry_and_fail_on_slow_call(HttpClient client) {
            super.should_retry_and_fail_on_slow_call(client);
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-three-endpoints.json")
        @Test
        void should_not_retry_on_fast_call(HttpClient client) {
            super.should_not_retry_on_fast_call(client);
        }

        @Override
        @Test
        @DeployApi("/apis/v4/http/failover/api-three-endpoints.json")
        void should_success_on_second_retry(HttpClient client) {
            super.should_success_on_second_retry(client);
        }

        @Override
        @Test
        @DeployApi("/apis/v4/http/failover/api-three-endpoints.json")
        void should_return_single_content_length_header_from_final_retry_response(HttpClient client) {
            super.should_return_single_content_length_header_from_final_retry_response(client);
        }

        @Override
        @Test
        @DeployApi("/apis/v4/http/failover/api-three-endpoints-query-params.json")
        void should_success_on_second_retry_with_endpoint_having_query_params(HttpClient client) {
            super.should_success_on_second_retry_with_endpoint_having_query_params(client);
        }
    }

    @Nested
    @GatewayTest
    class DynamicRoutingToEndpoint extends FailoverV4EmulationIntegrationTest.DynamicRoutingToEndpoint {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (isLegacyApi(definitionClass)) {
                throw new IllegalStateException("should be testing a v4 API");
            }
            final Api definition = (Api) api.getDefinition();
            definition
                .getEndpointGroups()
                .stream()
                .filter(group -> group.getName().equals("second-group"))
                .flatMap(group -> group.getEndpoints().stream())
                .forEach(endpoint ->
                    endpoint.setConfiguration(endpoint.getConfiguration().replace("8080", Integer.toString(wiremockPort)))
                );
            // Redeploy api with updated endpoint config
            var manager = applicationContext.getBean(ApiManager.class);
            manager.unregister(api.getId());
            manager.register(api);
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-second-group-routing-to-one-endpoint.json")
        @Test
        void should_not_retry_on_fast_call(HttpClient client) {
            super.should_not_retry_on_fast_call(client);
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-second-group-routing-to-one-endpoint.json")
        @Test
        void should_retry_and_fail_on_slow_call(HttpClient client) {
            super.should_retry_and_fail_on_slow_call(client);
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-second-group-routing-to-one-endpoint.json")
        @Test
        void should_success_on_first_retry(HttpClient client) {
            super.should_success_on_first_retry(client);
        }
    }

    @Nested
    @GatewayTest
    class DynamicRoutingToGroup extends FailoverV4EmulationIntegrationTest.DynamicRoutingToGroup {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            final int availablePort = getAvailablePort();
            final HttpServer httpServer = Vertx.vertx().createHttpServer(new HttpServerOptions().setPort(availablePort));
            httpServer.connectionHandler(connection -> {
                System.out.println("🤞connection");
            });
            httpServer.requestHandler(request -> {
                System.out.println(" request: " + request.absoluteURI());
                if (request.absoluteURI().contains("dynamic-param")) {
                    //                    request.response().setStatusCode(200).end("ok from backend - 1");
                }
            });
            httpServer.listen().subscribe();

            if (isLegacyApi(definitionClass)) {
                throw new IllegalStateException("should be testing a v4 API");
            }
            final Api definition = (Api) api.getDefinition();
            definition
                .getEndpointGroups()
                .stream()
                .filter(group -> group.getName().equals("second-group"))
                .flatMap(group -> group.getEndpoints().stream())
                .forEach(endpoint ->
                    endpoint.setConfiguration(endpoint.getConfiguration().replace("8080", Integer.toString(wiremockPort)))
                );
            // Redeploy api with updated endpoint config
            var manager = applicationContext.getBean(ApiManager.class);
            manager.unregister(api.getId());
            manager.register(api);
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-second-group-routing-to-a-endpoints-group.json")
        @Test
        void should_retry_and_fail_on_slow_call(HttpClient client) {
            super.should_retry_and_fail_on_slow_call(client);
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-second-group-routing-to-a-endpoints-group.json")
        @Test
        void should_not_retry_on_fast_call(HttpClient client) {
            super.should_not_retry_on_fast_call(client);
        }

        @Override
        @DeployApi("/apis/v4/http/failover/api-second-group-routing-to-a-endpoints-group.json")
        @Test
        void should_success_on_second_retry(HttpClient client) {
            super.should_success_on_second_retry(client);
        }
    }

    @NonNull
    private static Flowable<JsonObject> extractPlainTextMessages(Buffer body) {
        final List<JsonObject> messages = new ArrayList<>();

        final String[] lines = body.toString().split("\n");

        JsonObject jsonObject = new JsonObject();

        for (String line : lines) {
            if (line.equals("item")) {
                jsonObject = new JsonObject();
                messages.add(jsonObject);
            } else if (line.startsWith("id:")) {
                jsonObject.put("id", Integer.parseInt(line.replace("id: ", "")));
            } else if (line.startsWith("content:")) {
                jsonObject.put("content", line.replace("content: ", ""));
            }
        }

        return Flowable.fromIterable(messages);
    }
}
