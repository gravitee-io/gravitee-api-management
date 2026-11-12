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
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetBucketResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesBucketResult;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * What every measure of a built-in metric means, read on real request documents: {@code lp-001} to {@code lp-005} of
 * {@code v4-metrics.ftl}, ten days back under an API of their own. {@code lp-004} carries no LLM value and no endpoint
 * response time, {@code lp-003} only the sent side, {@code lp-005} only the received side.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
class AnalyticsElasticsearchRepositoryBuiltInMeasuresTest extends AbstractElasticsearchRepositoryTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("DEFAULT", "DEFAULT");
    private static final Instant DAY = TimeProvider.now().truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(10));
    private static final TimeRange TIME_RANGE = new TimeRange(DAY, DAY.plus(Duration.ofDays(1)));
    private static final Filter BY_API = new Filter(Filter.Name.API, Filter.Operator.IN, List.of("llm-measures-api-001"));

    private static final AdditionalMetric TOKENS_SENT_KEY = new AdditionalMetric(
        new AdditionalMetricField(ValueType.LONG, "long_llm-proxy_tokens-sent")
    );

    @Autowired
    private AnalyticsElasticsearchRepository cut;

    @Test
    void should_measure_a_built_in_and_the_additional_metric_it_reads_alike() {
        var measured = Set.of(Measure.SUM, Measure.AVG, Measure.MIN, Measure.MAX, Measure.P99);
        var builtIn = new MetricMeasuresQuery(Metric.LLM_PROMPT_TOKEN_SENT, measured);
        var key = new MetricMeasuresQuery(TOKENS_SENT_KEY, measured);

        var result = measures(builtIn, key);

        // Over lp-001, lp-002 and lp-003 (100, 300, 40): lp-004 and lp-005 send no token and count for nothing.
        var sent = measuresOf(result, Metric.LLM_PROMPT_TOKEN_SENT);
        assertThat(sent.get(Measure.SUM).doubleValue()).isEqualTo(440.0);
        assertThat(sent.get(Measure.AVG).doubleValue()).isCloseTo(440.0 / 3, within(1e-9));
        assertThat(sent.get(Measure.MIN).doubleValue()).isEqualTo(40.0);
        assertThat(sent.get(Measure.MAX).doubleValue()).isEqualTo(300.0);
        assertThat(sent.get(Measure.P99).doubleValue()).isBetween(40.0, 300.0);
        assertThat(measuresOf(result, TOKENS_SENT_KEY)).isEqualTo(sent);
    }

    /** lp-003 carries only sent tokens and cost, lp-005 only received ones: both are counted; lp-004 carries neither. */
    @Test
    void should_average_an_llm_total_over_the_requests_carrying_either_of_its_two_values() {
        var result = measures(
            new MetricMeasuresQuery(Metric.LLM_PROMPT_TOTAL_TOKEN, Set.of(Measure.AVG)),
            new MetricMeasuresQuery(Metric.LLM_PROMPT_TOKEN_TOTAL_COST, Set.of(Measure.AVG))
        );

        assertThat(measuresOf(result, Metric.LLM_PROMPT_TOTAL_TOKEN).get(Measure.AVG).doubleValue()).isCloseTo(200.0, within(1e-9));
        assertThat(measuresOf(result, Metric.LLM_PROMPT_TOKEN_TOTAL_COST).get(Measure.AVG).doubleValue()).isCloseTo(0.002, within(1e-9));
    }

    /** lp-004 has no endpoint response time: neither the average, the bounds nor a percentile reads it as 0. */
    @Test
    void should_leave_a_request_without_the_value_out_of_a_built_in_average_bound_or_percentile() {
        var responseTime = new MetricMeasuresQuery(
            Metric.HTTP_ENDPOINT_RESPONSE_TIME,
            Set.of(Measure.COUNT, Measure.AVG, Measure.MIN, Measure.MAX, Measure.P50)
        );

        var measures = measuresOf(measures(responseTime), Metric.HTTP_ENDPOINT_RESPONSE_TIME);

        assertThat(measures.get(Measure.COUNT).longValue()).isEqualTo(4L);
        assertThat(measures.get(Measure.AVG).doubleValue()).isCloseTo(250.0, within(1e-9));
        assertThat(measures.get(Measure.MIN).doubleValue()).isEqualTo(100.0);
        assertThat(measures.get(Measure.MAX).doubleValue()).isEqualTo(400.0);
        assertThat(measures.get(Measure.P50).doubleValue()).isCloseTo(250.0, within(1.0));
    }

    @ParameterizedTest
    @EnumSource(value = Metric.class, names = { "LLM_PROMPT_TOKEN_SENT", "LLM_PROMPT_TOTAL_TOKEN", "HTTP_ENDPOINT_RESPONSE_TIME" })
    void should_read_no_value_but_a_zero_count_and_sum_on_a_window_no_request_carries_the_value_in(Metric metric) {
        var empty = new TimeRange(DAY.minus(Duration.ofDays(1)), DAY);
        var measured = metric == Metric.LLM_PROMPT_TOTAL_TOKEN
            ? Set.of(Measure.COUNT, Measure.AVG)
            : Set.of(Measure.COUNT, Measure.SUM, Measure.AVG, Measure.MIN, Measure.MAX, Measure.P99);

        var result = cut.searchHTTPMeasures(
            QUERY_CONTEXT,
            new MeasuresQuery(empty, List.of(BY_API), List.of(new MetricMeasuresQuery(metric, measured)))
        );

        var measures = measuresOf(result, metric);
        assertThat(measured).allSatisfy(measure -> {
            if (measure == Measure.COUNT || measure == Measure.SUM) {
                assertThat(measures.get(measure).doubleValue()).as(measure.name()).isZero();
            } else {
                assertThat(measures).as(measure.name()).containsEntry(measure, null);
            }
        });
    }

    @Test
    void should_read_no_value_in_a_time_bucket_no_request_carries_the_value_in() {
        var morning = new TimeRange(DAY.plus(Duration.ofHours(8)), DAY.plus(Duration.ofHours(10)).minusMillis(1));
        var tokens = new MetricMeasuresQuery(Metric.LLM_PROMPT_TOKEN_SENT, Set.of(Measure.SUM, Measure.AVG));

        var result = cut.searchHTTPTimeSeries(
            QUERY_CONTEXT,
            new TimeSeriesQuery(morning, List.of(BY_API), Duration.ofHours(1).toMillis(), List.of(tokens))
        );

        assertThat(result.metrics().getFirst().buckets())
            .extracting(TimeSeriesBucketResult::measures)
            .containsExactly(Map.of(Measure.SUM, 440.0, Measure.AVG, 440.0 / 3), mapOfNullable(Measure.SUM, 0.0, Measure.AVG, null));
    }

    /**
     * mistral (lp-004) carries no sent token. Ranked by a measure that reads no value there, Elasticsearch would put it
     * first and give it the limit: the buckets ranked are the ones carrying the value.
     */
    @Test
    void should_rank_only_the_buckets_carrying_a_built_in_value_by_its_average_or_bounds() {
        var byModel = List.<FacetName>of(Facet.LLM_PROXY_MODEL);

        var average = facets(sorted(Metric.LLM_PROMPT_TOKEN_SENT, Measure.AVG, MetricMeasuresQuery.Sort.Order.DESC), byModel);
        var minimum = facets(sorted(Metric.LLM_PROMPT_TOKEN_SENT, Measure.MIN, MetricMeasuresQuery.Sort.Order.DESC), byModel);
        var maximum = facets(sorted(Metric.LLM_PROMPT_TOKEN_SENT, Measure.MAX, MetricMeasuresQuery.Sort.Order.ASC), byModel);
        var total = facets(sorted(Metric.LLM_PROMPT_TOTAL_TOKEN, Measure.AVG, MetricMeasuresQuery.Sort.Order.DESC), byModel);

        assertThat(average).extracting(FacetBucketResult::key).containsExactly("gpt-4", "claude-3");
        assertThat(minimum).extracting(FacetBucketResult::key).containsExactly("gpt-4", "claude-3");
        assertThat(maximum).extracting(FacetBucketResult::key).containsExactly("claude-3", "gpt-4");
        assertThat(total)
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.AVG).doubleValue())
            .containsExactly(tuple("gpt-4", 760.0 / 3), tuple("claude-3", 40.0));
    }

    /** Without a ranking by a measure that can read no value, the buckets stay those of every request, as before. */
    @Test
    void should_keep_the_buckets_of_every_request_when_counting_or_summing_a_built_in() {
        var bySum = sorted(Metric.LLM_PROMPT_TOKEN_SENT, Measure.SUM, MetricMeasuresQuery.Sort.Order.DESC);

        var buckets = facets(bySum, List.of(Facet.LLM_PROXY_MODEL));

        assertThat(buckets)
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.SUM).doubleValue())
            .containsExactly(tuple("gpt-4", 400.0), tuple("claude-3", 40.0), tuple("mistral", 0.0));
    }

    private MeasuresResult measures(MetricMeasuresQuery... metrics) {
        return cut.searchHTTPMeasures(QUERY_CONTEXT, new MeasuresQuery(TIME_RANGE, List.of(BY_API), List.of(metrics)));
    }

    private List<FacetBucketResult> facets(MetricMeasuresQuery metric, List<FacetName> facets) {
        return cut
            .searchHTTPFacets(QUERY_CONTEXT, new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(metric), facets, 10))
            .metrics()
            .getFirst()
            .buckets();
    }

    private static MetricMeasuresQuery sorted(MetricName metric, Measure measure, MetricMeasuresQuery.Sort.Order order) {
        return new MetricMeasuresQuery(metric, Set.of(measure), List.of(new MetricMeasuresQuery.Sort(measure, order)));
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

    private static Map<Measure, Number> mapOfNullable(Measure k1, Number v1, Measure k2, Number v2) {
        var map = new HashMap<Measure, Number>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
}
