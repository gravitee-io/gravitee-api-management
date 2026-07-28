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
package io.gravitee.apim.integration.tests.http.flows;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.gateway.tests.sdk.utils.HttpClientUtils.extractHeaders;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.fake.LatencyPolicy;
import io.gravitee.apim.integration.tests.fake.SetAttributePolicy;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * When an earlier V4 flow runs an asynchronous policy, the condition of a subsequent flow must still be evaluated
 * only after that flow completed, so a flow depending on an attribute set by a previous flow is correctly skipped.
 *
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DeployApi(
    {
        "/apis/v4/http/flows/api-condition-after-async-policy.json",
        "/apis/v4/http/flows/api-condition-after-sync-policy.json",
        "/apis/v4/http/flows/api-bestmatch-condition.json",
    }
)
class FlowConditionAfterAsyncPolicyV4IntegrationTest extends AbstractGatewayTest {

    private static final String RESPONSE_FROM_BACKEND = "response from backend";
    private static final String FLOW2_MARKER_HEADER = "X-Flow2-Executed";

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
        policies.put(
            "transform-headers",
            PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
        );
        policies.put("latency", PolicyBuilder.build("latency", LatencyPolicy.class, LatencyPolicy.LatencyConfiguration.class));
        policies.put("set-attribute", PolicyBuilder.build("set-attribute", SetAttributePolicy.class));
    }

    @Test
    @DisplayName("Downstream flow condition must be evaluated after an async policy in a previous flow completes")
    void should_skip_downstream_flow_when_condition_becomes_false_after_async_policy_in_previous_flow(HttpClient client) {
        wiremock.stubFor(get("/endpoint").willReturn(ok(RESPONSE_FROM_BACKEND)));

        assertFlow2IsSkipped(client, "/test-condition-after-async");
    }

    @Test
    @DisplayName("Downstream flow condition is correctly evaluated when the previous flow has no async policy")
    void should_skip_downstream_flow_when_previous_flow_has_no_async_policy(HttpClient client) {
        wiremock.stubFor(get("/endpoint").willReturn(ok(RESPONSE_FROM_BACKEND)));

        assertFlow2IsSkipped(client, "/test-condition-after-sync");
    }

    @Test
    @DisplayName("Best match selection must consider the flow condition, not only the path")
    void should_best_match_among_flows_passing_their_condition(HttpClient client) {
        wiremock.stubFor(get("/endpoint/item").willReturn(ok(RESPONSE_FROM_BACKEND)));

        // The request best-matches the more specific flow (/item), but its condition is false; the less specific flow
        // (/) has a true condition and must be selected instead.
        assertBestMatchSelection(client, "A", "A");
        // When the more specific flow's condition also holds, best match still selects it.
        assertBestMatchSelection(client, "B", "B");
    }

    private void assertBestMatchSelection(HttpClient client, String pick, String expectedSelection) {
        client
            .rxRequest(HttpMethod.GET, "/test-bestmatch/item")
            .flatMap(request -> request.putHeader("X-Pick", pick).rxSend())
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(extractHeaders(response)).containsEntry("X-Selected", expectedSelection);
                return response.body();
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body).hasToString(RESPONSE_FROM_BACKEND);
                return true;
            });
    }

    private void assertFlow2IsSkipped(HttpClient client, String path) {
        client
            .rxRequest(HttpMethod.GET, path)
            .flatMap(request -> request.rxSend())
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                // The first flow already assigns "foo", so the second flow's condition
                // {#context.attributes['foo'] == null} must be false and the flow must be skipped.
                assertThat(extractHeaders(response)).doesNotContainKey(FLOW2_MARKER_HEADER);
                return response.body();
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body).hasToString(RESPONSE_FROM_BACKEND);
                return true;
            });
    }
}
