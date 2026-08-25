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
import org.junit.jupiter.api.Test;

/**
 * Pins how the gateway treats dot segments under {@code http.pathHandling: RAW}.
 *
 * <p>Two v4 HTTP proxy APIs share one upstream host, each on its own path prefix: {@code alpha} on
 * {@code /alpha/api/} with no plan, so it is reachable without any credential, and {@code beta} on
 * {@code /beta/api/} behind an api-key plan, so it must never be reached without a key.
 *
 * <p>The gateway resolves the listener context path against the request path exactly as it arrived,
 * enforces the matched API's plan, then appends the remaining path to the endpoint target without
 * resolving it. A conforming upstream applies RFC 3986 §5.2.4 and serves a different resource than
 * the one the gateway authorized.
 *
 * <p><strong>These assertions describe RAW, not the behaviour we want.</strong> They were written
 * when RAW was the default, so that the change would be visible the day the gateway started
 * normalizing. That day is 4.13.0: the default is now NORMALIZE, this class sets RAW explicitly to
 * go on describing the mode it was written for, and {@code PathHandlingV4IntegrationTest} carries
 * the counterpart under NORMALIZE, where these same paths answer 401 or 404.
 *
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DeployApi({ "/apis/v4/http/pathtraversal/api-alpha.json", "/apis/v4/http/pathtraversal/api-beta.json" })
class PathTraversalV4IntegrationTest extends AbstractGatewayTest {

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

    /** Only beta carries a plan, so any bypass below is obtained with no credential at all. */
    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        if (isV4Api(definitionClass) && "beta".equals(api.getId())) {
            configurePlans((Api) api.getDefinition(), Set.of("api-key"));
        }
    }

    @Override
    public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
        // Explicit, because this class describes RAW, not "whatever the default happens to be".
        // The default became NORMALIZE in 4.13.0, and under it these assertions no longer hold.
        configurationBuilder.set("http.pathHandling", "RAW");
    }

    @Test
    void should_serve_alpha_on_a_regular_path(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

        assertStatus(httpClient, "/alpha/api/echo", 200);

        assertThat(singleUpstreamRequestUrl()).isEqualTo("/alpha/api/echo");
    }

    @Test
    void should_reject_a_direct_call_to_beta_without_its_api_key(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

        assertStatus(httpClient, "/beta/api/echo", 401);

        assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
    }

    @Test
    void should_reach_beta_backend_through_dot_segments_without_any_credential(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

        // Same resource as the call above, spelled so that the raw path still starts with alpha's
        // context path. Alpha is selected, alpha's absent plan lets it through, and the upstream
        // resolves the segments back to beta.
        assertStatus(httpClient, "/alpha/api/../../beta/api/echo", 200);

        // The gateway resolved nothing: it emitted its own target's base path followed by the
        // client-controlled remainder, which lands outside that base path once resolved.
        assertThat(singleUpstreamRequestUrl()).isEqualTo("/alpha/api/../../beta/api/echo");
    }

    @Test
    void should_reach_beta_backend_through_encoded_dot_segments(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

        // Percent-encoded dots. Removing dot segments without first decoding the unreserved
        // characters, as RFC 3986 §6.2.2.2 requires, would leave this variant open.
        assertStatus(httpClient, "/alpha/api/%2e%2e/%2e%2e/beta/api/echo", 200);

        assertThat(singleUpstreamRequestUrl()).isEqualTo("/alpha/api/%2e%2e/%2e%2e/beta/api/echo");
    }

    @Test
    void should_forward_a_path_that_escapes_the_configured_endpoint_target(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

        // Nothing to do with a second API: the emitted URI simply leaves the subtree of the target
        // configured on alpha's endpoint, which is enough to reach any neighbour of that target.
        assertStatus(httpClient, "/alpha/api/../../elsewhere/resource", 200);

        assertThat(singleUpstreamRequestUrl()).isEqualTo("/alpha/api/../../elsewhere/resource");
    }

    private void assertStatus(final HttpClient httpClient, final String rawPath, final int expectedStatus) throws InterruptedException {
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

    /** The request line the gateway emitted upstream, verbatim. */
    private String singleUpstreamRequestUrl() {
        final List<LoggedRequest> upstreamRequests = wiremock.findAll(getRequestedFor(anyUrl()));
        assertThat(upstreamRequests).hasSize(1);
        return upstreamRequests.get(0).getUrl();
    }
}
