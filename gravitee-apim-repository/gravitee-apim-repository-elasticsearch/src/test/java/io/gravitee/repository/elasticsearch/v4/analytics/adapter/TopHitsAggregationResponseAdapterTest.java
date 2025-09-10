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

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.END_VALUE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.PER_INTERVAL;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.START_VALUE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.TOP_HITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.Aggregation;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TopHitsAggregationResponseAdapterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String API_ID = "273f4728-1e30-4c78-bf47-281e304c78a5";
    private static final Long FROM = 1756104349879L;
    private static final Long TO = 1756106149879L;

    @Nested
    class AdaptResponse {

        @Test
        void should_return_empty_when_response_is_null() {
            var result = TopHitsAggregationResponseAdapter.adapt(null, null);

            assertTrue(result.isEmpty());
        }

        @Test
        void should_return_empty_when_response_is_timed_out() {
            var response = new SearchResponse();
            response.setTimedOut(true);

            var result = TopHitsAggregationResponseAdapter.adapt(response, null);

            assertTrue(result.isEmpty());
        }

        @Test
        void should_return_empty_when_response_has_no_hits() {
            List<Aggregation> aggregations = getAggregations(
                AggregationType.VALUE,
                "downstream-active-connections",
                "upstream-active-connections"
            );
            var response = new SearchResponse();
            response.setTimedOut(false);
            response.setAggregations(Map.of());

            var result = TopHitsAggregationResponseAdapter.adapt(response, buildHistogramQuery(aggregations));

            assertTrue(result.isEmpty());
        }

        @Test
        void should_adapt_top_value_hits_response() {
            List<Aggregation> aggs = getAggregations(AggregationType.VALUE, "downstream-active-connections", "upstream-active-connections");
            var response = new SearchResponse();
            response.setTimedOut(false);
            Map<String, io.gravitee.elasticsearch.model.Aggregation> aggregations = new HashMap<>();

            // downstream-active-connections_latest
            io.gravitee.elasticsearch.model.Aggregation agg1 = new io.gravitee.elasticsearch.model.Aggregation();
            io.gravitee.elasticsearch.model.Aggregation topHits1 = new io.gravitee.elasticsearch.model.Aggregation();
            SearchHits hits1 = new SearchHits();
            hits1.setHits(List.of(createHit("downstream-active-connections", 24L)));
            topHits1.setHits(hits1);

            Map<String, io.gravitee.elasticsearch.model.Aggregation> subAggs1 = new HashMap<>();
            subAggs1.put(TOP_HITS, topHits1);
            agg1.getAggregations().putAll(subAggs1);
            aggregations.put("downstream-active-connections_latest", agg1);

            // upstream-active-connections_latest
            io.gravitee.elasticsearch.model.Aggregation agg2 = new io.gravitee.elasticsearch.model.Aggregation();
            io.gravitee.elasticsearch.model.Aggregation topHits2 = new io.gravitee.elasticsearch.model.Aggregation();
            SearchHits hits2 = new SearchHits();
            hits2.setHits(List.of(createHit("upstream-active-connections", 24L)));
            topHits2.setHits(hits2);

            Map<String, io.gravitee.elasticsearch.model.Aggregation> subAggs2 = new HashMap<>();
            subAggs2.put(TOP_HITS, topHits2);
            agg2.getAggregations().putAll(subAggs2);
            aggregations.put("upstream-active-connections_latest", agg2);

            response.setAggregations(aggregations);

            var result = TopHitsAggregationResponseAdapter.adapt(response, buildHistogramQuery(aggs));

            assertTrue(result.isPresent());
            Map<String, Map<String, List<Long>>> values = result.get().values();
            assertFalse(values.isEmpty());
            assertTrue(values.containsKey("downstream-active-connections_latest"));
            assertTrue(values.containsKey("upstream-active-connections_latest"));
            assertEquals(24L, values.get("downstream-active-connections_latest").get("downstream-active-connections").getFirst());
            assertEquals(24L, values.get("upstream-active-connections_latest").get("upstream-active-connections").getFirst());
        }

        @Test
        void should_adapt_top_delta_hits_response() {
            List<Aggregation> aggs = getAggregations(
                AggregationType.DELTA,
                "downstream-publish-messages-total",
                "upstream-publish-messages-total"
            );
            var response = new SearchResponse();
            response.setTimedOut(false);
            Map<String, io.gravitee.elasticsearch.model.Aggregation> aggregations = new HashMap<>();

            // downstream-publish-messages-total_delta
            io.gravitee.elasticsearch.model.Aggregation agg = new io.gravitee.elasticsearch.model.Aggregation();
            io.gravitee.elasticsearch.model.Aggregation startValue = new io.gravitee.elasticsearch.model.Aggregation();
            io.gravitee.elasticsearch.model.Aggregation endValue = new io.gravitee.elasticsearch.model.Aggregation();

            SearchHits startHits = new SearchHits();
            startHits.setHits(List.of(createHit("downstream-publish-messages-total", 10L)));
            startValue.setHits(startHits);

            SearchHits endHits = new SearchHits();
            endHits.setHits(List.of(createHit("downstream-publish-messages-total", 150L)));
            endValue.setHits(endHits);

            Map<String, io.gravitee.elasticsearch.model.Aggregation> subAggs = new HashMap<>();
            subAggs.put(START_VALUE, startValue);
            subAggs.put(END_VALUE, endValue);
            agg.getAggregations().putAll(subAggs);
            aggregations.put("downstream-publish-messages-total_delta", agg);

            // upstream-publish-messages-total_delta
            io.gravitee.elasticsearch.model.Aggregation agg2 = new io.gravitee.elasticsearch.model.Aggregation();
            io.gravitee.elasticsearch.model.Aggregation startValue2 = new io.gravitee.elasticsearch.model.Aggregation();
            io.gravitee.elasticsearch.model.Aggregation endValue2 = new io.gravitee.elasticsearch.model.Aggregation();

            SearchHits startHits2 = new SearchHits();
            startHits2.setHits(List.of(createHit("upstream-publish-messages-total", 10L)));
            startValue2.setHits(startHits2);

            SearchHits endHits2 = new SearchHits();
            endHits2.setHits(List.of(createHit("upstream-publish-messages-total", 150L)));
            endValue2.setHits(endHits2);

            Map<String, io.gravitee.elasticsearch.model.Aggregation> subAggs2 = new HashMap<>();
            subAggs2.put(START_VALUE, startValue2);
            subAggs2.put(END_VALUE, endValue2);
            agg2.getAggregations().putAll(subAggs2);
            aggregations.put("upstream-publish-messages-total_delta", agg2);

            response.setAggregations(aggregations);

            var result = TopHitsAggregationResponseAdapter.adapt(response, buildHistogramQuery(aggs));

            assertTrue(result.isPresent());
            Map<String, Map<String, List<Long>>> values = result.get().values();
            assertFalse(values.isEmpty());
            assertTrue(values.containsKey("downstream-publish-messages-total_delta"));
            assertTrue(values.containsKey("upstream-publish-messages-total_delta"));
            assertEquals(140L, values.get("downstream-publish-messages-total_delta").get("downstream-publish-messages-total").getFirst());
            assertEquals(140L, values.get("upstream-publish-messages-total_delta").get("upstream-publish-messages-total").getFirst());
        }

        @Test
        void should_adapt_top_trend_response() throws IOException {
            List<Aggregation> aggs = getAggregations(
                AggregationType.TREND,
                "downstream-subscribe-messages-total",
                "upstream-subscribe-messages-total"
            );
            var response = new SearchResponse();
            response.setTimedOut(false);
            Map<String, io.gravitee.elasticsearch.model.Aggregation> map = new HashMap<>();
            io.gravitee.elasticsearch.model.Aggregation agg = new io.gravitee.elasticsearch.model.Aggregation();
            agg.setBuckets(getDeltaBuckets());
            map.put(PER_INTERVAL, agg);
            response.setAggregations(map);

            var result = TopHitsAggregationResponseAdapter.adapt(response, buildHistogramQuery(aggs));

            assertTrue(result.isPresent());
            Map<String, Map<String, List<Long>>> values = result.get().values();
            assertFalse(values.isEmpty());
            assertTrue(values.containsKey("downstream-subscribe-messages-total_delta"));
            assertTrue(values.containsKey("upstream-subscribe-messages-total_delta"));
            assertEquals(
                120L,
                values.get("downstream-subscribe-messages-total_delta").get("downstream-subscribe-messages-total").getFirst()
            );
            assertEquals(120L, values.get("upstream-subscribe-messages-total_delta").get("upstream-subscribe-messages-total").getFirst());
        }

        private @NotNull List<JsonNode> getDeltaBuckets() throws IOException {
            InputStream stream = this.getClass().getResourceAsStream("/buckets/event-metrics-delta-buckets.json");
            JsonNode node = MAPPER.readTree(stream);

            JsonNode upstream = node.get("upstream-subscribe-messages-total_delta");
            JsonNode downstream = node.get("downstream-subscribe-messages-total_delta");
            JsonNode node1 = MAPPER.createObjectNode().set("upstream-subscribe-messages-total_delta", upstream);
            JsonNode node2 = MAPPER.createObjectNode().set("downstream-subscribe-messages-total_delta", downstream);

            return List.of(node1, node2);
        }

        private static @NotNull SearchHit createHit(String fieldName, long value) {
            SearchHit hit = new SearchHit();
            var source = MAPPER.createObjectNode();
            source.put(fieldName, value);
            hit.setSource(source);

            return hit;
        }
    }

    private static @NotNull List<Aggregation> getAggregations(AggregationType value, String field1, String field2) {
        Aggregation aggx = new Aggregation(field1, value);
        Aggregation aggy = new Aggregation(field2, value);

        return List.of(aggx, aggy);
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
