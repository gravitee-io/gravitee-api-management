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
package io.gravitee.repository.elasticsearch.v4.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
class AnalyticsElasticsearchRepositoryTest extends AbstractElasticsearchRepositoryTest {

    private static final String API_ID = "f1608475-dd77-4603-a084-75dd775603e9";

    @Autowired
    private AnalyticsElasticsearchRepository cut;

    @Nested
    class RequestsCount {

        @Test
        void should_return_all_the_requests_count_by_entrypoint_for_a_given_api() {
            var result = cut.searchRequestsCount(new QueryContext("org#1", "env#1"), RequestsCountQuery.builder().apiId(API_ID).build());

            assertThat(result)
                .hasValueSatisfying(countAggregate -> {
                    assertThat(countAggregate.getTotal()).isEqualTo(10);
                    assertThat(countAggregate.getCountBy())
                        .containsAllEntriesOf(Map.of("http-post", 3L, "http-get", 1L, "websocket", 3L, "sse", 2L, "webhook", 1L));
                });
        }
    }

    @Nested
    class AverageMessagesPerRequest {

        @Test
        void should_return_average_messages_per_request_by_entrypoint_for_a_given_api() {
            var result = cut.searchAverageMessagesPerRequest(
                new QueryContext("org#1", "env#1"),
                AverageMessagesPerRequestQuery.builder().apiId(API_ID).build()
            );

            assertThat(result)
                .hasValueSatisfying(averageAggregate -> {
                    assertThat(averageAggregate.getAverage()).isCloseTo(45.7, offset(0.1d));
                    assertThat(averageAggregate.getAverageBy())
                        .containsAllEntriesOf(Map.of("http-get", 9.8, "websocket", 27.5, "sse", 100.0));
                });
        }
    }

    @Nested
    class AverageConnectionDuration {

        @Test
        void should_return_average_connection_duration_by_entrypoint_for_a_given_api() {
            var result = cut.searchAverageConnectionDuration(
                new QueryContext("org#1", "env#1"),
                AverageConnectionDurationQuery.builder().apiId(API_ID).build()
            );

            assertThat(result)
                .hasValueSatisfying(averageAggregate -> {
                    assertThat(averageAggregate.getAverage()).isCloseTo(20261.25, offset(0.1d));
                    assertThat(averageAggregate.getAverageBy())
                        .containsAllEntriesOf(Map.of("http-get", 30_000.0, "websocket", 50_000.0, "sse", 645.0, "http-post", 400.0));
                });
        }
    }

    @Nested
    class ResponseStatusCount {

        @Test
        void should_return_response_status_by_entrypoint_for_a_given_api() {
            var result = cut.searchResponseStatusRanges(
                new QueryContext("org#1", "env#1"),
                ResponseStatusQueryCriteria.builder().apiIds(List.of(API_ID)).build()
            );

            assertThat(result)
                .hasValueSatisfying(responseStatusAggregate -> {
                    assertRanges(responseStatusAggregate.getRanges(), 3L, 7L);
                    var statusRangesCountByEntrypoint = responseStatusAggregate.getStatusRangesCountByEntrypoint();
                    assertThat(statusRangesCountByEntrypoint).containsOnlyKeys("websocket", "http-post", "webhook", "sse", "http-get");
                    assertRanges(statusRangesCountByEntrypoint.get("websocket"), 0L, 3L);
                    assertRanges(statusRangesCountByEntrypoint.get("http-post"), 2L, 1L);
                    assertRanges(statusRangesCountByEntrypoint.get("webhook"), 0L, 1L);
                    assertRanges(statusRangesCountByEntrypoint.get("sse"), 1L, 1L);
                    assertRanges(statusRangesCountByEntrypoint.get("http-get"), 0L, 1L);
                });
        }

        @Test
        void should_return_response_status_by_entrypoint_for_a_given_api_and_date_range() {
            var yesterdayAtStartOfTheDayEpochMilli = LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
            var yesterdayAtEndOfTheDayEpochMilli = LocalDate.now().minusDays(1).atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli();

            var result = cut.searchResponseStatusRanges(
                new QueryContext("org#1", "env#1"),
                ResponseStatusQueryCriteria
                    .builder()
                    .apiIds(List.of(API_ID))
                    .from(yesterdayAtStartOfTheDayEpochMilli)
                    .to(yesterdayAtEndOfTheDayEpochMilli)
                    .build()
            );

            assertThat(result)
                .hasValueSatisfying(responseStatusAggregate -> {
                    assertRanges(responseStatusAggregate.getRanges(), 2L, 0L);
                    var statusRangesCountByEntrypoint = responseStatusAggregate.getStatusRangesCountByEntrypoint();
                    assertThat(statusRangesCountByEntrypoint).containsOnlyKeys("http-post", "sse");
                    assertRanges(statusRangesCountByEntrypoint.get("http-post"), 1L, 0L);
                    assertRanges(statusRangesCountByEntrypoint.get("sse"), 1L, 0L);
                });
        }

        @Test
        void should_response_empty_ranges_for_empty_ids_list() {
            var result = cut.searchResponseStatusRanges(
                new QueryContext("org#1", "env#1"),
                ResponseStatusQueryCriteria.builder().apiIds(List.of()).build()
            );

            assertThat(result).isNotNull().get().extracting(ResponseStatusRangesAggregate::getRanges).isEqualTo(Map.of());
        }

        @Test
        void should_response_empty_ranges_for_null__query_criteria() {
            var result = cut.searchResponseStatusRanges(new QueryContext("org#1", "env#1"), null);

            assertThat(result).isNotNull().get().extracting(ResponseStatusRangesAggregate::getRanges).isEqualTo(Map.of());
        }

        private static void assertRanges(Map<String, Long> ranges, long status2xx, long status4xx) {
            assertThat(ranges)
                .containsAllEntriesOf(
                    Map.of("100.0-200.0", 0L, "200.0-300.0", status2xx, "300.0-400.0", 0L, "400.0-500.0", status4xx, "500.0-600.0", 0L)
                );
        }
    }
}
