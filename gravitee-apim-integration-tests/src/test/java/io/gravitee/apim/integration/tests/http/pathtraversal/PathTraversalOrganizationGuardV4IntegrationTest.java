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

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployOrganization;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
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
import io.gravitee.policy.groovy.GroovyInitializer;
import io.gravitee.policy.groovy.GroovyPolicy;
import io.gravitee.policy.groovy.configuration.GroovyPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Covers the mitigation available without a gateway change: a Groovy policy on an organization flow
 * that rejects any request whose path carries dot segments.
 *
 * <p>An organization flow runs inside the API reactor but ahead of the security chain and of the
 * backend call, so it closes the traversal for every API on the gateway at once, including APIs
 * published later, and without touching a single API definition.
 *
 * <p>Its limit is worth stating precisely, and the last test pins it: the API has already been
 * selected by the time the policy runs, so the guard can only <em>reject</em> the request. It cannot
 * route it to the API the resolved path actually designates. Reaching that behaviour requires
 * normalizing before listener resolution, which is a gateway-level change.
 *
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DeployOrganization(
    organization = "/organizations/organization-reject-dot-segments.json",
    apis = { "/apis/v4/http/pathtraversal/api-alpha.json", "/apis/v4/http/pathtraversal/api-beta.json" }
)
class PathTraversalOrganizationGuardV4IntegrationTest extends AbstractGatewayTest {

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("groovy", PolicyBuilder.build("groovy", GroovyPolicy.class, GroovyPolicyConfiguration.class, GroovyInitializer.class));
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

    @Test
    void should_still_serve_a_regular_path(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

        assertStatus(httpClient, "/alpha/api/echo", 200);
    }

    @Test
    void should_reject_dot_segments_before_the_backend_is_called(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

        assertStatus(httpClient, "/alpha/api/../../beta/api/echo", 400);

        assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
    }

    @Test
    void should_reject_encoded_dot_segments_before_the_backend_is_called(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

        assertStatus(httpClient, "/alpha/api/%2e%2e/%2e%2e/beta/api/echo", 400);

        assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
    }

    @Test
    void should_reject_rather_than_route_to_the_api_the_resolved_path_designates(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

        // Once resolved the path designates beta, whose plan would answer 401 without an api key.
        // The guard answers 400 instead: it interrupts, it does not re-route. This is the gap that
        // only front-door normalization closes.
        assertStatus(httpClient, "/alpha/api/../../beta/api/echo", 400);

        assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
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
}
