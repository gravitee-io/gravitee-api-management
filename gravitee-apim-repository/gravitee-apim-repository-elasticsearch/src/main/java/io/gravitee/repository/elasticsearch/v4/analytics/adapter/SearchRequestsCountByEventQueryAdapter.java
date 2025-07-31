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

import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.CountByAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountByEventQuery;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchRequestsCountByEventQueryAdapter {

    public static String adapt(RequestsCountByEventQuery query) {
        var jsonContent = new HashMap<String, Object>();
        jsonContent.put("size", 0);
        jsonContent.put("query", buildElasticQuery(query));

        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildElasticQuery(RequestsCountByEventQuery query) {
        List<JsonObject> filters = new ArrayList<>();
        addTermFilter(filters, query.searchTermId());
        filters.add(new JsonObject(TimeRangeAdapter.toRangeNode(query.timeRange()).toString()));
        return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(filters.toArray())));
    }

    private static void addTermFilter(List<JsonObject> filters, SearchTermId terms) {
        filters.add(JsonObject.of("term", JsonObject.of(terms.searchTerm().getField(), terms.id())));
    }

    public static Optional<CountByAggregate> adaptResponse(SearchResponse searchResponse) {
        if (searchResponse == null) {
            return Optional.empty();
        }
        long totalHits = searchResponse.getSearchHits().getTotal().getValue();
        return Optional.of(new CountByAggregate(totalHits));
    }
}
