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
package io.gravitee.apim.integration.tests.http.failover;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.MutatingHttpProxyEndpointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingContent;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.logging.LoggingPhase;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.reporter.api.v4.log.Log;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests failover behavior when the endpoint connector mutates the request (body, headers, pathInfo).
 * Uses {@link MutatingHttpProxyEndpointConnectorFactory} to simulate connectors like LLM proxy
 * that transform the request before forwarding to the backend.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class FailoverV4MutatingEndpointIntegrationTest {

    private static final String RESPONSE_FROM_BACKEND = "response-from-backend";

    @Nested
    @GatewayTest
    class MultipleEndpointsWithMutatingConnector extends AbstractGatewayTest {

        BehaviorSubject<Metrics> metricsSubject;
        BehaviorSubject<Log> logSubject;

        @BeforeEach
        void setUp() {
            metricsSubject = BehaviorSubject.create();
            logSubject = BehaviorSubject.create();

            FakeReporter fakeReporter = getBean(FakeReporter.class);
            fakeReporter.setReportableHandler(reportable -> {
                switch (reportable) {
                    case Log l:
                        logSubject.onNext(l);
                        break;
                    case Metrics metrics:
                        metricsSubject.onNext(metrics.toBuilder().build());
                        break;
                    default:
                }
            });
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent(
                "mutating-http-proxy",
                EndpointBuilder.build("mutating-http-proxy", MutatingHttpProxyEndpointConnectorFactory.class)
            );
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (api.getDefinition() instanceof Api apiDefinition) {
                var analytics = apiDefinition.getAnalytics() != null ? apiDefinition.getAnalytics() : new Analytics();
                analytics.setEnabled(true);
                if (analytics.getLogging() == null) {
                    analytics.setLogging(
                        Logging.builder()
                            .mode(LoggingMode.builder().entrypoint(true).endpoint(true).build())
                            .phase(LoggingPhase.builder().request(true).response(true).build())
                            .content(LoggingContent.builder().headers(true).payload(true).build())
                            .build()
                    );
                }
                apiDefinition.setAnalytics(analytics);
            }
        }

        @Test
        @DeployApi("/apis/v4/http/failover/api-mutating-three-endpoints-post.json")
        void should_success_on_second_retry_with_mutated_body(HttpClient client) {
            // Given backend answers in 750ms on first two endpoints (slow call), and immediately on the third
            // The mutating connector transforms the body to "transformed-request-body"
            // and sets pathInfo to "/mutated-path", so wiremock receives on /endpoint-X/mutated-path
            wiremock.stubFor(post("/endpoint-1/mutated-path").willReturn(ok(RESPONSE_FROM_BACKEND + " - 1").withFixedDelay(750)));
            wiremock.stubFor(post("/endpoint-2/mutated-path").willReturn(ok(RESPONSE_FROM_BACKEND + " - 2").withFixedDelay(750)));
            wiremock.stubFor(post("/endpoint-3/mutated-path").willReturn(ok(RESPONSE_FROM_BACKEND + " - 3")));

            // When requesting the API with a body
            client
                .rxRequest(HttpMethod.POST, "/test")
                .flatMap(request -> request.rxSend(Buffer.buffer("request-body")))
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response).hasToString(RESPONSE_FROM_BACKEND + " - 3");
                    return true;
                });

            // Then each endpoint should have been called with the transformed body
            wiremock.verify(postRequestedFor(urlPathEqualTo("/endpoint-1/mutated-path")).withRequestBody(equalTo("mutated-request-body")));
            wiremock.verify(postRequestedFor(urlPathEqualTo("/endpoint-2/mutated-path")).withRequestBody(equalTo("mutated-request-body")));
            wiremock.verify(postRequestedFor(urlPathEqualTo("/endpoint-3/mutated-path")).withRequestBody(equalTo("mutated-request-body")));
        }

        @Test
        @DeployApi("/apis/v4/http/failover/api-mutating-failure-condition-post.json")
        void should_record_failover_metrics_with_mutated_request(HttpClient client) {
            // Given a backend that returns 500 on the first call, then 200 on the second
            wiremock.stubFor(
                post("/endpoint/mutated-path")
                    .inScenario("metrics-retry-post")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(serverError().withBody("error"))
                    .willSetStateTo("recovered")
            );
            wiremock.stubFor(
                post("/endpoint/mutated-path")
                    .inScenario("metrics-retry-post")
                    .whenScenarioStateIs("recovered")
                    .willReturn(ok(RESPONSE_FROM_BACKEND))
            );

            // When requesting the API with a body
            client
                .rxRequest(HttpMethod.POST, "/test")
                .flatMap(request -> request.rxSend(Buffer.buffer("request-body")))
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete();

            // Then failover metrics should be recorded
            metricsSubject
                .take(1)
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValue(metrics -> {
                    assertThat(metrics.longAdditionalMetrics()).isNotNull().containsEntry("long_failover_count", 1L);
                    assertThat(metrics.keywordAdditionalMetrics())
                        .isNotNull()
                        .containsKey("keyword_failover_first-failed-endpoint")
                        .containsEntry("keyword_failover_successful-endpoint", "default");
                    return true;
                });

            // Then the backend should have been called twice, each time with the transformed body
            wiremock.verify(2, postRequestedFor(urlPathEqualTo("/endpoint/mutated-path")).withRequestBody(equalTo("mutated-request-body")));

            // Then the log should contain the transformed body (not the original, not duplicated)
            logSubject
                .take(1)
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValue(log -> {
                    assertThat(log.getEntrypointRequest()).isNotNull();
                    assertThat(log.getEndpointRequest()).isNotNull();
                    assertThat(log.getEndpointRequest().getBody()).isEqualTo("mutated-request-body");
                    return true;
                });
        }
    }
}
