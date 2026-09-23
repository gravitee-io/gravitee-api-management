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
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthzScopedFacetAggregationTest extends AbstractQueryAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SearchResponse response(String json) throws Exception {
        return objectMapper.readValue(json, SearchResponse.class);
    }

    private FacetsQuery facetsQuery(Metric... metrics) {
        var metricQueries = List.of(metrics)
            .stream()
            .map(metric -> new MetricMeasuresQuery(metric, Set.of(Measure.COUNT)))
            .toList();
        return new FacetsQuery(buildTimeRange(), List.of(), metricQueries, List.of(Facet.AUTHZ_ACTION));
    }

    private static List<String> keys(List<JsonNode> buckets) {
        return buckets
            .stream()
            .map(bucket -> bucket.get("key").asText())
            .toList();
    }

    @Test
    void lifts_the_facet_of_a_scoped_metric_out_of_its_scope_filter() throws Exception {
        var response = response(
            """
            {"timed_out": false, "aggregations": {
              "AUTHZ_FORBIDS#__FILTER__": {"doc_count": 1, "AUTHZ_FORBIDS#AUTHZ_ACTION": {"buckets": [
                {"key": "write", "doc_count": 1, "AUTHZ_FORBIDS#COUNT": {"value": 1}}
              ]}},
              "AUTHZ_DECISIONS#AUTHZ_ACTION": {"buckets": [
                {"key": "read", "doc_count": 3, "AUTHZ_DECISIONS#COUNT": {"value": 3}},
                {"key": "write", "doc_count": 1, "AUTHZ_DECISIONS#COUNT": {"value": 1}}
              ]}
            }}
            """
        );

        var aggregations = AuthzScopedFacetAggregation.unwrap(
            response,
            facetsQuery(Metric.AUTHZ_FORBIDS, Metric.AUTHZ_DECISIONS)
        ).getAggregations();

        assertThat(aggregations.keySet()).containsExactlyInAnyOrder("AUTHZ_FORBIDS#AUTHZ_ACTION", "AUTHZ_DECISIONS#AUTHZ_ACTION");
        assertThat(keys(aggregations.get("AUTHZ_FORBIDS#AUTHZ_ACTION").getBuckets())).containsExactly("write");
        assertThat(keys(aggregations.get("AUTHZ_DECISIONS#AUTHZ_ACTION").getBuckets())).containsExactly("read", "write");
    }

    @Test
    void lifts_the_facet_of_a_scoped_metric_out_of_its_scope_filter_in_each_time_bucket() throws Exception {
        var response = response(
            """
            {"timed_out": false, "aggregations": {"AUTHZ_FAILURES#TIME_SERIES": {"buckets": [
              {"key_as_string": "t1", "key": 1, "doc_count": 1,
               "AUTHZ_FAILURES#__FILTER__": {"doc_count": 0, "AUTHZ_FAILURES#AUTHZ_ACTION": {"buckets": []}}},
              {"key_as_string": "t2", "key": 2, "doc_count": 5,
               "AUTHZ_FAILURES#__FILTER__": {"doc_count": 2, "AUTHZ_FAILURES#AUTHZ_ACTION": {"buckets": [
                 {"key": "read", "doc_count": 2, "AUTHZ_FAILURES#COUNT": {"value": 2}}
               ]}}}
            ]}}}
            """
        );
        var query = new TimeSeriesQuery(
            buildTimeRange(),
            List.of(),
            1000L,
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_FAILURES, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_ACTION),
            null,
            null
        );

        var timeBuckets = AuthzScopedFacetAggregation.unwrap(response, query)
            .getAggregations()
            .get("AUTHZ_FAILURES#TIME_SERIES")
            .getBuckets();

        assertThat(timeBuckets)
            .extracting(bucket -> bucket.get("key_as_string").asText())
            .containsExactly("t1", "t2");
        assertThat(timeBuckets).noneMatch(bucket -> bucket.has("AUTHZ_FAILURES#__FILTER__"));
        assertThat(facetKeys(timeBuckets.getFirst())).isEmpty();
        assertThat(facetKeys(timeBuckets.getLast())).containsExactly("read");
    }

    @Test
    void leaves_the_measures_of_a_scoped_time_series_without_facet_in_their_scope_filter() throws Exception {
        var response = response(
            """
            {"timed_out": false, "aggregations": {"AUTHZ_FAILURES#TIME_SERIES": {"buckets": [
              {"key_as_string": "t1", "key": 1, "doc_count": 1,
               "AUTHZ_FAILURES#__FILTER__": {"doc_count": 1, "AUTHZ_FAILURES#COUNT": {"value": 1}}}
            ]}}}
            """
        );
        var query = new TimeSeriesQuery(
            buildTimeRange(),
            List.of(),
            1000L,
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_FAILURES, Set.of(Measure.COUNT)))
        );

        var timeBuckets = AuthzScopedFacetAggregation.unwrap(response, query)
            .getAggregations()
            .get("AUTHZ_FAILURES#TIME_SERIES")
            .getBuckets();

        assertThat(timeBuckets.getFirst().get("AUTHZ_FAILURES#__FILTER__").get("AUTHZ_FAILURES#COUNT").get("value").asInt()).isEqualTo(1);
    }

    private static List<String> facetKeys(JsonNode timeBucket) {
        return StreamSupport.stream(timeBucket.get("AUTHZ_FAILURES#AUTHZ_ACTION").get("buckets").spliterator(), false)
            .map(bucket -> bucket.get("key").asText())
            .toList();
    }
}
