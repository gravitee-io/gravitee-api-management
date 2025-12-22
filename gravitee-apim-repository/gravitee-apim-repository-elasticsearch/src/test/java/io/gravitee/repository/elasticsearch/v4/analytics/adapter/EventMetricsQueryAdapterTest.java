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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.*;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.*;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.*;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.*;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.DESC;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens.FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.log.v4.model.analytics.Aggregation;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class EventMetricsQueryAdapterTest {

    private static final String API_ID = "273f4728-1e30-4c78-bf47-281e304c78a5";
    private static final Long FROM = 1756104349879L;
    private static final Long TO = 1756190749879L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void should_throw_when_no_aggregate_functions_specified() {
        assertThrows(IllegalArgumentException.class, () ->
            EventMetricsQueryAdapter.toESQuery(buildHistogramQuery(API_ID, Instant.now(), Instant.now(), List.of(), null))
        );
    }

    @Test
    void should_throw_when_more_than_one_aggregate_functions_specified() {
        Aggregation agg1 = new Aggregation("downstream-active-connections", AggregationType.VALUE);
        Aggregation agg2 = new Aggregation("upstream-active-connections", AggregationType.DELTA);
        HistogramQuery query = buildHistogramQuery(API_ID, Instant.now(), Instant.now(), List.of(agg1, agg2), null);

        assertThrows(IllegalArgumentException.class, () -> EventMetricsQueryAdapter.toESQuery(query));
    }

    @Test
    void should_build_value_query() throws Exception {
        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofHours(14));
        List<Aggregation> aggregations = List.of(new Aggregation("downstream-active-connections", AggregationType.VALUE));
        HistogramQuery query = buildHistogramQuery(API_ID, from, to, aggregations, null);

        String json = EventMetricsQueryAdapter.toESQuery(query);

        JsonNode node = MAPPER.readTree(json);
        // Root settings
        assertEquals(0, node.get(SIZE).asInt());
        assertFalse(node.get(TRACK_TOTAL_HITS).asBoolean());

        // Query filters
        JsonNode filters = node.at("/query/bool/filter");
        assertThat(filters).hasSize(2);
        assertTrue(hasTermFilter(filters, "api-id", API_ID));
        assertTrue(hasTimestampRange(filters));

        // Aggregations
        JsonNode aggs = node.get(AGGS);
        assertNotNull(aggs);

        String field = "downstream-active-connections";
        JsonNode filteredAgg = aggs.get(FILTERED_PREFIX + field);
        assertNotNull(filteredAgg);
        assertEquals("api", filteredAgg.at("/filter/term/doc-type").asText());

        JsonNode latestAgg = filteredAgg.at("/aggs/" + LATEST_PREFIX + field);
        assertNotNull(latestAgg);
        JsonNode topHits = latestAgg.get(TOP_HITS);
        assertEquals(1, topHits.get(SIZE).asInt());
        assertEquals(DESC, topHits.at("/sort/0/" + TIMESTAMP + "/order").asText());
        assertEquals(field, topHits.at("/_source/includes/0").asText());
    }

    @Test
    void should_build_delta_query() throws JsonProcessingException {
        String apiId = API_ID;
        Instant from = Instant.ofEpochMilli(FROM);
        Instant to = Instant.ofEpochMilli(TO);
        List<Aggregation> aggregations = List.of(new Aggregation("downstream-publish-messages-count-increment", AggregationType.DELTA));
        HistogramQuery query = buildHistogramQuery(apiId, from, to, aggregations, null);

        String json = EventMetricsQueryAdapter.toESQuery(query);

        JsonNode node = MAPPER.readTree(json);
        // Root settings
        assertEquals(0, node.get(SIZE).asInt());
        assertFalse(node.get(TRACK_TOTAL_HITS).asBoolean());

        // Query filters
        JsonNode filters = node.at("/query/bool/filter");
        assertThat(filters).hasSize(2);
        assertTrue(hasTermFilter(filters, "api-id", apiId));
        assertTrue(hasTimestampRange(filters));

        // Aggregations
        JsonNode aggs = node.get(AGGS);
        assertNotNull(aggs);

        String field = "downstream-publish-messages-count-increment";
        JsonNode filteredAgg = aggs.get(FILTERED_PREFIX + field);
        assertNotNull(filteredAgg);
        assertEquals("topic", filteredAgg.at("/filter/term/doc-type").asText());

        JsonNode totalAgg = filteredAgg.at("/aggs/" + TOTAL_PREFIX + field);
        assertNotNull(totalAgg);
        assertEquals(field, totalAgg.at("/sum/field").asText());
    }

    @Test
    void should_build_trend_query() throws JsonProcessingException {
        Aggregation agg1 = new Aggregation("downstream-publish-messages-count-increment", AggregationType.TREND);
        HistogramQuery query = buildHistogramQuery(
            API_ID,
            Instant.ofEpochMilli(FROM),
            Instant.ofEpochMilli(TO),
            List.of(agg1),
            Duration.ofMillis(60_000)
        );

        String json = EventMetricsQueryAdapter.toESQuery(query);

        JsonNode node = MAPPER.readTree(json);
        // Root settings
        assertEquals(0, node.get(SIZE).asInt());
        assertFalse(node.get(TRACK_TOTAL_HITS).asBoolean());

        // Query filters
        JsonNode filters = node.get(QUERY).get(BOOL).get(FILTER);
        assertThat(filters).hasSize(2);
        assertTrue(hasTermFilter(filters, "api-id", API_ID));
        assertTrue(hasTimestampRange(filters));

        // Aggregations
        JsonNode aggs = node.get(AGGS);
        assertNotNull(aggs);

        JsonNode perInterval = aggs.get(PER_INTERVAL);
        assertNotNull(perInterval);
        JsonNode histogram = perInterval.get(DATE_HISTOGRAM);
        assertNotNull(histogram);
        assertEquals(TIMESTAMP, histogram.get(FIELD).asText());
        assertEquals("60000ms", histogram.get(FIXED_INTERVAL).asText());
        assertEquals(0, histogram.get(MIN_DOC_COUNT).asInt());
        assertEquals(FROM, histogram.at("/extended_bounds/min").asLong());
        assertEquals(TO, histogram.at("/extended_bounds/max").asLong());

        String field = "downstream-publish-messages-count-increment";
        JsonNode perIntervalAggs = perInterval.get(AGGS);
        JsonNode totalAgg = perIntervalAggs.get(TOTAL_PREFIX + field);
        assertNotNull(totalAgg);
        assertEquals(field, totalAgg.at("/sum/field").asText());
    }

    private static boolean hasTermFilter(JsonNode filters, String field, String value) {
        for (JsonNode f : filters) {
            if (f.has(TERM) && f.get(TERM).has(field) && value.equals(f.get(TERM).get(field).asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTimestampRange(JsonNode filters) {
        for (JsonNode f : filters) {
            if (f.has(RANGE) && f.get(RANGE).has(TIMESTAMP)) {
                JsonNode ts = f.get(RANGE).get(TIMESTAMP);
                return (ts.hasNonNull(GTE)) && (ts.hasNonNull(LTE));
            }
        }
        return false;
    }

    private static @NotNull HistogramQuery buildHistogramQuery(
        String apiId,
        Instant from,
        Instant to,
        List<Aggregation> aggregations,
        Duration interval
    ) {
        var searchTermId = new SearchTermId(SearchTermId.SearchTerm.API, apiId);
        var timeRange = new io.gravitee.repository.log.v4.model.analytics.TimeRange(
            from,
            to,
            interval == null ? Optional.empty() : Optional.of(interval)
        );

        return new HistogramQuery(searchTermId, timeRange, aggregations, Optional.empty(), null);
    }
}
