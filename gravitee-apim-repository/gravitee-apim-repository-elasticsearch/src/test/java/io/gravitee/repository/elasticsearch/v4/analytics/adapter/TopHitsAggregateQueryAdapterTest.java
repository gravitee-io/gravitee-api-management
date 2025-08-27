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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.analytics.query.stats.EventAnalyticsQuery;
import io.gravitee.repository.log.v4.model.analytics.Aggregation;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class TopHitsAggregateQueryAdapterTest {

    public static final String API_ID = "273f4728-1e30-4c78-bf47-281e304c78a5";
    public static final Long FROM = 1756104349879L;
    public static final Long TO = 1756190749879L;

    @Test
    void should_adapt_top_value_hits_query() throws JsonProcessingException {
        var query = createEventAnalyticsQuery();

        String json = TopHitsAggregateQueryAdapter.adapt(query);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        // Assert aggregations
        JsonNode aggregations = node.get("aggs");

        // Downstream
        JsonNode downstreamConnections = aggregations.get("downstream-active-connections_latest");
        assertFalse(downstreamConnections.isNull());
        // Assert exists filter for downstream
        assertTrue(downstreamConnections.get("filter").has("exists"));
        assertEquals("downstream-active-connections", downstreamConnections.get("filter").get("exists").get("field").asText());
        JsonNode downstreamTopHits = downstreamConnections.get("aggs").get("top_hits").get("top_hits");
        assertEquals(1, downstreamTopHits.get("size").asInt());
        assertEquals("desc", downstreamTopHits.get("sort").get(0).get("@timestamp").get("order").textValue());
        assertEquals("downstream-active-connections", downstreamTopHits.get("_source").get(0).textValue());

        // Upstream
        JsonNode upstreamConnections = aggregations.get("upstream-active-connections_latest");
        assertFalse(upstreamConnections.isNull());
        // Assert exists filter for upstream
        assertTrue(upstreamConnections.get("filter").has("exists"));
        assertEquals("upstream-active-connections", upstreamConnections.get("filter").get("exists").get("field").asText());
        JsonNode upstreamTopHits = upstreamConnections.get("aggs").get("top_hits").get("top_hits");
        assertEquals(1, upstreamTopHits.get("size").asInt());
        assertEquals("desc", upstreamTopHits.get("sort").get(0).get("@timestamp").get("order").textValue());
        assertEquals("upstream-active-connections", upstreamTopHits.get("_source").get(0).textValue());

        // Assert query filters
        JsonNode filters = node.get("query").get("bool").get("filter");
        assertFalse(filters.isEmpty());
        assertEquals(API_ID, filters.get(0).get("term").get("api-id").asText());
        // Assert time range filters
        assertThat(filters.get(1).get("range").get("@timestamp").get("gte").asLong()).isEqualTo(FROM);
        assertThat(filters.get(1).get("range").get("@timestamp").get("lte").asLong()).isEqualTo(TO);
    }

    private static @NotNull EventAnalyticsQuery createEventAnalyticsQuery() {
        Aggregation agg1 = new Aggregation("downstream-active-connections", AggregationType.VALUE);
        Aggregation agg2 = new Aggregation("upstream-active-connections", AggregationType.VALUE);
        var aggregations = List.of(agg1, agg2);

        return new EventAnalyticsQuery(API_ID, new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)), aggregations);
    }
}
