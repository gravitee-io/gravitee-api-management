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

import static io.gravitee.definition.model.DefinitionVersion.V2;
import static io.gravitee.definition.model.DefinitionVersion.V4;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.assertj.core.api.Assertions.not;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Assertions.withPrecision;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.gravitee.repository.log.v4.model.analytics.Aggregation;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.ApiMetricsDetailQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.GroupByQuery;
import io.gravitee.repository.log.v4.model.analytics.HistogramAggregate;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountByEventQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.StatsQuery;
import io.gravitee.repository.log.v4.model.analytics.Term;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import io.gravitee.repository.log.v4.model.analytics.TopFailedAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopFailedQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
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
    private static final String APIV2_1 = "e2c0ecd5-893a-458d-80ec-d5893ab58d12";
    private static final String APIV2_2 = "4d8d6ca8-c2c7-4ab8-8d6c-a8c2c79ab8a1";
    private static final String API_ID_SSE = "43276d60-7058-4588-8023-52bf1154f903";

    @Autowired
    private AnalyticsElasticsearchRepository cut;

    @Nested
    class RequestsCount {

        @Test
        void should_return_total_requests_count_from_status_ranges_aggregation() {
            var result = cut.searchRequestsCount(new QueryContext("org#1", "env#1"), new RequestsCountQuery(API_ID));

            assertThat(result).hasValueSatisfying(countAggregate -> {
                assertThat(countAggregate.getTotal()).isEqualTo(11);
                assertThat(countAggregate.getCountBy()).containsAllEntriesOf(
                    Map.of("http-post", 3L, "http-get", 1L, "websocket", 3L, "sse", 2L, "webhook", 1L)
                );
            });
        }
    }

    @Nested
    class AverageMessagesPerRequest {

        @Test
        void should_return_average_messages_per_request_by_entrypoint_for_a_given_api() {
            var result = cut.searchAverageMessagesPerRequest(
                new QueryContext("org#1", "env#1"),
                new AverageMessagesPerRequestQuery(API_ID)
            );

            assertThat(result).hasValueSatisfying(averageAggregate -> {
                assertThat(averageAggregate.getAverage()).isCloseTo(45.7, offset(0.1d));
                assertThat(averageAggregate.getAverageBy()).containsAllEntriesOf(Map.of("http-get", 9.8, "websocket", 27.5, "sse", 100.0));
            });
        }
    }

    @Nested
    class AverageConnectionDuration {

        @Test
        void should_return_average_connection_duration_by_entrypoint_for_a_given_api() {
            var result = cut.searchAverageConnectionDuration(
                new QueryContext("org#1", "env#1"),
                new AverageConnectionDurationQuery(API_ID)
            );

            assertThat(result).hasValueSatisfying(averageAggregate -> {
                assertThat(averageAggregate.getAverage()).isCloseTo(20261.25, offset(0.1d));
                assertThat(averageAggregate.getAverageBy()).containsAllEntriesOf(
                    Map.of("http-get", 30_000.0, "websocket", 50_000.0, "sse", 645.0, "http-post", 400.0)
                );
            });
        }

        @Test
        void should_return_average_connection_duration_by_entrypoint_for_a_time_period() {
            var now = Instant.now();
            var from = now.truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(1));
            var to = now.truncatedTo(ChronoUnit.DAYS);

            var result = cut.searchAverageConnectionDuration(
                new QueryContext("org#1", "env#1"),
                new AverageConnectionDurationQuery(Optional.of(API_ID), Optional.of(from), Optional.of(to))
            );

            assertThat(result).hasValueSatisfying(averageAggregate -> {
                assertThat(averageAggregate.getAverage()).isCloseTo(332.5, offset(0.1d));
                assertThat(averageAggregate.getAverageBy()).containsAllEntriesOf(Map.of("sse", 645.0, "http-post", 20.0));
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

            assertThat(result).hasValueSatisfying(responseStatusAggregate -> {
                assertRanges(responseStatusAggregate.getRanges(), 3L, 8L);
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
        void should_return_response_status_for_V4_and_V2_definitions() {
            var result = cut.searchResponseStatusRanges(
                new QueryContext("org#1", "env#1"),
                ResponseStatusQueryCriteria.builder()
                    .apiIds(List.of(API_ID, APIV2_1, APIV2_2))
                    .definitionVersions(EnumSet.of(V4, V2))
                    .build()
            );

            assertThat(result).hasValueSatisfying(responseStatusAggregate -> {
                assertRanges(responseStatusAggregate.getRanges(), 7L, 11L);
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
                ResponseStatusQueryCriteria.builder()
                    .apiIds(List.of(API_ID))
                    .from(yesterdayAtStartOfTheDayEpochMilli)
                    .to(yesterdayAtEndOfTheDayEpochMilli)
                    .build()
            );

            assertThat(result).hasValueSatisfying(responseStatusAggregate -> {
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

            assertThat(result)
                .isNotNull()
                .get()
                .extracting(ResponseStatusRangesAggregate::getRanges)
                .isEqualTo(Map.of("100.0-200.0", 0L, "200.0-300.0", 0L, "300.0-400.0", 0L, "400.0-500.0", 0L, "500.0-600.0", 0L));
        }

        private static void assertRanges(Map<String, Long> ranges, long status2xx, long status4xx) {
            assertThat(ranges).containsAllEntriesOf(
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
                    new ResponseTimeRangeQuery(List.of("f1608475-dd77-4603-a084-75dd775603e9"), from, to, interval)
                )
                .blockingGet();

            // Then
            long nbBuckets = Duration.between(from, to).dividedBy(interval);
            assertThat(requireNonNull(result).getAverageBy().entrySet())
                .hasSize((int) nbBuckets + 1)
                .haveExactly(1, bucketOfTimeHaveValue("14:00:00.000Z", 20.))
                .haveExactly(1, STRICT_POSITIVE);
        }

        @Test
        void should_return_response_status_for_api_v2_and_v4() {
            // Given
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = from.plus(Duration.ofDays(1));
            Duration interval = Duration.ofMinutes(10);

            // When
            AverageAggregate result = cut
                .searchResponseTimeOverTime(
                    new QueryContext("org#1", "env#1"),
                    new ResponseTimeRangeQuery(
                        List.of("f1608475-dd77-4603-a084-75dd775603e9", APIV2_1, APIV2_2),
                        from,
                        to,
                        interval,
                        EnumSet.of(V4, V2)
                    )
                )
                .blockingGet();

            // Then
            long nbBuckets = Duration.between(from, to).dividedBy(interval);
            assertThat(requireNonNull(result).getAverageBy().entrySet()).hasSize((int) nbBuckets + 1).haveAtMost(2, STRICT_POSITIVE);
            double[] array = result
                .getAverageBy()
                .values()
                .stream()
                .mapToDouble(l -> l)
                .filter(d -> d > 0)
                .toArray();
            assertThat(array).containsOnly(36.25, 20.0);
        }

        private static Condition<Map.Entry<String, Double>> bucketOfTimeHaveValue(String timeSuffix, double value) {
            return bucket(key -> key.endsWith(timeSuffix), d -> d == value, "entry for '%s' with value %f".formatted(timeSuffix, value));
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
                ResponseStatusOverTimeQuery.builder().apiIds(List.of(API_ID, API_ID_SSE)).from(from).to(to).interval(interval).build()
            );

            var nbBuckets = Duration.between(from, to).dividedBy(interval) + 1;
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getStatusCount()).containsOnlyKeys("200", "202", "404");
                softly.assertThat(result.getStatusCount().get("200")).hasSize((int) nbBuckets).contains(1L, atIndex(28));
                softly.assertThat(result.getStatusCount().get("202")).hasSize((int) nbBuckets).contains(1L, atIndex(61));
                softly.assertThat(result.getStatusCount().get("404")).hasSize((int) nbBuckets).contains(2L, atIndex(61));
            });
        }

        @Test
        void should_return_response_status_over_time_for_api_v2_and_v4() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var interval = Duration.ofMinutes(30);

            var result = cut.searchResponseStatusOvertime(
                new QueryContext("org#1", "env#1"),
                ResponseStatusOverTimeQuery.builder()
                    .apiIds(List.of(API_ID, APIV2_1, APIV2_2))
                    .from(from)
                    .to(to)
                    .interval(interval)
                    .versions(EnumSet.of(V4, V2))
                    .build()
            );

            var nbBuckets = Duration.between(from, to).dividedBy(interval) + 1;
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getStatusCount()).containsOnlyKeys("200", "202", "404", "401");
                softly
                    .assertThat(
                        result
                            .getStatusCount()
                            .get("200")
                            .stream()
                            .mapToLong(l -> l)
                            .sum()
                    )
                    .isEqualTo(5);
                softly.assertThat(result.getStatusCount().get("200")).hasSize((int) nbBuckets).haveAtMost(5, not(is(0)));
                softly
                    .assertThat(result.getStatusCount().get("202"))
                    .hasSize((int) nbBuckets)
                    .haveExactly(1, is(1))
                    .haveAtMost(1, not(is(0)));
                softly
                    .assertThat(
                        result
                            .getStatusCount()
                            .get("404")
                            .stream()
                            .mapToLong(l -> l)
                            .sum()
                    )
                    .isEqualTo(2);
                softly.assertThat(result.getStatusCount().get("404")).hasSize((int) nbBuckets).haveAtMost(2, not(is(0)));
            });
        }

        private Condition<Long> is(long expected) {
            return new Condition<>(l -> l == expected, Long.toString(expected));
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
                new TopHitsQueryCriteria(List.of(API_ID, API_ID_SSE), yesterdayAtStartOfTheDayEpochMilli, yesterdayAtEndOfTheDayEpochMilli)
            );

            assertThat(result)
                .isNotNull()
                .isPresent()
                .get()
                .extracting(TopHitsAggregate::getTopHitsCounts)
                .hasFieldOrPropertyWithValue(API_ID, 2L);
        }

        @Test
        void should_return_top_hits_count_for_api_v2_and_v4() {
            var yesterdayAtStartOfTheDayEpochMilli = LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
            var yesterdayAtEndOfTheDayEpochMilli = LocalDate.now().minusDays(1).atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli();

            var result = cut.searchTopHitsApi(
                new QueryContext("org#1", "env#1"),
                new TopHitsQueryCriteria(
                    List.of(API_ID, APIV2_1, APIV2_2),
                    yesterdayAtStartOfTheDayEpochMilli,
                    yesterdayAtEndOfTheDayEpochMilli
                )
            );

            assertThat(result)
                .isNotNull()
                .isPresent()
                .get()
                .extracting(TopHitsAggregate::getTopHitsCounts)
                .hasFieldOrPropertyWithValue(API_ID, 2L)
                .hasFieldOrPropertyWithValue(APIV2_1, 1L)
                .hasFieldOrPropertyWithValue(APIV2_2, 3L);
        }

        @Test
        void should_return_empty_top_hits_count_for_empty_ids_list() {
            var result = cut.searchTopHitsApi(
                new QueryContext("org#1", "env#1"),
                new TopHitsQueryCriteria(List.of(API_ID, API_ID_SSE), FROM, TO)
            );

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
                new RequestResponseTimeQueryCriteria(List.of(API_ID, API_ID_SSE), from, to, EnumSet.of(V4))
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
        void should_return_top_hits_count_for_a_apiv2_and_v4() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS).toEpochMilli();
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS).toEpochMilli();

            var result = cut.searchRequestResponseTimes(
                new QueryContext("org#1", "env#1"),
                new RequestResponseTimeQueryCriteria(List.of(API_ID, APIV2_1, APIV2_2, API_ID_SSE), from, to, EnumSet.of(V4, V2))
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getRequestsPerSecond()).isCloseTo(6.36574074074074E-5, withPrecision(.01d));
                softly.assertThat(result.getRequestsTotal()).isEqualTo(11L);
                softly.assertThat(result.getResponseMinTime()).isEqualTo(2.0);
                softly.assertThat(result.getResponseMaxTime()).isEqualTo(30000.0);
                softly.assertThat(result.getResponseAvgTime()).isCloseTo(2916.3635, withPrecision(.01d));
            });
        }

        @Test
        void should_return_empty_request_response_time_aggregate_for_empty_ids_list() {
            var from = 1728992401566L;
            var to = 1729078801566L;
            var result = cut.searchRequestResponseTimes(
                new QueryContext("org#1", "env#1"),
                new RequestResponseTimeQueryCriteria(List.of(API_ID, API_ID_SSE), from, to, EnumSet.of(V4))
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getRequestsPerSecond()).isEqualTo(0d);
                softly.assertThat(result.getRequestsTotal()).isEqualTo(0L);
                softly.assertThat(result.getResponseMinTime()).isEqualTo(0d);
                softly.assertThat(result.getResponseMaxTime()).isEqualTo(0d);
                softly.assertThat(result.getResponseAvgTime()).isEqualTo(0d);
            });
        }
    }

    @Nested
    class TopHitApps {

        private static final String V4_API_ID = "f1608475-dd77-4603-a084-75dd775603e9";
        private static final long FROM = 1728992401566L;
        private static final long TO = 1729078801566L;

        @Test
        void should_return_top_hits_count_for_a_given_api_and_date_range() {
            var yesterdayAtStartOfTheDayEpochMilli = LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
            var yesterdayAtEndOfTheDayEpochMilli = LocalDate.now().minusDays(1).atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli();

            var result = cut.searchTopApps(
                new QueryContext("org#1", "env#1"),
                new TopHitsQueryCriteria(
                    List.of(V4_API_ID, API_ID_SSE),
                    yesterdayAtStartOfTheDayEpochMilli,
                    yesterdayAtEndOfTheDayEpochMilli
                )
            );

            assertThat(result)
                .isNotNull()
                .isPresent()
                .get()
                .extracting(TopHitsAggregate::getTopHitsCounts)
                .isEqualTo(Map.of("f37a5799-0490-43f6-ba57-990490f3f678", 1L, "613dc986-41ce-4b5b-bdc9-8641cedb5bdb", 1L));
        }

        @Test
        void should_return_top_hits_fom_V4_and_V2() {
            var yesterdayAtStartOfTheDayEpochMilli = LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
            var yesterdayAtEndOfTheDayEpochMilli = LocalDate.now().minusDays(1).atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli();

            var result = cut.searchTopApps(
                new QueryContext("org#1", "env#1"),
                new TopHitsQueryCriteria(
                    List.of(V4_API_ID, APIV2_1, APIV2_2),
                    yesterdayAtStartOfTheDayEpochMilli,
                    yesterdayAtEndOfTheDayEpochMilli
                )
            );

            assertThat(result)
                .isNotNull()
                .isPresent()
                .get()
                .extracting(TopHitsAggregate::getTopHitsCounts)
                .isEqualTo(
                    Map.of(
                        "f37a5799-0490-43f6-ba57-990490f3f678",
                        1L,
                        "613dc986-41ce-4b5b-bdc9-8641cedb5bdb",
                        1L,
                        "31b0d824-4f6a-4f58-b0d8-244f6a4f58d7",
                        2L
                    )
                );
        }

        @Test
        void should_return_empty_top_hits_count_for_empty_ids_list() {
            var result = cut.searchTopApps(new QueryContext("org#1", "env#1"), new TopHitsQueryCriteria(List.of(API_ID), FROM, TO));

            assertThat(result).isNotNull().get().extracting(TopHitsAggregate::getTopHitsCounts).isEqualTo(Map.of());
        }

        @Test
        void should_return_empty_top_hits_count_for_null_query_criteria() {
            var result = cut.searchTopApps(new QueryContext("org#1", "env#1"), null);

            assertThat(result).isNotNull().get().extracting(TopHitsAggregate::getTopHitsCounts).isEqualTo(Map.of());
        }
    }

    @Nested
    class TopFailedApis {

        private static final String V4_API_ID = "4a6895d5-a1bc-4041-a895-d5a1bce041ae";
        private static final Instant NOW = Instant.now();
        private static final long FROM = NOW.truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(1)).toEpochMilli();
        private static final long TO = NOW.truncatedTo(ChronoUnit.DAYS).toEpochMilli();

        @Test
        void should_return_top_failed_apis() {
            var result = cut.searchTopFailedApis(
                new QueryContext("org#1", "env#1"),
                new TopFailedQueryCriteria(List.of(V4_API_ID, API_ID_SSE), FROM, TO)
            );

            assertThat(result)
                .isNotNull()
                .isPresent()
                .get()
                .extracting(TopFailedAggregate::failedApis)
                .isEqualTo(Map.of("4a6895d5-a1bc-4041-a895-d5a1bce041ae", new TopFailedAggregate.FailedApiInfo(1L, 0.5)));
        }

        @Test
        void should_return_top_failed_apis2() {
            var result = cut.searchTopFailedApis(
                new QueryContext("org#1", "env#1"),
                new TopFailedQueryCriteria(List.of(V4_API_ID, APIV2_1, APIV2_2), FROM, TO)
            );

            assertThat(result)
                .isNotNull()
                .isPresent()
                .get()
                .extracting(TopFailedAggregate::failedApis)
                .isEqualTo(Map.of("4a6895d5-a1bc-4041-a895-d5a1bce041ae", new TopFailedAggregate.FailedApiInfo(1L, 0.5)));
        }

        @Test
        void should_return_empty_top_failed_apis_for_empty_ids_list() {
            var result = cut.searchTopFailedApis(
                new QueryContext("org#1", "env#1"),
                new TopFailedQueryCriteria(List.of(API_ID, API_ID_SSE), FROM, TO)
            );

            assertThat(result).isNotNull().get().extracting(TopFailedAggregate::failedApis).isEqualTo(Map.of());
        }

        @Test
        void should_return_empty_top_failed_apis_for_null_query_criteria() {
            var result = cut.searchTopFailedApis(new QueryContext("org#1", "env#1"), null);

            assertThat(result).isNotNull().get().extracting(TopFailedAggregate::failedApis).isEqualTo(Map.of());
        }
    }

    @Nested
    class Histogram {

        @Test
        void should_return_histogram_aggregates_for_a_given_api() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var interval = Duration.ofMinutes(30);

            var query = new HistogramQuery(
                new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                new TimeRange(from, to, interval),
                List.of(new Aggregation("status", AggregationType.FIELD)),
                Optional.empty(),
                null
            );

            var result = cut.searchHistogram(new QueryContext("org#1", "env#1"), query);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);

            var first = result.getFirst();
            var histogram = (HistogramAggregate.Counts) first;
            assertThat(histogram.counts()).isNotNull();
            assertThat(histogram.counts()).hasSize(3);

            assertThat(histogram.counts().keySet()).containsExactlyInAnyOrder("200", "202", "404");

            var bucket200 = histogram.counts().get("200");
            var bucket202 = histogram.counts().get("202");
            var bucket404 = histogram.counts().get("404");

            assertThat(bucket200).hasSizeGreaterThan(28);
            assertThat(bucket200.get(28)).isEqualTo(1L);

            assertThat(bucket202).hasSizeGreaterThan(61);
            assertThat(bucket202.get(61)).isEqualTo(1L);

            assertThat(bucket404).hasSizeGreaterThan(61);
            assertThat(bucket404.get(61)).isEqualTo(2L);
        }

        @Test
        void should_return_histogram_aggregates_for_avg_gateway_response_time_ms() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var interval = Duration.ofMinutes(30);

            var query = new HistogramQuery(
                new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                new TimeRange(from, to, interval),
                List.of(new Aggregation("gateway-response-time-ms", AggregationType.AVG)),
                Optional.empty(),
                null
            );

            var result = cut.searchHistogram(new QueryContext("org#1", "env#1"), query);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);

            var first = result.getFirst();
            var histogram = (HistogramAggregate.Metric) first;
            assertThat(histogram.values()).isNotNull();

            var avgBucket = histogram.values();
            assertThat(avgBucket).isNotEmpty();
            assertThat(
                avgBucket
                    .stream()
                    .filter(v -> v > 0)
                    .count()
            ).isEqualTo(2);
        }

        @Test
        void should_return_histogram_aggregates_for_a_given_api_with_query_string() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var interval = Duration.ofMinutes(30);

            var query = new HistogramQuery(
                new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                new TimeRange(from, to, interval),
                List.of(new Aggregation("status", AggregationType.FIELD)),
                Optional.of("status:404"),
                null
            );

            var result = cut.searchHistogram(new QueryContext("org#1", "env#1"), query);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);

            var first = result.getFirst();
            var histogram = (HistogramAggregate.Counts) first;
            assertThat(histogram.counts()).isNotNull();
            assertThat(histogram.counts()).hasSize(1);

            assertThat(histogram.counts().keySet()).containsExactly("404");

            var bucket404 = histogram.counts().get("404");
            assertThat(bucket404).hasSizeGreaterThan(61);
            assertThat(bucket404.get(61)).isEqualTo(2L);
        }
    }

    @Nested
    class GroupByAggregate {

        @Test
        void should_return_group_by_aggregate_for_terms() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);

            var query = new GroupByQuery(
                new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                "entrypoint-id",
                Collections.emptyList(),
                Optional.empty(),
                new TimeRange(from, to),
                Optional.empty()
            );
            var result = cut.searchGroupBy(new QueryContext("org#1", "env#1"), query);

            assertThat(result)
                .isPresent()
                .hasValueSatisfying(aggregate -> {
                    assertThat(aggregate.name()).isEqualTo("by_entrypoint-id");
                    assertThat(aggregate.field()).isEqualTo("entrypoint-id");
                    assertThat(aggregate.values()).containsKeys("http-get", "http-post");
                });
        }

        @Test
        void should_return_group_by_aggregate_for_range() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);

            var query = new GroupByQuery(
                new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                "response-time",
                List.of(new GroupByQuery.Group(0, 100), new GroupByQuery.Group(100, 200)),
                Optional.empty(),
                new TimeRange(from, to),
                Optional.empty()
            );
            var result = cut.searchGroupBy(new QueryContext("org#1", "env#1"), query);

            assertThat(result)
                .isPresent()
                .hasValueSatisfying(aggregate -> {
                    assertThat(aggregate.name()).isEqualTo("by_response-time_range");
                    assertThat(aggregate.field()).isEqualTo("response-time");
                    assertThat(aggregate.values()).containsKeys("0.0-100.0", "100.0-200.0");
                });
        }

        @Test
        void should_return_group_by_aggregate_for_terms_with_query_parameter() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);

            var queryString = "status:404 AND http-method:8";
            var query = new GroupByQuery(
                new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                "entrypoint-id",
                Collections.emptyList(),
                Optional.empty(),
                new TimeRange(from, to),
                Optional.of(queryString)
            );
            var result = cut.searchGroupBy(new QueryContext("org#1", "env#1"), query);

            assertThat(result)
                .isPresent()
                .hasValueSatisfying(aggregate -> {
                    assertThat(aggregate.name()).isEqualTo("by_entrypoint-id");
                    assertThat(aggregate.field()).isEqualTo("entrypoint-id");
                    assertThat(aggregate.values()).containsExactlyEntriesOf(Map.of("http-get", 1L));
                });
        }

        @Test
        void should_return_group_by_aggregate_for_terms_with_order_avg() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);

            var order = new GroupByQuery.Order("gateway-response-time-ms", false, "AVG");
            var query = new GroupByQuery(
                new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                "entrypoint-id",
                Collections.emptyList(),
                Optional.of(order),
                new TimeRange(from, to),
                Optional.empty()
            );
            var result = cut.searchGroupBy(new QueryContext("org#1", "env#1"), query);

            assertThat(result)
                .isPresent()
                .hasValueSatisfying(aggregate -> {
                    assertThat(aggregate.name()).isEqualTo("by_entrypoint-id");
                    assertThat(aggregate.field()).isEqualTo("entrypoint-id");
                    assertThat(aggregate.values()).containsKeys("http-get", "http-post");
                    assertThat(aggregate.order()).containsExactly("http-get", "http-post");
                });
        }

        @Test
        void should_return_group_by_aggregate_for_terms_with_order_value() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);

            var order = new GroupByQuery.Order("_key", true, "VALUE");
            var query = new GroupByQuery(
                new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                "entrypoint-id",
                Collections.emptyList(),
                Optional.of(order),
                new TimeRange(from, to),
                Optional.empty()
            );
            var result = cut.searchGroupBy(new QueryContext("org#1", "env#1"), query);

            assertThat(result)
                .isPresent()
                .hasValueSatisfying(aggregate -> {
                    assertThat(aggregate.name()).isEqualTo("by_entrypoint-id");
                    assertThat(aggregate.field()).isEqualTo("entrypoint-id");
                    assertThat(aggregate.values()).containsKeys("http-get", "http-post");
                    assertThat(aggregate.order()).containsExactly("http-get", "http-post");
                });
        }
    }

    @Nested
    class StatsAnalytics {

        @Test
        void should_return_stats_for_a_given_api_and_field() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);

            var query = new StatsQuery(
                "gateway-response-time-ms",
                new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                new TimeRange(from, to),
                Optional.empty()
            );

            Optional<StatsAggregate> result = cut.searchStats(new QueryContext("org#1", "env#1"), query);

            assertThat(result)
                .isPresent()
                .hasValueSatisfying(stats -> {
                    assertThat(stats.field()).isEqualTo("gateway-response-time-ms");
                    assertThat(stats.count()).isEqualTo(8L);
                    assertThat(stats.sum()).isEqualTo(131864L);
                    assertThat(stats.avg()).isEqualTo(16483L);
                    assertThat(stats.min()).isEqualTo(19L);
                    assertThat(stats.max()).isEqualTo(60000L);
                });
        }

        @Test
        void should_return_empty_if_no_stats_found() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(10)).truncatedTo(ChronoUnit.DAYS);
            var to = from.plus(Duration.ofDays(1));

            var query = new StatsQuery(
                "non-existent-field",
                new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                new TimeRange(from, to),
                Optional.empty()
            );

            Optional<StatsAggregate> result = cut.searchStats(new QueryContext("org#1", "env#1"), query);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_stats_for_a_given_api_and_field_with_query_string() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var queryString = "status:404 AND http-method:8";

            var query = new StatsQuery(
                "gateway-response-time-ms",
                new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                new TimeRange(from, to),
                Optional.of(queryString)
            );

            Optional<StatsAggregate> result = cut.searchStats(new QueryContext("org#1", "env#1"), query);

            assertThat(result)
                .isPresent()
                .hasValueSatisfying(stats -> {
                    assertThat(stats.field()).isEqualTo("gateway-response-time-ms");
                    assertThat(stats.count()).isEqualTo(2L);
                    assertThat(stats.sum()).isEqualTo(70000L);
                    assertThat(stats.avg()).isEqualTo(35000L);
                    assertThat(stats.min()).isEqualTo(30000L);
                    assertThat(stats.max()).isEqualTo(40000L);
                    assertThat(stats.rps()).isEqualTo(0L);
                    assertThat(stats.rpm()).isEqualTo(0L);
                    assertThat(stats.rph()).isEqualTo(0L);
                });
        }
    }

    @Nested
    class RequestCountByEvent {

        @Test
        void should_return_all_the_requests_count_by_entrypoint_for_a_given_api() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var result = cut.searchRequestsCountByEvent(
                new QueryContext("org#1", "env#1"),
                new RequestsCountByEventQuery(
                    new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                    new TimeRange(from, to),
                    Optional.empty()
                )
            );
            assertThat(result).hasValueSatisfying(countAggregate -> {
                assertThat(countAggregate.total()).isEqualTo(11);
            });
        }

        @Test
        void should_return_count_for_a_given_api_and_field_with_query_string() {
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var queryString = "status:404 AND http-method:8";

            var result = cut.searchRequestsCountByEvent(
                new QueryContext("org#1", "env#1"),
                new RequestsCountByEventQuery(
                    new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
                    new TimeRange(from, to),
                    Optional.of(queryString)
                )
            );
            assertThat(result).hasValueSatisfying(countAggregate -> {
                assertThat(countAggregate.total()).isGreaterThan(0);
            });
        }
    }

    @Nested
    class findApiMetricsDetail {

        private final QueryContext queryContext = new QueryContext("org#1", "env#1");

        @Test
        void should_return_empty_result_when_api_does_not_exist() {
            var result = cut.findApiMetricsDetail(
                queryContext,
                new ApiMetricsDetailQuery("notExisting", "8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
            );
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_result_when_request_id_does_not_exist() {
            var result = cut.findApiMetricsDetail(
                queryContext,
                new ApiMetricsDetailQuery("f1608475-dd77-4603-a084-75dd775603e9", "notExisting")
            );
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_v4_metric_result() {
            var result = cut.findApiMetricsDetail(
                queryContext,
                new ApiMetricsDetailQuery("f1608475-dd77-4603-a084-75dd775603e9", "8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
            );

            assertThat(result).hasValueSatisfying(apiMetricsDetail ->
                assertThat(apiMetricsDetail)
                    .hasFieldOrPropertyWithValue("apiId", "f1608475-dd77-4603-a084-75dd775603e9")
                    .hasFieldOrPropertyWithValue("requestId", "8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                    .hasFieldOrPropertyWithValue("transactionId", "8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                    .hasFieldOrPropertyWithValue("host", "apim-master-gateway.team-apim.gravitee.dev")
                    .hasFieldOrPropertyWithValue("applicationId", "1e478236-e6e4-4cf5-8782-36e6e4ccf57d")
                    .hasFieldOrPropertyWithValue("planId", "733b78f1-1a16-4c16-bb78-f11a16ac1693")
                    .hasFieldOrPropertyWithValue("gateway", "2c99d50d-d318-42d3-99d5-0dd31862d3d2")
                    .hasFieldOrPropertyWithValue("uri", "/jgi-message-logs-kafka/")
                    .hasFieldOrPropertyWithValue("status", 404)
                    .hasFieldOrPropertyWithValue("requestContentLength", 0L)
                    .hasFieldOrPropertyWithValue("responseContentLength", 41L)
                    .hasFieldOrPropertyWithValue("remoteAddress", "127.0.0.1")
                    .hasFieldOrPropertyWithValue("method", HttpMethod.GET)
            );
        }
    }

    @Nested
    class EventAnalytics {

        private static final String NATIVE_API_ID = "273f4728-1e30-4c78-bf47-281e304c78a5";
        private static final QueryContext QUERY_CONTEXT = new QueryContext("DEFAULT", "DEFAULT");
        private static final TimeProvider TIME_PROVIDER = new TimeProvider();

        @Test
        void should_search_top_value_hits_for_active_connections() {
            Aggregation agg1 = new Aggregation("downstream-active-connections", AggregationType.VALUE);
            Aggregation agg2 = new Aggregation("upstream-active-connections", AggregationType.VALUE);

            var result = cut.searchEventAnalytics(QUERY_CONTEXT, buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID));

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, Map<String, List<Long>>> data = aggregate.values();
                assertThat(data).containsKey("downstream-active-connections_latest");
                assertThat(data).containsKey("upstream-active-connections_latest");
                Map<String, List<Long>> downstreamConnectionsBucket = data.get("downstream-active-connections_latest");
                assertThat(downstreamConnectionsBucket).containsKey("downstream-active-connections");
                assertThat(downstreamConnectionsBucket.get("downstream-active-connections").getFirst()).isEqualTo(3L);
                Map<String, List<Long>> upstreamConnectionsBucket = data.get("upstream-active-connections_latest");
                assertThat(upstreamConnectionsBucket).containsKey("upstream-active-connections");
                assertThat(upstreamConnectionsBucket.get("upstream-active-connections").getFirst()).isEqualTo(3L);
            });
        }

        @Test
        void should_search_top_value_hits_for_messages_consumed() {
            Aggregation agg1 = new Aggregation("downstream-subscribe-messages-total", AggregationType.VALUE);
            Aggregation agg2 = new Aggregation("upstream-subscribe-messages-total", AggregationType.VALUE);
            Aggregation agg3 = new Aggregation("downstream-subscribe-message-bytes", AggregationType.VALUE);
            Aggregation agg4 = new Aggregation("upstream-subscribe-message-bytes", AggregationType.VALUE);

            var result = cut.searchEventAnalytics(QUERY_CONTEXT, buildHistogramQuery(List.of(agg1, agg2, agg3, agg4), null, NATIVE_API_ID));

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, Map<String, List<Long>>> data = aggregate.values();
                assertThat(data).containsKey("downstream-subscribe-messages-total_latest");
                assertThat(data).containsKey("upstream-subscribe-messages-total_latest");
                assertThat(data).containsKey("downstream-subscribe-message-bytes_latest");
                assertThat(data).containsKey("upstream-subscribe-message-bytes_latest");
                Map<String, List<Long>> downstreamMessagesConsumedBucket = data.get("downstream-subscribe-messages-total_latest");
                assertThat(downstreamMessagesConsumedBucket).containsKey("downstream-subscribe-messages-total");
                assertThat(downstreamMessagesConsumedBucket.get("downstream-subscribe-messages-total").getFirst()).isEqualTo(4056L);
                Map<String, List<Long>> downstreamMessageBytesConsumedBucket = data.get("downstream-subscribe-message-bytes_latest");
                assertThat(downstreamMessageBytesConsumedBucket).containsKey("downstream-subscribe-message-bytes");
                assertThat(downstreamMessageBytesConsumedBucket.get("downstream-subscribe-message-bytes").getFirst()).isEqualTo(40638L);
                Map<String, List<Long>> upstreamMessagesConsumedBucket = data.get("upstream-subscribe-messages-total_latest");
                assertThat(upstreamMessagesConsumedBucket).containsKey("upstream-subscribe-messages-total");
                assertThat(upstreamMessagesConsumedBucket.get("upstream-subscribe-messages-total").getFirst()).isEqualTo(4056L);
                Map<String, List<Long>> upstreamMessageBytesConsumedBucket = data.get("upstream-subscribe-message-bytes_latest");
                assertThat(upstreamMessageBytesConsumedBucket).containsKey("upstream-subscribe-message-bytes");
                assertThat(upstreamMessageBytesConsumedBucket.get("upstream-subscribe-message-bytes").getFirst()).isEqualTo(40638L);
            });
        }

        @Test
        void should_search_top_value_hits_for_messages_produced() {
            Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.VALUE);
            Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.VALUE);
            Aggregation agg3 = new Aggregation("downstream-publish-message-bytes", AggregationType.VALUE);
            Aggregation agg4 = new Aggregation("upstream-publish-message-bytes", AggregationType.VALUE);

            var result = cut.searchEventAnalytics(QUERY_CONTEXT, buildHistogramQuery(List.of(agg1, agg2, agg3, agg4), null, NATIVE_API_ID));

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, Map<String, List<Long>>> data = aggregate.values();
                assertThat(data).containsKey("downstream-publish-messages-total_latest");
                assertThat(data).containsKey("upstream-publish-messages-total_latest");
                assertThat(data).containsKey("downstream-publish-message-bytes_latest");
                assertThat(data).containsKey("upstream-publish-message-bytes_latest");
                Map<String, List<Long>> downstreamMessagesPublishedBucket = data.get("downstream-publish-messages-total_latest");
                assertThat(downstreamMessagesPublishedBucket).containsKey("downstream-publish-messages-total");
                assertThat(downstreamMessagesPublishedBucket.get("downstream-publish-messages-total").getFirst()).isEqualTo(4056L);
                Map<String, List<Long>> downstreamMessageBytesPublishedBucket = data.get("downstream-publish-message-bytes_latest");
                assertThat(downstreamMessageBytesPublishedBucket).containsKey("downstream-publish-message-bytes");
                assertThat(downstreamMessageBytesPublishedBucket.get("downstream-publish-message-bytes").getFirst()).isEqualTo(42749L);
                Map<String, List<Long>> upstreamMessagesPublishedBucket = data.get("upstream-publish-messages-total_latest");
                assertThat(upstreamMessagesPublishedBucket).containsKey("upstream-publish-messages-total");
                assertThat(upstreamMessagesPublishedBucket.get("upstream-publish-messages-total").getFirst()).isEqualTo(4056L);
                Map<String, List<Long>> upstreamMessageBytesPublishedBucket = data.get("upstream-publish-message-bytes_latest");
                assertThat(upstreamMessageBytesPublishedBucket).containsKey("upstream-publish-message-bytes");
                assertThat(upstreamMessageBytesPublishedBucket.get("upstream-publish-message-bytes").getFirst()).isEqualTo(42749L);
            });
        }

        @Test
        void should_search_top_delta_hits_for_messages_produced() {
            Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.DELTA);
            Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.DELTA);

            var result = cut.searchEventAnalytics(QUERY_CONTEXT, buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID));

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, Map<String, List<Long>>> data = aggregate.values();
                assertThat(data).containsKey("downstream-publish-messages-total_delta");
                assertThat(data).containsKey("upstream-publish-messages-total_delta");
                Map<String, List<Long>> downstreamMessagesPublishedBucket = data.get("downstream-publish-messages-total_delta");
                assertThat(downstreamMessagesPublishedBucket).containsKey("downstream-publish-messages-total");
                assertThat(downstreamMessagesPublishedBucket.get("downstream-publish-messages-total").getFirst()).isEqualTo(2256L);
                Map<String, List<Long>> upstreamMessagesPublishedBucket = data.get("upstream-publish-messages-total_delta");
                assertThat(upstreamMessagesPublishedBucket).containsKey("upstream-publish-messages-total");
                assertThat(upstreamMessagesPublishedBucket.get("upstream-publish-messages-total").getFirst()).isEqualTo(2256L);
            });
        }

        @Test
        void should_search_top_delta_hits_for_messages_consumed() {
            Aggregation agg1 = new Aggregation("downstream-subscribe-messages-total", AggregationType.DELTA);
            Aggregation agg2 = new Aggregation("upstream-subscribe-messages-total", AggregationType.DELTA);

            var result = cut.searchEventAnalytics(QUERY_CONTEXT, buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID));

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, Map<String, List<Long>>> data = aggregate.values();
                assertThat(data).containsKey("downstream-subscribe-messages-total_delta");
                assertThat(data).containsKey("upstream-subscribe-messages-total_delta");
                Map<String, List<Long>> downstreamMessagesConsumedBucket = data.get("downstream-subscribe-messages-total_delta");
                assertThat(downstreamMessagesConsumedBucket).containsKey("downstream-subscribe-messages-total");
                assertThat(downstreamMessagesConsumedBucket.get("downstream-subscribe-messages-total").getFirst()).isEqualTo(4044L);
                Map<String, List<Long>> upstreamMessagesConsumedBucket = data.get("upstream-subscribe-messages-total_delta");
                assertThat(upstreamMessagesConsumedBucket).containsKey("upstream-subscribe-messages-total");
                assertThat(upstreamMessagesConsumedBucket.get("upstream-subscribe-messages-total").getFirst()).isEqualTo(4044L);
            });
        }

        @Test
        void should_search_top_delta_hits_for_message_bytes_produced() {
            Aggregation agg1 = new Aggregation("downstream-publish-message-bytes", AggregationType.DELTA);
            Aggregation agg2 = new Aggregation("upstream-publish-message-bytes", AggregationType.DELTA);

            var result = cut.searchEventAnalytics(QUERY_CONTEXT, buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID));

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, Map<String, List<Long>>> data = aggregate.values();
                assertThat(data).containsKey("downstream-publish-message-bytes_delta");
                assertThat(data).containsKey("upstream-publish-message-bytes_delta");
                Map<String, List<Long>> downstreamMessageBytesPublishedBucket = data.get("downstream-publish-message-bytes_delta");
                assertThat(downstreamMessageBytesPublishedBucket).containsKey("downstream-publish-message-bytes");
                assertThat(downstreamMessageBytesPublishedBucket.get("downstream-publish-message-bytes").getFirst()).isEqualTo(24059L);
                Map<String, List<Long>> upstreamMessageBytesPublishedBucket = data.get("upstream-publish-message-bytes_delta");
                assertThat(upstreamMessageBytesPublishedBucket).containsKey("upstream-publish-message-bytes");
                assertThat(upstreamMessageBytesPublishedBucket.get("upstream-publish-message-bytes").getFirst()).isEqualTo(24059L);
            });
        }

        @Test
        void should_search_top_delta_hits_for_message_bytes_consumed() {
            Aggregation agg1 = new Aggregation("downstream-subscribe-message-bytes", AggregationType.DELTA);
            Aggregation agg2 = new Aggregation("upstream-subscribe-message-bytes", AggregationType.DELTA);

            var result = cut.searchEventAnalytics(QUERY_CONTEXT, buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID));

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, Map<String, List<Long>>> data = aggregate.values();
                assertThat(data).containsKey("downstream-subscribe-message-bytes_delta");
                assertThat(data).containsKey("upstream-subscribe-message-bytes_delta");
                Map<String, List<Long>> downstreamMessageBytesConsumedBucket = data.get("downstream-subscribe-message-bytes_delta");
                assertThat(downstreamMessageBytesConsumedBucket).containsKey("downstream-subscribe-message-bytes");
                assertThat(downstreamMessageBytesConsumedBucket.get("downstream-subscribe-message-bytes").getFirst()).isEqualTo(40518L);
                Map<String, List<Long>> upstreamMessageBytesConsumedBucket = data.get("upstream-subscribe-message-bytes_delta");
                assertThat(upstreamMessageBytesConsumedBucket).containsKey("upstream-subscribe-message-bytes");
                assertThat(upstreamMessageBytesConsumedBucket.get("upstream-subscribe-message-bytes").getFirst()).isEqualTo(40518L);
            });
        }

        @Test
        void should_search_top_delta_buckets_for_messages_consumed() {
            Aggregation agg1 = new Aggregation("downstream-subscribe-messages-total", AggregationType.TREND);
            Aggregation agg2 = new Aggregation("upstream-subscribe-messages-total", AggregationType.TREND);
            HistogramQuery query = buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID);

            var result = cut.searchEventAnalytics(QUERY_CONTEXT, query);

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, Map<String, List<Long>>> data = aggregate.values();
                assertThat(data).containsKey("downstream-subscribe-messages-total_delta");
                assertThat(data).containsKey("upstream-subscribe-messages-total_delta");
                Map<String, List<Long>> downstreamConsumptionTrend = data.get("downstream-subscribe-messages-total_delta");
                assertThat(downstreamConsumptionTrend).containsKey("downstream-subscribe-messages-total");
                Map<String, List<Long>> upstreamConsumptionTrend = data.get("upstream-subscribe-messages-total_delta");
                assertThat(upstreamConsumptionTrend).containsKey("upstream-subscribe-messages-total");
                List<Long> trendValues1 = downstreamConsumptionTrend.get("downstream-subscribe-messages-total");
                List<Long> trendValues2 = upstreamConsumptionTrend.get("upstream-subscribe-messages-total");
                assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L), trendValues1);
                assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L), trendValues2);
            });
        }

        @Test
        void should_search_top_delta_buckets_for_messages_produced() {
            Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.TREND);
            Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.TREND);
            HistogramQuery query = buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID);

            var result = cut.searchEventAnalytics(QUERY_CONTEXT, query);

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, Map<String, List<Long>>> data = aggregate.values();
                assertThat(data).containsKey("downstream-publish-messages-total_delta");
                assertThat(data).containsKey("upstream-publish-messages-total_delta");
                Map<String, List<Long>> downstreamConsumptionTrend = data.get("downstream-publish-messages-total_delta");
                assertThat(downstreamConsumptionTrend).containsKey("downstream-publish-messages-total");
                Map<String, List<Long>> upstreamConsumptionTrend = data.get("upstream-publish-messages-total_delta");
                assertThat(upstreamConsumptionTrend).containsKey("upstream-publish-messages-total");
                List<Long> trendValues1 = downstreamConsumptionTrend.get("downstream-publish-messages-total");
                List<Long> trendValues2 = upstreamConsumptionTrend.get("upstream-publish-messages-total");
                assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L), trendValues1);
                assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L), trendValues2);
            });
        }

        @Test
        void should_search_top_delta_buckets_for_bytes_consumed() {
            Aggregation agg1 = new Aggregation("downstream-subscribe-message-bytes", AggregationType.TREND);
            Aggregation agg2 = new Aggregation("upstream-subscribe-message-bytes", AggregationType.TREND);
            HistogramQuery query = buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID);

            var result = cut.searchEventAnalytics(QUERY_CONTEXT, query);

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, Map<String, List<Long>>> data = aggregate.values();
                assertThat(data).containsKey("downstream-subscribe-message-bytes_delta");
                assertThat(data).containsKey("upstream-subscribe-message-bytes_delta");
                Map<String, List<Long>> downstreamConsumptionTrend = data.get("downstream-subscribe-message-bytes_delta");
                assertThat(downstreamConsumptionTrend).containsKey("downstream-subscribe-message-bytes");
                Map<String, List<Long>> upstreamConsumptionTrend = data.get("upstream-subscribe-message-bytes_delta");
                assertThat(upstreamConsumptionTrend).containsKey("upstream-subscribe-message-bytes");
                List<Long> trendValues1 = downstreamConsumptionTrend.get("downstream-subscribe-message-bytes");
                List<Long> trendValues2 = upstreamConsumptionTrend.get("upstream-subscribe-message-bytes");
                assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L), trendValues1);
                assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L), trendValues2);
            });
        }

        @Test
        void should_search_top_delta_buckets_for_bytes_produced() {
            Aggregation agg1 = new Aggregation("downstream-publish-message-bytes", AggregationType.TREND);
            Aggregation agg2 = new Aggregation("upstream-publish-message-bytes", AggregationType.TREND);
            HistogramQuery query = buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID);

            var result = cut.searchEventAnalytics(QUERY_CONTEXT, query);

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, Map<String, List<Long>>> data = aggregate.values();
                assertThat(data).containsKey("downstream-publish-message-bytes_delta");
                assertThat(data).containsKey("upstream-publish-message-bytes_delta");
                Map<String, List<Long>> downstreamConsumptionTrend = data.get("downstream-publish-message-bytes_delta");
                assertThat(downstreamConsumptionTrend).containsKey("downstream-publish-message-bytes");
                Map<String, List<Long>> upstreamConsumptionTrend = data.get("upstream-publish-message-bytes_delta");
                assertThat(upstreamConsumptionTrend).containsKey("upstream-publish-message-bytes");
                List<Long> trendValues1 = downstreamConsumptionTrend.get("downstream-publish-message-bytes");
                List<Long> trendValues2 = upstreamConsumptionTrend.get("upstream-publish-message-bytes");
                assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L), trendValues1);
                assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L), trendValues2);
            });
        }

        @Test
        void should_apply_optional_filters() {
            Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.VALUE);
            Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.VALUE);
            Aggregation agg3 = new Aggregation("downstream-publish-message-bytes", AggregationType.VALUE);
            Aggregation agg4 = new Aggregation("upstream-publish-message-bytes", AggregationType.VALUE);
            Term appIdFilter = new Term("app-id", "yyy-yyy-yyy");

            var result = cut.searchEventAnalytics(
                QUERY_CONTEXT,
                buildHistogramQuery(List.of(agg1, agg2, agg3, agg4), List.of(appIdFilter), "xxx-xxx-xxx")
            );

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, Map<String, List<Long>>> data = aggregate.values();
                assertThat(data).containsKey("downstream-publish-messages-total_latest");
                assertThat(data).containsKey("upstream-publish-messages-total_latest");
                assertThat(data).containsKey("downstream-publish-message-bytes_latest");
                assertThat(data).containsKey("upstream-publish-message-bytes_latest");
                Map<String, List<Long>> downstreamMessagesPublishedBucket = data.get("downstream-publish-messages-total_latest");
                assertThat(downstreamMessagesPublishedBucket).containsKey("downstream-publish-messages-total");
                assertThat(downstreamMessagesPublishedBucket.get("downstream-publish-messages-total").getFirst()).isEqualTo(157L);
                Map<String, List<Long>> downstreamMessageBytesPublishedBucket = data.get("downstream-publish-message-bytes_latest");
                assertThat(downstreamMessageBytesPublishedBucket).containsKey("downstream-publish-message-bytes");
                assertThat(downstreamMessageBytesPublishedBucket.get("downstream-publish-message-bytes").getFirst()).isEqualTo(8421L);
                Map<String, List<Long>> upstreamMessagesPublishedBucket = data.get("upstream-publish-messages-total_latest");
                assertThat(upstreamMessagesPublishedBucket).containsKey("upstream-publish-messages-total");
                assertThat(upstreamMessagesPublishedBucket.get("upstream-publish-messages-total").getFirst()).isEqualTo(157L);
                Map<String, List<Long>> upstreamMessageBytesPublishedBucket = data.get("upstream-publish-message-bytes_latest");
                assertThat(upstreamMessageBytesPublishedBucket).containsKey("upstream-publish-message-bytes");
                assertThat(upstreamMessageBytesPublishedBucket.get("upstream-publish-message-bytes").getFirst()).isEqualTo(8421L);
            });
        }

        private static @NotNull HistogramQuery buildHistogramQuery(List<Aggregation> aggregations, List<Term> terms, String apiId) {
            var now = TIME_PROVIDER.getNow();
            var from = now.minusSeconds(4 * 60);
            var to = now.plusSeconds(6 * 60);

            return new HistogramQuery(
                new SearchTermId(SearchTermId.SearchTerm.API, apiId),
                new TimeRange(from, to, Duration.ofMillis(2 * 60 * 1000)),
                aggregations,
                null,
                terms
            );
        }
    }
}
