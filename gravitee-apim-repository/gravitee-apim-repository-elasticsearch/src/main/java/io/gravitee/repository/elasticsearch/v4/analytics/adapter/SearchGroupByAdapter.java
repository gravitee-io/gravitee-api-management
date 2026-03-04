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
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByQueryCriteria;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchGroupByAdapter {

    private static final String GROUP_BY_VALUES = "group_by_values";
    private static final String FIELD = "field";
    private static final String SIZE = "size";
    private static final String ORDER = "order";
    private static final String KEY = "key";
    private static final String DOC_COUNT = "doc_count";
    private static final String STATUS = "status";
    private static final int DEFAULT_SIZE = 10;

    public static String adaptQuery(GroupByQueryCriteria query) {
        var jsonContent = new HashMap<String, Object>();
        var safeQuery = Optional.ofNullable(query).orElse(new GroupByQueryCriteria());

        jsonContent.put("size", 0);
        jsonContent.put("query", buildElasticQuery(safeQuery));
        jsonContent.put("aggs", buildGroupByAggregate(safeQuery));
        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildGroupByAggregate(GroupByQueryCriteria query) {
        var field = query.field().orElse(STATUS);
        var size = query.size().orElse(DEFAULT_SIZE);
        var order = query.order().orElse(GroupByQueryCriteria.Order.DESC) == GroupByQueryCriteria.Order.ASC ? "asc" : "desc";

        return JsonObject.of(
            GROUP_BY_VALUES,
            JsonObject.of(
                "terms",
                JsonObject.of(
                    FIELD,
                    field,
                    SIZE,
                    size,
                    ORDER,
                    new JsonArray().add(JsonObject.of("_count", order)).add(JsonObject.of("_key", "asc"))
                )
            )
        );
    }

    private static JsonObject buildElasticQuery(GroupByQueryCriteria query) {
        var terms = new ArrayList<JsonObject>();
        query.apiId().ifPresent(apiId -> terms.add(JsonObject.of("term", JsonObject.of("api-id", apiId))));

        var timestamp = new JsonObject();
        query.from().ifPresent(from -> timestamp.put("from", from.toEpochMilli()).put("include_lower", true));
        query.to().ifPresent(to -> timestamp.put("to", to.toEpochMilli()).put("include_upper", true));

        if (!timestamp.isEmpty()) {
            terms.add(JsonObject.of("range", JsonObject.of("@timestamp", timestamp)));
        }

        if (terms.isEmpty()) {
            return JsonObject.of("match_all", JsonObject.of());
        }

        return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(terms.toArray())));
    }

    public static Optional<GroupByAggregate> adaptResponse(SearchResponse response) {
        var aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }

        var aggregation = aggregations.get(GROUP_BY_VALUES);
        if (aggregation == null || aggregation.getBuckets() == null) {
            return Optional.of(GroupByAggregate.builder().values(Map.of()).build());
        }

        var values = new LinkedHashMap<String, Long>();
        aggregation.getBuckets().forEach(bucket -> values.put(bucket.get(KEY).asText(), bucket.get(DOC_COUNT).asLong()));

        return Optional.of(GroupByAggregate.builder().values(values).build());
    }
}
