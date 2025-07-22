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

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByQuery;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GroupByQueryAdapterTest {

    private static final String FIELD = "status";
    private static final String API_ID = "api-123";
    private static final long FROM = 1700000000000L;
    private static final long TO = 1700003600000L;

    private final GroupByQueryAdapter cut = new GroupByQueryAdapter();

    @Nested
    class AdaptQuery {

        @Test
        void shouldGenerateCorrectTermsQueryJson() throws Exception {
            GroupByQuery query = new GroupByQuery(
                API_ID,
                FIELD,
                null,
                null,
                Instant.ofEpochMilli(FROM),
                Instant.ofEpochMilli(TO),
                null,
                null
            );
            String json = cut.adapt(query);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            assertEquals(0, node.get("size").asInt());
            assertEquals(API_ID, node.at("/query/bool/filter/1/term/api-id").asText());
            assertEquals(FROM, node.at("/query/bool/filter/2/range/@timestamp/from").asLong());
            assertEquals(TO, node.at("/query/bool/filter/2/range/@timestamp/to").asLong());
            assertEquals(FIELD, node.at("/aggregations/by_status/terms/field").asText());
        }

        @Test
        void shouldGenerateCorrectRangeQueryJson() throws Exception {
            List<GroupByQuery.Group> groups = List.of(new GroupByQuery.Group(0, 100), new GroupByQuery.Group(100, 200));
            GroupByQuery query = new GroupByQuery(
                API_ID,
                FIELD,
                groups,
                null,
                Instant.ofEpochMilli(FROM),
                Instant.ofEpochMilli(TO),
                null,
                null
            );
            String json = cut.adapt(query);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            assertEquals(0, node.get("size").asInt());
            assertEquals(API_ID, node.at("/query/bool/filter/1/term/api-id").asText());
            assertEquals(FROM, node.at("/query/bool/filter/2/range/@timestamp/from").asLong());
            assertEquals(TO, node.at("/query/bool/filter/2/range/@timestamp/to").asLong());
            assertEquals(2, node.at("/aggregations/by_status_range/range/ranges").size());
            assertEquals(0, node.at("/aggregations/by_status_range/range/ranges/0/from").asInt());
            assertEquals(100, node.at("/aggregations/by_status_range/range/ranges/0/to").asInt());
        }

        @Test
        void shouldIncludeQueryStringIfPresent() throws Exception {
            String queryString = "status:200";
            GroupByQuery query = new GroupByQuery(
                API_ID,
                FIELD,
                null,
                null,
                Instant.ofEpochMilli(FROM),
                Instant.ofEpochMilli(TO),
                null,
                queryString
            );
            String json = cut.adapt(query);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            // Should include the query_string node
            boolean hasQueryString = false;
            for (JsonNode filterNode : node.at("/query/bool/filter")) {
                if (filterNode.has("query_string")) {
                    hasQueryString = true;
                    assertEquals(queryString, filterNode.get("query_string").get("query").asText());
                }
            }
            assertTrue(hasQueryString, "Should contain query_string filter");
        }
    }

    @Nested
    class AdaptResponse {

        @Test
        void shouldAdaptTermsResponseCorrectly() {
            cut.adapt(new GroupByQuery(API_ID, FIELD, null, null, Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO), null, null));

            Aggregation agg = new Aggregation();
            agg.setBuckets(List.of(createBucket("200", 10), createBucket("404", 5)));

            Map<String, Aggregation> aggs = new HashMap<>();
            aggs.put("by_status", agg);

            SearchResponse response = new SearchResponse();
            response.setAggregations(aggs);

            Optional<GroupByAggregate> result = cut.adaptResponse(response);
            assertTrue(result.isPresent());
            GroupByAggregate groupBy = result.get();
            assertEquals("by_status", groupBy.name());
            assertEquals(FIELD, groupBy.field());
            assertEquals(2, groupBy.values().size());
            assertEquals(10L, groupBy.values().get("200"));
            assertEquals(5L, groupBy.values().get("404"));
        }

        @Test
        void shouldAdaptRangeResponseCorrectly() {
            List<GroupByQuery.Group> groups = List.of(new GroupByQuery.Group(0, 100), new GroupByQuery.Group(100, 200));
            cut.adapt(new GroupByQuery(API_ID, FIELD, groups, null, Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO), null, null));

            Aggregation agg = new Aggregation();
            agg.setBuckets(List.of(createBucket("0.0-100.0", 7), createBucket("100.0-200.0", 3)));

            Map<String, Aggregation> aggs = new HashMap<>();
            aggs.put("by_status_range", agg);

            SearchResponse response = new SearchResponse();
            response.setAggregations(aggs);

            Optional<GroupByAggregate> result = cut.adaptResponse(response);
            assertTrue(result.isPresent());
            GroupByAggregate groupBy = result.get();
            assertEquals("by_status_range", groupBy.name());
            assertEquals(FIELD, groupBy.field());
            assertEquals(2, groupBy.values().size());
            assertEquals(7L, groupBy.values().get("0.0-100.0"));
            assertEquals(3L, groupBy.values().get("100.0-200.0"));
        }

        @Test
        void shouldReturnEmptyIfNoAggregations() {
            cut.adapt(new GroupByQuery(API_ID, FIELD, null, null, Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO), null, null));
            SearchResponse response = new SearchResponse();
            response.setAggregations(null);
            assertTrue(cut.adaptResponse(response).isEmpty());
        }

        @Test
        void shouldReturnEmptyIfNoMatchingAggregation() {
            cut.adapt(new GroupByQuery(API_ID, FIELD, null, null, Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO), null, null));
            SearchResponse response = new SearchResponse();
            response.setAggregations(new HashMap<>());
            assertTrue(cut.adaptResponse(response).isEmpty());
        }

        private com.fasterxml.jackson.databind.node.ObjectNode createBucket(String key, long docCount) {
            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode node = mapper.createObjectNode();
            node.put("key", key);
            node.put("doc_count", docCount);
            return node;
        }
    }
}
