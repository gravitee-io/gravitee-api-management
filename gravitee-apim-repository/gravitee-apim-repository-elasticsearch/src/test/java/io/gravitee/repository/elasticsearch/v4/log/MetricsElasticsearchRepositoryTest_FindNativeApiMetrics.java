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
package io.gravitee.repository.elasticsearch.v4.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetrics;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link MetricsElasticsearchRepository#findNativeApiMetrics} against a real testcontainers ES,
 * seeded by {@code freemarker-v4-analytics/v4-metrics.ftl}.
 *
 * <p>Fixture: four native-kafka events on api {@code kafka-api-001}, all inside {@link #FROM_MILLIS}/
 * {@link #TO_MILLIS}:
 * <ul>
 *   <li>{@code conn-connected-001}    @T10:00 — CONNECTED, app-001/plan-001/sub-A</li>
 *   <li>{@code conn-connected-002}    @T10:01 — CONNECTED, app-002/plan-002/sub-C</li>
 *   <li>{@code conn-error-001}        @T10:05 — CONNECTION_ERROR (no broker assigned), app-001/plan-001/sub-B</li>
 *   <li>{@code conn-disconnect-001}   @T10:30 — DISCONNECTED after 30 minutes, app-001/plan-001/sub-A</li>
 * </ul>
 */
public class MetricsElasticsearchRepositoryTest_FindNativeApiMetrics extends AbstractElasticsearchRepositoryTest {

    /** Window [today 09:00 UTC, today 11:00 UTC) covering all native fixture events. */
    private static final long FROM_MILLIS =
        ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).withHour(9).withMinute(0).withSecond(0).withNano(0).toEpochSecond() * 1000;
    private static final long TO_MILLIS =
        ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).withHour(11).withMinute(0).withSecond(0).withNano(0).toEpochSecond() * 1000;

    private static final String API_ID = "kafka-api-001";
    private static final String GATEWAY_ID = "2c99d50d-d318-42d3-99d5-0dd31862d3d2";
    private static final String LOCAL_ADDRESS = "10.0.2.208";
    private static final String NATIVE_HOST = "broker.kafka.local:9092";
    private static final String ENTRYPOINT_ID = "native-kafka";

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private MetricsElasticsearchRepository metricsV4Repository;

    @Autowired
    private TimeProvider timeProvider;

    private String today;

    @PostConstruct
    public void init() {
        today = timeProvider.getDateToday();
    }

    @Test
    void returns_empty_optional_when_request_id_is_unknown() throws AnalyticsException {
        var result = metricsV4Repository.findNativeApiMetrics(queryContext, API_ID, "no-such-request", FROM_MILLIS, TO_MILLIS);

        assertThat(result).isEmpty();
    }

    @Test
    void returns_connected_event_with_full_metric_content() throws AnalyticsException {
        var result = findById("conn-connected-001");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getApiId()).isEqualTo(API_ID);
            soft.assertThat(result.getRequestId()).isEqualTo("conn-connected-001");
            soft.assertThat(result.getTransactionId()).isEqualTo("conn-connected-001");
            soft.assertThat(result.getApplicationId()).isEqualTo("kafka-app-001");
            soft.assertThat(result.getPlanId()).isEqualTo("kafka-plan-001");
            soft.assertThat(result.getSubscriptionId()).isEqualTo("kafka-sub-A");
            soft.assertThat(result.getClientIdentifier()).isEqualTo("client-fp-A");
            soft.assertThat(result.getEntrypointId()).isEqualTo(ENTRYPOINT_ID);
            soft.assertThat(result.getGateway()).isEqualTo(GATEWAY_ID);
            soft.assertThat(result.getLocalAddress()).isEqualTo(LOCAL_ADDRESS);
            soft.assertThat(result.getRemoteAddress()).isEqualTo("192.168.1.10");
            soft.assertThat(result.getHost()).isEqualTo(NATIVE_HOST);
            soft.assertThat(result.getTimestamp()).isEqualTo(today + "T10:00:00.000Z");
            soft.assertThat(result.getErrorKey()).as("no error fields on a CONNECTED event").isNull();
            soft.assertThat(result.getMessage()).as("no error message on a CONNECTED event").isNull();
            soft
                .assertThat(result.getAdditionalMetrics())
                .containsEntry(NativeApiMetricKeys.CLIENT_ID, "consumer-app-1-A")
                .containsEntry(NativeApiMetricKeys.BROKER_ID, "broker-1")
                .containsEntry(NativeApiMetricKeys.CONNECTION_STATUS, "CONNECTED")
                .doesNotContainKey(NativeApiMetricKeys.CONNECTION_DURATION_MS);
        });
    }

    @Test
    void returns_connection_error_event_with_full_metric_content() throws AnalyticsException {
        var result = findById("conn-error-001");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getApiId()).isEqualTo(API_ID);
            soft.assertThat(result.getRequestId()).isEqualTo("conn-error-001");
            soft.assertThat(result.getTransactionId()).isEqualTo("conn-error-001");
            soft.assertThat(result.getApplicationId()).isEqualTo("kafka-app-001");
            soft.assertThat(result.getPlanId()).isEqualTo("kafka-plan-001");
            soft.assertThat(result.getSubscriptionId()).isEqualTo("kafka-sub-B");
            soft.assertThat(result.getClientIdentifier()).isEqualTo("client-fp-B");
            soft.assertThat(result.getEntrypointId()).isEqualTo(ENTRYPOINT_ID);
            soft.assertThat(result.getGateway()).isEqualTo(GATEWAY_ID);
            soft.assertThat(result.getLocalAddress()).isEqualTo(LOCAL_ADDRESS);
            soft.assertThat(result.getRemoteAddress()).isEqualTo("192.168.1.11");
            soft.assertThat(result.getHost()).isEqualTo(NATIVE_HOST);
            soft.assertThat(result.getTimestamp()).isEqualTo(today + "T10:05:00.000Z");
            soft.assertThat(result.getErrorKey()).isEqualTo("KAFKA_AUTH_FAILED");
            soft.assertThat(result.getMessage()).isEqualTo("SASL authentication failed: Invalid credentials");
            soft
                .assertThat(result.getAdditionalMetrics())
                .containsEntry(NativeApiMetricKeys.CLIENT_ID, "consumer-app-1-B")
                .containsEntry(NativeApiMetricKeys.CONNECTION_STATUS, "CONNECTION_ERROR")
                .doesNotContainKey(NativeApiMetricKeys.BROKER_ID)
                .doesNotContainKey(NativeApiMetricKeys.CONNECTION_DURATION_MS);
        });
    }

    @Test
    void returns_disconnected_event_with_full_metric_content() throws AnalyticsException {
        var result = findById("conn-disconnect-001");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getApiId()).isEqualTo(API_ID);
            soft.assertThat(result.getRequestId()).isEqualTo("conn-disconnect-001");
            soft.assertThat(result.getTransactionId()).isEqualTo("conn-disconnect-001");
            soft.assertThat(result.getApplicationId()).isEqualTo("kafka-app-001");
            soft.assertThat(result.getPlanId()).isEqualTo("kafka-plan-001");
            soft.assertThat(result.getSubscriptionId()).isEqualTo("kafka-sub-A");
            soft.assertThat(result.getClientIdentifier()).isEqualTo("client-fp-A");
            soft.assertThat(result.getEntrypointId()).isEqualTo(ENTRYPOINT_ID);
            soft.assertThat(result.getGateway()).isEqualTo(GATEWAY_ID);
            soft.assertThat(result.getLocalAddress()).isEqualTo(LOCAL_ADDRESS);
            soft.assertThat(result.getRemoteAddress()).isEqualTo("192.168.1.10");
            soft.assertThat(result.getHost()).isEqualTo(NATIVE_HOST);
            soft.assertThat(result.getTimestamp()).isEqualTo(today + "T10:30:00.000Z");
            soft.assertThat(result.getErrorKey()).isEqualTo("KAFKA_CONSUMER_LEFT_GROUP");
            soft
                .assertThat(result.getAdditionalMetrics())
                .containsEntry(NativeApiMetricKeys.CLIENT_ID, "consumer-app-1-A")
                .containsEntry(NativeApiMetricKeys.BROKER_ID, "broker-1")
                .containsEntry(NativeApiMetricKeys.CONNECTION_STATUS, "DISCONNECTED")
                .containsEntry(NativeApiMetricKeys.CONNECTION_DURATION_MS, 1800000);
        });
    }

    private NativeApiMetrics findById(String requestId) throws AnalyticsException {
        return metricsV4Repository.findNativeApiMetrics(queryContext, API_ID, requestId, FROM_MILLIS, TO_MILLIS).orElseThrow();
    }
}
