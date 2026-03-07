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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
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
        assertThat(buckets.get(0).measures()).containsEntry(Measure.COUNT, 42.0);

        assertThat(buckets.get(1).key()).isEqualTo("4xx");
        assertThat(buckets.get(1).measures()).containsEntry(Measure.COUNT, 10.0);
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
        assertThat(buckets.get(0).measures()).containsEntry(Measure.COUNT, 42.0);

        assertThat(buckets.get(1).key()).isEqualTo("4xx");
        assertThat(buckets.get(1).measures()).containsEntry(Measure.COUNT, 10.0);
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
        assertThat(buckets.get(0).measures().get(Measure.COUNT)).isEqualTo(42.0);
    }

    private static ObjectNode buildBucketWithFilterAgg(String key, int docCount, String metricName, String measureName, double value) {
        var bucket = JSON.createObjectNode().put("key", key).put("doc_count", docCount);
        var filterAgg = bucket.putObject(metricName + "#__FILTER__").put("doc_count", (int) value);
        filterAgg.putObject(metricName + "#" + measureName).put("value", value);
        return bucket;
    }
}
