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
package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.DateHistoAggregate;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchDateHistogramResponseAdapterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void should_return_empty_date_histo_when_aggregations_null() {
        var response = new SearchResponse();
        var result = SearchDateHistogramResponseAdapter.adapt(response);
        assertThat(result)
            .hasValueSatisfying(agg -> {
                assertThat(agg.timestamps()).isEmpty();
                assertThat(agg.values()).isEmpty();
            });
    }

    @Test
    void should_return_empty_date_histo_when_aggregations_empty() {
        var response = new SearchResponse();
        response.setAggregations(java.util.Map.of());
        var result = SearchDateHistogramResponseAdapter.adapt(response);
        assertThat(result)
            .hasValueSatisfying(agg -> {
                assertThat(agg.timestamps()).isEmpty();
                assertThat(agg.values()).isEmpty();
            });
    }

    @Test
    void should_parse_date_histogram_with_nested_terms_buckets() {
        var dateBucket1 = createDateBucket(1609459200000L, List.of(createFieldBucket("200", 100L), createFieldBucket("404", 5L)));
        var dateBucket2 = createDateBucket(1609462800000L, List.of(createFieldBucket("200", 150L), createFieldBucket("404", 2L)));
        var dateBucket3 = createDateBucket(1609466400000L, List.of(createFieldBucket("200", 80L)));

        var byDateAgg = new Aggregation();
        byDateAgg.setBuckets(List.of(dateBucket1, dateBucket2, dateBucket3));

        var response = new SearchResponse();
        response.setAggregations(java.util.Map.of("by_date", byDateAgg));

        var result = SearchDateHistogramResponseAdapter.adapt(response);

        assertThat(result).isPresent();
        var agg = result.get();
        assertThat(agg.timestamps()).containsExactly(1609459200000L, 1609462800000L, 1609466400000L);

        assertThat(agg.values()).hasSize(2);
        var status200 = agg.values().stream().filter(v -> "200".equals(v.field())).findFirst().orElseThrow();
        assertThat(status200.buckets()).containsExactly(100L, 150L, 80L);
        assertThat(status200.metadata()).containsEntry("name", "200");

        var status404 = agg.values().stream().filter(v -> "404".equals(v.field())).findFirst().orElseThrow();
        assertThat(status404.buckets()).containsExactly(5L, 2L, 0L);
        assertThat(status404.metadata()).containsEntry("name", "404");
    }

    @Test
    void should_return_empty_date_histo_when_no_buckets() {
        var byDateAgg = new Aggregation();
        byDateAgg.setBuckets(List.of());

        var response = new SearchResponse();
        response.setAggregations(java.util.Map.of("by_date", byDateAgg));

        var result = SearchDateHistogramResponseAdapter.adapt(response);

        assertThat(result)
            .hasValueSatisfying(agg -> {
                assertThat(agg.timestamps()).isEmpty();
                assertThat(agg.values()).isEmpty();
            });
    }

    @Test
    void should_return_empty_date_histo_when_by_date_agg_missing() {
        var otherAgg = new Aggregation();
        otherAgg.setBuckets(List.of(createDateBucket(1609459200000L, List.of(createFieldBucket("x", 1L)))));

        var response = new SearchResponse();
        response.setAggregations(java.util.Map.of("other_agg", otherAgg));

        var result = SearchDateHistogramResponseAdapter.adapt(response);

        assertThat(result)
            .hasValueSatisfying(agg -> {
                assertThat(agg.timestamps()).isEmpty();
                assertThat(agg.values()).isEmpty();
            });
    }

    private static JsonNode createDateBucket(long key, List<JsonNode> fieldBuckets) {
        var node = OBJECT_MAPPER.createObjectNode();
        node.put("key", key);
        var byField = OBJECT_MAPPER.createObjectNode();
        var bucketsArray = OBJECT_MAPPER.createArrayNode();
        fieldBuckets.forEach(bucketsArray::add);
        byField.set("buckets", bucketsArray);
        node.set("by_field", byField);
        return node;
    }

    private static JsonNode createFieldBucket(String key, long docCount) {
        var node = OBJECT_MAPPER.createObjectNode();
        node.put("key", key);
        node.put("doc_count", docCount);
        return node;
    }
}
