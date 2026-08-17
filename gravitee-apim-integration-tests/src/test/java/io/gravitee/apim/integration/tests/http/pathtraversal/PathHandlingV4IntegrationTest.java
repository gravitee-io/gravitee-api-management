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
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.apikey.ApiKeyPolicy;
import io.gravitee.policy.apikey.ApiKeyPolicyInitializer;
import io.gravitee.policy.apikey.configuration.ApiKeyPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Covers the three values of {@code http.pathHandling} against the same pair of APIs:
 * {@code alpha} on {@code /alpha/api/} with no plan, {@code beta} on {@code /beta/api/} behind an
 * api-key plan, both pointing at the same upstream host.
 *
 * <p>The request under test, {@code /alpha/api/../../beta/api/echo}, means one thing to the gateway
 * when it reads the path as it arrived, and another to any receiver applying RFC 3986 §5.2.4. Each
 * mode below is one answer to that disagreement.
 *
 * <p>V4 APIs only. The V3 handler keeps its historical behaviour whatever the setting, because its
 * request wrapper exposes the native path and cannot be rewritten.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PathHandlingV4IntegrationTest {

    private static final String TRAVERSAL = "/alpha/api/../../beta/api/echo";
    private static final String ENCODED_TRAVERSAL = "/alpha/api/%2e%2e/%2e%2e/beta/api/echo";
    private static final String REGULAR = "/alpha/api/echo";

    /**
     * Base wiring shared by the three modes. Only {@link #configureGateway} differs between them,
     * which is the whole point of the comparison.
     */
    abstract static class PathHandlingTest extends AbstractGatewayTest {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.put(
                "api-key",
                PolicyBuilder.build("api-key", ApiKeyPolicy.class, ApiKeyPolicyConfiguration.class, ApiKeyPolicyInitializer.class)
            );
        }

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
            if (isV4Api(definitionClass) && "beta".equals(api.getId())) {
                configurePlans((Api) api.getDefinition(), Set.of("api-key"));
            }
        }

        protected void assertStatus(final HttpClient httpClient, final String rawPath, final int expectedStatus)
            throws InterruptedException {
            httpClient
                .rxRequest(HttpMethod.GET, rawPath)
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .await()
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(expectedStatus);
                    return true;
                })
                .assertNoErrors();
        }

        protected String singleUpstreamRequestUrl() {
            final List<LoggedRequest> upstreamRequests = wiremock.findAll(getRequestedFor(anyUrl()));
            assertThat(upstreamRequests).hasSize(1);
            return upstreamRequests.get(0).getUrl();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/pathtraversal/api-alpha.json", "/apis/v4/http/pathtraversal/api-beta.json" })
    class With_raw_handling extends PathHandlingTest {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "RAW");
        }

        @Test
        void should_serve_a_regular_path(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, REGULAR, 200);
        }

        @Test
        void should_keep_the_bypass_because_nothing_is_resolved(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            // The historical behaviour, kept as the default so no deployment changes on upgrade.
            assertStatus(httpClient, TRAVERSAL, 200);

            assertThat(singleUpstreamRequestUrl()).isEqualTo(TRAVERSAL);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/pathtraversal/api-alpha.json", "/apis/v4/http/pathtraversal/api-beta.json" })
    class With_reject_handling extends PathHandlingTest {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "REJECT");
        }

        @Test
        void should_serve_a_regular_path(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, REGULAR, 200);

            assertThat(singleUpstreamRequestUrl()).isEqualTo(REGULAR);
        }

        @Test
        void should_reject_dot_segments_before_any_api_is_selected(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, TRAVERSAL, 400);

            assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
        }

        @Test
        void should_reject_encoded_dot_segments(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, ENCODED_TRAVERSAL, 400);

            assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/pathtraversal/api-alpha.json", "/apis/v4/http/pathtraversal/api-beta.json" })
    class With_normalize_handling extends PathHandlingTest {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "NORMALIZE");
        }

        @Test
        void should_serve_a_regular_path(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, REGULAR, 200);

            assertThat(singleUpstreamRequestUrl()).isEqualTo(REGULAR);
        }

        @Test
        void should_enforce_the_plan_of_the_api_the_resolved_path_designates(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            // Resolved, the request is a call to beta, so beta's api-key plan applies and answers
            // 401 for want of a key. The request is not blocked as malformed, it is simply read
            // for what it means.
            assertStatus(httpClient, TRAVERSAL, 401);

            assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
        }

        @Test
        void should_enforce_that_plan_on_encoded_dot_segments_too(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, ENCODED_TRAVERSAL, 401);

            assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
        }

        @Test
        void should_resolve_a_chain_of_dot_segments(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            // Three hops rather than one: alpha, back out, beta, back out, beta again. Whatever the
            // depth, what counts is where the chain lands, and beta's plan answers for it.
            assertStatus(httpClient, "/alpha/api/../../beta/api/../../beta/api/echo", 401);

            assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
        }

        @Test
        void should_answer_not_found_when_the_resolved_path_matches_no_api(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            // The other half of the defect: escaping the configured endpoint target no longer
            // reaches a neighbour of that target, because the gateway now reads where the request
            // actually points and finds no listener there.
            assertStatus(httpClient, "/alpha/api/../../elsewhere/resource", 404);

            assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
        }
    }
}
