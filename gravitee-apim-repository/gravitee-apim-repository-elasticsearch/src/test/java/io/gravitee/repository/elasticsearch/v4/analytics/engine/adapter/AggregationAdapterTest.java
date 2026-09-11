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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AggregationAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final TimeRange TIME_RANGE = new TimeRange(Instant.now().minusSeconds(3600), Instant.now());

    @Test
    void should_extract_measures_from_facet_buckets_without_metric_filters() {
        var query = new FacetsQuery(
            TIME_RANGE,
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT))),
            List.of(Facet.HTTP_STATUS_CODE_GROUP)
        );

        var bucket2xx = JSON.createObjectNode().put("key", "2xx").put("doc_count", 100);
        bucket2xx.putObject("HTTP_REQUESTS#COUNT").put("value", 42);

        var bucket4xx = JSON.createObjectNode().put("key", "4xx").put("doc_count", 50);
        bucket4xx.putObject("HTTP_REQUESTS#COUNT").put("value", 10);

        var facetAgg = new Aggregation();
        facetAgg.setBuckets(List.of(bucket2xx, bucket4xx));

        var aggregations = Map.of("HTTP_REQUESTS#HTTP_STATUS_CODE_GROUP", facetAgg);

        var result = AggregationAdapter.toMetricsAndBuckets(aggregations, query);

        assertThat(result).hasSize(1);
        var metricResult = result.get(0);
        assertThat(metricResult.metric()).isEqualTo(Metric.HTTP_REQUESTS);

        var buckets = metricResult.buckets();
        assertThat(buckets).hasSize(2);

        assertThat(buckets.get(0).key()).isEqualTo("2xx");
        assertThat(buckets.get(0).measures().get(Measure.COUNT).doubleValue()).isEqualTo(42.0);

        assertThat(buckets.get(1).key()).isEqualTo("4xx");
        assertThat(buckets.get(1).measures().get(Measure.COUNT).doubleValue()).isEqualTo(10.0);
    }

    @Test
    void should_extract_measures_from_facet_buckets_with_metric_filters() {
        var metricFilters = List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of("llm-api-id")));
        var query = new FacetsQuery(
            TIME_RANGE,
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT), metricFilters, List.of())),
            List.of(Facet.HTTP_STATUS_CODE_GROUP)
        );

        var bucket2xx = buildBucketWithFilterAgg("2xx", 100, "HTTP_REQUESTS", "COUNT", 42);
        var bucket4xx = buildBucketWithFilterAgg("4xx", 50, "HTTP_REQUESTS", "COUNT", 10);

        var facetAgg = new Aggregation();
        facetAgg.setBuckets(List.of(bucket2xx, bucket4xx));

        var aggregations = Map.of("HTTP_REQUESTS#HTTP_STATUS_CODE_GROUP", facetAgg);

        var result = AggregationAdapter.toMetricsAndBuckets(aggregations, query);

        assertThat(result).hasSize(1);
        var metricResult = result.get(0);
        assertThat(metricResult.metric()).isEqualTo(Metric.HTTP_REQUESTS);

        var buckets = metricResult.buckets();
        assertThat(buckets).hasSize(2);

        assertThat(buckets.get(0).key()).isEqualTo("2xx");
        assertThat(buckets.get(0).measures().get(Measure.COUNT).doubleValue()).isEqualTo(42.0);

        assertThat(buckets.get(1).key()).isEqualTo("4xx");
        assertThat(buckets.get(1).measures().get(Measure.COUNT).doubleValue()).isEqualTo(10.0);
    }

    @Test
    void should_name_http_method_buckets_after_the_method_rather_than_its_reported_code() {
        var query = new FacetsQuery(
            TIME_RANGE,
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT))),
            List.of(Facet.HTTP_METHOD)
        );

        var get = JSON.createObjectNode().put("key", 3).put("doc_count", 30);
        get.putObject("HTTP_REQUESTS#COUNT").put("value", 30);
        var post = JSON.createObjectNode().put("key", 7).put("doc_count", 12);
        post.putObject("HTTP_REQUESTS#COUNT").put("value", 12);

        var facetAgg = new Aggregation();
        facetAgg.setBuckets(List.of(get, post));

        var result = AggregationAdapter.toMetricsAndBuckets(Map.of("HTTP_REQUESTS#HTTP_METHOD", facetAgg), query);

        assertThat(result.get(0).buckets())
            .extracting(bucket -> bucket.key(), bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .containsExactly(org.assertj.core.groups.Tuple.tuple("GET", 30L), org.assertj.core.groups.Tuple.tuple("POST", 12L));
    }

    @Test
    void should_name_http_method_buckets_nested_under_another_facet() {
        var query = new FacetsQuery(
            TIME_RANGE,
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT))),
            List.of(Facet.API, Facet.HTTP_METHOD)
        );

        var get = JSON.createObjectNode().put("key", 3).put("doc_count", 30);
        get.putObject("HTTP_REQUESTS#COUNT").put("value", 30);
        var api = JSON.createObjectNode().put("key", "api-1").put("doc_count", 30);
        api.putObject("HTTP_REQUESTS#HTTP_METHOD").putArray("buckets").add(get);

        var facetAgg = new Aggregation();
        facetAgg.setBuckets(List.of(api));

        var result = AggregationAdapter.toMetricsAndBuckets(Map.of("HTTP_REQUESTS#API", facetAgg), query);

        var apiBucket = result.get(0).buckets().get(0);
        assertThat(apiBucket.key()).isEqualTo("api-1");
        assertThat(apiBucket.buckets())
            .singleElement()
            .extracting(bucket -> bucket.key())
            .isEqualTo("GET");
    }

    @Test
    void should_return_zero_measures_without_fix_when_filter_agg_wraps_measures() {
        var query = new FacetsQuery(
            TIME_RANGE,
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT))),
            List.of(Facet.HTTP_STATUS_CODE_GROUP)
        );

        var bucket = buildBucketWithFilterAgg("2xx", 100, "HTTP_REQUESTS", "COUNT", 42);

        var facetAgg = new Aggregation();
        facetAgg.setBuckets(List.of(bucket));

        var aggregations = Map.of("HTTP_REQUESTS#HTTP_STATUS_CODE_GROUP", facetAgg);

        var result = AggregationAdapter.toMetricsAndBuckets(aggregations, query);

        assertThat(result).hasSize(1);
        var buckets = result.get(0).buckets();
        assertThat(buckets).hasSize(1);
        assertThat(buckets.get(0).measures().get(Measure.COUNT).doubleValue()).isEqualTo(42.0);
    }

    @Test
    void should_report_the_same_total_for_measures_and_for_the_sum_of_its_facet_buckets() throws Exception {
        // Counts on a busy API run well past 2^24, where a float can no longer hold every integer.
        var applicationCounts = Map.of("application-a", 120_800_000L, "application-b", 12_767L);
        var total = applicationCounts.values().stream().mapToLong(Long::longValue).sum();

        var measuresQuery = new MeasuresQuery(
            TIME_RANGE,
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)))
        );
        // Deserialized the way the Elasticsearch client does, so a regression in the aggregation
        // model is caught here too.
        var totalAgg = JSON.readValue("{\"value\":" + total + "}", Aggregation.class);

        var measures = AggregationAdapter.toMetricsAndMeasures(Map.of("HTTP_REQUESTS#COUNT", totalAgg), measuresQuery);

        var facetsQuery = new FacetsQuery(
            TIME_RANGE,
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT))),
            List.of(Facet.APPLICATION)
        );
        var applicationAgg = new Aggregation();
        applicationAgg.setBuckets(
            applicationCounts
                .entrySet()
                .stream()
                .map(entry -> {
                    var bucket = JSON.createObjectNode().put("key", entry.getKey()).put("doc_count", entry.getValue());
                    bucket.putObject("HTTP_REQUESTS#COUNT").put("value", entry.getValue());
                    return (JsonNode) bucket;
                })
                .toList()
        );

        var facets = AggregationAdapter.toMetricsAndBuckets(Map.of("HTTP_REQUESTS#APPLICATION", applicationAgg), facetsQuery);

        var facetsSum = facets
            .get(0)
            .buckets()
            .stream()
            .mapToLong(bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .sum();

        assertThat(measures.get(Metric.HTTP_REQUESTS).get(Measure.COUNT).longValue()).isEqualTo(total).isEqualTo(facetsSum);
    }

    private static ObjectNode buildBucketWithFilterAgg(String key, int docCount, String metricName, String measureName, double value) {
        var bucket = JSON.createObjectNode().put("key", key).put("doc_count", docCount);
        var filterAgg = bucket.putObject(metricName + "#__FILTER__").put("doc_count", (int) value);
        filterAgg.putObject(metricName + "#" + measureName).put("value", value);
        return bucket;
    }
}
