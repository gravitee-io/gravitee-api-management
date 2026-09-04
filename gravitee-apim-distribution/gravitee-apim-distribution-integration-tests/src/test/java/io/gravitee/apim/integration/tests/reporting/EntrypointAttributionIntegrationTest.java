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
package io.gravitee.apim.integration.tests.reporting;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_APIKEY_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.createSubscription;
import static io.gravitee.gateway.reactive.api.policy.SecurityToken.TokenType.API_KEY;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.graviteesource.entrypoint.mcp.MCPEntrypointConnectorFactory;
import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import com.graviteesource.entrypoint.websocket.WebSocketEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.apikey.ApiKeyPolicy;
import io.gravitee.policy.apikey.ApiKeyPolicyInitializer;
import io.gravitee.policy.apikey.configuration.ApiKeyPolicyConfiguration;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * A request the gateway refuses before selecting an entrypoint connector (plan 401, CORS preflight) is reported
 * with the {@code entrypoint-id} of the entrypoint it was addressed to, resolved with the same matching rules as
 * accepted traffic, on APIs exposing several entrypoints. Status codes are asserted alongside to show the request
 * chain itself is unchanged: security still runs before entrypoint selection.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EntrypointAttributionIntegrationTest {

    private static final String API_KEY_HEADER = "X-Gravitee-Api-Key";
    private static final String MCP_ACCEPT = "application/json, text/event-stream";
    private static final String MCP_INITIALIZE =
        "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"clientInfo\":{\"name\":\"attribution-test\",\"version\":\"1.0\"}}}";

    abstract static class AttributionTest extends AbstractGatewayTest {

        ReplaySubject<Metrics> reported;

        @BeforeEach
        void captureReportedMetrics() {
            reported = ReplaySubject.create();
            getBean(FakeReporter.class).setReportableHandler(reportable -> {
                if (reportable instanceof Metrics metrics) {
                    reported.onNext(metrics.toBuilder().build());
                }
            });
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (isV4Api(definitionClass)) {
                Api definition = (Api) api.getDefinition();
                configurePlans(definition, Set.of("api-key"));
                Analytics analytics = new Analytics();
                analytics.setEnabled(true);
                definition.setAnalytics(analytics);
            }
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.put(
                "api-key",
                PolicyBuilder.build("api-key", ApiKeyPolicy.class, ApiKeyPolicyConfiguration.class, ApiKeyPolicyInitializer.class)
            );
        }

        /** The v4 metrics document reported for the request, identified by its URI and status. */
        Metrics reportedFor(String uri, int status) {
            return reported
                .filter(metrics -> uri.equals(metrics.getUri()) && metrics.getStatus() == status)
                .firstOrError()
                .timeout(10, SECONDS)
                .blockingGet();
        }

        ApiKey givenAValidApiKey(String apiId) {
            ApiKey apiKey = new ApiKey();
            apiKey.setApi(apiId);
            apiKey.setApplication("application-id");
            apiKey.setSubscription("subscription-id");
            apiKey.setPlan(PLAN_APIKEY_ID);
            apiKey.setKey("attribution-api-key");
            when(getBean(ApiKeyService.class).getByApiAndKey(any(), any())).thenReturn(Optional.of(apiKey));
            when(
                getBean(SubscriptionService.class).getByApiAndSecurityToken(
                    eq(apiId),
                    argThat(token -> token.getTokenType().equals(API_KEY.name()) && token.getTokenValue().equals(apiKey.getKey())),
                    eq(PLAN_APIKEY_ID)
                )
            ).thenReturn(Optional.of(createSubscription(apiId, PLAN_APIKEY_ID, false)));
            return apiKey;
        }

        static int send(HttpClient client, HttpMethod method, String path, Map<String, String> headers, String body) {
            HttpClientResponse response = client
                .rxRequest(method, path)
                .flatMap(request -> {
                    headers.forEach(request::putHeader);
                    return body == null ? request.rxSend() : request.rxSend(body);
                })
                .timeout(10, SECONDS)
                .blockingGet();
            response.rxBody().timeout(10, SECONDS).blockingGet();
            return response.statusCode();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/entrypoint-attribution/proxy-with-mcp.json")
    class ProxyApiExposingHttpProxyAndMcp extends AttributionTest {

        private static final String API_ID = "entrypoint-attribution-proxy";
        private static final String MCP_URI = "/entrypoint-attribution/mcp";
        private static final String PROXY_URI = "/entrypoint-attribution/resource";

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
            entrypoints.putIfAbsent("mcp", EntrypointBuilder.build("mcp", MCPEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        @Test
        void should_attribute_a_refused_mcp_call_to_the_mcp_entrypoint(HttpClient client) {
            int status = send(client, HttpMethod.POST, MCP_URI, Map.of("Accept", MCP_ACCEPT), MCP_INITIALIZE);

            assertThat(status).isEqualTo(401);
            assertThat(reportedFor(MCP_URI, 401).getEntrypointId()).isEqualTo("mcp");
        }

        @Test
        void should_attribute_a_refused_proxy_call_to_the_http_proxy_entrypoint(HttpClient client) {
            int status = send(client, HttpMethod.GET, PROXY_URI, Map.of(), null);

            assertThat(status).isEqualTo(401);
            assertThat(reportedFor(PROXY_URI, 401).getEntrypointId()).isEqualTo("http-proxy");
        }

        @Test
        void should_attribute_a_cors_preflight_to_the_entrypoint_that_would_serve_the_request(HttpClient client) {
            int status = send(
                client,
                HttpMethod.OPTIONS,
                PROXY_URI,
                Map.of("Origin", "https://mydomain.com", "Access-Control-Request-Method", "GET"),
                null
            );

            assertThat(status).isEqualTo(200);
            assertThat(reportedFor(PROXY_URI, 200).getEntrypointId()).isEqualTo("http-proxy");
        }

        @Test
        void should_keep_attributing_an_accepted_proxy_call_to_the_connector_that_handled_it(HttpClient client) {
            wiremock.stubFor(get("/endpoint/resource").willReturn(ok("endpoint response")));
            ApiKey apiKey = givenAValidApiKey(API_ID);

            int status = send(client, HttpMethod.GET, PROXY_URI, Map.of(API_KEY_HEADER, apiKey.getKey()), null);

            assertThat(status).isEqualTo(200);
            assertThat(reportedFor(PROXY_URI, 200).getEntrypointId()).isEqualTo("http-proxy");
        }

        @Test
        void should_keep_attributing_an_accepted_mcp_call_to_the_mcp_entrypoint(HttpClient client) {
            ApiKey apiKey = givenAValidApiKey(API_ID);

            int status = send(
                client,
                HttpMethod.POST,
                MCP_URI,
                Map.of("Accept", MCP_ACCEPT, API_KEY_HEADER, apiKey.getKey()),
                MCP_INITIALIZE
            );

            assertThat(status).isNotIn(401, 404);
            assertThat(reportedFor(MCP_URI, status).getEntrypointId()).isEqualTo("mcp");
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/messages/entrypoint-attribution/message-with-several-entrypoints.json")
    class MessageApiExposingSeveralEntrypoints extends AttributionTest {

        private static final String URI = "/entrypoint-attribution-message";

        @Override
        public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
            reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-post", EntrypointBuilder.build("http-post", HttpPostEntrypointConnectorFactory.class));
            entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
            entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
            entrypoints.putIfAbsent("websocket", EntrypointBuilder.build("websocket", WebSocketEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", MockEndpointConnectorFactory.class));
        }

        @Test
        void should_attribute_a_refused_event_stream_subscription_to_the_sse_entrypoint(HttpClient client) {
            int status = send(client, HttpMethod.GET, URI, Map.of("Accept", "text/event-stream"), null);

            assertThat(status).isEqualTo(401);
            assertThat(reportedFor(URI, 401).getEntrypointId()).isEqualTo("sse");
        }

        @Test
        void should_attribute_a_refused_publication_to_the_http_post_entrypoint(HttpClient client) {
            int status = send(client, HttpMethod.POST, URI, Map.of(), "{\"message\":\"hello\"}");

            assertThat(status).isEqualTo(401);
            assertThat(reportedFor(URI, 401).getEntrypointId()).isEqualTo("http-post");
        }

        @Test
        void should_attribute_a_refused_poll_to_the_http_get_entrypoint(HttpClient client) {
            int status = send(client, HttpMethod.GET, URI, Map.of(), null);

            assertThat(status).isEqualTo(401);
            assertThat(reportedFor(URI, 401).getEntrypointId()).isEqualTo("http-get");
        }
    }
}
