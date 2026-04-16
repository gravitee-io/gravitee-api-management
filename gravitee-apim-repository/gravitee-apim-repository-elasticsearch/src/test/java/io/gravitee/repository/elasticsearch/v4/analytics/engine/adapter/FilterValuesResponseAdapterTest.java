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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FilterValuesResponseAdapterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final FilterValuesResponseAdapter adapter = new FilterValuesResponseAdapter();

    @Test
    void should_return_empty_result_for_null_response() {
        var result = adapter.adapt(null);

        assertThat(result.values()).isEmpty();
        assertThat(result.afterKey()).isNull();
        assertThat(result.totalCount()).isZero();
    }

    @Test
    void should_return_empty_result_for_empty_aggregations() {
        var response = new SearchResponse();
        response.setAggregations(new HashMap<>());

        var result = adapter.adapt(response);

        assertThat(result.values()).isEmpty();
        assertThat(result.afterKey()).isNull();
        assertThat(result.totalCount()).isZero();
    }

    @Test
    void should_extract_values_from_composite_buckets() {
        var response = buildSearchResponse(new String[] { "gw-1", "gw-2", "gw-3" }, null, 0);

        var result = adapter.adapt(response);

        assertThat(result.values()).containsExactly("gw-1", "gw-2", "gw-3");
        assertThat(result.afterKey()).isNull();
    }

    @Test
    void should_extract_after_key() {
        var afterKeyNode = MAPPER.createObjectNode().put("value", "gw-3");
        var response = buildSearchResponse(new String[] { "gw-1", "gw-2", "gw-3" }, afterKeyNode, 0);

        var result = adapter.adapt(response);

        assertThat(result.values()).hasSize(3);
        assertThat(result.afterKey()).containsEntry("value", "gw-3");
    }

    @Test
    void should_extract_after_key_with_type_aware_values() {
        var afterKeyNode = MAPPER.createObjectNode().put("value", "gw-3").put("timestamp", 1700000000000L).put("status", 200);
        var response = buildSearchResponse(new String[] { "gw-1" }, afterKeyNode, 0);

        var result = adapter.adapt(response);

        assertThat(result.afterKey())
            .containsEntry("value", "gw-3")
            .containsEntry("timestamp", 1700000000000L)
            .containsEntry("status", 200);
    }

    @Test
    void should_return_null_after_key_when_missing() {
        var response = buildSearchResponse(new String[] { "gw-1" }, null, 0);

        var result = adapter.adapt(response);

        assertThat(result.afterKey()).isNull();
    }

    @Test
    void should_extract_total_count_from_cardinality_aggregation() {
        var response = buildSearchResponse(new String[] { "gw-1", "gw-2" }, null, 42);

        var result = adapter.adapt(response);

        assertThat(result.totalCount()).isEqualTo(42);
    }

    @Test
    void should_return_zero_total_count_when_cardinality_aggregation_missing() {
        var response = buildSearchResponse(new String[] { "gw-1" }, null, 0);
        response.getAggregations().remove("total_count");

        var result = adapter.adapt(response);

        assertThat(result.totalCount()).isZero();
    }

    private SearchResponse buildSearchResponse(String[] values, ObjectNode afterKeyNode, long totalCount) {
        ArrayNode bucketsArray = MAPPER.createArrayNode();
        for (String value : values) {
            ObjectNode bucket = MAPPER.createObjectNode();
            bucket.set("key", MAPPER.createObjectNode().put("value", value));
            bucket.put("doc_count", 1);
            bucketsArray.add(bucket);
        }

        var agg = new Aggregation();
        agg.setBuckets(new java.util.ArrayList<>());
        for (JsonNode node : bucketsArray) {
            agg.getBuckets().add(node);
        }

        if (afterKeyNode != null) {
            agg.setAfterKey(afterKeyNode);
        }

        var aggregations = new HashMap<String, Aggregation>();
        aggregations.put("filter_values", agg);

        if (totalCount > 0) {
            var totalCountAgg = new Aggregation();
            totalCountAgg.setValue((float) totalCount);
            aggregations.put("total_count", totalCountAgg);
        }

        var response = new SearchResponse();
        response.setAggregations(aggregations);
        return response;
    }
}
