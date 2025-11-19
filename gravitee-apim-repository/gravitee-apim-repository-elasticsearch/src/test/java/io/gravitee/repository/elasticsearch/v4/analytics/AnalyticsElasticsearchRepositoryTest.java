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
import static io.gravitee.repository.elasticsearch.TestConfiguration.DEFAULT_SEARCH_TYPE;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.assertj.core.api.Assertions.not;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Assertions.withPrecision;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.NumberRange;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
                    assertThat(aggregate.values()).containsExactlyInAnyOrderEntriesOf(
                        Map.of("http-get", 1L, "sse", 1L, "webhook", 1L, "websocket", 2L)
                    );
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
                    assertThat(aggregate.values()).containsKeys("http-get", "http-post", "sse", "webhook", "websocket");
                    assertThat(aggregate.order()).containsExactlyInAnyOrder("http-get", "http-post", "sse", "webhook", "websocket");
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
                    assertThat(aggregate.values()).containsKeys("http-get", "http-post", "sse", "webhook", "websocket");
                    assertThat(aggregate.order()).containsExactlyInAnyOrder("http-get", "http-post", "sse", "webhook", "websocket");
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
                    assertThat(stats.count()).isEqualTo(8.0f);
                    assertThat(stats.sum()).isEqualTo(131864.0f);
                    assertThat(stats.avg()).isEqualTo(16483.0f);
                    assertThat(stats.min()).isEqualTo(19.0f);
                    assertThat(stats.max()).isEqualTo(60000.0f);
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
                    assertThat(stats.count()).isEqualTo(2.0f);
                    assertThat(stats.sum()).isEqualTo(70000.0f);
                    assertThat(stats.avg()).isEqualTo(35000.0f);
                    assertThat(stats.min()).isEqualTo(30000.0f);
                    assertThat(stats.max()).isEqualTo(40000.0f);
                    assertThat(stats.rps()).isEqualTo(1.1574074E-5f);
                    assertThat(stats.rpm()).isEqualTo(6.9444446E-4f);
                    assertThat(stats.rph()).isEqualTo(0.041666668f);
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
        private static final Instant NOW = TIME_PROVIDER.getNow();

        @Value("${search.type:" + DEFAULT_SEARCH_TYPE + "}")
        private String searchType;

        @BeforeEach
        void beforeEach() {
            // Event Analytics can only be executed on Elasticsearch.
            assumeTrue(searchType.equals(DEFAULT_SEARCH_TYPE));
        }

        @Test
        void should_return_latest_value_summed_per_key() {
            // VALUE = sum(latest per key ≤ end) → 2 (gw=1 at now-1) + 4 (gw=2 at now+1, end includes) = 6
            Aggregation agg1 = new Aggregation("downstream-active-connections", AggregationType.VALUE);
            Aggregation agg2 = new Aggregation("upstream-active-connections", AggregationType.VALUE);

            var result = cut.searchEventAnalytics(
                QUERY_CONTEXT,
                buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID, NOW.minusSeconds(360), NOW.plusSeconds(360), null)
            );

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, List<Long>> data = aggregate.values();
                assertThat(data).containsKey("downstream-active-connections");
                assertThat(data).containsKey("upstream-active-connections");
                assertThat(data.get("downstream-active-connections").getFirst()).isEqualTo(6L);
                assertThat(data.get("upstream-active-connections").getFirst()).isEqualTo(6L);
            });
        }

        @Test
        void should_return_latest_value_ignoring_keys_with_no_snapshot_at_end_time() {
            // VALUE = sum(latest per key ≤ end) → gw=1: 2 (now-1), gw=2: 0 (latest at end is 0; now+1 is excluded) ⇒ 2
            var from = NOW.minus(Duration.ofMinutes(6));
            // excludes the NOW+1 sample
            Aggregation valueAgg = new Aggregation("downstream-active-connections", AggregationType.VALUE);

            var result = cut.searchEventAnalytics(
                new QueryContext("DEFAULT", "DEFAULT"),
                buildHistogramQuery(List.of(valueAgg), null, "273f4728-1e30-4c78-bf47-281e304c78a5", from, NOW, null)
            );

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, List<Long>> data = aggregate.values();
                assertThat(data).containsKey("downstream-active-connections");
                assertThat(data.get("downstream-active-connections").getFirst()).isEqualTo(2L);
            });
        }

        @Test
        void should_return_empty_as_delta_when_no_documents_before_end_time() {
            // DELTA requires at least one snapshot ≤ end; none found ⇒ empty result
            Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.DELTA);
            Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.DELTA);

            var result = cut.searchEventAnalytics(
                QUERY_CONTEXT,
                buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID, NOW.minusSeconds(600), NOW.minusSeconds(540), null)
            );

            assertThat(result).isEmpty();
        }

        @Test
        void should_compute_monotonic_increase_happy_path_for_single_key_both_directions() {
            // Monotonic DELTA (single key) = 50 − 10 = 40 per direction for gw-id=1, topic=1.
            Aggregation deltaDown = new Aggregation("downstream-publish-messages-total", AggregationType.DELTA);
            Aggregation deltaUp = new Aggregation("upstream-publish-messages-total", AggregationType.DELTA);

            // Pick start strictly after the 10 snapshot, end just after the 50 snapshot.
            Instant from = NOW.minusSeconds(4 * 60 - 1); // 1s after nowMinus4
            Instant to = NOW.plusSeconds(4 * 60 + 1); // just after nowPlus4, still before nowPlus5

            var result = cut.searchEventAnalytics(
                QUERY_CONTEXT,
                buildHistogramQuery(
                    List.of(deltaDown, deltaUp),
                    List.of(new Term("gw-id", "1"), new Term("topic", "1")),
                    NATIVE_API_ID,
                    from,
                    to,
                    null
                )
            );

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, List<Long>> data = aggregate.values();
                assertThat(data.get("downstream-publish-messages-total").getFirst()).isEqualTo(40L);
                assertThat(data.get("upstream-publish-messages-total").getFirst()).isEqualTo(40L);
            });
        }

        @Test
        void should_return_end_value_when_no_documents_before_start_time() {
            // DELTA (no start snapshot) over window covering end=50 for gw=1 and gw=2 ⇒ sum per direction = 50 + 50 = 100
            Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.DELTA);
            Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.DELTA);
            var start = NOW.minusSeconds(360);
            var end = NOW.plusSeconds(360);

            var result = cut.searchEventAnalytics(
                QUERY_CONTEXT,
                buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID, start, end, null)
            );

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, List<Long>> data = aggregate.values();
                assertThat(data.get("downstream-publish-messages-total").getFirst()).isEqualTo(100L);
                assertThat(data.get("upstream-publish-messages-total").getFirst()).isEqualTo(100L);
            });
        }

        @Test
        void should_return_delta_when_documents_present_before_start_time() {
            // DELTA = end − start when start snapshot exists
            // Here: 90 − 10 = 80 per direction (monotonic window)
            Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.DELTA);
            Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.DELTA);

            var result = cut.searchEventAnalytics(
                QUERY_CONTEXT,
                buildHistogramQuery(List.of(agg1, agg2), null, NATIVE_API_ID, NOW.minusSeconds(180), NOW.plusSeconds(360), null)
            );

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, List<Long>> data = aggregate.values();
                assertThat(data).containsKey("downstream-publish-messages-total");
                assertThat(data).containsKey("upstream-publish-messages-total");
                assertThat(data.get("downstream-publish-messages-total").getFirst()).isEqualTo(80L);
                assertThat(data.get("upstream-publish-messages-total").getFirst()).isEqualTo(80L);
            });
        }

        @Test
        void should_handle_counter_reset_with_reset_aware_end_value() {
            // Reset-aware: if end < start for a key, delta = end (not negative)
            // Here: 5 < 50 ⇒ delta=5 per direction
            Aggregation deltaDown = new Aggregation("downstream-publish-messages-total", AggregationType.DELTA);
            Aggregation deltaUp = new Aggregation("upstream-publish-messages-total", AggregationType.DELTA);

            Instant from = NOW.minusSeconds(13 * 60);
            Instant to = NOW.minusSeconds(10 * 60);

            var result = cut.searchEventAnalytics(
                new QueryContext("DEFAULT", "DEFAULT"),
                buildHistogramQuery(List.of(deltaDown, deltaUp), List.of(new Term("topic", "reset-key")), NATIVE_API_ID, from, to, null)
            );

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, List<Long>> data = aggregate.values();
                assertThat(data.get("downstream-publish-messages-total").getFirst()).isEqualTo(5L);
                assertThat(data.get("upstream-publish-messages-total").getFirst()).isEqualTo(5L);
            });
        }

        @Test
        void should_build_trend_series_aligned_to_interval() {
            Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.TREND);
            Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.TREND);
            HistogramQuery query = buildHistogramQuery(
                List.of(agg1, agg2),
                null,
                NATIVE_API_ID,
                NOW.minusSeconds(360),
                NOW.plusSeconds(360),
                Duration.ofMinutes(1)
            );

            var result = cut.searchEventAnalytics(QUERY_CONTEXT, query);

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, List<Long>> data = aggregate.values();
                List<Long> trend = new ArrayList<>(Arrays.asList(null, 0L, 0L, null, null, null, null, null, null, null, 40L, 40L, null));
                assertThat(data.get("downstream-publish-messages-total")).isEqualTo(trend);
                assertThat(data.get("upstream-publish-messages-total")).isEqualTo(trend);
            });
        }

        @Test
        void should_apply_optional_terms_filters_with_or_semantics() {
            Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.DELTA);
            Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.DELTA);

            var result = cut.searchEventAnalytics(
                QUERY_CONTEXT,
                buildHistogramQuery(
                    List.of(agg1, agg2),
                    List.of(new Term("topic", "2")),
                    NATIVE_API_ID,
                    NOW.minusSeconds(360),
                    NOW.minusSeconds(180),
                    null
                )
            );

            assertThat(result).hasValueSatisfying(aggregate -> {
                Map<String, List<Long>> data = aggregate.values();
                assertThat(data).containsKey("downstream-publish-messages-total");
                assertThat(data).containsKey("upstream-publish-messages-total");
                assertThat(data.get("downstream-publish-messages-total").getFirst()).isEqualTo(10L);
                assertThat(data.get("upstream-publish-messages-total").getFirst()).isEqualTo(10L);
            });
        }

        private static @NotNull HistogramQuery buildHistogramQuery(
            List<Aggregation> aggregations,
            List<Term> terms,
            String apiId,
            Instant from,
            Instant to,
            Duration interval
        ) {
            return new HistogramQuery(
                new SearchTermId(SearchTermId.SearchTerm.API, apiId),
                new TimeRange(from, to, interval == null ? Optional.empty() : Optional.of(interval)),
                aggregations,
                null,
                terms
            );
        }
    }

    @Nested
    class Engine {

        private static final QueryContext QUERY_CONTEXT = new QueryContext("DEFAULT", "DEFAULT");

        private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.DAYS);
        private static final Instant TOMORROW = NOW.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
        private static final Instant YESTERDAY = NOW.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);

        static io.gravitee.repository.analytics.engine.api.query.TimeRange buildTimeRange() {
            return new io.gravitee.repository.analytics.engine.api.query.TimeRange(YESTERDAY, TOMORROW);
        }

        @Nested
        class HTTPMeasures {

            static MeasuresQuery buildQuery() {
                var timeRange = buildTimeRange();
                var metrics = List.of(
                    new MetricMeasuresQuery(Metric.HTTP_GATEWAY_RESPONSE_TIME, Set.of(Measure.AVG)),
                    new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.RPS)),
                    new MetricMeasuresQuery(Metric.HTTP_ERRORS, Set.of(Measure.PERCENTAGE))
                );

                var filter = new Filter(
                    Filter.Name.API,
                    Filter.Operator.IN,
                    List.of("4a6895d5-a1bc-4041-a895-d5a1bce041ae", "f1608475-dd77-4603-a084-75dd775603e9")
                );

                return new MeasuresQuery(timeRange, List.of(filter), metrics);
            }

            @Test
            void should_return_http_measures() {
                var query = buildQuery();
                var result = cut.searchHTTPMeasures(QUERY_CONTEXT, query);

                assertThat(result).isNotNull();

                var measures = result.measures();
                assertThat(measures).hasSize(query.metrics().size());

                for (var measure : measures) {
                    assertThat(measure.measures().values()).hasSize(1);
                    assertThat(measure.measures().values().iterator().next()).satisfies(n -> assertThat(n.doubleValue()).isNotZero());
                }
            }
        }

        @Nested
        class HTTPFacets {

            static FacetsQuery buildQuery() {
                var timeRange = buildTimeRange();
                var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));

                var ranges = List.of(
                    new NumberRange(100, 199),
                    new NumberRange(200, 299),
                    new NumberRange(300, 399),
                    new NumberRange(400, 499),
                    new NumberRange(500, 599)
                );

                var filter = new Filter(
                    Filter.Name.API,
                    Filter.Operator.IN,
                    List.of("4a6895d5-a1bc-4041-a895-d5a1bce041ae", "f1608475-dd77-4603-a084-75dd775603e9")
                );

                return new FacetsQuery(timeRange, List.of(filter), metrics, List.of(Facet.HTTP_STATUS), ranges);
            }

            @Test
            void should_return_http_facet_buckets() {
                var query = buildQuery();
                var result = cut.searchHTTPFacets(QUERY_CONTEXT, query);

                assertThat(result).isNotNull();

                assertThat(result.metrics()).hasSize(1);

                var metric = result.metrics().getFirst();

                assertThat(metric.metric()).isEqualTo(Metric.HTTP_REQUESTS);

                assertThat(metric.buckets()).hasSize(5);

                assertThat(metric.buckets()).satisfiesExactly(
                    bucket -> assertThat(bucket.key()).isEqualTo("100-199"),
                    bucket -> assertThat(bucket.key()).isEqualTo("200-299"),
                    bucket -> assertThat(bucket.key()).isEqualTo("300-399"),
                    bucket -> assertThat(bucket.key()).isEqualTo("400-499"),
                    bucket -> assertThat(bucket.key()).isEqualTo("500-599")
                );
            }
        }

        @Nested
        class HTTPTimeSeries {

            static TimeSeriesQuery buildQuery() {
                var timeRange = buildTimeRange();
                var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
                var interval = Duration.ofHours(1).toMillis();
                var filter = new Filter(
                    Filter.Name.API,
                    Filter.Operator.IN,
                    List.of("4a6895d5-a1bc-4041-a895-d5a1bce041ae", "f1608475-dd77-4603-a084-75dd775603e9")
                );
                return new TimeSeriesQuery(timeRange, List.of(filter), interval, metrics);
            }

            @Test
            void should_return_http_time_series_buckets() {
                var query = buildQuery();
                var result = cut.searchHTTPTimeSeries(QUERY_CONTEXT, query);

                assertThat(result).isNotNull();

                assertThat(result.metrics()).hasSize(1);

                assertThat(result.metrics().getFirst().buckets()).isNotEmpty();
            }
        }
    }
}
