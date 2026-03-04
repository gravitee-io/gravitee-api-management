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
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchGroupByResponseAdapterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void should_return_empty_group_by_when_aggregations_null() {
        var response = new SearchResponse();
        var result = SearchGroupByResponseAdapter.adapt(response);
        assertThat(result)
            .hasValueSatisfying(agg -> {
                assertThat(agg.values()).isEmpty();
                assertThat(agg.metadata()).isEmpty();
            });
    }

    @Test
    void should_return_empty_group_by_when_aggregations_empty() {
        var response = new SearchResponse();
        response.setAggregations(java.util.Map.of());
        var result = SearchGroupByResponseAdapter.adapt(response);
        assertThat(result)
            .hasValueSatisfying(agg -> {
                assertThat(agg.values()).isEmpty();
                assertThat(agg.metadata()).isEmpty();
            });
    }

    @Test
    void should_return_group_by_from_terms_aggregation_buckets() {
        List<JsonNode> buckets = List.of(createBucket("200", 1500L), createBucket("404", 50L), createBucket("500", 5L));

        var aggregation = new Aggregation();
        aggregation.setBuckets(buckets);

        var response = new SearchResponse();
        response.setAggregations(java.util.Map.of("group_by_field", aggregation));

        var result = SearchGroupByResponseAdapter.adapt(response);

        assertThat(result)
            .hasValueSatisfying(g -> {
                assertThat(g.values()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of("200", 1500L, "404", 50L, "500", 5L));
                assertThat(g.metadata()).containsKeys("200", "404", "500");
                assertThat(g.metadata().get("200")).containsEntry("name", "200");
            });
    }

    @Test
    void should_return_empty_group_by_when_no_matching_docs() {
        var aggregation = new Aggregation();
        aggregation.setBuckets(List.of());

        var response = new SearchResponse();
        response.setAggregations(java.util.Map.of("group_by_field", aggregation));

        var result = SearchGroupByResponseAdapter.adapt(response);

        assertThat(result)
            .hasValueSatisfying(g -> {
                assertThat(g.values()).isEmpty();
                assertThat(g.metadata()).isEmpty();
            });
    }

    @Test
    void should_return_empty_group_by_when_group_by_agg_missing() {
        var otherAgg = new Aggregation();
        otherAgg.setBuckets(List.of(createBucket("x", 1L)));
        var response = new SearchResponse();
        response.setAggregations(java.util.Map.of("other_agg", otherAgg));

        var result = SearchGroupByResponseAdapter.adapt(response);

        assertThat(result)
            .hasValueSatisfying(agg -> {
                assertThat(agg.values()).isEmpty();
                assertThat(agg.metadata()).isEmpty();
            });
    }

    private static JsonNode createBucket(String key, long docCount) {
        var node = OBJECT_MAPPER.createObjectNode();
        node.put("key", key);
        node.put("doc_count", docCount);
        return node;
    }
}
