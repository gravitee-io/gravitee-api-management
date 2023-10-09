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
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.log.v4.model.connection.ConnectionLog;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogQuery;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogQuery.Filter;
import io.gravitee.repository.log.v4.model.message.MessageLog;
import io.gravitee.repository.log.v4.model.message.MessageLogQuery;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LogElasticsearchRepositoryTest extends AbstractElasticsearchRepositoryTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    @Autowired
    private LogElasticsearchRepository logV4Repository;

    @Nested
    class SearchConnectionLog {

        @Test
        void should_return_the_1st_page_of_connection_logs_of_an_api() {
            var today = DATE_FORMATTER.format(Instant.now());

            var result = logV4Repository.searchConnectionLog(
                ConnectionLogQuery.builder().filter(Filter.builder().apiId("f1608475-dd77-4603-a084-75dd775603e9").build()).size(2).build()
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
                            .build()
                    )
                );
        }

        @Test
        void should_return_a_page_of_connection_logs_of_an_api() {
            var yesterday = DATE_FORMATTER.format(Instant.now().minus(1, ChronoUnit.DAYS));

            var result = logV4Repository.searchConnectionLog(
                ConnectionLogQuery
                    .builder()
                    .page(3)
                    .size(2)
                    .filter(Filter.builder().apiId("f1608475-dd77-4603-a084-75dd775603e9").build())
                    .build()
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

            var result = logV4Repository.searchConnectionLog(
                ConnectionLogQuery.builder().page(1).size(10).filter(Filter.builder().from(from).to(to).build()).build()
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

            var result = logV4Repository.searchConnectionLog(
                ConnectionLogQuery.builder().page(1).size(10).filter(Filter.builder().to(to).build()).build()
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

            var result = logV4Repository.searchConnectionLog(
                ConnectionLogQuery.builder().page(1).size(10).filter(Filter.builder().from(from).build()).build()
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
            var today = DATE_FORMATTER.format(Instant.now());
            var yesterday = DATE_FORMATTER.format(Instant.now().minus(1, ChronoUnit.DAYS));

            var result = logV4Repository.searchConnectionLog(
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
                    .build()
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
            var today = DATE_FORMATTER.format(Instant.now());
            var yesterday = DATE_FORMATTER.format(Instant.now().minus(1, ChronoUnit.DAYS));

            var result = logV4Repository.searchConnectionLog(
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
                    .build()
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
    }

    @Nested
    class SearchMessageLog {

        @Test
        void should_return_the_1st_page_of_message_logs_of_an_api_and_request_id() {
            var today = DATE_FORMATTER.format(Instant.now());

            var result = logV4Repository.searchMessageLog(
                MessageLogQuery
                    .builder()
                    .filter(
                        MessageLogQuery.Filter
                            .builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .build()
                    )
                    .size(2)
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(2);
            assertThat(result.data())
                .isEqualTo(
                    List.of(
                        MessageLog
                            .builder()
                            .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                            .correlationId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .operation("subscribe")
                            .connectorType("endpoint")
                            .connectorId("kafka")
                            .timestamp(today + "T06:57:44.893Z")
                            .message(
                                MessageLog.Message
                                    .builder()
                                    .id("0")
                                    .payload("message")
                                    .headers(Map.of("X-Header", List.of("kafka-header")))
                                    .metadata(Map.of("MessageMetadata", "kafka-metadata"))
                                    .build()
                            )
                            .build(),
                        MessageLog
                            .builder()
                            .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                            .correlationId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .operation("subscribe")
                            .connectorType("entrypoint")
                            .connectorId("http-get")
                            .timestamp(today + "T06:56:44.552Z")
                            .message(
                                MessageLog.Message
                                    .builder()
                                    .id("0")
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
        void should_return_the_1st_page_of_message_logs_for_a_request_id() {
            var today = DATE_FORMATTER.format(Instant.now());

            var result = logV4Repository.searchMessageLog(
                MessageLogQuery
                    .builder()
                    .filter(MessageLogQuery.Filter.builder().requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48").build())
                    .size(2)
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(2);
            assertThat(result.data())
                .isEqualTo(
                    List.of(
                        MessageLog
                            .builder()
                            .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                            .correlationId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .operation("subscribe")
                            .connectorType("endpoint")
                            .connectorId("kafka")
                            .timestamp(today + "T06:57:44.893Z")
                            .message(
                                MessageLog.Message
                                    .builder()
                                    .id("0")
                                    .payload("message")
                                    .headers(Map.of("X-Header", List.of("kafka-header")))
                                    .metadata(Map.of("MessageMetadata", "kafka-metadata"))
                                    .build()
                            )
                            .build(),
                        MessageLog
                            .builder()
                            .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                            .correlationId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .operation("subscribe")
                            .connectorType("entrypoint")
                            .connectorId("http-get")
                            .timestamp(today + "T06:56:44.552Z")
                            .message(
                                MessageLog.Message
                                    .builder()
                                    .id("0")
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
        void should_return_the_1st_page_of_message_logs_of_an_api() {
            var today = DATE_FORMATTER.format(Instant.now());

            var result = logV4Repository.searchMessageLog(
                MessageLogQuery
                    .builder()
                    .filter(MessageLogQuery.Filter.builder().apiId("f1608475-dd77-4603-a084-75dd775603e9").build())
                    .size(2)
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(6);
            assertThat(result.data())
                .isEqualTo(
                    List.of(
                        MessageLog
                            .builder()
                            .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                            .correlationId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .operation("subscribe")
                            .connectorType("endpoint")
                            .connectorId("kafka")
                            .timestamp(today + "T06:57:44.893Z")
                            .message(
                                MessageLog.Message
                                    .builder()
                                    .id("0")
                                    .payload("message")
                                    .headers(Map.of("X-Header", List.of("kafka-header")))
                                    .metadata(Map.of("MessageMetadata", "kafka-metadata"))
                                    .build()
                            )
                            .build(),
                        MessageLog
                            .builder()
                            .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                            .correlationId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                            .operation("subscribe")
                            .connectorType("entrypoint")
                            .connectorId("http-get")
                            .timestamp(today + "T06:56:44.552Z")
                            .message(
                                MessageLog.Message
                                    .builder()
                                    .id("0")
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
        void should_return_a_page_of_connection_logs_of_an_api() {
            var yesterday = DATE_FORMATTER.format(Instant.now().minus(1, ChronoUnit.DAYS));

            var result = logV4Repository.searchMessageLog(
                MessageLogQuery
                    .builder()
                    .page(3)
                    .size(2)
                    .filter(MessageLogQuery.Filter.builder().apiId("f1608475-dd77-4603-a084-75dd775603e9").build())
                    .size(2)
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(6);
            assertThat(result.data())
                .isEqualTo(
                    List.of(
                        MessageLog
                            .builder()
                            .requestId("bf98c96b-fb84-4e25-98c9-6bfb84fe257a")
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                            .correlationId("ebaa9b08-eac8-490d-aa9b-08eac8590d3c")
                            .operation("subscribe")
                            .connectorType("endpoint")
                            .connectorId("kafka")
                            .timestamp(yesterday + "T14:08:59.901Z")
                            .message(
                                MessageLog.Message
                                    .builder()
                                    .id("0")
                                    .payload("message")
                                    .headers(Map.of("X-Header", List.of("kafka-header")))
                                    .metadata(Map.of("MessageMetadata", "kafka-metadata"))
                                    .build()
                            )
                            .build(),
                        MessageLog
                            .builder()
                            .requestId("bf98c96b-fb84-4e25-98c9-6bfb84fe257a")
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                            .correlationId("aed3a207-d5c0-4073-93a2-07d5c0007336")
                            .operation("subscribe")
                            .connectorType("entrypoint")
                            .connectorId("http-get")
                            .timestamp(yesterday + "T14:08:45.994Z")
                            .message(
                                MessageLog.Message
                                    .builder()
                                    .id("0")
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
        void should_return_the_1st_page_of_connection_logs_without_filter() {
            var today = DATE_FORMATTER.format(Instant.now());

            var result = logV4Repository.searchMessageLog(MessageLogQuery.builder().size(2).build());
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(8);
            assertThat(result.data())
                .extracting(MessageLog::getRequestId, MessageLog::getTimestamp)
                .containsExactly(
                    tuple("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48", today + "T06:57:44.893Z"),
                    tuple("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48", today + "T06:56:44.552Z")
                );
        }
    }
}
