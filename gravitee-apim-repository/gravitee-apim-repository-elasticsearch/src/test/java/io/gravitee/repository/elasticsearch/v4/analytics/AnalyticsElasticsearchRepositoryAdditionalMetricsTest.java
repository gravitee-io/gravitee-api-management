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

import io.gravitee.repository.analytics.engine.api.metric.AdditionalMetric;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.metric.MetricName;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField.ValueType;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetName;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.NumberRange;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetBucketResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Real request documents carrying additional metrics, filtered, grouped and measured through the HTTP path:
 * {@code am-001} to {@code am-003} carry the fields and answered 200, {@code am-004} carries none and answered
 * 500. They live in {@code v4-metrics.ftl} in a day
 * of their own, ten days back, under an API of their own, so the fixtures other suites count are untouched.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
class AnalyticsElasticsearchRepositoryAdditionalMetricsTest extends AbstractElasticsearchRepositoryTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("DEFAULT", "DEFAULT");
    private static final String API_ID = "additional-metrics-api-001";
    private static final Instant DAY = TimeProvider.now().truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(10));
    private static final TimeRange TIME_RANGE = new TimeRange(DAY, DAY.plus(Duration.ofDays(1)));

    private static final AdditionalMetricField TIER = new AdditionalMetricField(ValueType.KEYWORD, "keyword_fraud-check_tier");
    private static final AdditionalMetricField FAILOVERS = new AdditionalMetricField(ValueType.LONG, "long_failover_count");
    private static final AdditionalMetricField SCORE = new AdditionalMetricField(ValueType.DOUBLE, "double_fraud-check_score");
    private static final AdditionalMetricField CACHE_HIT = new AdditionalMetricField(ValueType.BOOL, "bool_cache_hit");
    private static final AdditionalMetricField NEVER_WRITTEN = new AdditionalMetricField(ValueType.KEYWORD, "keyword_never_written");

    private static final AdditionalMetric FAILOVER_METRIC = new AdditionalMetric(FAILOVERS);
    private static final AdditionalMetric SCORE_METRIC = new AdditionalMetric(SCORE);

    private static final MetricMeasuresQuery REQUESTS = new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT));
    private static final Filter BY_API = new Filter(Filter.Name.API, Filter.Operator.IN, List.of(API_ID));

    @Autowired
    private AnalyticsElasticsearchRepository cut;

    @Test
    void should_count_only_the_requests_matching_a_keyword() {
        var filter = new Filter(TIER, Filter.Operator.EQ, "high");

        assertThat(count(List.of(BY_API, filter))).isEqualTo(2L);
    }

    @Test
    void should_count_only_the_requests_matching_one_of_several_keywords() {
        var filter = new Filter(TIER, Filter.Operator.IN, List.of("high", "low"));

        assertThat(count(List.of(BY_API, filter))).isEqualTo(3L);
    }

    @Test
    void should_count_only_the_requests_within_number_bounds() {
        var atLeastOne = new Filter(FAILOVERS, Filter.Operator.GTE, "1");
        var belowHalf = new Filter(SCORE, Filter.Operator.LTE, "0.5");

        assertThat(count(List.of(BY_API, atLeastOne))).isEqualTo(2L);
        assertThat(count(List.of(BY_API, belowHalf))).isEqualTo(2L);
    }

    @Test
    void should_count_only_the_requests_matching_a_boolean() {
        var filter = new Filter(CACHE_HIT, Filter.Operator.EQ, "true");

        assertThat(count(List.of(BY_API, filter))).isEqualTo(2L);
    }

    @Test
    void should_count_nothing_on_a_field_no_document_carries() {
        var filter = new Filter(NEVER_WRITTEN, Filter.Operator.EQ, "anything");

        assertThat(count(List.of(BY_API, filter))).isZero();
    }

    @Test
    void should_return_one_bucket_per_observed_keyword() {
        var result = cut.searchHTTPFacets(QUERY_CONTEXT, new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(REQUESTS), List.of(TIER)));

        assertThat(result.metrics().getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .containsExactlyInAnyOrder(tuple("high", 2L), tuple("low", 1L));
    }

    @Test
    void should_group_by_a_keyword_under_a_built_in_facet() {
        List<FacetName> facets = List.of(Facet.API, TIER);

        var result = cut.searchHTTPFacets(QUERY_CONTEXT, new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(REQUESTS), facets));

        var api = result.metrics().getFirst().buckets().getFirst();
        assertThat(api.key()).isEqualTo(API_ID);
        assertThat(api.buckets()).extracting(FacetBucketResult::key).containsExactlyInAnyOrder("high", "low");
    }

    @Test
    void should_return_two_buckets_for_a_boolean() {
        var result = cut.searchHTTPFacets(
            QUERY_CONTEXT,
            new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(REQUESTS), List.of(CACHE_HIT))
        );

        assertThat(result.metrics().getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .containsExactlyInAnyOrder(tuple("true", 2L), tuple("false", 1L));
    }

    @Test
    void should_group_a_number_by_the_requested_ranges() {
        var ranges = List.of(new NumberRange(0, 1), new NumberRange(2, 10));

        var result = cut.searchHTTPFacets(
            QUERY_CONTEXT,
            new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(REQUESTS), List.of(FAILOVERS), ranges)
        );

        assertThat(result.metrics().getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .containsExactly(tuple("0-1", 1L), tuple("2-10", 2L));
    }

    @Test
    void should_include_the_upper_bound_of_a_decimal_range() {
        var ranges = List.of(new NumberRange(0, 0.4));

        var result = cut.searchHTTPFacets(
            QUERY_CONTEXT,
            new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(REQUESTS), List.of(SCORE), ranges)
        );

        assertThat(result.metrics().getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .containsExactly(tuple("0-0.4", 2L));
    }

    @Test
    void should_return_no_bucket_for_a_field_no_document_carries() {
        var result = cut.searchHTTPFacets(
            QUERY_CONTEXT,
            new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(REQUESTS), List.of(NEVER_WRITTEN))
        );

        assertThat(result.metrics().getFirst().buckets()).isEmpty();
    }

    @Test
    void should_break_a_time_series_down_by_a_keyword() {
        List<FacetName> facets = List.of(TIER);

        var result = cut.searchHTTPTimeSeries(
            QUERY_CONTEXT,
            new TimeSeriesQuery(TIME_RANGE, List.of(BY_API), Duration.ofDays(1).toMillis(), List.of(REQUESTS), facets)
        );

        var keys = result
            .metrics()
            .getFirst()
            .buckets()
            .stream()
            .flatMap(bucket -> bucket.buckets() == null ? Stream.empty() : bucket.buckets().stream())
            .map(FacetBucketResult::key)
            .distinct()
            .toList();
        assertThat(keys).containsExactlyInAnyOrder("high", "low");
    }

    @Test
    void should_measure_a_number_over_the_requests_carrying_it_only() {
        var failovers = new MetricMeasuresQuery(
            FAILOVER_METRIC,
            Set.of(Measure.AVG, Measure.MIN, Measure.MAX, Measure.P50, Measure.P95, Measure.SUM, Measure.COUNT)
        );

        var result = cut.searchHTTPMeasures(QUERY_CONTEXT, new MeasuresQuery(TIME_RANGE, List.of(BY_API), List.of(failovers, REQUESTS)));

        // Over am-001..003 (0, 2, 5): am-004, which has no failover count, is no zero.
        var measures = measuresOf(result, FAILOVER_METRIC);
        assertThat(measures.get(Measure.AVG).doubleValue()).isCloseTo(7.0 / 3, within(1e-9));
        assertThat(measures.get(Measure.MIN).doubleValue()).isZero();
        assertThat(measures.get(Measure.MAX).doubleValue()).isEqualTo(5.0);
        assertThat(measures.get(Measure.P50).doubleValue()).isEqualTo(2.0);
        // The engines estimate it between 2 and 5: 5.0 on Elasticsearch 7 and OpenSearch, 4.7 on Elasticsearch 8 and 9
        assertThat(measures.get(Measure.P95).doubleValue()).isBetween(4.7 - 1e-9, 5.0);
        assertThat(measures.get(Measure.SUM).doubleValue()).isEqualTo(7.0);
        assertThat(measures.get(Measure.COUNT).longValue()).isEqualTo(3L);
        assertThat(measuresOf(result, Metric.HTTP_REQUESTS).get(Measure.COUNT).longValue()).isEqualTo(4L);
    }

    @Test
    void should_sum_a_decimal() {
        var score = new MetricMeasuresQuery(SCORE_METRIC, Set.of(Measure.SUM));

        var result = cut.searchHTTPMeasures(QUERY_CONTEXT, new MeasuresQuery(TIME_RANGE, List.of(BY_API), List.of(score)));

        assertThat(measuresOf(result, SCORE_METRIC).get(Measure.SUM).doubleValue()).isCloseTo(1.4, within(1e-9));
    }

    @Test
    void should_read_no_value_but_a_zero_sum_and_count_on_a_window_no_request_carries_the_number_in() {
        var empty = new TimeRange(DAY.minus(Duration.ofDays(1)), DAY);
        var failovers = new MetricMeasuresQuery(FAILOVER_METRIC, Set.of(Measure.AVG, Measure.MIN, Measure.P95, Measure.SUM, Measure.COUNT));

        var result = cut.searchHTTPMeasures(QUERY_CONTEXT, new MeasuresQuery(empty, List.of(BY_API), List.of(failovers)));

        assertThat(measuresOf(result, FAILOVER_METRIC))
            .containsEntry(Measure.AVG, null)
            .containsEntry(Measure.MIN, null)
            .containsEntry(Measure.P95, null)
            .hasEntrySatisfying(Measure.SUM, sum -> assertThat(sum.doubleValue()).isZero())
            .hasEntrySatisfying(Measure.COUNT, count -> assertThat(count.longValue()).isZero());
    }

    @Test
    void should_measure_a_number_per_value_of_a_keyword_ordered_by_a_percentile() {
        var failovers = new MetricMeasuresQuery(
            FAILOVER_METRIC,
            Set.of(Measure.P90, Measure.COUNT),
            List.of(new MetricMeasuresQuery.Sort(Measure.P90, MetricMeasuresQuery.Sort.Order.DESC))
        );

        var result = cut.searchHTTPFacets(QUERY_CONTEXT, new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(failovers), List.of(TIER)));

        assertThat(result.metrics().getFirst().metric()).isEqualTo(FAILOVER_METRIC);
        assertThat(result.metrics().getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .containsExactly(tuple("low", 1L), tuple("high", 2L));
    }

    /** Without the order, Elasticsearch ranks the buckets by document count: `high` (2) before `low` (1). */
    @Test
    void should_order_a_keyword_by_a_built_in_percentile() {
        var latency = new MetricMeasuresQuery(
            Metric.HTTP_GATEWAY_RESPONSE_TIME,
            Set.of(Measure.P90),
            List.of(new MetricMeasuresQuery.Sort(Measure.P90, MetricMeasuresQuery.Sort.Order.DESC))
        );

        var result = cut.searchHTTPFacets(QUERY_CONTEXT, new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(latency), List.of(TIER)));

        // response times: high 10 and 20, low 30
        assertThat(result.metrics().getFirst().buckets()).extracting(FacetBucketResult::key).containsExactly("low", "high");
    }

    @Test
    void should_order_a_keyword_by_the_average_of_a_number_field() {
        var failovers = new MetricMeasuresQuery(
            FAILOVER_METRIC,
            Set.of(Measure.AVG),
            List.of(new MetricMeasuresQuery.Sort(Measure.AVG, MetricMeasuresQuery.Sort.Order.DESC))
        );

        var result = cut.searchHTTPFacets(QUERY_CONTEXT, new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(failovers), List.of(TIER)));

        assertThat(result.metrics().getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.AVG).doubleValue())
            .containsExactly(tuple("low", 5.0), tuple("high", 1.0));
    }

    /**
     * The 500 bucket holds no failover count: Elasticsearch would read its minimum as +Infinity and its maximum as
     * -Infinity, rank it first and give it the limit.
     */
    @Test
    void should_rank_only_the_buckets_carrying_a_number_field_by_its_minimum_or_maximum() {
        var byMinimumDescending = new MetricMeasuresQuery(
            FAILOVER_METRIC,
            Set.of(Measure.MIN),
            List.of(new MetricMeasuresQuery.Sort(Measure.MIN, MetricMeasuresQuery.Sort.Order.DESC))
        );
        var byMaximumAscending = new MetricMeasuresQuery(
            FAILOVER_METRIC,
            Set.of(Measure.MAX),
            List.of(new MetricMeasuresQuery.Sort(Measure.MAX, MetricMeasuresQuery.Sort.Order.ASC))
        );
        List<FacetName> byStatus = List.of(Facet.HTTP_STATUS);

        var minimum = cut.searchHTTPFacets(
            QUERY_CONTEXT,
            new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(byMinimumDescending), byStatus, 1)
        );
        var maximum = cut.searchHTTPFacets(
            QUERY_CONTEXT,
            new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(byMaximumAscending), byStatus, 1)
        );

        assertThat(minimum.metrics().getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.MIN))
            .containsExactly(tuple("200", 0.0));
        assertThat(maximum.metrics().getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.MAX))
            .containsExactly(tuple("200", 5.0));
    }

    /** A bucket without the field is absent, not a 0 count: as on the wire, where a measure with no value is left out. */
    @Test
    void should_leave_out_the_buckets_carrying_no_number_field_when_counting_or_summing_it() {
        var failovers = new MetricMeasuresQuery(FAILOVER_METRIC, Set.of(Measure.COUNT, Measure.SUM));

        var result = cut.searchHTTPFacets(
            QUERY_CONTEXT,
            new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(failovers, REQUESTS), List.of(Facet.HTTP_STATUS))
        );

        assertThat(result.metrics())
            .filteredOn(metric -> metric.metric().equals(FAILOVER_METRIC))
            .singleElement()
            .satisfies(metric ->
                assertThat(metric.buckets())
                    .extracting(
                        FacetBucketResult::key,
                        bucket -> bucket.measures().get(Measure.COUNT).longValue(),
                        bucket -> bucket.measures().get(Measure.SUM).doubleValue()
                    )
                    .containsExactly(tuple("200", 3L, 7.0))
            );
        assertThat(result.metrics())
            .filteredOn(metric -> metric.metric().equals(Metric.HTTP_REQUESTS))
            .singleElement()
            .satisfies(metric -> assertThat(metric.buckets()).extracting(FacetBucketResult::key).containsExactlyInAnyOrder("200", "500"));
    }

    @Test
    void should_scope_every_level_of_the_facets_of_a_number_field_to_the_documents_carrying_it() {
        var failovers = new MetricMeasuresQuery(FAILOVER_METRIC, Set.of(Measure.COUNT, Measure.AVG));
        List<FacetName> facets = List.of(Facet.HTTP_STATUS, FAILOVERS);
        var ranges = List.of(new NumberRange(0, 1), new NumberRange(2, 10));

        var result = cut.searchHTTPFacets(QUERY_CONTEXT, new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(failovers), facets, ranges));

        var status = result.metrics().getFirst().buckets();
        assertThat(status).extracting(FacetBucketResult::key).containsExactly("200");
        assertThat(status.getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .containsExactly(tuple("0-1", 1L), tuple("2-10", 2L));
    }

    @Test
    void should_break_the_time_series_of_a_number_down_by_a_keyword() {
        var failovers = new MetricMeasuresQuery(FAILOVER_METRIC, Set.of(Measure.AVG));
        List<FacetName> facets = List.of(TIER);

        var result = cut.searchHTTPTimeSeries(
            QUERY_CONTEXT,
            new TimeSeriesQuery(TIME_RANGE, List.of(BY_API), Duration.ofDays(1).toMillis(), List.of(failovers), facets)
        );

        assertThat(result.metrics().getFirst().metric()).isEqualTo(FAILOVER_METRIC);
        var averages = result
            .metrics()
            .getFirst()
            .buckets()
            .stream()
            .flatMap(bucket -> bucket.buckets() == null ? Stream.empty() : bucket.buckets().stream())
            .map(bucket -> tuple(bucket.key(), bucket.measures().get(Measure.AVG).doubleValue()))
            .toList();
        assertThat(averages).containsExactlyInAnyOrder(tuple("high", 1.0), tuple("low", 5.0));
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

    private long count(List<Filter> filters) {
        var result = cut.searchHTTPMeasures(QUERY_CONTEXT, new MeasuresQuery(TIME_RANGE, filters, List.of(REQUESTS)));
        return result.measures().getFirst().measures().get(Measure.COUNT).longValue();
    }
}
