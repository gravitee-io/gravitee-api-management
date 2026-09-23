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
package io.gravitee.apim.integration.tests.reporting;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.integration.tests.fake.ReportDecisionPolicy;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.reporter.api.v4.metric.AdditionalMetric;
import io.gravitee.reporter.api.v4.report.DecisionReport;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DeployApi({ "/apis/v4/http/api-decision-permit.json", "/apis/v4/http/api-decision-fail-closed.json" })
class DecisionReportReportingIntegrationTest extends AbstractGatewayTest {

    private BehaviorSubject<DecisionReport> reported;

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
            ReportDecisionPolicy.POLICY_ID,
            PolicyBuilder.build(
                ReportDecisionPolicy.POLICY_ID,
                ReportDecisionPolicy.class,
                ReportDecisionPolicy.ReportDecisionConfiguration.class
            )
        );
    }

    @BeforeEach
    void captureReports() {
        reported = BehaviorSubject.create();
        getBean(FakeReporter.class).setReportableHandler(reportable -> {
            if (reportable instanceof DecisionReport decision) {
                reported.onNext(decision);
            }
        });
    }

    @Test
    void should_report_a_decision_emitted_by_a_policy(HttpClient httpClient) throws InterruptedException {
        final TestObserver<DecisionReport> decision = call(httpClient, "/test-decision-permit");

        decision
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(reportable -> {
                assertThat(reportable.getApiId()).isEqualTo("my-api-decision-permit-v4");
                assertThat(reportable.getGatewayId()).isNotBlank();
                assertThat(reportable.getDecisionPointType()).isEqualTo(DecisionReport.DECISION_POINT_GUARDIAN);
                assertThat(reportable.getCheckpoint()).isEqualTo("request.prompt");
                assertThat(reportable.getPhase()).isEqualTo(DecisionReport.Phase.RESOLVED);
                assertThat(reportable.getStatus()).isEqualTo(DecisionReport.Status.SUCCESS);
                assertThat(reportable.getSubjectId()).isEqualTo("jane@example.com");
                assertThat(reportable.getActorId()).isEqualTo("support-bot");
                assertThat(reportable.getOutcome()).isEqualTo(DecisionReport.Outcome.ALLOW);
                assertThat(reportable.getEnforced()).isEqualTo(DecisionReport.Enforced.ALLOW);
                assertThat(reportable.getConfidence()).isEqualTo(0.94);
                assertThat(reportable.getReasons()).containsExactly("no_pii_detected", "within_policy");
                assertThat(reportable.getMatchedRules())
                    .singleElement()
                    .satisfies(rule -> {
                        assertThat(rule.id()).isEqualTo("rule-1");
                        assertThat(rule.version()).isEqualTo("rev-1");
                    });
                assertThat(reportable.getRequestId()).isNotBlank();
                assertThat(reportable.timestamp()).isAfter(Instant.now().minusSeconds(300));
                return true;
            });
    }

    @Test
    void should_report_a_fail_closed_timeout_as_indeterminate_and_denied(HttpClient httpClient) throws InterruptedException {
        final TestObserver<DecisionReport> decision = call(httpClient, "/test-decision-fail-closed");

        decision
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(reportable -> {
                assertThat(reportable.getOutcome()).isEqualTo(DecisionReport.Outcome.INDETERMINATE);
                assertThat(reportable.getEnforced()).isEqualTo(DecisionReport.Enforced.DENY);
                assertThat(reportable.getIndeterminateCause()).isEqualTo(DecisionReport.IndeterminateCause.TIMEOUT);
                return true;
            });
    }

    @Test
    void should_carry_additional_metrics_through_the_pipeline(HttpClient httpClient) throws InterruptedException {
        final TestObserver<DecisionReport> decision = call(httpClient, "/test-decision-permit");

        decision
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(reportable -> {
                assertThat(reportable.getAdditionalMetrics()).containsExactlyInAnyOrder(
                    new AdditionalMetric.LongMetric("long_tokens", 512L),
                    new AdditionalMetric.KeywordMetric("keyword_model", "gpt-4o")
                );
                return true;
            });
    }

    private TestObserver<DecisionReport> call(HttpClient httpClient, String path) throws InterruptedException {
        wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

        final TestObserver<DecisionReport> decision = reported.firstOrError().test();

        httpClient
            .rxRequest(HttpMethod.GET, path)
            .flatMap(HttpClientRequest::rxSend)
            .test()
            .await()
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return true;
            })
            .assertNoErrors();

        return decision;
    }
}
