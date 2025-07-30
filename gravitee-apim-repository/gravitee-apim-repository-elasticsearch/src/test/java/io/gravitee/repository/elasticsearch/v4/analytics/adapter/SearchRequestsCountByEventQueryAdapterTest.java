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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import io.gravitee.repository.log.v4.model.analytics.CountByAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountByEventQuery;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class SearchRequestsCountByEventQueryAdapterTest {

    @Test
    void shouldAdaptQueryWithAllFields() {
        var query = RequestsCountByEventQuery
            .builder()
            .terms(Map.of("api-id", "api-123"))
            .timeRange(new TimeRange(Instant.ofEpochMilli(1650000000000L), Instant.ofEpochMilli(1650003600000L)))
            .build();

        String result = SearchRequestsCountByEventQueryAdapter.adapt(query);
        assertNotNull(result);

        var json = new JsonObject(result);

        assertEquals(0, json.getInteger("size"));

        var boolQuery = json.getJsonObject("query").getJsonObject("bool");
        var mustArray = boolQuery.getJsonArray("must");

        assertEquals(2, mustArray.size());

        JsonObject termEntry = mustArray.stream().map(JsonObject.class::cast).filter(o -> o.containsKey("term")).findFirst().orElseThrow();

        JsonObject rangeEntry = mustArray
            .stream()
            .map(JsonObject.class::cast)
            .filter(o -> o.containsKey("range"))
            .findFirst()
            .orElseThrow();

        assertEquals("api-123", termEntry.getJsonObject("term").getString("api-id"));

        var timestamp = rangeEntry.getJsonObject("range").getJsonObject("@timestamp");
        assertEquals(1650000000000L, timestamp.getLong("from"));
        assertTrue(timestamp.getBoolean("include_lower"));
        assertEquals(1650003600000L, timestamp.getLong("to"));
        assertTrue(timestamp.getBoolean("include_upper"));
    }

    @Test
    void shouldAdaptQueryWithOnlyTimestamps() {
        var query = RequestsCountByEventQuery
            .builder()
            .terms(Map.of())
            .timeRange(new TimeRange(Instant.ofEpochMilli(1650000000000L), Instant.ofEpochMilli(1650003600000L)))
            .build();

        String result = SearchRequestsCountByEventQueryAdapter.adapt(query);
        assertNotNull(result);

        var json = new JsonObject(result);

        assertEquals(0, json.getInteger("size"));

        var mustArray = json.getJsonObject("query").getJsonObject("bool").getJsonArray("must");
        assertEquals(1, mustArray.size());

        var rangeEntry = mustArray.getJsonObject(0).getJsonObject("range");
        var timestamp = rangeEntry.getJsonObject("@timestamp");

        assertEquals(1650000000000L, timestamp.getLong("from"));
        assertTrue(timestamp.getBoolean("include_lower"));
        assertEquals(1650003600000L, timestamp.getLong("to"));
        assertTrue(timestamp.getBoolean("include_upper"));
    }

    @Test
    void shouldAdaptQueryWithNullQuery() {
        String result = SearchRequestsCountByEventQueryAdapter.adapt(null);

        assertNotNull(result);
        assertEquals("{\"size\":0}", result);
    }

    @Test
    void shouldAdaptResponseWithValidSearchResponse() {
        var totalHits = new TotalHits(123L);
        var searchHits = new SearchHits();
        searchHits.setTotal(totalHits);

        var searchResponse = new SearchResponse();
        searchResponse.setSearchHits(searchHits);

        Optional<CountByAggregate> result = SearchRequestsCountByEventQueryAdapter.adaptResponse(searchResponse);

        assertTrue(result.isPresent());
        assertEquals(123L, result.get().getTotal());
    }

    @Test
    void shouldReturnEmptyWhenSearchResponseIsNull() {
        Optional<CountByAggregate> result = SearchRequestsCountByEventQueryAdapter.adaptResponse(null);
        assertTrue(result.isEmpty());
    }
}
