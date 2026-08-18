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
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
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
}
