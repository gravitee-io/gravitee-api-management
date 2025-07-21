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
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.StatsQuery;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StatsQueryAdapterTest {

    private static final String FIELD = "response-time";
    private static final String API_ID = "api-123";
    private static final long FROM = 1700000000000L;
    private static final long TO = 1700003600000L;

    private final StatsQueryAdapter cut = new StatsQueryAdapter();

    @Nested
    class AdaptQuery {

        @Test
        void shouldGenerateCorrectQueryJson() throws Exception {
            StatsQuery query = new StatsQuery(FIELD, API_ID, new StatsQuery.TimeRange(FROM, TO));
            String json = cut.adapt(query);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            assertEquals(0, node.get("size").asInt());
            assertEquals(API_ID, node.at("/query/bool/filter/0/term/api-id").asText());
            assertEquals(FROM, node.at("/query/bool/filter/1/range/@timestamp/from").asLong());
            assertEquals(TO, node.at("/query/bool/filter/1/range/@timestamp/to").asLong());
            assertEquals(FIELD, node.at("/aggregations/by_response-time/stats/field").asText());
        }
    }

    @Nested
    class AdaptResponse {

        @Test
        void shouldAdaptResponseCorrectly() {
            cut.adapt(new StatsQuery(FIELD, API_ID, new StatsQuery.TimeRange(1L, 3601L)));

            Aggregation agg = new Aggregation();
            agg.setCount(3600f);
            agg.setSum(100f);
            agg.setAvg(10f);
            agg.setMin(1f);
            agg.setMax(20f);

            HashMap<String, Aggregation> aggs = new HashMap<>();
            aggs.put("by_response-time", agg);

            SearchResponse response = new SearchResponse();
            response.setAggregations(aggs);

            Optional<StatsAggregate> result = cut.adaptResponse(response);
            assertTrue(result.isPresent());
            StatsAggregate stats = result.get();
            assertEquals(FIELD, stats.field());
            assertEquals(3600L, stats.count());
            assertEquals(100f, stats.sum());
            assertEquals(10f, stats.avg());
            assertEquals(1f, stats.min());
            assertEquals(20f, stats.max());
            assertEquals(1200f, stats.rps());
            assertEquals(72000f, stats.rpm());
            assertEquals(4320000f, stats.rph());
        }

        @Test
        void shouldReturnEmptyIfNoAggregations() {
            cut.adapt(new StatsQuery(FIELD, API_ID, new StatsQuery.TimeRange(1L, 2L)));
            SearchResponse response = new SearchResponse();
            response.setAggregations(null);
            assertTrue(cut.adaptResponse(response).isEmpty());
        }

        @Test
        void shouldReturnEmptyIfNoMatchingAggregation() {
            cut.adapt(new StatsQuery(FIELD, API_ID, new StatsQuery.TimeRange(1L, 2L)));
            SearchResponse response = new SearchResponse();
            response.setAggregations(new HashMap<>());
            assertTrue(cut.adaptResponse(response).isEmpty());
        }
    }
}
