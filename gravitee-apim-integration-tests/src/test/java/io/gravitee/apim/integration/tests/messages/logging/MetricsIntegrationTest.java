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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import com.graviteesource.entrypoint.webhook.WebhookEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.MessageStorage;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.integration.tests.messages.webhook.WebhookTestingActions;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.reporter.api.v4.common.MessageConnectorType;
import io.gravitee.reporter.api.v4.common.MessageOperation;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@Slf4j
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricsIntegrationTest extends AbstractGatewayTest {

    private static final String WEBHOOK_URL_PATH = "/webhook";
    public static final String CLIENT_ID = "my-client-id";
    public static final String TRANSACTION_ID = "my-transaction-id";
    public static final String SSE_API_ID = "sse-metrics-message-api";
    private static final String WEBHOOK_API_ID = "webhook-metrics-message-api";
    ReplaySubject<Metrics> metricsSubject;
    ReplaySubject<MessageMetrics> messageMetricsSubject;

    @BeforeEach
    void setUp() {
        metricsSubject = ReplaySubject.create();
        messageMetricsSubject = ReplaySubject.create();

        FakeReporter fakeReporter = getBean(FakeReporter.class);
        fakeReporter.setReportableHandler(reportable -> {
            if (reportable instanceof Metrics metrics) {
                log.info("REPORT");
                metricsSubject.onNext(metrics.toBuilder().build());
            } else if (reportable instanceof MessageMetrics messageMetrics) {
                log.info("REPORT MESSAGE METRICS {}", messageMetrics);
                messageMetricsSubject.onNext(messageMetrics);
            }
        });
    }

    @Test
    @DeployApi("/apis/v4/messages/metrics/sse-entrypoint-mock-endpoint.json")
    void should_report_the_metrics_for_sse_request(HttpClient httpClient) {
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
        var partialMetric = metricsSubject
            .take(1)
            .test()
            .awaitDone(30, SECONDS)
            .assertValue(metrics -> {
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(metrics.getApiId()).isEqualTo(SSE_API_ID);
                    soft.assertThat(metrics.getPlanId()).isEqualTo("default_plan");
                    soft.assertThat(metrics.getApplicationId()).isEqualTo("1");
                    soft.assertThat(metrics.getClientIdentifier()).isEqualTo(CLIENT_ID);
                    soft.assertThat(metrics.getTransactionId()).isEqualTo(TRANSACTION_ID);
                    soft.assertThat(metrics.isRequestEnded()).isFalse();
                    soft.assertThat(metrics.getStatus()).isEqualTo(200);
                    soft.assertThat(metrics.getEndpointResponseTimeMs()).isPositive();
                    soft.assertThat(metrics.getGatewayLatencyMs()).isEqualTo(0);
                    soft.assertThat(metrics.getGatewayResponseTimeMs()).isEqualTo(0);
                });

                return true;
            })
            .values()
            .getFirst();

        // 3. Stop the SSE request
        sseRequest.cancel();

        // 4. Check that a full metrics has been reported once the connection is ended
        metricsSubject
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
                    soft.assertThat(fullMetrics.getEndpointResponseTimeMs()).isGreaterThanOrEqualTo(0);
                    soft.assertThat(fullMetrics.getGatewayLatencyMs()).isGreaterThan(0);
                });
                return true;
            });
    }

    @Test
    @DeployApi("/apis/v4/messages/metrics/webhook-entrypoint-mock-endpoint.json")
    void should_report_the_metrics_for_webhook_request() throws JsonProcessingException {
        MessageStorage messageStorage = getBean(MessageStorage.class);
        WebhookTestingActions webhookActions = new WebhookTestingActions(wiremock, getBean(SubscriptionDispatcher.class));
        final int messageCount = 10;
        final String callbackPath = WEBHOOK_URL_PATH + "/test";
        final Subscription subscription = webhookActions.createSubscription(WEBHOOK_API_ID, callbackPath);
        try {
            webhookActions
                .dispatchSubscription(subscription)
                .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
                .test()
                .awaitDone(10, SECONDS)
                .assertComplete();

            webhookActions.verifyMessagesAtLeast(messageCount, callbackPath, "message");

            // 2. Check that a partial metrics has been reported while the request is running
            var partialMetric = metricsSubject
                .take(1)
                .test()
                .awaitDone(30, SECONDS)
                .assertValue(metrics -> {
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(metrics.getApiId()).isEqualTo(WEBHOOK_API_ID);
                        soft.assertThat(metrics.getPlanId()).isEqualTo(subscription.getPlan());
                        soft.assertThat(metrics.getApplicationId()).isNotBlank();
                        soft.assertThat(metrics.getClientIdentifier()).isNotEmpty(); // Auto-generated by internal subscription process.
                        soft.assertThat(metrics.getTransactionId()).isNotEmpty(); // Auto-generated by internal subscription process.
                        soft.assertThat(metrics.isRequestEnded()).isFalse();
                        soft.assertThat(metrics.getStatus()).isEqualTo(0);
                        soft.assertThat(metrics.getEndpointResponseTimeMs()).isGreaterThanOrEqualTo(0);
                        soft.assertThat(metrics.getGatewayLatencyMs()).isGreaterThanOrEqualTo(0);
                        soft.assertThat(metrics.getGatewayResponseTimeMs()).isGreaterThanOrEqualTo(0);
                    });

                    return true;
                })
                .values()
                .getFirst();

            // 3. Stop the webhook
            webhookActions.closeSubscription(subscription);

            // 4. Check that a full metrics has been reported once the connection is ended
            metricsSubject
                .skip(1)
                .take(1)
                .test()
                .awaitDone(10, SECONDS)
                .assertValue(fullMetrics -> {
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(fullMetrics.getRequestId()).isEqualTo(partialMetric.getRequestId());
                        soft.assertThat(fullMetrics.isRequestEnded()).isTrue();
                        soft.assertThat(fullMetrics.getStatus()).isEqualTo(200);
                        soft.assertThat(fullMetrics.getGatewayResponseTimeMs()).isGreaterThan(0);
                        soft.assertThat(fullMetrics.getEndpointResponseTimeMs()).isGreaterThanOrEqualTo(0);
                        soft.assertThat(fullMetrics.getGatewayLatencyMs()).isGreaterThan(0);
                    });
                    return true;
                });

            // Check the message metrics.
            messageMetricsSubject
                .take(2)
                .test()
                .awaitDone(10, SECONDS)
                .assertValueAt(0, messageMetrics -> {
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(messageMetrics.getApiId()).isEqualTo(WEBHOOK_API_ID);
                        soft.assertThat(messageMetrics.getConnectorType()).isEqualTo(MessageConnectorType.ENDPOINT);
                        soft.assertThat(messageMetrics.getOperation()).isEqualTo(MessageOperation.SUBSCRIBE);
                        soft.assertThat(messageMetrics.getConnectorId()).isEqualTo("mock");
                        soft.assertThat(messageMetrics.isError()).isFalse();
                        soft.assertThat(messageMetrics.getClientIdentifier()).isNotEmpty(); // Auto-generated by internal subscription process.
                        soft.assertThat(messageMetrics.getRequestId()).isEqualTo(partialMetric.getRequestId());
                        soft.assertThat(messageMetrics.getCount()).isPositive();
                        soft.assertThat(messageMetrics.getCountIncrement()).isPositive();
                        soft.assertThat(messageMetrics.getErrorCount()).isEqualTo(-1);
                        soft.assertThat(messageMetrics.getErrorCountIncrement()).isEqualTo(-1);
                        soft.assertThat(messageMetrics.getGatewayLatencyMs()).isEqualTo(-1);
                        soft.assertThat(messageMetrics.getAdditionalMetrics()).isEmpty();
                    });
                    return true;
                })
                .assertValueAt(1, messageMetrics -> {
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(messageMetrics.getApiId()).isEqualTo(WEBHOOK_API_ID);
                        soft.assertThat(messageMetrics.getConnectorType()).isEqualTo(MessageConnectorType.ENTRYPOINT);
                        soft.assertThat(messageMetrics.getOperation()).isEqualTo(MessageOperation.SUBSCRIBE);
                        soft.assertThat(messageMetrics.getConnectorId()).isEqualTo("webhook");
                        soft.assertThat(messageMetrics.isError()).isFalse();
                        soft.assertThat(messageMetrics.getClientIdentifier()).isNotEmpty(); // Auto-generated by internal subscription process.
                        soft.assertThat(messageMetrics.getRequestId()).isEqualTo(partialMetric.getRequestId());
                        soft.assertThat(messageMetrics.getCount()).isPositive();
                        soft.assertThat(messageMetrics.getCountIncrement()).isPositive();
                        soft.assertThat(messageMetrics.getErrorCount()).isEqualTo(-1);
                        soft.assertThat(messageMetrics.getErrorCountIncrement()).isEqualTo(-1);
                        soft.assertThat(messageMetrics.getGatewayLatencyMs()).isGreaterThanOrEqualTo(0); // Time to process the message
                        soft.assertThat(messageMetrics.getAdditionalMetrics()).isNotEmpty();
                    });
                    return true;
                });
        } finally {
            messageStorage.reset();
            webhookActions.closeSubscription(subscription);
        }
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("webhook", EntrypointBuilder.build("webhook", WebhookEntrypointConnectorFactory.class));
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
