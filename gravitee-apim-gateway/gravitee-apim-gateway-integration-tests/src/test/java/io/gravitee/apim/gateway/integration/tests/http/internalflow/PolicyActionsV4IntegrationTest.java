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

import io.gravitee.apim.gateway.integration.tests.fake.ActionOutcomePolicy;
import io.gravitee.apim.gateway.integration.tests.fake.ActionOutcomePolicyConfiguration;
import io.gravitee.apim.gateway.integration.tests.fake.ActionTracePolicy;
import io.gravitee.apim.gateway.integration.tests.fake.FlowTracePolicy;
import io.gravitee.apim.gateway.integration.tests.fake.FlowTracePolicyConfiguration;
import io.gravitee.apim.gateway.integration.tests.fake.InterruptPolicy;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@GatewayTest
@DeployApi(
    {
        "/apis/v4/http/internalflow/api-policy-actions.json",
        "/apis/v4/http/internalflow/api-policy-actions-interrupted.json",
        "/apis/v4/http/internalflow/api-policy-actions-interrupting.json",
        "/apis/v4/http/internalflow/api-policy-actions-interrupting-endpoint-request.json",
        "/apis/v4/http/internalflow/api-policy-actions-failing-entrypoint-response.json",
        "/apis/v4/http/internalflow/api-policy-actions-failing-terminate.json",
    }
)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PolicyActionsV4IntegrationTest extends AbstractGatewayTest {

    private ReplaySubject<Metrics> reportedMetrics;

    @BeforeEach
    void captureReportedMetrics() {
        reportedMetrics = ReplaySubject.create();
        getBean(FakeReporter.class).setReportableHandler(reportable -> {
            if (reportable instanceof Metrics metrics) {
                reportedMetrics.onNext(metrics);
            }
        });
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("flow-trace", PolicyBuilder.build("flow-trace", FlowTracePolicy.class, FlowTracePolicyConfiguration.class));
        policies.put("action-trace", PolicyBuilder.build("action-trace", ActionTracePolicy.class, FlowTracePolicyConfiguration.class));
        policies.put("interrupt", PolicyBuilder.build("interrupt", InterruptPolicy.class));
        policies.put(
            "action-outcome",
            PolicyBuilder.build("action-outcome", ActionOutcomePolicy.class, ActionOutcomePolicyConfiguration.class)
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

    @Test
    void should_run_the_endpoint_request_actions_in_order_before_the_endpoint_and_the_entrypoint_response_ones_in_reverse_order_last(
        HttpClient client
    ) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        assertThat(flowTrace(client, "/policy-actions", 200)).isEqualTo(
            String.join(
                ",",
                "internal-request",
                "api-request",
                "internal-action-endpoint-request",
                "api-action-endpoint-request",
                "api-response",
                "internal-response",
                "api-action-entrypoint-response",
                "internal-action-entrypoint-response"
            )
        );
    }

    @Test
    void should_run_the_terminate_actions_in_reverse_order_once_the_response_has_been_handled(HttpClient client) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        flowTrace(client, "/policy-actions", 200);

        assertThat(reportedTrace("policy-actions")).endsWith(
            "api-action-entrypoint-response,internal-action-entrypoint-response,api-action-terminate,internal-action-terminate"
        );
    }

    @Test
    void should_run_only_the_entrypoint_response_and_terminate_actions_when_the_request_never_reaches_the_endpoint(HttpClient client) {
        assertThat(flowTrace(client, "/policy-actions-interrupted", 403)).isEqualTo("internal-action-entrypoint-response");
        assertThat(reportedTrace("policy-actions-interrupted")).isEqualTo("internal-action-entrypoint-response,internal-action-terminate");

        wiremock.verify(0, getRequestedFor(anyUrl()));
    }

    @Test
    void should_answer_with_the_interruption_of_an_endpoint_request_action_without_reaching_the_endpoint(HttpClient client) {
        flowTrace(client, "/policy-actions-interrupting-endpoint-request", ActionOutcomePolicy.STATUS);

        wiremock.verify(0, getRequestedFor(anyUrl()));
    }

    @Test
    void should_answer_with_the_interruption_of_an_entrypoint_response_action(HttpClient client) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        flowTrace(client, "/policy-actions-interrupting", ActionOutcomePolicy.STATUS);

        wiremock.verify(1, getRequestedFor(anyUrl()));
    }

    @Test
    void should_answer_with_a_500_when_an_entrypoint_response_action_fails(HttpClient client) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        flowTrace(client, "/policy-actions-failing-entrypoint-response", 500);
    }

    @Test
    void should_keep_the_response_and_run_the_other_terminate_actions_when_one_fails(HttpClient client) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        flowTrace(client, "/policy-actions-failing-terminate", 200);

        assertThat(reportedTrace("policy-actions-failing-terminate")).endsWith("internal-action-terminate");
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

    private String reportedTrace(String apiId) {
        return reportedMetrics
            .filter(metrics -> apiId.equals(metrics.getApiId()))
            .firstOrError()
            .map(metrics -> metrics.getCustomMetrics().get(ActionTracePolicy.TRACE_METRIC))
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .values()
            .getFirst();
    }
}
