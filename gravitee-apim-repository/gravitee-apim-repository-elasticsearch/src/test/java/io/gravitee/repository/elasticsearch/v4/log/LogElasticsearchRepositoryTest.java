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
import io.gravitee.repository.log.v4.model.ConnectionLog;
import io.gravitee.repository.log.v4.model.ConnectionLogQuery;
import io.gravitee.repository.log.v4.model.ConnectionLogQuery.Filter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
                ConnectionLogQuery.builder().filter(Filter.builder().appId("f1608475-dd77-4603-a084-75dd775603e9").build()).size(2).build()
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
                            .applicationId("1")
                            .planId("7733172a-8d19-4c4f-b317-2a8d19ec4f1c")
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
                            .applicationId("1")
                            .planId("7733172a-8d19-4c4f-b317-2a8d19ec4f1c")
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
                    .filter(Filter.builder().appId("f1608475-dd77-4603-a084-75dd775603e9").build())
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
                            .applicationId("1")
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
                            .applicationId("1")
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
        void should_return_the_1st_page_of_connection_logs_without_filter() {
            var today = DATE_FORMATTER.format(Instant.now());

            var result = logV4Repository.searchConnectionLog(ConnectionLogQuery.builder().size(2).build());
            assertThat(result).isNotNull();
            assertThat(result.total()).isEqualTo(7);
            assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getTimestamp)
                .containsExactly(
                    tuple("e71f2ae0-7673-4d7e-9f2a-e076730d7e69", today + "T06:57:44.893Z"),
                    tuple("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48", today + "T06:56:44.552Z")
                );
        }
    }
}
