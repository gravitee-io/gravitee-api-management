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
package io.gravitee.apim.integration.tests.http.pathtraversal;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.gateway.tests.fakes.policies.PathToHeaderPolicy.X_PATH;
import static io.gravitee.gateway.tests.fakes.policies.PathToHeaderPolicy.X_PATH_INFO;
import static io.gravitee.gateway.tests.fakes.policies.PathToHeaderPolicy.X_URI;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.tests.fakes.policies.PathToHeaderPolicy;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * What a policy sees of the path, which is where the whole decision lands.
 *
 * <p>A policy attached to a path-scoped flow is only as sound as the path it reasons on. If it read
 * the value as received while the gateway routed on the resolved one, a rule guarding
 * {@code /admin} would be evaded by asking for {@code /public/../admin} — the reported traversal in
 * miniature, inside a single API.
 *
 * <p>Each engine is exercised in both {@code RAW} and {@code NORMALIZE}. The {@code RAW} classes are
 * the control: without them, these tests would show that a policy sees a resolved path without
 * showing that the setting is what makes it so.
 *
 * <p>{@code uri()} is asserted alongside, because it must keep reporting the bytes as received
 * whatever the mode — that is what a signature or a raw-path integration relies on.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PathSeenByPolicyIntegrationTest {

    private static final String TRAVERSAL = "/alpha/api/../../alpha/api/echo";
    private static final String RESOLVED_PATH = "/alpha/api/echo";
    private static final String RESOLVED_PATH_INFO = "/echo";
    private static final String RAW_PATH_INFO = "/../../alpha/api/echo";

    abstract static class PathSeenByPolicyTest extends AbstractGatewayTest {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.put("path-to-header", PolicyBuilder.build("path-to-header", PathToHeaderPolicy.class));
        }

        protected HttpClientResponse call(final HttpClient httpClient, final String rawPath) throws InterruptedException {
            return httpClient
                .rxRequest(HttpMethod.GET, rawPath)
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .await()
                .assertComplete()
                .assertNoErrors()
                .values()
                .get(0);
        }
    }

    abstract static class OnAV4Definition extends PathSeenByPolicyTest {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/pathtraversal/api-alpha-path-policy.json")
    class On_a_v4_definition_in_normalize extends OnAV4Definition {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "NORMALIZE");
        }

        @Test
        void should_expose_the_resolved_path_to_the_policy(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            final HttpClientResponse response = call(httpClient, TRAVERSAL);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.getHeader(X_PATH)).isEqualTo(RESOLVED_PATH);
            assertThat(response.getHeader(X_PATH_INFO)).isEqualTo(RESOLVED_PATH_INFO);
        }

        @Test
        void should_still_expose_the_uri_as_received(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertThat(call(httpClient, TRAVERSAL).getHeader(X_URI)).isEqualTo(TRAVERSAL);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/pathtraversal/api-alpha-path-policy.json")
    class On_a_v4_definition_in_raw extends OnAV4Definition {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "RAW");
        }

        @Test
        void should_expose_the_path_as_received_to_the_policy(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            final HttpClientResponse response = call(httpClient, TRAVERSAL);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.getHeader(X_PATH)).isEqualTo(TRAVERSAL);
            assertThat(response.getHeader(X_PATH_INFO)).isEqualTo(RAW_PATH_INFO);
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi("/apis/http/pathtraversal/api-v2-alpha-path-policy.json")
    class On_the_legacy_engine_in_normalize extends PathSeenByPolicyTest {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "NORMALIZE");
        }

        @Test
        void should_expose_the_resolved_path_to_the_policy(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            final HttpClientResponse response = call(httpClient, TRAVERSAL);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.getHeader(X_PATH)).isEqualTo(RESOLVED_PATH);
            assertThat(response.getHeader(X_PATH_INFO)).isEqualTo(RESOLVED_PATH_INFO);
        }

        @Test
        void should_still_expose_the_uri_as_received(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertThat(call(httpClient, TRAVERSAL).getHeader(X_URI)).isEqualTo(TRAVERSAL);
        }
    }

    @Nested
    @GatewayTest(v2ExecutionMode = ExecutionMode.V3)
    @DeployApi("/apis/http/pathtraversal/api-v2-alpha-path-policy.json")
    class On_the_legacy_engine_in_raw extends PathSeenByPolicyTest {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "RAW");
        }

        @Test
        void should_expose_the_path_as_received_to_the_policy(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            final HttpClientResponse response = call(httpClient, TRAVERSAL);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.getHeader(X_PATH)).isEqualTo(TRAVERSAL);
            assertThat(response.getHeader(X_PATH_INFO)).isEqualTo(RAW_PATH_INFO);
        }
    }
}
