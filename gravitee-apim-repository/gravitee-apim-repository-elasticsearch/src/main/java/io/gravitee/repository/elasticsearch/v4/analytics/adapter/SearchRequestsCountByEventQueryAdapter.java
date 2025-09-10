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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.CountByAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountByEventQuery;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchRequestsCountByEventQueryAdapter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String adapt(RequestsCountByEventQuery query) {
        var jsonContent = new HashMap<String, Object>();
        jsonContent.put("size", 0);
        jsonContent.put("query", buildElasticQuery(query));
        try {
            return objectMapper.writeValueAsString(jsonContent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize query", e);
        }
    }

    private static Map<String, Object> buildElasticQuery(RequestsCountByEventQuery query) {
        List<Object> filters = new ArrayList<>();
        addTermFilter(filters, query.searchTermId());
        filters.add(TimeRangeAdapter.toRangeNode(query.timeRange()));
        addQueryFilter(filters, query.query());
        Map<String, Object> boolNode = new HashMap<>();
        boolNode.put("must", filters);
        Map<String, Object> queryNode = new HashMap<>();
        queryNode.put("bool", boolNode);
        return queryNode;
    }

    private static void addQueryFilter(List<Object> filters, Optional<String> query) {
        // Query string support (as in count.ftl)
        query.ifPresent(q -> {
            if (!q.trim().isEmpty()) {
                filters.add(Map.of("query_string", Map.of("query", q)));
            }
        });
    }

    private static void addTermFilter(List<Object> filters, SearchTermId terms) {
        Map<String, Object> termNode = new HashMap<>();
        termNode.put(terms.searchTerm().getField(), terms.id());
        Map<String, Object> termWrapper = new HashMap<>();
        termWrapper.put("term", termNode);
        filters.add(termWrapper);
    }

    public static Optional<CountByAggregate> adaptResponse(SearchResponse searchResponse) {
        if (searchResponse == null) {
            return Optional.empty();
        }
        long totalHits = searchResponse.getSearchHits().getTotal().getValue();
        return Optional.of(new CountByAggregate(totalHits));
    }
}
