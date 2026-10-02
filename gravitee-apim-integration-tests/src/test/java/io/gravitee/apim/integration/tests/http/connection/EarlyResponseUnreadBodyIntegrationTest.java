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
package io.gravitee.apim.integration.tests.http.connection;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static io.gravitee.apim.integration.tests.http.connection.RawHttp1Connection.sendTwoRequestsOnSameConnection;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.definition.model.ExecutionMode.V3;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.fake.ReadBodyThenInterruptPolicy;
import io.gravitee.apim.integration.tests.http.connection.RawHttp1Connection.Outcome;
import io.gravitee.apim.integration.tests.http.connection.RawHttp1Connection.Request;
import io.gravitee.apim.integration.tests.http.connection.RawHttp1Connection.SecondOutcome;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.apikey.ApiKeyPolicy;
import io.gravitee.policy.apikey.ApiKeyPolicyInitializer;
import io.gravitee.policy.apikey.configuration.ApiKeyPolicyConfiguration;
import io.vertx.rxjava3.core.Vertx;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * APIM-15175: when the gateway answers before having read the request body (404, 401…), the next request sent on the
 * same HTTP/1.1 connection must be answered — or, when the body is too large or of unknown length to be discarded, the
 * connection must be announced and actually closed. It must never be left hanging.
 *
 * @author GraviteeSource Team
 */
class EarlyResponseUnreadBodyIntegrationTest {

    // Large enough to overflow the socket and Netty buffers, so that TCP back-pressure kicks in when the body is not read.
    static final int LARGE_BODY = 400_000;
    static final int SMALL_BODY = 1_000;
    static final int BODY_OVER_DISCARD_LIMIT = 2 * 1024 * 1024;

    static void assertConnectionKeptAlive(Outcome outcome, int expectedStatus) {
        assertThat(outcome.first().status()).isEqualTo(expectedStatus);
        assertThat(outcome.first().header("Connection")).isNotEqualTo("close");
        assertThat(outcome.second()).isEqualTo(SecondOutcome.RESPONDED);
        assertThat(outcome.secondResponse().status()).isEqualTo(expectedStatus);
    }

    static void assertConnectionAnnouncedAndClosed(Outcome outcome, int expectedStatus) {
        assertThat(outcome.first().status()).isEqualTo(expectedStatus);
        assertThat(outcome.first().header("Connection")).isEqualTo("close");
        assertThat(outcome.second()).isEqualTo(SecondOutcome.CLOSED_AFTER_FIRST);
    }

    @Nested
    @GatewayTest
    class NoApiMatches extends AbstractGatewayTest {

        @Test
        void should_keep_connection_alive_when_404_response_leaves_large_body_unread(Vertx vertx, GatewayDynamicConfig.Config config)
            throws Exception {
            var outcome = sendTwoRequestsOnSameConnection(vertx, config.httpPort(), Request.post("/no-api-here", LARGE_BODY));

            assertConnectionKeptAlive(outcome, 404);
        }

        @Test
        void should_keep_connection_alive_when_404_response_leaves_small_body_unread(Vertx vertx, GatewayDynamicConfig.Config config)
            throws Exception {
            var outcome = sendTwoRequestsOnSameConnection(vertx, config.httpPort(), Request.post("/no-api-here", SMALL_BODY));

            assertConnectionKeptAlive(outcome, 404);
        }

        @Test
        void should_keep_connection_alive_when_404_get_has_no_body(Vertx vertx, GatewayDynamicConfig.Config config) throws Exception {
            var outcome = sendTwoRequestsOnSameConnection(vertx, config.httpPort(), Request.get("/no-api-here"));

            assertConnectionKeptAlive(outcome, 404);
        }

        @Test
        void should_close_connection_when_404_response_leaves_body_over_discard_limit_unread(
            Vertx vertx,
            GatewayDynamicConfig.Config config
        ) throws Exception {
            var outcome = sendTwoRequestsOnSameConnection(vertx, config.httpPort(), Request.post("/no-api-here", BODY_OVER_DISCARD_LIMIT));

            assertConnectionAnnouncedAndClosed(outcome, 404);
        }

        @Test
        void should_close_connection_when_404_response_leaves_chunked_body_unread(Vertx vertx, GatewayDynamicConfig.Config config)
            throws Exception {
            var outcome = sendTwoRequestsOnSameConnection(vertx, config.httpPort(), Request.post("/no-api-here", LARGE_BODY).chunked());

            assertConnectionAnnouncedAndClosed(outcome, 404);
        }

        @Test
        void should_close_connection_when_404_response_leaves_body_expecting_100_continue_unread(
            Vertx vertx,
            GatewayDynamicConfig.Config config
        ) throws Exception {
            var outcome = sendTwoRequestsOnSameConnection(
                vertx,
                config.httpPort(),
                Request.post("/no-api-here", LARGE_BODY).withHeader("Expect", "100-continue")
            );

            assertConnectionAnnouncedAndClosed(outcome, 404);
        }

        @Test
        void should_keep_http_1_0_connection_alive_when_404_response_leaves_large_body_unread(
            Vertx vertx,
            GatewayDynamicConfig.Config config
        ) throws Exception {
            var outcome = sendTwoRequestsOnSameConnection(
                vertx,
                config.httpPort(),
                Request.post("/no-api-here", LARGE_BODY).http10KeepAlive()
            );

            assertConnectionKeptAlive(outcome, 404);
        }

        @Test
        void should_close_http_1_0_connection_when_404_response_leaves_body_over_discard_limit_unread(
            Vertx vertx,
            GatewayDynamicConfig.Config config
        ) throws Exception {
            var outcome = sendTwoRequestsOnSameConnection(
                vertx,
                config.httpPort(),
                Request.post("/no-api-here", BODY_OVER_DISCARD_LIMIT).http10KeepAlive()
            );

            // Vert.x answers "Connection: keep-alive" to an HTTP/1.0 keep-alive request whatever the response headers say.
            assertThat(outcome.first().status()).isEqualTo(404);
            assertThat(outcome.second()).isEqualTo(SecondOutcome.CLOSED_AFTER_FIRST);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/plan/v4-proxy-api.json")
    class ApiKeyPlanRejected extends AbstractGatewayTest {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (isV4Api(definitionClass)) {
                configurePlans((io.gravitee.definition.model.v4.Api) api.getDefinition(), Set.of("api-key"));
            }
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            registerApiKeyPolicy(policies);
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            registerHttpProxyEntrypoint(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            registerHttpProxyEndpoint(endpoints);
        }

        @Test
        void should_keep_connection_alive_when_401_response_leaves_large_body_unread(Vertx vertx, GatewayDynamicConfig.Config config)
            throws Exception {
            var outcome = sendTwoRequestsOnSameConnection(vertx, config.httpPort(), Request.post("/v4-proxy-api", LARGE_BODY));

            assertConnectionKeptAlive(outcome, 401);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/plan/v2-api.json")
    class ApiKeyPlanRejectedV4Emulation extends AbstractV2ApiKeyPlanTest {

        @Test
        void should_keep_connection_alive_when_401_response_leaves_large_body_unread(Vertx vertx, GatewayDynamicConfig.Config config)
            throws Exception {
            var outcome = sendTwoRequestsOnSameConnection(vertx, config.httpPort(), Request.post("/v2-api", LARGE_BODY));

            assertConnectionKeptAlive(outcome, 401);
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = V3)
    @DeployApi("/apis/plan/v2-api.json")
    class ApiKeyPlanRejectedV3 extends AbstractV2ApiKeyPlanTest {

        @Test
        void should_keep_connection_alive_when_401_response_leaves_large_body_unread(Vertx vertx, GatewayDynamicConfig.Config config)
            throws Exception {
            var outcome = sendTwoRequestsOnSameConnection(vertx, config.httpPort(), Request.post("/v2-api", LARGE_BODY));

            // The V3 engine always sets "Connection: close" on errors, which Vert.x never applies: only reuse is checked.
            assertThat(outcome.first().status()).isEqualTo(401);
            assertThat(outcome.second()).isEqualTo(SecondOutcome.RESPONDED);
            assertThat(outcome.secondResponse().status()).isEqualTo(401);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/api.json")
    class BodyForwardedToBackend extends AbstractGatewayTest {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            registerHttpProxyEntrypoint(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            registerHttpProxyEndpoint(endpoints);
        }

        @Test
        void should_keep_connection_alive_when_large_body_is_forwarded_to_backend(Vertx vertx, GatewayDynamicConfig.Config config)
            throws Exception {
            wiremock.stubFor(post("/endpoint").willReturn(ok("backend response")));

            var outcome = sendTwoRequestsOnSameConnection(vertx, config.httpPort(), Request.post("/test", LARGE_BODY));

            assertConnectionKeptAlive(outcome, 200);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/connection/api-read-body-then-interrupt.json")
    class BodyReadByPolicyBeforeRejection extends AbstractGatewayTest {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.put("read-body-then-interrupt", PolicyBuilder.build("read-body-then-interrupt", ReadBodyThenInterruptPolicy.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            registerHttpProxyEntrypoint(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            registerHttpProxyEndpoint(endpoints);
        }

        @Test
        void should_keep_connection_alive_when_body_over_discard_limit_was_read_before_rejection(
            Vertx vertx,
            GatewayDynamicConfig.Config config
        ) throws Exception {
            var outcome = sendTwoRequestsOnSameConnection(
                vertx,
                config.httpPort(),
                Request.post("/read-body-then-interrupt", BODY_OVER_DISCARD_LIMIT)
            );

            assertConnectionKeptAlive(outcome, 400);
        }
    }

    abstract static class AbstractV2ApiKeyPlanTest extends AbstractGatewayTest {

        @Override
        public void configureApi(Api api) {
            configurePlans(api, Set.of("API_KEY"));
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            registerApiKeyPolicy(policies);
        }
    }

    static void registerApiKeyPolicy(Map<String, PolicyPlugin> policies) {
        policies.put(
            "api-key",
            PolicyBuilder.build("api-key", ApiKeyPolicy.class, ApiKeyPolicyConfiguration.class, ApiKeyPolicyInitializer.class)
        );
    }

    static void registerHttpProxyEntrypoint(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    static void registerHttpProxyEndpoint(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }
}
