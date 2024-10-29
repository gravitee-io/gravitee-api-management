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
import static org.assertj.core.api.Assertions.atIndex;
import static org.assertj.core.api.Assertions.offset;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
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
        void should_response_empty_ranges_for_null_query_criteria() {
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

    @Nested
    class ResponseTimeOverTime {

        private static final Condition<Map.Entry<String, Double>> STRICT_POSITIVE = bucket(key -> true, d -> d > 0, "positive");

        @Test
        void should_return_response_status_by_entrypoint_for_a_given_api() {
            // Given
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = from.plus(Duration.ofDays(1));
            Duration interval = Duration.ofMinutes(10);

            // When
            AverageAggregate result = cut
                .searchResponseTimeOverTime(
                    new QueryContext("org#1", "env#1"),
                    new ResponseTimeRangeQuery("f1608475-dd77-4603-a084-75dd775603e9", from, to, interval)
                )
                .blockingGet();

            // Then
            long nbBuckets = Duration.between(from, to).dividedBy(interval);
            assertThat(result.getAverageBy().entrySet())
                .hasSize((int) nbBuckets + 1)
                .haveExactly(1, bucketOfTimeHaveValue("14:00:00.000Z", 332.5))
                .haveExactly(1, STRICT_POSITIVE);
        }

        private static Condition<Map.Entry<String, Double>> bucketOfTimeHaveValue(String timeSuffix, double value) {
            return bucket(key -> key.endsWith(timeSuffix), d -> d == value, "entre for '%s' with value %f".formatted(timeSuffix, value));
        }

        private static Condition<Map.Entry<String, Double>> bucket(
            Predicate<String> keyPredicate,
            DoublePredicate value,
            String description
        ) {
            return new Condition<>(entry -> value.test(entry.getValue()) && keyPredicate.test(entry.getKey()), description);
        }
    }

    @Nested
    class ResponseStatusOverTime {

        @Test
        void should_return_response_status_over_time_for_a_given_api() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var interval = Duration.ofMinutes(30);

            var result = cut.searchResponseStatusOvertime(
                new QueryContext("org#1", "env#1"),
                ResponseStatusOverTimeQuery.builder().apiId(API_ID).from(from).to(to).interval(interval).build()
            );

            var nbBuckets = Duration.between(from, to).dividedBy(interval) + 1;
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getStatusCount()).containsOnlyKeys("200", "202", "404");
                softly.assertThat(result.getStatusCount().get("200")).hasSize((int) nbBuckets).contains(1L, atIndex(28));
                softly
                    .assertThat(result.getStatusCount().get("202"))
                    .hasSize((int) nbBuckets)
                    .contains(1L, atIndex(28))
                    .contains(1L, atIndex(61));
                softly.assertThat(result.getStatusCount().get("404")).hasSize((int) nbBuckets).contains(7L, atIndex(61));
            });
        }
    }

    @Nested
    class TopHitsCount {

        private static final long FROM = 1728992401566L;
        private static final long TO = 1729078801566L;

        @Test
        void should_return_top_hits_count_for_a_given_api_and_date_range() {
            var yesterdayAtStartOfTheDayEpochMilli = LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
            var yesterdayAtEndOfTheDayEpochMilli = LocalDate.now().minusDays(1).atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli();

            var result = cut.searchTopHitsApi(
                new QueryContext("org#1", "env#1"),
                new TopHitsQueryCriteria(List.of(API_ID), yesterdayAtStartOfTheDayEpochMilli, yesterdayAtEndOfTheDayEpochMilli)
            );

            assertThat(result)
                .isNotNull()
                .isPresent()
                .get()
                .extracting(TopHitsAggregate::getTopHitsCounts)
                .hasFieldOrPropertyWithValue(API_ID, 2L);
        }

        @Test
        void should_return_empty_top_hits_count_for_empty_ids_list() {
            var result = cut.searchTopHitsApi(new QueryContext("org#1", "env#1"), new TopHitsQueryCriteria(List.of(API_ID), FROM, TO));

            assertThat(result).isNotNull().get().extracting(TopHitsAggregate::getTopHitsCounts).isEqualTo(Map.of());
        }

        @Test
        void should_return_empty_top_hits_count_for_null_query_criteria() {
            var result = cut.searchTopHitsApi(new QueryContext("org#1", "env#1"), null);

            assertThat(result).isNotNull().get().extracting(TopHitsAggregate::getTopHitsCounts).isEqualTo(Map.of());
        }
    }

    @Nested
    class RequestResponseTime {

        @Test
        void should_return_top_hits_count_for_a_given_api_and_date_range() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS).toEpochMilli();
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS).toEpochMilli();

            var result = cut.searchRequestResponseTimes(
                new QueryContext("org#1", "env#1"),
                new RequestResponseTimeQueryCriteria(List.of(API_ID), from, to)
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getRequestsPerSecond()).isEqualTo(2.3148148148148147E-5);
                softly.assertThat(result.getRequestsTotal()).isEqualTo(4L);
                softly.assertThat(result.getResponseMinTime()).isEqualTo(20.0);
                softly.assertThat(result.getResponseMaxTime()).isEqualTo(30000.0);
                softly.assertThat(result.getResponseAvgTime()).isEqualTo(7800.0);
            });
        }

        @Test
        void should_return_empty_request_response_time_aggregate_for_empty_ids_list() {
            var from = 1728992401566L;
            var to = 1729078801566L;
            var result = cut.searchRequestResponseTimes(
                new QueryContext("org#1", "env#1"),
                new RequestResponseTimeQueryCriteria(List.of(API_ID), from, to)
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getRequestsPerSecond()).isEqualTo(0d);
                softly.assertThat(result.getRequestsTotal()).isEqualTo(0L);
                softly.assertThat(result.getResponseMinTime()).isEqualTo(0d);
                softly.assertThat(result.getResponseMaxTime()).isEqualTo(0d);
                softly.assertThat(result.getResponseAvgTime()).isEqualTo(0d);
            });
        }

        @Test
        void should_return_empty_request_response_time_aggregate_for_null_query_criteria() {
            var result = cut.searchRequestResponseTimes(new QueryContext("org#1", "env#1"), null);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getRequestsPerSecond()).isEqualTo(0d);
                softly.assertThat(result.getRequestsTotal()).isEqualTo(0L);
                softly.assertThat(result.getResponseMinTime()).isEqualTo(0d);
                softly.assertThat(result.getResponseMaxTime()).isEqualTo(0d);
                softly.assertThat(result.getResponseAvgTime()).isEqualTo(0d);
            });
        }
    }
}
