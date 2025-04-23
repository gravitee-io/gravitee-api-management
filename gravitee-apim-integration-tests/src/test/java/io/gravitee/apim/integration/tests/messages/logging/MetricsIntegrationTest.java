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
package io.gravitee.apim.integration.tests.messages.logging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DeployApi("/apis/v4/messages/metrics/sse-entrypoint-mock-endpoint.json")
class MetricsIntegrationTest extends AbstractGatewayTest {

    public static final String CLIENT_ID = "my-client-id";
    public static final String TRANSACTION_ID = "my-transaction-id";
    public static final String API_ID = "metrics-message-api";
    BehaviorSubject<Metrics> subject;

    @BeforeEach
    void setUp() {
        subject = BehaviorSubject.create();

        FakeReporter fakeReporter = getBean(FakeReporter.class);
        fakeReporter.setReportableHandler(reportable -> {
            if (reportable instanceof Metrics) {
                subject.onNext(((Metrics) reportable).toBuilder().build());
            }
        });
    }

    @Test
    void should_report_the_metrics_for_the_request(HttpClient httpClient) {
        // 1. Start the SSE request
        var sseRequest = httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request
                    .putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM)
                    .putHeader("X-Gravitee-Transaction-Id", TRANSACTION_ID)
                    .putHeader("X-Gravitee-Client-Identifier", CLIENT_ID);
                return request.rxSend();
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test();

        // 2. Check that a partial metrics has been reported while the request is running
        var partialMetric = subject
            .take(1)
            .test()
            .awaitDone(30, SECONDS)
            .assertValue(metrics -> {
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(metrics.getApiId()).isEqualTo(API_ID);
                    soft.assertThat(metrics.getPlanId()).isEqualTo("default_plan");
                    soft.assertThat(metrics.getApplicationId()).isEqualTo("1");
                    soft.assertThat(metrics.getClientIdentifier()).isEqualTo(CLIENT_ID);
                    soft.assertThat(metrics.getTransactionId()).isEqualTo(TRANSACTION_ID);
                    soft.assertThat(metrics.isRequestEnded()).isFalse();
                    soft.assertThat(metrics.getStatus()).isZero();
                    soft.assertThat(metrics.getEndpointResponseTimeMs()).isPositive();
                    soft.assertThat(metrics.getGatewayLatencyMs()).isEqualTo(0);
                    soft.assertThat(metrics.getGatewayResponseTimeMs()).isEqualTo(0);
                });

                return true;
            })
            .values()
            .get(0);

        // 3. Stop the SSE request
        sseRequest.cancel();

        // 4. Check that a full metrics has been reported once the connection is ended
        subject
            .skip(1)
            .take(1)
            .test()
            .awaitDone(30, SECONDS)
            .assertValue(fullMetrics -> {
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(fullMetrics.getRequestId()).isEqualTo(partialMetric.getRequestId());
                    soft.assertThat(fullMetrics.isRequestEnded()).isTrue();
                    soft.assertThat(fullMetrics.getStatus()).isEqualTo(200);
                    soft.assertThat(fullMetrics.getGatewayResponseTimeMs()).isGreaterThan(0);
                    soft.assertThat(fullMetrics.getEndpointResponseTimeMs()).isGreaterThan(0);
                    soft.assertThat(fullMetrics.getGatewayLatencyMs()).isGreaterThan(0);
                });
                return true;
            });
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", MockEndpointConnectorFactory.class));
    }

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        var analytics = new Analytics();
        analytics.setEnabled(true);

        if (api.getDefinition() instanceof Api) {
            ((Api) api.getDefinition()).setAnalytics(analytics);
        }
    }
}
