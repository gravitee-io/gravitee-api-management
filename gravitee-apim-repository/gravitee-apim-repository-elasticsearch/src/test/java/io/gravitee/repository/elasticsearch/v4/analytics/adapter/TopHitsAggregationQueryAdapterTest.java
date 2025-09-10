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

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TimeRangeAdapter.GTE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TimeRangeAdapter.LTE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TimeRangeAdapter.RANGE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.AGGS;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.ASC;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.DATE_HISTOGRAM;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.DESC;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.END_VALUE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.EXISTS;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.FIELD;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.FILTER;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.FIXED_INTERVAL;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.ORDER;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.PER_INTERVAL;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.SIZE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.SORT;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.SOURCE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.START_VALUE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.TERM;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.TIMESTAMP;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.TOP_HITS;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.adapt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.log.v4.model.analytics.Aggregation;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class TopHitsAggregationQueryAdapterTest {

    private static final String API_ID = "273f4728-1e30-4c78-bf47-281e304c78a5";
    private static final Long FROM = 1756104349879L;
    private static final Long TO = 1756190749879L;

    @Test
    void should_throw_when_no_aggregate_functions_specified() {
        assertThrows(IllegalArgumentException.class, () -> adapt(buildHistogramQuery(List.of())));
    }

    @Test
    void should_throw_when_more_than_one_aggregate_functions_specified() {
        Aggregation agg1 = new Aggregation("downstream-active-connections", AggregationType.VALUE);
        Aggregation agg2 = new Aggregation("upstream-active-connections", AggregationType.DELTA);
        var query = buildHistogramQuery(List.of(agg1, agg2));

        assertThrows(IllegalArgumentException.class, () -> adapt(query));
    }

    @Test
    void should_adapt_top_value_hits_query() throws JsonProcessingException {
        Aggregation agg1 = new Aggregation("downstream-active-connections", AggregationType.VALUE);
        Aggregation agg2 = new Aggregation("upstream-active-connections", AggregationType.VALUE);
        var query = buildHistogramQuery(List.of(agg1, agg2));

        String json = adapt(query);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        // Assert aggregations
        JsonNode aggregations = node.get(AGGS);
        // Downstream active connections
        JsonNode downstreamConnections = aggregations.get("downstream-active-connections_latest");
        assertFalse(downstreamConnections.isNull());
        // Assert exists filter
        assertTrue(downstreamConnections.get(FILTER).has(EXISTS));
        assertEquals("downstream-active-connections", downstreamConnections.get(FILTER).get(EXISTS).get(FIELD).asText());
        JsonNode downstreamTopHits = downstreamConnections.get(AGGS).get(TOP_HITS).get(TOP_HITS);
        assertEquals(1, downstreamTopHits.get(SIZE).asInt());
        assertEquals(DESC, downstreamTopHits.get(SORT).get(0).get(TIMESTAMP).get(ORDER).textValue());
        assertEquals("downstream-active-connections", downstreamTopHits.get(SOURCE).get(0).textValue());

        // Downstream active connections
        JsonNode upstreamConnections = aggregations.get("upstream-active-connections_latest");
        assertFalse(upstreamConnections.isNull());
        // Assert exists filter
        assertTrue(upstreamConnections.get(FILTER).has(EXISTS));
        assertEquals("upstream-active-connections", upstreamConnections.get(FILTER).get(EXISTS).get(FIELD).asText());
        JsonNode upstreamTopHits = upstreamConnections.get(AGGS).get(TOP_HITS).get(TOP_HITS);
        assertEquals(1, upstreamTopHits.get(SIZE).asInt());
        assertEquals(DESC, upstreamTopHits.get(SORT).get(0).get(TIMESTAMP).get(ORDER).textValue());
        assertEquals("upstream-active-connections", upstreamTopHits.get(SOURCE).get(0).textValue());
        // Assert query filters
        JsonNode filters = node.get("query").get("bool").get(FILTER);
        assertFalse(filters.isEmpty());
        // Assert term filters
        assertEquals(API_ID, filters.get(0).get(TERM).get("api-id").asText());
        // Assert time range filters
        assertEquals(FROM, filters.get(1).get(RANGE).get(TIMESTAMP).get(GTE).asLong());
        assertEquals(TO, filters.get(1).get(RANGE).get(TIMESTAMP).get(LTE).asLong());
    }

    @Test
    void should_adapt_top_delta_hits_query() throws JsonProcessingException {
        Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.DELTA);
        Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.DELTA);
        var query = buildHistogramQuery(List.of(agg1, agg2));

        String json = adapt(query);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        // Assert aggregations
        JsonNode aggregations = node.get(AGGS);
        assertNotNull(aggregations);
        assertFalse(aggregations.isEmpty());

        // Downstream publish messages total
        JsonNode downstreamMessagesProduced = aggregations.get("downstream-publish-messages-total_delta");
        assertFalse(downstreamMessagesProduced.isNull());
        // Assert exists filter
        assertTrue(downstreamMessagesProduced.get(FILTER).has(EXISTS));
        assertEquals("downstream-publish-messages-total", downstreamMessagesProduced.get(FILTER).get(EXISTS).get(FIELD).asText());
        // Start value aggregation
        JsonNode downstreamMinAgg = downstreamMessagesProduced.get(AGGS).get(START_VALUE).get(TOP_HITS);
        assertEquals(1, downstreamMinAgg.get(SIZE).asInt());
        assertEquals(ASC, downstreamMinAgg.get(SORT).get(0).get(TIMESTAMP).get(ORDER).textValue());
        assertEquals("downstream-publish-messages-total", downstreamMinAgg.get(SOURCE).get(0).textValue());
        // End value aggregation
        JsonNode downstreamMaxAgg = downstreamMessagesProduced.get(AGGS).get(END_VALUE).get(TOP_HITS);
        assertEquals(1, downstreamMaxAgg.get(SIZE).asInt());
        assertEquals(DESC, downstreamMaxAgg.get(SORT).get(0).get(TIMESTAMP).get(ORDER).textValue());
        assertEquals("downstream-publish-messages-total", downstreamMaxAgg.get(SOURCE).get(0).textValue());

        // Upstream publish messages total
        JsonNode upstreamMessagesProduced = aggregations.get("upstream-publish-messages-total_delta");
        assertFalse(upstreamMessagesProduced.isNull());
        // Assert exists filter
        assertTrue(upstreamMessagesProduced.get(FILTER).has(EXISTS));
        assertEquals("upstream-publish-messages-total", upstreamMessagesProduced.get(FILTER).get(EXISTS).get(FIELD).asText());
        // Start value aggregation
        JsonNode minAgg = upstreamMessagesProduced.get(AGGS).get(START_VALUE).get(TOP_HITS);
        assertEquals(1, minAgg.get(SIZE).asInt());
        assertEquals(ASC, minAgg.get(SORT).get(0).get(TIMESTAMP).get(ORDER).textValue());
        assertEquals("upstream-publish-messages-total", minAgg.get(SOURCE).get(0).textValue());
        // End value aggregation
        JsonNode maxAgg = upstreamMessagesProduced.get(AGGS).get(END_VALUE).get(TOP_HITS);
        assertEquals(1, maxAgg.get(SIZE).asInt());
        assertEquals(DESC, maxAgg.get(SORT).get(0).get(TIMESTAMP).get(ORDER).textValue());
        assertEquals("upstream-publish-messages-total", maxAgg.get(SOURCE).get(0).textValue());

        // Assert query filters
        JsonNode filters = node.get("query").get("bool").get(FILTER);
        assertFalse(filters.isEmpty());
        // Assert term filters
        assertEquals(API_ID, filters.get(0).get(TERM).get("api-id").asText());
        // Assert time range filters
        assertEquals(FROM, filters.get(1).get(RANGE).get(TIMESTAMP).get(GTE).asLong());
        assertEquals(TO, filters.get(1).get(RANGE).get(TIMESTAMP).get(LTE).asLong());
    }

    @Test
    void should_adapt_trend_query() throws JsonProcessingException {
        Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.TREND);
        Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.TREND);
        var query = buildHistogramQuery(List.of(agg1, agg2));

        String json = adapt(query);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        JsonNode aggs = node.get(AGGS);
        assertNotNull(aggs);
        assertFalse(aggs.isEmpty());

        // Per interval aggregation
        JsonNode perInterval = aggs.get(PER_INTERVAL);
        assertNotNull(perInterval);
        JsonNode histogram = perInterval.get(DATE_HISTOGRAM);
        assertNotNull(histogram);
        assertEquals(TIMESTAMP, histogram.get(FIELD).asText());
        assertNotNull(histogram.get(FIXED_INTERVAL));
        assertEquals("60000ms", histogram.get(FIXED_INTERVAL).asText());
        JsonNode perIntervalAggs = perInterval.get(AGGS);
        assertNotNull(perIntervalAggs);

        // Downstream publish messages total delta
        JsonNode downstreamDelta = perIntervalAggs.get("downstream-publish-messages-total_delta");
        assertNotNull(downstreamDelta);
        assertTrue(downstreamDelta.get(FILTER).has(EXISTS));
        assertEquals("downstream-publish-messages-total", downstreamDelta.get(FILTER).get(EXISTS).get(FIELD).asText());
        JsonNode downstreamStart = downstreamDelta.get(AGGS).get(START_VALUE).get(TOP_HITS);
        assertEquals(1, downstreamStart.get(SIZE).asInt());
        assertEquals(ASC, downstreamStart.get(SORT).get(0).get(TIMESTAMP).get(ORDER).asText());
        assertEquals("downstream-publish-messages-total", downstreamStart.get(SOURCE).get(0).asText());
        JsonNode downstreamEnd = downstreamDelta.get(AGGS).get(END_VALUE).get(TOP_HITS);
        assertEquals(1, downstreamEnd.get(SIZE).asInt());
        assertEquals(DESC, downstreamEnd.get(SORT).get(0).get(TIMESTAMP).get(ORDER).asText());
        assertEquals("downstream-publish-messages-total", downstreamEnd.get(SOURCE).get(0).asText());

        // Upstream publish messages total delta
        JsonNode upstreamDelta = perIntervalAggs.get("upstream-publish-messages-total_delta");
        assertNotNull(upstreamDelta);
        assertTrue(upstreamDelta.get(FILTER).has(EXISTS));
        assertEquals("upstream-publish-messages-total", upstreamDelta.get(FILTER).get(EXISTS).get(FIELD).asText());
        JsonNode upstreamStart = upstreamDelta.get(AGGS).get(START_VALUE).get(TOP_HITS);
        assertEquals(1, upstreamStart.get(SIZE).asInt());
        assertEquals(ASC, upstreamStart.get(SORT).get(0).get(TIMESTAMP).get(ORDER).asText());
        assertEquals("upstream-publish-messages-total", upstreamStart.get(SOURCE).get(0).asText());
        JsonNode upstreamEnd = upstreamDelta.get(AGGS).get(END_VALUE).get(TOP_HITS);
        assertEquals(1, upstreamEnd.get(SIZE).asInt());
        assertEquals(DESC, upstreamEnd.get(SORT).get(0).get(TIMESTAMP).get(ORDER).asText());
        assertEquals("upstream-publish-messages-total", upstreamEnd.get(SOURCE).get(0).asText());

        // Assert query filters
        JsonNode filters = node.get("query").get("bool").get(FILTER);
        assertFalse(filters.isEmpty());
        // Assert term filters
        assertEquals(API_ID, filters.get(0).get(TERM).get("api-id").asText());
        // Assert time range filters
        assertEquals(FROM, filters.get(1).get(RANGE).get(TIMESTAMP).get(GTE).asLong());
        assertEquals(TO, filters.get(1).get(RANGE).get(TIMESTAMP).get(LTE).asLong());
    }

    private static @NotNull HistogramQuery buildHistogramQuery(List<Aggregation> aggregations) {
        return new HistogramQuery(
            new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO), Duration.ofMillis(60000)),
            aggregations,
            null
        );
    }
}
