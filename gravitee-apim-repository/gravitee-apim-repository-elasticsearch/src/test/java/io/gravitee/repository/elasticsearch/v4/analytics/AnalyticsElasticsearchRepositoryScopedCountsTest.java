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
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.metric.MetricName;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetName;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetBucketResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MetricTimeSeriesResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesBucketResult;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * The built-in counts that select a kind of request rather than read a value, on the documents of {@code v4-metrics.ftl}
 * fourteen days back. Under {@code error-counts-api-001}: three 200, a 302, a 401, a 404, a 500, a 503 and a message
 * API connection answered 500, whose opening and end are both stored (nine requests, five errors). Under
 * {@code edge-counts-api-001}: three Edge heartbeats (two from {@code device-a}, one from {@code device-b}) and two
 * detections ({@code device-a}, {@code device-c}).
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
class AnalyticsElasticsearchRepositoryScopedCountsTest extends AbstractElasticsearchRepositoryTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("DEFAULT", "DEFAULT");
    private static final Instant DAY = TimeProvider.now().truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(14));
    private static final TimeRange TIME_RANGE = new TimeRange(DAY, DAY.plus(Duration.ofDays(1)));
    private static final TimeRange MORNING = new TimeRange(DAY.plus(Duration.ofHours(8)), DAY.plus(Duration.ofHours(10)).minusMillis(1));

    private static final Filter ERROR_COUNTS_API = new Filter(Filter.Name.API, Filter.Operator.IN, List.of("error-counts-api-001"));
    private static final Filter EDGE_COUNTS_API = new Filter(Filter.Name.API, Filter.Operator.IN, List.of("edge-counts-api-001"));
    private static final Filter EDGE_ENTRYPOINT = new Filter(Filter.Name.ENTRYPOINT, Filter.Operator.EQ, "edge");

    private static final MetricMeasuresQuery REQUESTS = new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT));
    private static final MetricMeasuresQuery ERRORS = new MetricMeasuresQuery(Metric.HTTP_ERRORS, Set.of(Measure.COUNT));
    private static final MetricMeasuresQuery ERROR_RATE = new MetricMeasuresQuery(Metric.HTTP_ERROR_RATE, Set.of(Measure.PERCENTAGE));
    private static final MetricMeasuresQuery HEARTBEATS = new MetricMeasuresQuery(Metric.EDGE_HEARTBEAT_COUNT, Set.of(Measure.COUNT));

    @Autowired
    private AnalyticsElasticsearchRepository cut;

    @Nested
    class HttpErrors {

        @Test
        void should_count_the_requests_answered_with_a_status_of_400_or_more() {
            var result = measures(List.of(ERROR_COUNTS_API), REQUESTS, ERRORS, ERROR_RATE);

            assertThat(count(result, Metric.HTTP_REQUESTS)).isEqualTo(9L);
            assertThat(count(result, Metric.HTTP_ERRORS)).isEqualTo(5L);
        }

        @Test
        void should_count_as_many_errors_as_the_error_rate_reads() {
            var result = measures(List.of(ERROR_COUNTS_API), REQUESTS, ERRORS, ERROR_RATE);

            var rate = measuresOf(result, Metric.HTTP_ERROR_RATE).get(Measure.PERCENTAGE).doubleValue();
            assertThat((double) count(result, Metric.HTTP_ERRORS)).isCloseTo(
                (count(result, Metric.HTTP_REQUESTS) * rate) / 100,
                within(1e-9)
            );
        }

        @Test
        void should_count_the_same_errors_when_the_caller_also_filters_on_the_error_status_groups() {
            var filtered = new MetricMeasuresQuery(
                Metric.HTTP_ERRORS,
                Set.of(Measure.COUNT),
                List.of(new Filter(Filter.Name.HTTP_STATUS_CODE_GROUP, Filter.Operator.IN, List.of("4XX", "5XX"))),
                List.of()
            );

            assertThat(count(measures(List.of(ERROR_COUNTS_API), filtered), Metric.HTTP_ERRORS)).isEqualTo(5L);
        }

        @Test
        void should_hold_only_error_statuses_in_its_facets() {
            var buckets = facets(List.of(ERROR_COUNTS_API), ERRORS, List.of(Facet.HTTP_STATUS), 10);

            assertThat(buckets)
                .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
                .containsExactlyInAnyOrder(tuple("401", 1L), tuple("404", 1L), tuple("500", 2L), tuple("503", 1L));
        }

        @Test
        void should_count_no_error_in_a_success_or_redirection_status_group() {
            var buckets = facets(List.of(ERROR_COUNTS_API), ERRORS, List.of(Facet.HTTP_STATUS_CODE_GROUP), 10);

            assertThat(buckets)
                .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
                .containsExactlyInAnyOrder(
                    tuple("100-199", 0L),
                    tuple("200-299", 0L),
                    tuple("300-399", 0L),
                    tuple("400-499", 2L),
                    tuple("500-599", 3L)
                );
        }

        /** Three 200 outnumber every error status: ranked over every request, 200 would take the only place. */
        @Test
        void should_rank_its_facets_over_the_error_requests_only() {
            var buckets = facets(List.of(ERROR_COUNTS_API), ERRORS, List.of(Facet.HTTP_STATUS), 1);

            assertThat(buckets)
                .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
                .containsExactly(tuple("500", 2L));
        }

        @Test
        void should_count_the_errors_of_each_time_bucket() {
            var result = cut.searchHTTPTimeSeries(
                QUERY_CONTEXT,
                new TimeSeriesQuery(MORNING, List.of(ERROR_COUNTS_API), Duration.ofHours(1).toMillis(), List.of(REQUESTS, ERRORS))
            );

            assertThat(timeSeriesCounts(result.metrics(), Metric.HTTP_REQUESTS)).containsExactly(9.0, 0.0);
            assertThat(timeSeriesCounts(result.metrics(), Metric.HTTP_ERRORS)).containsExactly(5.0, 0.0);
        }

        @Test
        void should_hold_only_error_statuses_in_the_facets_of_a_time_bucket() {
            var result = cut.searchHTTPTimeSeries(
                QUERY_CONTEXT,
                new TimeSeriesQuery(
                    MORNING,
                    List.of(ERROR_COUNTS_API),
                    Duration.ofHours(1).toMillis(),
                    List.of(ERRORS),
                    List.of(Facet.HTTP_STATUS)
                )
            );

            assertThat(result.metrics().getFirst().buckets().getFirst().buckets())
                .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
                .containsExactlyInAnyOrder(tuple("401", 1L), tuple("404", 1L), tuple("500", 2L), tuple("503", 1L));
        }
    }

    @Nested
    class EdgeHeartbeatCount {

        @Test
        void should_count_no_heartbeat_among_requests_that_are_not_edge_heartbeats() {
            assertThat(count(measures(List.of(ERROR_COUNTS_API), HEARTBEATS), Metric.EDGE_HEARTBEAT_COUNT)).isZero();
        }

        @Test
        void should_count_the_heartbeats_and_leave_the_detections_out() {
            var result = measures(List.of(EDGE_COUNTS_API, EDGE_ENTRYPOINT), HEARTBEATS);

            assertThat(count(result, Metric.EDGE_HEARTBEAT_COUNT)).isEqualTo(3L);
        }

        @Test
        void should_count_the_heartbeats_of_each_time_bucket() {
            var result = cut.searchHTTPTimeSeries(
                QUERY_CONTEXT,
                new TimeSeriesQuery(MORNING, List.of(EDGE_COUNTS_API, EDGE_ENTRYPOINT), Duration.ofHours(1).toMillis(), List.of(HEARTBEATS))
            );

            assertThat(timeSeriesCounts(result.metrics(), Metric.EDGE_HEARTBEAT_COUNT)).containsExactly(3.0, 0.0);
        }

        @Test
        void should_hold_only_the_devices_sending_heartbeats_in_its_edge_facets() {
            var buckets = cut
                .searchEdgeFacets(
                    QUERY_CONTEXT,
                    new FacetsQuery(TIME_RANGE, List.of(EDGE_COUNTS_API), List.of(HEARTBEATS), List.of(Facet.EDGE_CLIENT), 10)
                )
                .metrics()
                .getFirst()
                .buckets();

            assertThat(buckets)
                .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
                .containsExactlyInAnyOrder(tuple("device-a", 2L), tuple("device-b", 1L));
        }
    }

    private MeasuresResult measures(List<Filter> filters, MetricMeasuresQuery... metrics) {
        return cut.searchHTTPMeasures(QUERY_CONTEXT, new MeasuresQuery(TIME_RANGE, filters, List.of(metrics)));
    }

    private List<FacetBucketResult> facets(List<Filter> filters, MetricMeasuresQuery metric, List<FacetName> facets, int limit) {
        return cut
            .searchHTTPFacets(QUERY_CONTEXT, new FacetsQuery(TIME_RANGE, filters, List.of(metric), facets, limit))
            .metrics()
            .getFirst()
            .buckets();
    }

    private static long count(MeasuresResult result, MetricName metric) {
        return measuresOf(result, metric).get(Measure.COUNT).longValue();
    }

    private static Map<Measure, Number> measuresOf(MeasuresResult result, MetricName metric) {
        return result
            .measures()
            .stream()
            .filter(measures -> measures.metric().equals(metric))
            .findFirst()
            .orElseThrow()
            .measures();
    }

    private static List<Double> timeSeriesCounts(List<MetricTimeSeriesResult> metrics, MetricName metric) {
        return metrics
            .stream()
            .filter(series -> series.metric().equals(metric))
            .findFirst()
            .orElseThrow()
            .buckets()
            .stream()
            .map(TimeSeriesBucketResult::measures)
            .map(measures -> measures.get(Measure.COUNT).doubleValue())
            .toList();
    }
}
