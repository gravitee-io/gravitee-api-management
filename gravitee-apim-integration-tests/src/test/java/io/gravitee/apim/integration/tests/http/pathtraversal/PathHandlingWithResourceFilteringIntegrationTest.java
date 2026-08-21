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
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.resourcefiltering.ResourceFilteringPolicy;
import io.gravitee.policy.resourcefiltering.configuration.ResourceFilteringPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * What a request actually meets when {@code http.pathHandling} and a policy that normalizes on its
 * own are both in play.
 *
 * <p>The product carries two normalizers: the gateway's, applied before the listener is resolved,
 * and the one inside {@code gravitee-policy-resource-filtering}, applied inside the API. They do
 * not implement the same rules, and the policy's can be switched off — so a filter written against
 * one interpretation can be evaded by a path the other resolves differently. That is the reported
 * bug reproduced between two of our own components, and it is asserted here rather than reasoned
 * about.
 *
 * <p>The API denies the {@code /admin/**} subtree. The request under test asks for it the long way
 * round, through a dot segment, and each class below is one combination of the two switches.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PathHandlingWithResourceFilteringIntegrationTest {

    /** Denied outright by the policy, whatever else happens. */
    private static final String ADMIN = "/alpha/api/admin/secret";

    /** The same resource, spelled so that only a normalizer sees the {@code /admin} in it. */
    private static final String ADMIN_THROUGH_A_DOT_SEGMENT = "/alpha/api/public/../admin/secret";

    abstract static class ResourceFilteringTest extends AbstractGatewayTest {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.put(
                "resource-filtering",
                PolicyBuilder.build("resource-filtering", ResourceFilteringPolicy.class, ResourceFilteringPolicyConfiguration.class)
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

        protected void assertBackendWasNotCalled() {
            assertThat(wiremock.findAll(getRequestedFor(anyUrl()))).isEmpty();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/pathtraversal/api-alpha-resource-filtering.json")
    class With_raw_gateway_and_a_policy_that_does_not_normalize extends ResourceFilteringTest {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "RAW");
        }

        @Test
        void should_deny_the_admin_subtree_asked_for_plainly(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, ADMIN, 403);

            assertBackendWasNotCalled();
        }

        @Test
        void should_let_a_dot_segment_through_the_filter(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            // Neither component resolves the path, so the filter never sees an /admin to deny and
            // the request reaches the backend. This is the parser differential, between our own
            // components, and it is the reason both switches exist.
            assertStatus(httpClient, ADMIN_THROUGH_A_DOT_SEGMENT, 200);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/pathtraversal/api-alpha-resource-filtering.json")
    class With_normalize_gateway_and_a_policy_that_does_not_normalize extends ResourceFilteringTest {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "NORMALIZE");
        }

        @Test
        void should_deny_the_admin_subtree_asked_for_plainly(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            assertStatus(httpClient, ADMIN, 403);

            assertBackendWasNotCalled();
        }

        @Test
        void should_close_the_hole_for_a_policy_whose_own_normalization_is_off(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            // The result worth having: the gateway resolved the path before the API was even
            // selected, so the filter is handed /admin/secret and denies it — without the policy
            // being reconfigured, and without it knowing anything about the setting.
            assertStatus(httpClient, ADMIN_THROUGH_A_DOT_SEGMENT, 403);

            assertBackendWasNotCalled();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/pathtraversal/api-alpha-resource-filtering-normalizing.json")
    class With_raw_gateway_and_a_policy_that_normalizes extends ResourceFilteringTest {

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.set("http.pathHandling", "RAW");
        }

        @Test
        void should_defend_itself_without_help_from_the_gateway(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

            // The policy resolves the path itself before matching, so the filter holds even under
            // RAW. Worth pinning: it is the only reason the older deployments are not exposed here.
            assertStatus(httpClient, ADMIN_THROUGH_A_DOT_SEGMENT, 403);

            assertBackendWasNotCalled();
        }
    }
}
