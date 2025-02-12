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

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.gravitee.repository.log.v4.model.connection.ConnectionLog;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetailQuery;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogQuery;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogQuery.Filter;
import io.gravitee.repository.log.v4.model.message.AggregatedMessageLog;
import io.gravitee.repository.log.v4.model.message.MessageLogQuery;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LogElasticsearchRepositoryTest extends AbstractElasticsearchRepositoryTest {

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    @Autowired
    private LogElasticsearchRepository logV4Repository;

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
    class SearchConnectionLogsV4 {

        @Test
        void should_return_the_1st_page_of_connection_logs_of_an_api() {
            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery
                    .builder()
                    .filter(Filter.builder().apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9")).build())
                    .size(2)
                    .build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(6);
            assertThat(result.data())
                .isEqualTo(
                    List.of(
                        ConnectionLog
                            .builder()
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
                            .build(),
                        ConnectionLog
                            .builder()
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
                            .build()
                    )
                );
        }

        @Test
        void should_return_a_page_of_connection_logs_of_an_api() {
            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery
                    .builder()
                    .page(3)
                    .size(2)
                    .filter(Filter.builder().apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9")).build())
                    .build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(6);
            assertThat(result.data())
                .isEqualTo(
                    List.of(
                        ConnectionLog
                            .builder()
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
                            .build(),
                        ConnectionLog
                            .builder()
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
                            .build()
                    )
                );
        }

        @Test
        void should_return_a_page_of_connection_logs_from_yesterday() {
            var from =
                ZonedDateTime
                    .ofInstant(Instant.now(), ZoneOffset.UTC)
                    .minusDays(1)
                    .withHour(0)
                    .withMinute(1)
                    .withSecond(0)
                    .withNano(0)
                    .toEpochSecond() *
                1000;

            var to =
                ZonedDateTime
                    .ofInstant(Instant.now(), ZoneOffset.UTC)
                    .minusDays(1)
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(0)
                    .withNano(0)
                    .toEpochSecond() *
                1000;

            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery.builder().page(1).size(10).filter(Filter.builder().from(from).to(to).build()).build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(3);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId)
                .containsExactly(
                    "ebaa9b08-eac8-490d-aa9b-08eac8590d3c",
                    "aed3a207-d5c0-4073-93a2-07d5c0007336",
                    "3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41"
                );
        }

        @Test
        void should_return_a_page_of_connection_logs_from_whenever_to_yesterday() {
            var to =
                ZonedDateTime
                    .ofInstant(Instant.now(), ZoneOffset.UTC)
                    .minusDays(1)
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(0)
                    .withNano(0)
                    .toEpochSecond() *
                1000;

            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery.builder().page(1).size(10).filter(Filter.builder().to(to).build()).build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(3);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId)
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

            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery.builder().page(1).size(10).filter(Filter.builder().from(from).build()).build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId)
                .containsExactly(
                    "e71f2ae0-7673-4d7e-9f2a-e076730d7e69",
                    "8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48",
                    "26c61cfc-a4cc-4272-861c-fca4cc2272ab",
                    "5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4"
                );
        }

        @Test
        void should_return_the_applications_logs() {
            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery
                    .builder()
                    .page(1)
                    .size(10)
                    .filter(
                        Filter
                            .builder()
                            .applicationIds(Set.of("f37a5799-0490-43f6-ba57-990490f3f678", "613dc986-41ce-4b5b-bdc9-8641cedb5bdb"))
                            .build()
                    )
                    .build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getTimestamp)
                .containsExactly(
                    tuple("e71f2ae0-7673-4d7e-9f2a-e076730d7e69", today + "T06:57:44.893Z"),
                    tuple("ebaa9b08-eac8-490d-aa9b-08eac8590d3c", yesterday + "T14:08:59.901Z"),
                    tuple("aed3a207-d5c0-4073-93a2-07d5c0007336", yesterday + "T14:08:45.994Z"),
                    tuple("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41", yesterday + "T13:51:36.161Z")
                );
        }

        @Test
        void should_return_the_connection_logs_for_plans() {
            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery
                    .builder()
                    .page(1)
                    .size(10)
                    .filter(
                        Filter
                            .builder()
                            .planIds(Set.of("972b5d75-7901-4afd-ab5d-757901eafd0b", "733b78f1-1a16-4c16-bb78-f11a16ac1693"))
                            .build()
                    )
                    .build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getTimestamp)
                .containsExactly(
                    tuple("e71f2ae0-7673-4d7e-9f2a-e076730d7e69", today + "T06:57:44.893Z"),
                    tuple("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48", today + "T06:56:44.552Z"),
                    tuple("26c61cfc-a4cc-4272-861c-fca4cc2272ab", today + "T06:55:39.245Z"),
                    tuple("5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4", today + "T06:54:30.047Z")
                );
        }

        @Test
        void should_return_the_connection_logs_for_methods() {
            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery
                    .builder()
                    .page(1)
                    .size(10)
                    .filter(Filter.builder().methods(Set.of(HttpMethod.GET, HttpMethod.POST)).build())
                    .build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(5);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getMethod)
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
            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery.builder().page(1).size(10).filter(Filter.builder().statuses(Set.of(500, 200)).build()).build(),
                List.of(DefinitionVersion.V4)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(2);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getStatus)
                .containsExactly(tuple("aed3a207-d5c0-4073-93a2-07d5c0007336", 200), tuple("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41", 500));
        }
    }

    @Nested
    class SearchConnectionLogsV2AndV4 {

        @Test
        void should_return_the_1st_page_of_connection_logs_of_a_v4_api() {
            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery
                    .builder()
                    .filter(Filter.builder().apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9")).build())
                    .size(2)
                    .build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(6);
            assertThat(result.data())
                .isEqualTo(
                    List.of(
                        ConnectionLog
                            .builder()
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
                            .build(),
                        ConnectionLog
                            .builder()
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
                            .build()
                    )
                );
        }

        @Test
        void should_return_a_page_of_connection_logs_of_a_v4_api() {
            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery
                    .builder()
                    .page(3)
                    .size(2)
                    .filter(Filter.builder().apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9")).build())
                    .build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(6);
            assertThat(result.data())
                .isEqualTo(
                    List.of(
                        ConnectionLog
                            .builder()
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
                            .build(),
                        ConnectionLog
                            .builder()
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
                            .build()
                    )
                );
        }

        @Test
        void should_return_a_page_of_connection_logs_from_yesterday_from_multi_indexes() {
            var from =
                ZonedDateTime
                    .ofInstant(Instant.now(), ZoneOffset.UTC)
                    .minusDays(1)
                    .withHour(0)
                    .withMinute(1)
                    .withSecond(0)
                    .withNano(0)
                    .toEpochSecond() *
                1000;

            var to =
                ZonedDateTime
                    .ofInstant(Instant.now(), ZoneOffset.UTC)
                    .minusDays(1)
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(0)
                    .withNano(0)
                    .toEpochSecond() *
                1000;

            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery.builder().page(1).size(20).filter(Filter.builder().from(from).to(to).build()).build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(14);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId)
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
                ZonedDateTime
                    .ofInstant(Instant.now(), ZoneOffset.UTC)
                    .minusDays(1)
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(0)
                    .withNano(0)
                    .toEpochSecond() *
                1000;

            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery.builder().page(1).size(20).filter(Filter.builder().to(to).build()).build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(14);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId)
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

            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery.builder().page(1).size(20).filter(Filter.builder().from(from).build()).build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(10);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId)
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
            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery
                    .builder()
                    .page(1)
                    .size(10)
                    .filter(
                        Filter
                            .builder()
                            .applicationIds(Set.of("f37a5799-0490-43f6-ba57-990490f3f678", "613dc986-41ce-4b5b-bdc9-8641cedb5bdb"))
                            .build()
                    )
                    .build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getTimestamp)
                .containsExactly(
                    tuple("e71f2ae0-7673-4d7e-9f2a-e076730d7e69", today + "T06:57:44.893Z"),
                    tuple("ebaa9b08-eac8-490d-aa9b-08eac8590d3c", yesterday + "T14:08:59.901Z"),
                    tuple("aed3a207-d5c0-4073-93a2-07d5c0007336", yesterday + "T14:08:45.994Z"),
                    tuple("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41", yesterday + "T13:51:36.161Z")
                );
        }

        @Test
        void should_return_the_connection_logs_for_plans() {
            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery
                    .builder()
                    .page(1)
                    .size(10)
                    .filter(
                        Filter
                            .builder()
                            .planIds(Set.of("972b5d75-7901-4afd-ab5d-757901eafd0b", "733b78f1-1a16-4c16-bb78-f11a16ac1693"))
                            .build()
                    )
                    .build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getTimestamp)
                .containsExactly(
                    tuple("e71f2ae0-7673-4d7e-9f2a-e076730d7e69", today + "T06:57:44.893Z"),
                    tuple("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48", today + "T06:56:44.552Z"),
                    tuple("26c61cfc-a4cc-4272-861c-fca4cc2272ab", today + "T06:55:39.245Z"),
                    tuple("5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4", today + "T06:54:30.047Z")
                );
        }

        @Test
        void should_return_the_connection_logs_for_methods_from_multi_indexes() {
            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery
                    .builder()
                    .page(1)
                    .size(30)
                    .filter(Filter.builder().methods(Set.of(HttpMethod.GET, HttpMethod.POST)).build())
                    .build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(22);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getMethod)
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
            var result = logV4Repository.searchConnectionLogs(
                queryContext,
                ConnectionLogQuery.builder().page(1).size(20).filter(Filter.builder().statuses(Set.of(500, 200)).build()).build(),
                List.of(DefinitionVersion.V4, DefinitionVersion.V2)
            );

            var apiV2RequestIndexRequestId = "AVsJ2NpUuDfGHrKOwwSX";
            var apiV2RequestIndexStatus = 200;

            var apiV4RequestIndexRequestId = "3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41";
            var apiV4RequestIndexStatus = 500;

            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(15);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getStatus)
                .contains(
                    tuple(apiV2RequestIndexRequestId, apiV2RequestIndexStatus),
                    tuple(apiV4RequestIndexRequestId, apiV4RequestIndexStatus)
                );
        }
    }

    @Nested
    class SearchConnectionLogDetail {

        @Test
        void should_return_empty_result() {
            var result = logV4Repository.searchConnectionLogDetail(
                queryContext,
                ConnectionLogDetailQuery.builder().filter(ConnectionLogDetailQuery.Filter.builder().apiId("notExisting").build()).build()
            );
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_result() {
            var result = logV4Repository.searchConnectionLogDetail(
                queryContext,
                ConnectionLogDetailQuery
                    .builder()
                    .filter(
                        ConnectionLogDetailQuery.Filter
                            .builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("26c61cfc-a4cc-4272-861c-fca4cc2272ab")
                            .build()
                    )
                    .build()
            );
            assertThat(result)
                .hasValueSatisfying(connectionLogDetail -> {
                    assertThat(connectionLogDetail)
                        .hasFieldOrPropertyWithValue("apiId", "f1608475-dd77-4603-a084-75dd775603e9")
                        .hasFieldOrPropertyWithValue("requestId", "26c61cfc-a4cc-4272-861c-fca4cc2272ab")
                        .hasFieldOrPropertyWithValue("timestamp", today + "T06:55:39.245Z")
                        .hasFieldOrPropertyWithValue("clientIdentifier", "12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .hasFieldOrPropertyWithValue("requestEnded", true);
                    assertThat(connectionLogDetail.getEntrypointRequest())
                        .hasFieldOrPropertyWithValue("method", "POST")
                        .hasFieldOrPropertyWithValue("uri", "/jgi-message-logs-kafka/")
                        .extracting(ConnectionLogDetail.Request::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                        .containsEntry("Accept", List.of("application/json, */*;q=0.5"))
                        .containsEntry("X-Request-ID", List.of("41b21c20b0ffba8f15871e5be2913766"));
                    assertThat(connectionLogDetail.getEndpointRequest())
                        .hasFieldOrPropertyWithValue("method", "POST")
                        .hasFieldOrPropertyWithValue("uri", "")
                        .extracting(ConnectionLogDetail.Request::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                        .containsEntry("Accept", List.of("application/json, */*;q=0.5"))
                        .containsEntry("X-Request-ID", List.of("41b21c20b0ffba8f15871e5be2913766"));
                    assertThat(connectionLogDetail.getEntrypointResponse())
                        .hasFieldOrPropertyWithValue("status", 200)
                        .extracting(ConnectionLogDetail.Response::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                        .containsAllEntriesOf(
                            Map.of(
                                "X-Gravitee-Client-Identifier",
                                List.of("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0"),
                                "X-Gravitee-Transaction-Id",
                                List.of("26c61cfc-a4cc-4272-861c-fca4cc2272ab"),
                                "X-Gravitee-Request-Id",
                                List.of("26c61cfc-a4cc-4272-861c-fca4cc2272ab")
                            )
                        );
                    assertThat(connectionLogDetail.getEndpointResponse())
                        .hasFieldOrPropertyWithValue("status", 200)
                        .extracting(ConnectionLogDetail.Response::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                        .isEmpty();
                });
        }
    }

    @Nested
    class SearchAggregateMessageLog {

        @Test
        void should_return_aggregated_message_log_with_only_entrypoint() {
            var result = logV4Repository.searchAggregatedMessageLog(
                queryContext,
                MessageLogQuery
                    .builder()
                    .filter(
                        MessageLogQuery.Filter
                            .builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("bf98c96b-fb84-4e25-98c9-6bfb84fe257a")
                            .build()
                    )
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isOne();
            assertThat(result.data())
                .isEqualTo(
                    List.of(
                        AggregatedMessageLog
                            .builder()
                            .requestId("bf98c96b-fb84-4e25-98c9-6bfb84fe257a")
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                            .correlationId("aed3a207-d5c0-4073-93a2-07d5c0007336")
                            .operation("subscribe")
                            .timestamp(yesterday + "T14:08:45.994Z")
                            .entrypoint(
                                AggregatedMessageLog.Message
                                    .builder()
                                    .id("0")
                                    .timestamp(yesterday + "T14:08:45.994Z")
                                    .connectorId("http-get")
                                    .payload("message")
                                    .headers(Map.of("X-Header", List.of("http-get-header")))
                                    .metadata(Map.of("MessageMetadata", "http-get-metadata"))
                                    .build()
                            )
                            .build()
                    )
                );
        }

        @Test
        void should_return_aggregated_message_log_with_only_endpoint() {
            var result = logV4Repository.searchAggregatedMessageLog(
                queryContext,
                MessageLogQuery
                    .builder()
                    .filter(
                        MessageLogQuery.Filter
                            .builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("96b7d777-36f7-49c1-a5ad-993ad2ae64cb")
                            .build()
                    )
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isOne();
            assertThat(result.data())
                .isEqualTo(
                    List.of(
                        AggregatedMessageLog
                            .builder()
                            .requestId("96b7d777-36f7-49c1-a5ad-993ad2ae64cb")
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                            .correlationId("ebaa9b08-eac8-490d-aa9b-08eac8590d3c")
                            .operation("subscribe")
                            .timestamp(yesterday + "T14:08:59.901Z")
                            .endpoint(
                                AggregatedMessageLog.Message
                                    .builder()
                                    .id("0")
                                    .timestamp(yesterday + "T14:08:59.901Z")
                                    .connectorId("kafka")
                                    .payload("message")
                                    .headers(Map.of("X-Header", List.of("kafka-header")))
                                    .metadata(Map.of("MessageMetadata", "kafka-metadata"))
                                    .build()
                            )
                            .build()
                    )
                );
        }

        @Test
        void should_return_aggregated_message_log_with_entrypoint_and_endpoint_for_publish_operation() {
            var result = logV4Repository.searchAggregatedMessageLog(
                queryContext,
                MessageLogQuery
                    .builder()
                    .filter(
                        MessageLogQuery.Filter
                            .builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41")
                            .build()
                    )
                    .size(1)
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isOne();
            assertThat(result.data())
                .isEqualTo(
                    List.of(
                        AggregatedMessageLog
                            .builder()
                            .requestId("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41")
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                            .correlationId("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41")
                            .operation("publish")
                            .timestamp(yesterday + "T13:51:36.161Z")
                            .entrypoint(
                                AggregatedMessageLog.Message
                                    .builder()
                                    .id("0")
                                    .timestamp(yesterday + "T13:51:36.161Z")
                                    .connectorId("http-post")
                                    .payload("message")
                                    .headers(Map.of("X-Header", List.of("http-post-header")))
                                    .metadata(Map.of("MessageMetadata", "http-post-metadata"))
                                    .build()
                            )
                            .endpoint(
                                AggregatedMessageLog.Message
                                    .builder()
                                    .id("0")
                                    .timestamp(yesterday + "T13:51:37.161Z")
                                    .connectorId("kafka")
                                    .payload("message")
                                    .headers(Map.of("X-Header", List.of("kafka-header")))
                                    .metadata(Map.of("MessageMetadata", "kafka-metadata"))
                                    .build()
                            )
                            .build()
                    )
                );
        }

        @Test
        void should_return_aggregated_message_log_with_entrypoint_and_endpoint_for_subscribe_operation() {
            var result = logV4Repository.searchAggregatedMessageLog(
                queryContext,
                MessageLogQuery
                    .builder()
                    .filter(
                        MessageLogQuery.Filter
                            .builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4")
                            .build()
                    )
                    .size(1)
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isOne();
            assertThat(result.data())
                .isEqualTo(
                    List.of(
                        AggregatedMessageLog
                            .builder()
                            .requestId("5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4")
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                            .correlationId("5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4")
                            .operation("subscribe")
                            .timestamp(yesterday + "T06:54:30.047Z")
                            .entrypoint(
                                AggregatedMessageLog.Message
                                    .builder()
                                    .id("0")
                                    .timestamp(yesterday + "T06:55:39.245Z")
                                    .connectorId("http-get")
                                    .payload("message")
                                    .headers(Map.of("X-Header", List.of("http-get-header")))
                                    .metadata(Map.of("MessageMetadata", "http-get-metadata"))
                                    .build()
                            )
                            .endpoint(
                                AggregatedMessageLog.Message
                                    .builder()
                                    .id("0")
                                    .timestamp(yesterday + "T06:54:30.047Z")
                                    .connectorId("kafka")
                                    .payload("message")
                                    .headers(Map.of("X-Header", List.of("kafka-header")))
                                    .metadata(Map.of("MessageMetadata", "kafka-metadata"))
                                    .build()
                            )
                            .build()
                    )
                );
        }

        @Test
        void should_return_the_1st_page_of_aggregated_message_logs_of_an_api_and_request_id() {
            var result = logV4Repository.searchAggregatedMessageLog(
                queryContext,
                MessageLogQuery
                    .builder()
                    .filter(
                        MessageLogQuery.Filter
                            .builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("d789ffdc-d092-4675-97a9-213cf569350d")
                            .build()
                    )
                    .size(1)
                    .build()
            );

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result).isNotNull();
                soft.assertThat(result.total()).isEqualTo(2);
                soft
                    .assertThat(result.data())
                    .extracting(AggregatedMessageLog::getRequestId, AggregatedMessageLog::getCorrelationId)
                    .containsExactly(tuple("d789ffdc-d092-4675-97a9-213cf569350d", "8d6d8bd5-bc42-4aea-ad8b-d5bc421aea49"));
            });
        }

        @Test
        void should_return_the_2nd_page_of_aggregated_message_logs_of_an_api_and_request_id() {
            var result = logV4Repository.searchAggregatedMessageLog(
                queryContext,
                MessageLogQuery
                    .builder()
                    .filter(
                        MessageLogQuery.Filter
                            .builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("d789ffdc-d092-4675-97a9-213cf569350d")
                            .build()
                    )
                    .size(1)
                    .page(2)
                    .build()
            );

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result).isNotNull();
                soft.assertThat(result.total()).isEqualTo(2);
                soft
                    .assertThat(result.data())
                    .extracting(AggregatedMessageLog::getRequestId, AggregatedMessageLog::getCorrelationId)
                    .containsExactly(tuple("d789ffdc-d092-4675-97a9-213cf569350d", "8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48"));
            });
        }
    }
}
