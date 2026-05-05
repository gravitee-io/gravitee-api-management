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
import static org.assertj.core.api.Assertions.tuple;

import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetrics;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricsQuery;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link MetricsElasticsearchRepository#searchNativeApiMetrics} against a real testcontainers ES,
 * seeded by {@code freemarker-v4-analytics/v4-metrics.ftl}. Same fixture window as
 * {@link MetricsElasticsearchRepositoryTest_FindNativeApiMetrics}; tests focus on filter discrimination, pagination, and sort order.
 */
public class MetricsElasticsearchRepositoryTest_SearchNativeApiMetrics extends AbstractElasticsearchRepositoryTest {

    private static final long FROM_MILLIS =
        ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).withHour(9).withMinute(0).withSecond(0).withNano(0).toEpochSecond() * 1000;
    private static final long TO_MILLIS =
        ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).withHour(11).withMinute(0).withSecond(0).withNano(0).toEpochSecond() * 1000;

    private static final String API_ID = "kafka-api-001";

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
    void searchNativeApiMetrics_return_filtered_metrics_by_time_range() throws AnalyticsException {
        var result = metricsV4Repository.searchNativeApiMetrics(
            queryContext,
            NativeApiMetricsQuery.builder().apiId(API_ID).from(FROM_MILLIS).to(TO_MILLIS).page(1).size(10).build()
        );

        assertThat(result.total()).isEqualTo(4);
        assertThat(result.data())
            .extracting(NativeApiMetrics::getRequestId, NativeApiMetrics::getTimestamp)
            .containsExactly(
                tuple("conn-disconnect-001", today + "T10:30:00.000Z"),
                tuple("conn-error-001", today + "T10:05:00.000Z"),
                tuple("conn-connected-002", today + "T10:01:00.000Z"),
                tuple("conn-connected-001", today + "T10:00:00.000Z")
            );
    }

    @Test
    void searchNativeApiMetrics_return_filtered_metrics_by_application_ids() throws AnalyticsException {
        var result = metricsV4Repository.searchNativeApiMetrics(
            queryContext,
            NativeApiMetricsQuery.builder().apiId(API_ID).from(FROM_MILLIS).to(TO_MILLIS).applicationIds(Set.of("kafka-app-002")).build()
        );

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.data()).extracting(NativeApiMetrics::getRequestId).containsExactly("conn-connected-002");
    }

    @Test
    void searchNativeApiMetrics_return_filtered_metrics_by_plan_ids() throws AnalyticsException {
        var result = metricsV4Repository.searchNativeApiMetrics(
            queryContext,
            NativeApiMetricsQuery.builder().apiId(API_ID).from(FROM_MILLIS).to(TO_MILLIS).planIds(Set.of("kafka-plan-002")).build()
        );

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.data()).extracting(NativeApiMetrics::getRequestId).containsExactly("conn-connected-002");
    }

    @Test
    void searchNativeApiMetrics_return_filtered_metrics_by_connection_status() throws AnalyticsException {
        var result = metricsV4Repository.searchNativeApiMetrics(
            queryContext,
            NativeApiMetricsQuery.builder()
                .apiId(API_ID)
                .from(FROM_MILLIS)
                .to(TO_MILLIS)
                .connectionStatuses(Set.of("CONNECTION_ERROR", "DISCONNECTED"))
                .build()
        );

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.data())
            .extracting(NativeApiMetrics::getRequestId)
            .containsExactlyInAnyOrder("conn-error-001", "conn-disconnect-001");
    }
}
