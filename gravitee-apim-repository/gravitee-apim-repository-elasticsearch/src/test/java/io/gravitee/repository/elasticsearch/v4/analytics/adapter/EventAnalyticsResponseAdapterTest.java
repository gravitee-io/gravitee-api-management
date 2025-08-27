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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EventAnalyticsResponseAdapterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    class AdaptResponse {

        @Test
        void should_return_empty_when_response_is_null() {
            var result = EventAnalyticsResponseAdapter.adapt(null);

            assertTrue(result.isEmpty());
        }

        @Test
        void should_return_empty_when_response_is_timed_out() {
            var response = new SearchResponse();
            response.setTimedOut(true);

            var result = EventAnalyticsResponseAdapter.adapt(response);

            assertTrue(result.isEmpty());
        }

        @Test
        void should_return_empty_when_response_has_no_hits() {
            var response = new SearchResponse();
            response.setTimedOut(false);
            response.setAggregations(Map.of());

            var result = EventAnalyticsResponseAdapter.adapt(response);

            assertTrue(result.isEmpty());
        }

        @Test
        void should_adapt_response() {
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
            subAggs1.put("top_hits", topHits1);
            agg1.getAggregations().putAll(subAggs1);
            aggregations.put("downstream-active-connections_latest", agg1);

            // upstream-active-connections_latest
            io.gravitee.elasticsearch.model.Aggregation agg2 = new io.gravitee.elasticsearch.model.Aggregation();
            io.gravitee.elasticsearch.model.Aggregation topHits2 = new io.gravitee.elasticsearch.model.Aggregation();
            SearchHits hits2 = new SearchHits();
            hits2.setHits(List.of(createHit("upstream-active-connections", 24L)));
            topHits2.setHits(hits2);

            Map<String, io.gravitee.elasticsearch.model.Aggregation> subAggs2 = new HashMap<>();
            subAggs2.put("top_hits", topHits2);
            agg2.getAggregations().putAll(subAggs2);
            aggregations.put("upstream-active-connections_latest", agg2);

            response.setAggregations(aggregations);

            var result = EventAnalyticsResponseAdapter.adapt(response);

            assertTrue(result.isPresent());
            Map<String, Map<String, Long>> values = result.get().values();
            assertFalse(values.isEmpty());
            assertTrue(values.containsKey("downstream-active-connections_latest"));
            assertTrue(values.containsKey("upstream-active-connections_latest"));
            assertEquals(24L, values.get("downstream-active-connections_latest").get("downstream-active-connections"));
            assertEquals(24L, values.get("upstream-active-connections_latest").get("upstream-active-connections"));
        }

        private static @NotNull SearchHit createHit(String fieldName, long value) {
            SearchHit hit = new SearchHit();
            var source = MAPPER.createObjectNode();
            source.put(fieldName, value);
            hit.setSource(source);

            return hit;
        }
    }
}
