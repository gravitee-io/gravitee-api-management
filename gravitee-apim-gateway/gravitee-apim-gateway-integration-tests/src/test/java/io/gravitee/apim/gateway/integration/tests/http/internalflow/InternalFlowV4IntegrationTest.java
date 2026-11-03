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
package io.gravitee.apim.gateway.integration.tests.http.internalflow;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.integration.tests.fake.FlowTracePolicy;
import io.gravitee.apim.gateway.integration.tests.fake.FlowTracePolicyConfiguration;
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
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@GatewayTest
@DeployApi(
    {
        "/apis/v4/http/internalflow/api-internal-flow.json",
        "/apis/v4/http/internalflow/api-disabled-internal-flow.json",
        "/apis/v4/http/internalflow/api-internal-flow-match-required.json",
    }
)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class InternalFlowV4IntegrationTest extends AbstractGatewayTest {

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("flow-trace", PolicyBuilder.build("flow-trace", FlowTracePolicy.class, FlowTracePolicyConfiguration.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Test
    void should_run_the_internal_flow_around_the_api_flows(HttpClient client) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        assertThat(flowTrace(client, "/internal-flow", 200)).isEqualTo("internal-request,api-request,api-response,internal-response");
    }

    @Test
    void should_not_run_a_disabled_internal_flow(HttpClient client) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        assertThat(flowTrace(client, "/disabled-internal-flow", 200)).isEqualTo("api-request,api-response");
    }

    @Test
    void should_still_reject_a_request_no_api_flow_matches_when_a_flow_match_is_required(HttpClient client) {
        flowTrace(client, "/internal-flow-match-required", 404);

        wiremock.verify(0, getRequestedFor(anyUrl()));
    }

    private static String flowTrace(HttpClient client, String path, int expectedStatus) {
        return client
            .rxRequest(HttpMethod.GET, path)
            .flatMap(HttpClientRequest::rxSend)
            .map(response -> {
                assertThat(response.statusCode()).isEqualTo(expectedStatus);
                return String.valueOf(response.getHeader(FlowTracePolicy.TRACE_HEADER));
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .values()
            .getFirst();
    }
}
