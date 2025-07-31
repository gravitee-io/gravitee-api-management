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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import io.gravitee.repository.log.v4.model.analytics.CountByAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountByEventQuery;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class SearchRequestsCountByEventQueryAdapterTest {

    @Test
    void shouldAdaptQueryWithAllFields() throws Exception {
        var query = new RequestsCountByEventQuery(
            new SearchTermId(SearchTermId.SearchTerm.API, "api-123"),
            new TimeRange(Instant.ofEpochMilli(1650000000000L), Instant.ofEpochMilli(1650003600000L))
        );
        String result = SearchRequestsCountByEventQueryAdapter.adapt(query);
        assertNotNull(result);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(result);

        assertEquals(0, node.at("/size").asInt());
        assertEquals("api-123", node.at("/query/bool/must/0/term/api-id").asText());
        assertEquals(1650000000000L, node.at("/query/bool/must/1/range/@timestamp/from").asLong());
        assertTrue(node.at("/query/bool/must/1/range/@timestamp/include_lower").asBoolean());
        assertEquals(1650003600000L, node.at("/query/bool/must/1/range/@timestamp/to").asLong());
        assertTrue(node.at("/query/bool/must/1/range/@timestamp/include_upper").asBoolean());
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
        assertEquals(123L, result.get().total());
    }

    @Test
    void shouldReturnEmptyWhenSearchResponseIsNull() {
        Optional<CountByAggregate> result = SearchRequestsCountByEventQueryAdapter.adaptResponse(null);
        assertTrue(result.isEmpty());
    }
}
