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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery.Filter;
import io.gravitee.repository.log.v4.model.message.MessageMetrics;
import io.gravitee.repository.log.v4.model.message.MessageMetricsQuery;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class MetricsElasticsearchRepositoryTest extends AbstractElasticsearchRepositoryTest {

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private MetricsElasticsearchRepository metricsV4Repository;

    @Autowired
    private TimeProvider timeProvider;

    private String today;
    private String yesterday;

    @PostConstruct
    public void init() {
        today = timeProvider.getDateToday();
        yesterday = timeProvider.getDateYesterday();
    }

    @Nested
    class SearchMetricssV4 {

        @Test
        void should_return_the_1st_page_of_connection_logs_of_an_api() {
            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder()
                    .filter(Filter.builder().apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9")).build())
                    .size(2)
                    .build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(6);
            assertThat(result.data()).isEqualTo(
                List.of(
                    Metrics.builder()
                        .requestId("e71f2ae0-7673-4d7e-9f2a-e076730d7e69")
                        .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                        .applicationId("613dc986-41ce-4b5b-bdc9-8641cedb5bdb")
                        .planId("733b78f1-1a16-4c16-bb78-f11a16ac1693")
                        .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .transactionId("e71f2ae0-7673-4d7e-9f2a-e076730d7e69")
                        .requestEnded(true)
                        .method(HttpMethod.PUT)
                        .status(404)
                        .timestamp(today + "T06:57:44.893Z")
                        .gateway("2c99d50d-d318-42d3-99d5-0dd31862d3d2")
                        .uri("/jgi-message-logs-kafka/")
                        .gatewayResponseTime(2L)
                        .requestContentLength(0L)
                        .responseContentLength(41L)
                        .build(),
                    Metrics.builder()
                        .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                        .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                        .applicationId("1e478236-e6e4-4cf5-8782-36e6e4ccf57d")
                        .planId("733b78f1-1a16-4c16-bb78-f11a16ac1693")
                        .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .transactionId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                        .requestEnded(true)
                        .method(HttpMethod.GET)
                        .status(404)
                        .timestamp(today + "T06:56:44.552Z")
                        .gateway("2c99d50d-d318-42d3-99d5-0dd31862d3d2")
                        .uri("/jgi-message-logs-kafka/")
                        .gatewayResponseTime(2L)
                        .requestContentLength(0L)
                        .responseContentLength(41L)
                        .build()
                )
            );
        }

        @Test
        void should_return_a_page_of_connection_logs_of_an_api() {
            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder()
                    .page(3)
                    .size(2)
                    .filter(Filter.builder().apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9")).build())
                    .build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(6);
            assertThat(result.data()).isEqualTo(
                List.of(
                    Metrics.builder()
                        .requestId("ebaa9b08-eac8-490d-aa9b-08eac8590d3c")
                        .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                        .applicationId("613dc986-41ce-4b5b-bdc9-8641cedb5bdb")
                        .planId("7733172a-8d19-4c4f-b317-2a8d19ec4f1c")
                        .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .transactionId("ebaa9b08-eac8-490d-aa9b-08eac8590d3c")
                        .requestEnded(true)
                        .method(HttpMethod.POST)
                        .status(202)
                        .timestamp(yesterday + "T14:08:59.901Z")
                        .gateway("a125e26c-b289-4dbf-a5e2-6cb2897dbf20")
                        .uri("/jgi-message-logs-kafka/")
                        .gatewayResponseTime(645L)
                        .requestContentLength(21L)
                        .responseContentLength(0L)
                        .build(),
                    Metrics.builder()
                        .requestId("aed3a207-d5c0-4073-93a2-07d5c0007336")
                        .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                        .applicationId("f37a5799-0490-43f6-ba57-990490f3f678")
                        .planId("7733172a-8d19-4c4f-b317-2a8d19ec4f1c")
                        .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .transactionId("aed3a207-d5c0-4073-93a2-07d5c0007336")
                        .requestEnded(true)
                        .method(HttpMethod.GET)
                        .status(200)
                        .timestamp(yesterday + "T14:08:45.994Z")
                        .gateway("a125e26c-b289-4dbf-a5e2-6cb2897dbf20")
                        .uri("/jgi-message-logs-kafka/")
                        .gatewayResponseTime(29703L)
                        .requestContentLength(0L)
                        .responseContentLength(28L)
                        .build()
                )
            );
        }

        @Test
        void should_return_a_page_of_connection_logs_from_yesterday() {
            var from =
                ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    .minusDays(1)
                    .withHour(0)
                    .withMinute(1)
                    .withSecond(0)
                    .withNano(0)
                    .toEpochSecond() *
                1000;

            var to =
                ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    .minusDays(1)
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(0)
                    .withNano(0)
                    .toEpochSecond() *
                1000;

            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder().page(1).size(10).filter(Filter.builder().from(from).to(to).build()).build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(3);
            assertThat(result.data())
                .extracting(Metrics::getRequestId)
                .containsExactly(
                    "ebaa9b08-eac8-490d-aa9b-08eac8590d3c",
                    "aed3a207-d5c0-4073-93a2-07d5c0007336",
                    "3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41"
                );
        }

        @Test
        void should_return_a_page_of_connection_logs_from_whenever_to_yesterday() {
            var to =
                ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    .minusDays(1)
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(0)
                    .withNano(0)
                    .toEpochSecond() *
                1000;

            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder().page(1).size(10).filter(Filter.builder().to(to).build()).build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(3);
            assertThat(result.data())
                .extracting(Metrics::getRequestId)
                .containsExactly(
                    "ebaa9b08-eac8-490d-aa9b-08eac8590d3c",
                    "aed3a207-d5c0-4073-93a2-07d5c0007336",
                    "3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41"
                );
        }

        @Test
        void should_return_a_page_of_connection_logs_from_today_to_whenever() {
            var from =
                ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).withHour(0).withMinute(1).withSecond(0).withNano(0).toEpochSecond() *
                1000;

            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder().page(1).size(10).filter(Filter.builder().from(from).build()).build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.data())
                .extracting(Metrics::getRequestId)
                .containsExactly(
                    "e71f2ae0-7673-4d7e-9f2a-e076730d7e69",
                    "8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48",
                    "26c61cfc-a4cc-4272-861c-fca4cc2272ab",
                    "5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4"
                );
        }

        @Test
        void should_return_the_applications_logs() {
            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder()
                    .page(1)
                    .size(10)
                    .filter(
                        Filter.builder()
                            .applicationIds(Set.of("f37a5799-0490-43f6-ba57-990490f3f678", "613dc986-41ce-4b5b-bdc9-8641cedb5bdb"))
                            .build()
                    )
                    .build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.data())
                .extracting(Metrics::getRequestId, Metrics::getTimestamp)
                .containsExactly(
                    tuple("e71f2ae0-7673-4d7e-9f2a-e076730d7e69", today + "T06:57:44.893Z"),
                    tuple("ebaa9b08-eac8-490d-aa9b-08eac8590d3c", yesterday + "T14:08:59.901Z"),
                    tuple("aed3a207-d5c0-4073-93a2-07d5c0007336", yesterday + "T14:08:45.994Z"),
                    tuple("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41", yesterday + "T13:51:36.161Z")
                );
        }

        @Test
        void should_return_the_connection_logs_for_plans() {
            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder()
                    .page(1)
                    .size(10)
                    .filter(
                        Filter.builder()
                            .planIds(Set.of("972b5d75-7901-4afd-ab5d-757901eafd0b", "733b78f1-1a16-4c16-bb78-f11a16ac1693"))
                            .build()
                    )
                    .build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.data())
                .extracting(Metrics::getRequestId, Metrics::getTimestamp)
                .containsExactly(
                    tuple("e71f2ae0-7673-4d7e-9f2a-e076730d7e69", today + "T06:57:44.893Z"),
                    tuple("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48", today + "T06:56:44.552Z"),
                    tuple("26c61cfc-a4cc-4272-861c-fca4cc2272ab", today + "T06:55:39.245Z"),
                    tuple("5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4", today + "T06:54:30.047Z")
                );
        }

        @Test
        void should_return_the_connection_logs_for_methods() {
            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder()
                    .page(1)
                    .size(10)
                    .filter(Filter.builder().methods(Set.of(HttpMethod.GET, HttpMethod.POST)).build())
                    .build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(5);
            assertThat(result.data())
                .extracting(Metrics::getRequestId, Metrics::getMethod)
                .containsExactly(
                    tuple("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48", HttpMethod.GET),
                    tuple("26c61cfc-a4cc-4272-861c-fca4cc2272ab", HttpMethod.POST),
                    tuple("ebaa9b08-eac8-490d-aa9b-08eac8590d3c", HttpMethod.POST),
                    tuple("aed3a207-d5c0-4073-93a2-07d5c0007336", HttpMethod.GET),
                    tuple("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41", HttpMethod.GET)
                );
        }

        @Test
        void should_return_the_connection_logs_for_statuses() {
            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder().page(1).size(10).filter(Filter.builder().statuses(Set.of(500, 200)).build()).build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(2);
            assertThat(result.data())
                .extracting(Metrics::getRequestId, Metrics::getStatus)
                .containsExactly(tuple("aed3a207-d5c0-4073-93a2-07d5c0007336", 200), tuple("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41", 500));
        }
    }

    @Nested
    class SearchMetricssV2AndV4 {

        @Test
        void should_return_the_1st_page_of_connection_logs_of_a_v4_api() {
            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder()
                    .filter(Filter.builder().apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9")).build())
                    .size(2)
                    .build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(6);
            assertThat(result.data()).isEqualTo(
                List.of(
                    Metrics.builder()
                        .requestId("e71f2ae0-7673-4d7e-9f2a-e076730d7e69")
                        .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                        .applicationId("613dc986-41ce-4b5b-bdc9-8641cedb5bdb")
                        .planId("733b78f1-1a16-4c16-bb78-f11a16ac1693")
                        .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .transactionId("e71f2ae0-7673-4d7e-9f2a-e076730d7e69")
                        .requestEnded(true)
                        .method(HttpMethod.PUT)
                        .status(404)
                        .timestamp(today + "T06:57:44.893Z")
                        .gateway("2c99d50d-d318-42d3-99d5-0dd31862d3d2")
                        .uri("/jgi-message-logs-kafka/")
                        .gatewayResponseTime(2L)
                        .requestContentLength(0L)
                        .responseContentLength(41L)
                        .build(),
                    Metrics.builder()
                        .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                        .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                        .applicationId("1e478236-e6e4-4cf5-8782-36e6e4ccf57d")
                        .planId("733b78f1-1a16-4c16-bb78-f11a16ac1693")
                        .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .transactionId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                        .requestEnded(true)
                        .method(HttpMethod.GET)
                        .status(404)
                        .timestamp(today + "T06:56:44.552Z")
                        .gateway("2c99d50d-d318-42d3-99d5-0dd31862d3d2")
                        .uri("/jgi-message-logs-kafka/")
                        .gatewayResponseTime(2L)
                        .requestContentLength(0L)
                        .responseContentLength(41L)
                        .build()
                )
            );
        }

        @Test
        void should_return_a_page_of_connection_logs_of_a_v4_api() {
            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder()
                    .page(3)
                    .size(2)
                    .filter(Filter.builder().apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9")).build())
                    .build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(6);
            assertThat(result.data()).isEqualTo(
                List.of(
                    Metrics.builder()
                        .requestId("ebaa9b08-eac8-490d-aa9b-08eac8590d3c")
                        .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                        .applicationId("613dc986-41ce-4b5b-bdc9-8641cedb5bdb")
                        .planId("7733172a-8d19-4c4f-b317-2a8d19ec4f1c")
                        .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .transactionId("ebaa9b08-eac8-490d-aa9b-08eac8590d3c")
                        .requestEnded(true)
                        .method(HttpMethod.POST)
                        .status(202)
                        .timestamp(yesterday + "T14:08:59.901Z")
                        .gateway("a125e26c-b289-4dbf-a5e2-6cb2897dbf20")
                        .uri("/jgi-message-logs-kafka/")
                        .gatewayResponseTime(645L)
                        .requestContentLength(21L)
                        .responseContentLength(0L)
                        .build(),
                    Metrics.builder()
                        .requestId("aed3a207-d5c0-4073-93a2-07d5c0007336")
                        .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                        .applicationId("f37a5799-0490-43f6-ba57-990490f3f678")
                        .planId("7733172a-8d19-4c4f-b317-2a8d19ec4f1c")
                        .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .transactionId("aed3a207-d5c0-4073-93a2-07d5c0007336")
                        .requestEnded(true)
                        .method(HttpMethod.GET)
                        .status(200)
                        .timestamp(yesterday + "T14:08:45.994Z")
                        .gateway("a125e26c-b289-4dbf-a5e2-6cb2897dbf20")
                        .uri("/jgi-message-logs-kafka/")
                        .gatewayResponseTime(29703L)
                        .requestContentLength(0L)
                        .responseContentLength(28L)
                        .build()
                )
            );
        }

        @Test
        void should_return_a_page_of_connection_logs_from_yesterday_from_multi_indexes() {
            var from =
                ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    .minusDays(1)
                    .withHour(0)
                    .withMinute(1)
                    .withSecond(0)
                    .withNano(0)
                    .toEpochSecond() *
                1000;

            var to =
                ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    .minusDays(1)
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(0)
                    .withNano(0)
                    .toEpochSecond() *
                1000;

            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder().page(1).size(20).filter(Filter.builder().from(from).to(to).build()).build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(14);
            assertThat(result.data())
                .extracting(Metrics::getRequestId)
                .contains(
                    // V4
                    "ebaa9b08-eac8-490d-aa9b-08eac8590d3c",
                    "aed3a207-d5c0-4073-93a2-07d5c0007336",
                    "3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41",
                    // V2
                    "AVyN4EtnFQI2bNU8MTYY",
                    "AVyN4SJ6FQI2bNU8MTYv",
                    "AVxa0gYCLKmeaYDnLd34"
                );
        }

        @Test
        void should_return_a_page_of_connection_logs_from_whenever_to_yesterday_from_multi_indexes() {
            var to =
                ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    .minusDays(1)
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(0)
                    .withNano(0)
                    .toEpochSecond() *
                1000;

            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder().page(1).size(20).filter(Filter.builder().to(to).build()).build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(14);
            assertThat(result.data())
                .extracting(Metrics::getRequestId)
                .contains(
                    // V4
                    "ebaa9b08-eac8-490d-aa9b-08eac8590d3c",
                    "aed3a207-d5c0-4073-93a2-07d5c0007336",
                    "3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41",
                    // V2
                    "AVyN4EtnFQI2bNU8MTYY",
                    "AVyN4SJ6FQI2bNU8MTYv",
                    "AVxa0gYCLKmeaYDnLd34"
                );
        }

        @Test
        void should_return_a_page_of_connection_logs_from_today_to_whenever_from_multi_indexes() {
            var from =
                ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).withHour(0).withMinute(1).withSecond(0).withNano(0).toEpochSecond() *
                1000;

            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder().page(1).size(20).filter(Filter.builder().from(from).build()).build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(10);
            assertThat(result.data())
                .extracting(Metrics::getRequestId)
                .contains(
                    // V4
                    "e71f2ae0-7673-4d7e-9f2a-e076730d7e69",
                    "8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48",
                    "26c61cfc-a4cc-4272-861c-fca4cc2272ab",
                    "5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4",
                    // V2
                    "AVsJ2NpUuDfGHrKOwwSX",
                    "AVsGhMNOooztmMPf1gPL",
                    "AVxVhwWQrXoTnnvkvhKV"
                );
        }

        @Test
        void should_return_the_applications_logs() {
            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder()
                    .page(1)
                    .size(10)
                    .filter(
                        Filter.builder()
                            .applicationIds(Set.of("f37a5799-0490-43f6-ba57-990490f3f678", "613dc986-41ce-4b5b-bdc9-8641cedb5bdb"))
                            .build()
                    )
                    .build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.data())
                .extracting(Metrics::getRequestId, Metrics::getTimestamp)
                .containsExactly(
                    tuple("e71f2ae0-7673-4d7e-9f2a-e076730d7e69", today + "T06:57:44.893Z"),
                    tuple("ebaa9b08-eac8-490d-aa9b-08eac8590d3c", yesterday + "T14:08:59.901Z"),
                    tuple("aed3a207-d5c0-4073-93a2-07d5c0007336", yesterday + "T14:08:45.994Z"),
                    tuple("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41", yesterday + "T13:51:36.161Z")
                );
        }

        @Test
        void should_return_the_connection_logs_for_plans() {
            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder()
                    .page(1)
                    .size(10)
                    .filter(
                        Filter.builder()
                            .planIds(Set.of("972b5d75-7901-4afd-ab5d-757901eafd0b", "733b78f1-1a16-4c16-bb78-f11a16ac1693"))
                            .build()
                    )
                    .build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.data())
                .extracting(Metrics::getRequestId, Metrics::getTimestamp)
                .containsExactly(
                    tuple("e71f2ae0-7673-4d7e-9f2a-e076730d7e69", today + "T06:57:44.893Z"),
                    tuple("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48", today + "T06:56:44.552Z"),
                    tuple("26c61cfc-a4cc-4272-861c-fca4cc2272ab", today + "T06:55:39.245Z"),
                    tuple("5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4", today + "T06:54:30.047Z")
                );
        }

        @Test
        void should_return_the_connection_logs_for_methods_from_multi_indexes() {
            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder()
                    .page(1)
                    .size(30)
                    .filter(Filter.builder().methods(Set.of(HttpMethod.GET, HttpMethod.POST)).build())
                    .build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(22);
            assertThat(result.data())
                .extracting(Metrics::getRequestId, Metrics::getMethod)
                .contains(
                    // V4
                    tuple("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48", HttpMethod.GET),
                    tuple("26c61cfc-a4cc-4272-861c-fca4cc2272ab", HttpMethod.POST),
                    tuple("ebaa9b08-eac8-490d-aa9b-08eac8590d3c", HttpMethod.POST),
                    tuple("aed3a207-d5c0-4073-93a2-07d5c0007336", HttpMethod.GET),
                    tuple("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41", HttpMethod.GET),
                    // V2
                    tuple("AVxViNrrrXoTnnvkvhK9", HttpMethod.GET),
                    tuple("AVxa0i0fLKmeaYDnLd3-", HttpMethod.GET),
                    tuple("AVxViO53rXoTnnvkvhLA", HttpMethod.POST)
                );
        }

        @Test
        void should_return_the_connection_logs_for_statuses_across_indexes() {
            var result = metricsV4Repository.searchMetrics(
                queryContext,
                MetricsQuery.builder().page(1).size(20).filter(Filter.builder().statuses(Set.of(500, 200)).build()).build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );

            var apiV2RequestIndexRequestId = "AVsJ2NpUuDfGHrKOwwSX";
            var apiV2RequestIndexStatus = 200;

            var apiV4RequestIndexRequestId = "3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41";
            var apiV4RequestIndexStatus = 500;

            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(15);
            assertThat(result.data())
                .extracting(Metrics::getRequestId, Metrics::getStatus)
                .contains(
                    tuple(apiV2RequestIndexRequestId, apiV2RequestIndexStatus),
                    tuple(apiV4RequestIndexRequestId, apiV4RequestIndexStatus)
                );
        }
    }

    @Nested
    class SearchMessageMetrics {

        @Test
        void should_find_one_hit_with_webhook_and_additional_data() {
            var result = metricsV4Repository.searchMessageMetrics(
                queryContext,
                MessageMetricsQuery.builder()
                    .filter(
                        MessageMetricsQuery.Filter.builder()
                            .operation("subscribe")
                            .connectorType("entrypoint")
                            .connectorId("webhook")
                            .apiId("eec4f752-f4bc-4c63-bc70-9eaaa2be24d5")
                            .requestId("55138ad4-9c01-4e78-b027-b60ad9742441")
                            .build()
                    )
                    .page(1)
                    .size(2)
                    .build()
            );

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result).isNotNull();
                soft.assertThat(result.total()).isEqualTo(1);
                soft.assertThat(result.data()).hasSize(1);
                soft
                    .assertThat(result.data())
                    .first()
                    .isInstanceOfSatisfying(MessageMetrics.class, messageMetrics -> {
                        soft.assertThat(messageMetrics.getTimestamp()).isEqualTo(today + "T06:58:41.222Z");
                        soft.assertThat(messageMetrics.getApiId()).isEqualTo("eec4f752-f4bc-4c63-bc70-9eaaa2be24d5");
                        soft.assertThat(messageMetrics.getRequestId()).isEqualTo("55138ad4-9c01-4e78-b027-b60ad9742441");
                        soft.assertThat(messageMetrics.getConnectorId()).isEqualTo("webhook");
                        soft.assertThat(messageMetrics.getConnectorType()).isEqualTo("entrypoint");
                        soft.assertThat(messageMetrics.getOperation()).isEqualTo("subscribe");
                        soft.assertThat(messageMetrics.getContentLength()).isEqualTo(12L);
                        soft
                            .assertThat(messageMetrics.getClientIdentifier())
                            .isEqualTo("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0");
                        soft.assertThat(messageMetrics.getGateway()).isEqualTo("a125e26c-b289-4dbf-a5e2-6cb2897dbf20");
                        soft.assertThat(messageMetrics.getCount()).isEqualTo(1L);
                        soft.assertThat(messageMetrics.getCountIncrement()).isEqualTo(1L);
                        soft.assertThat(messageMetrics.getErrorCount()).isEqualTo(1L);
                        soft.assertThat(messageMetrics.getErrorCountIncrement()).isEqualTo(1L);
                        soft.assertThat(messageMetrics.getGatewayLatencyMs()).isEqualTo(512L);
                        soft.assertThat(messageMetrics.getCustom()).containsExactly(Map.entry("bespoke", "all mine"));
                        soft
                            .assertThat(messageMetrics.getAdditionalMetrics())
                            .containsAllEntriesOf(
                                Map.of(
                                    "int_test",
                                    42,
                                    "long_test",
                                    100000000000000L,
                                    "double_test",
                                    3.1415,
                                    "bool_test",
                                    true,
                                    "keyword_test",
                                    "foo",
                                    "string_test",
                                    "hello world",
                                    "json_test",
                                    "{\"message\": \"hello\"}"
                                )
                            );
                    });
            });
        }

        @Test
        void should_find_two_hits_with_webhook_and_additional_data() {
            var result = metricsV4Repository.searchMessageMetrics(
                queryContext,
                MessageMetricsQuery.builder()
                    .filter(
                        MessageMetricsQuery.Filter.builder()
                            .operation("subscribe")
                            .connectorType("entrypoint")
                            .connectorId("webhook")
                            .apiId("eec4f752-f4bc-4c63-bc70-9eaaa2be24d5")
                            .build()
                    )
                    .page(1)
                    .size(3)
                    .build()
            );

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result).isNotNull();
                soft.assertThat(result.total()).isEqualTo(2);
                soft.assertThat(result.data()).hasSize(2);
                soft
                    .assertThat(result.data())
                    .extracting(MessageMetrics::getCorrelationId)
                    .containsExactly("55138ad4-9987-4ecd-b2a5-0abe18692111", "8919324a-9c01-4e78-b027-b60ad9742441");
                soft
                    .assertThat(result.data())
                    .extracting(MessageMetrics::getRequestId)
                    .containsExactly("8919324a-9987-4ecd-b2a5-0abe18692111", "55138ad4-9c01-4e78-b027-b60ad9742441");
            });
        }

        @Test
        void should_find_one_hits_in_time_range() {
            var result = metricsV4Repository.searchMessageMetrics(
                queryContext,
                MessageMetricsQuery.builder()
                    .filter(
                        MessageMetricsQuery.Filter.builder()
                            .apiId("eec4f752-f4bc-4c63-bc70-9eaaa2be24d5")
                            .from(OffsetDateTime.parse(today + "T06:58:00.000Z").toInstant().toEpochMilli())
                            .to(OffsetDateTime.parse(today + "T06:59:00.000Z").toInstant().toEpochMilli())
                            .build()
                    )
                    .page(1)
                    .size(2)
                    .build()
            );

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result).isNotNull();
                soft.assertThat(result.data()).hasSize(1);
                soft
                    .assertThat(result.data())
                    .extracting(MessageMetrics::getCorrelationId)
                    .containsExactly("8919324a-9c01-4e78-b027-b60ad9742441");
                soft
                    .assertThat(result.data())
                    .extracting(MessageMetrics::getRequestId)
                    .containsExactly("55138ad4-9c01-4e78-b027-b60ad9742441");
            });
        }

        @Test
        void should_find_all_entrypoint() {
            var result = metricsV4Repository.searchMessageMetrics(
                queryContext,
                MessageMetricsQuery.builder()
                    .filter(MessageMetricsQuery.Filter.builder().connectorType("entrypoint").build())
                    .page(1)
                    .size(10)
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(6);
            assertThat(result.data()).hasSize(6);
        }

        @Test
        void should_find_all_endpoint_subscribe() {
            var result = metricsV4Repository.searchMessageMetrics(
                queryContext,
                MessageMetricsQuery.builder()
                    .filter(MessageMetricsQuery.Filter.builder().connectorType("endpoint").operation("subscribe").build())
                    .page(1)
                    .size(10)
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(3);
            assertThat(result.data()).hasSize(3);
        }

        @Test
        void should_find_all() {
            var result = metricsV4Repository.searchMessageMetrics(
                queryContext,
                MessageMetricsQuery.builder().filter(MessageMetricsQuery.Filter.builder().build()).page(1).size(20).build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(10);
            assertThat(result.data()).hasSize(10);
        }

        @Test
        void should_find_none() {
            var result = metricsV4Repository.searchMessageMetrics(
                queryContext,
                MessageMetricsQuery.builder()
                    .filter(
                        MessageMetricsQuery.Filter.builder()
                            .operation("subscribe")
                            .connectorType("entrypoint")
                            .connectorId("webhook")
                            .apiId("foo")
                            .build()
                    )
                    .page(1)
                    .size(10)
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isZero();
            assertThat(result.data()).isEmpty();
        }
    }
}
