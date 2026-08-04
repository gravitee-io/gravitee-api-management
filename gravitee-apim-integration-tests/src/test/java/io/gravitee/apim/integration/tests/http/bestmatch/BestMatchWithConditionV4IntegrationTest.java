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
package io.gravitee.apim.integration.tests.http.bestmatch;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.transformheaders.TransformHeadersPolicy;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Characterizes how a flow condition takes part in the BEST_MATCH selection, end to end.
 *
 * A condition makes a flow eligible, or not, <b>before</b> the most specific flow is picked. It is not a filter
 * applied afterwards to the winner: when the most specific flow is ruled out by its condition, a less specific one
 * is selected in its place.
 *
 * The API declares, both at plan and at API level, a specific flow reserved to the gold tier and a broader
 * unconditional one, so that both chains are covered by the same requests.
 *
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DeployApi("/apis/v4/http/bestmatch/api-condition.json")
class BestMatchWithConditionV4IntegrationTest extends AbstractGatewayTest {

    private static final String PLAN_FLOW_SELECTED = "X-Plan-Flow-Selected";
    private static final String API_FLOW_SELECTED = "X-Api-Flow-Selected";
    private static final String TIER = "X-Tier";

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.putIfAbsent(
            "transform-headers",
            PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
        );
    }

    @Test
    void should_select_the_most_specific_flow_when_its_condition_is_true(HttpClient client) {
        callApi(client, "/books/145", "gold");

        wiremock.verify(
            getRequestedFor(urlPathEqualTo("/endpoint/books/145"))
                .withHeader(PLAN_FLOW_SELECTED, equalTo("/books/:bookId"))
                .withHeader(API_FLOW_SELECTED, equalTo("/books/:bookId"))
        );
    }

    @Test
    void should_select_the_less_specific_flow_when_the_most_specific_condition_is_false(HttpClient client) {
        callApi(client, "/books/145", "silver");

        // The gold flow is not a candidate at all, so the broader flow wins the selection.
        wiremock.verify(
            getRequestedFor(urlPathEqualTo("/endpoint/books/145"))
                .withHeader(PLAN_FLOW_SELECTED, equalTo("/books"))
                .withHeader(API_FLOW_SELECTED, equalTo("/books"))
        );
    }

    @Test
    void should_select_no_flow_when_the_only_candidate_condition_is_false(HttpClient client) {
        callApi(client, "/vip", "silver");

        // No fallback flow is declared under /vip: the request is still proxied, without any flow being executed.
        wiremock.verify(
            getRequestedFor(urlPathEqualTo("/endpoint/vip"))
                .withHeader(PLAN_FLOW_SELECTED, absent())
                .withHeader(API_FLOW_SELECTED, absent())
        );
    }

    @Test
    void should_select_the_conditional_flow_when_no_other_flow_matches_the_path(HttpClient client) {
        callApi(client, "/vip", "gold");

        wiremock.verify(
            getRequestedFor(urlPathEqualTo("/endpoint/vip"))
                .withHeader(PLAN_FLOW_SELECTED, absent())
                .withHeader(API_FLOW_SELECTED, equalTo("/vip"))
        );
    }

    private void callApi(final HttpClient client, final String path, final String tier) {
        wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

        client
            .rxRequest(HttpMethod.GET, "/test-bestmatch-condition" + path)
            .flatMap(request -> request.putHeader(TIER, tier).rxSend())
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete();
    }
}
