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

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.log.v4.model.analytics.Aggregation;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.gravitee.repository.log.v4.model.analytics.TopFailedAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopFailedQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private static final String APIV2_1 = "e2c0ecd5-893a-458d-80ec-d5893ab58d12";
    private static final String APIV2_2 = "4d8d6ca8-c2c7-4ab8-8d6c-a8c2c79ab8a1";
    private static final String API_ID_SSE = "43276d60-7058-4588-8023-52bf1154f903";

    @Autowired
    private AnalyticsElasticsearchRepository cut;

    @Nested
    class RequestsCount {

        @Test
        void should_return_all_the_requests_count_by_entrypoint_for_a_given_api() {
            var result = cut.searchRequestsCount(new QueryContext("org#1", "env#1"), new RequestsCountQuery(API_ID), "user_agent.name");

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
                new AverageMessagesPerRequestQuery(API_ID)
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
                new AverageConnectionDurationQuery(API_ID)
            );

            assertThat(result)
                .hasValueSatisfying(averageAggregate -> {
                    assertThat(averageAggregate.getAverage()).isCloseTo(20261.25, offset(0.1d));
                    assertThat(averageAggregate.getAverageBy())
                        .containsAllEntriesOf(Map.of("http-get", 30_000.0, "websocket", 50_000.0, "sse", 645.0, "http-post", 400.0));
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

            assertThat(result)
                .hasValueSatisfying(averageAggregate -> {
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
        void should_return_response_status_for_V4_and_V2_definitions() {
            var result = cut.searchResponseStatusRanges(
                new QueryContext("org#1", "env#1"),
                ResponseStatusQueryCriteria
                    .builder()
                    .apiIds(List.of(API_ID, APIV2_1, APIV2_2))
                    .definitionVersions(EnumSet.of(V4, V2))
                    .build()
            );

            assertThat(result)
                .hasValueSatisfying(responseStatusAggregate -> {
                    assertRanges(responseStatusAggregate.getRanges(), 7L, 10L);
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

            assertThat(result)
                .isNotNull()
                .get()
                .extracting(ResponseStatusRangesAggregate::getRanges)
                .isEqualTo(Map.of("100.0-200.0", 0L, "200.0-300.0", 0L, "300.0-400.0", 0L, "400.0-500.0", 0L, "500.0-600.0", 0L));
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
            double[] array = result.getAverageBy().values().stream().mapToDouble(l -> l).filter(d -> d > 0).toArray();
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
                ResponseStatusOverTimeQuery
                    .builder()
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
                softly.assertThat(result.getStatusCount().get("200").stream().mapToLong(l -> l).sum()).isEqualTo(5);
                softly.assertThat(result.getStatusCount().get("200")).hasSize((int) nbBuckets).haveAtMost(5, not(is(0)));
                softly
                    .assertThat(result.getStatusCount().get("202"))
                    .hasSize((int) nbBuckets)
                    .haveExactly(1, is(1))
                    .haveAtMost(1, not(is(0)));
                softly.assertThat(result.getStatusCount().get("404").stream().mapToLong(l -> l).sum()).isEqualTo(2);
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

            var query = io.gravitee.repository.log.v4.model.analytics.HistogramQuery
                .builder()
                .from(from)
                .to(to)
                .interval(interval)
                .apiId(API_ID)
                .aggregations(List.of(new Aggregation("status", AggregationType.FIELD)))
                .build();

            var result = cut.searchHistogram(new QueryContext("org#1", "env#1"), query);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);

            var histogram = result.getFirst();
            assertThat(histogram.getBuckets()).isNotNull();
            assertThat(histogram.getBuckets()).hasSize(3);

            assertThat(histogram.getBuckets().keySet()).containsExactlyInAnyOrder("200", "202", "404");

            var bucket200 = histogram.getBuckets().get("200");
            var bucket202 = histogram.getBuckets().get("202");
            var bucket404 = histogram.getBuckets().get("404");

            assertThat(bucket200).hasSizeGreaterThan(28);
            assertThat(bucket200.get(28)).isEqualTo(1L);

            assertThat(bucket202).hasSizeGreaterThan(61);
            assertThat(bucket202.get(61)).isEqualTo(1L);

            assertThat(bucket404).hasSizeGreaterThan(61);
            assertThat(bucket404.get(61)).isEqualTo(2L);
        }
    }
}
