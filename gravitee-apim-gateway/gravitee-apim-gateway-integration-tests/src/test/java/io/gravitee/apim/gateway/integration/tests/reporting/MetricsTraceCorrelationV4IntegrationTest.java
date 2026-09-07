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
package io.gravitee.apim.gateway.integration.tests.reporting;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.tracing.Tracing;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Runs a real tracer, so the ids only reach the metrics if the dispatcher has opened the root span
 * by the time {@code MetricsProcessor} reads them.
 *
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/v4/http/api.json" })
class MetricsTraceCorrelationV4IntegrationTest extends AbstractGatewayTest {

    private static final String CLIENT_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String CLIENT_SPAN_ID = "00f067aa0ba902b7";

    private BehaviorSubject<Metrics> reported;

    @Override
    public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
        configurationBuilder.set("services.opentelemetry.enabled", true);
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
        if (api.getDefinition() instanceof Api definition) {
            var analytics = new Analytics();
            analytics.setEnabled(true);
            var tracing = new Tracing();
            tracing.setEnabled(true);
            analytics.setTracing(tracing);
            definition.setAnalytics(analytics);
        }
    }

    @BeforeEach
    void captureReports() {
        reported = BehaviorSubject.create();
        getBean(FakeReporter.class).setReportableHandler(reportable -> {
            if (reportable instanceof Metrics metrics) {
                reported.onNext(metrics);
            }
        });
    }

    @Test
    void should_report_the_ids_of_the_root_span_when_tracing_is_enabled(HttpClient httpClient) throws InterruptedException {
        Metrics metrics = callApiAndAwaitMetrics(httpClient, null);

        assertThat(metrics.getTraceId()).matches("[0-9a-f]{32}");
        assertThat(metrics.getSpanId()).matches("[0-9a-f]{16}");
    }

    @Test
    void should_report_the_trace_of_the_client_and_the_span_of_the_gateway(HttpClient httpClient) throws InterruptedException {
        Metrics metrics = callApiAndAwaitMetrics(httpClient, "00-" + CLIENT_TRACE_ID + "-" + CLIENT_SPAN_ID + "-01");

        assertThat(metrics.getTraceId()).isEqualTo(CLIENT_TRACE_ID);
        assertThat(metrics.getSpanId()).matches("[0-9a-f]{16}").isNotEqualTo(CLIENT_SPAN_ID);
    }

    private Metrics callApiAndAwaitMetrics(HttpClient httpClient, String traceparent) throws InterruptedException {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));
        TestObserver<Metrics> metrics = reported.firstOrError().test();

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                if (traceparent != null) {
                    request.putHeader("traceparent", traceparent);
                }
                return request.rxSend();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return true;
            });

        // ReporterProcessor runs after the response has ended, so the client can observe it first.
        metrics.await(30, TimeUnit.SECONDS);
        metrics.assertComplete();
        return metrics.values().getFirst();
    }
}
