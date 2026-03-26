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

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchGroupByAdapter {

    private static final String GROUP_BY_AGG = "group_by_field";

    public static String adaptQuery(GroupByQuery query) {
        var jsonContent = new JsonObject();
        jsonContent.put("size", 0);

        var esQuery = buildElasticQuery(query);
        if (esQuery != null) {
            jsonContent.put("query", esQuery);
        }

        jsonContent.put("aggs", buildGroupByAggregation(query.field(), query.size()));
        return jsonContent.encode();
    }

    public static Optional<GroupByAggregate> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }

        var groupByAgg = aggregations.get(GROUP_BY_AGG);
        if (groupByAgg == null || groupByAgg.getBuckets() == null) {
            return Optional.empty();
        }

        var values = new LinkedHashMap<String, Long>();
        groupByAgg
            .getBuckets()
            .forEach(bucket -> {
                var key = bucket.get("key").asText();
                var docCount = bucket.get("doc_count").asLong();
                values.put(key, docCount);
            });

        return Optional.of(GroupByAggregate.builder().values(values).build());
    }

    private static JsonObject buildGroupByAggregation(String field, int size) {
        return JsonObject.of(
            GROUP_BY_AGG,
            JsonObject.of("terms", JsonObject.of("field", field, "size", size, "order", JsonObject.of("_count", "desc")))
        );
    }

    private static JsonObject buildElasticQuery(GroupByQuery query) {
        if (query == null) {
            return null;
        }

        var terms = new ArrayList<JsonObject>();
        query.optionalApiId().ifPresent(apiId -> terms.add(JsonObject.of("term", JsonObject.of("api-id", apiId))));

        var timestamp = new JsonObject();
        query.optionalFrom().ifPresent(from -> timestamp.put("from", from.toEpochMilli()).put("include_lower", true));
        query.optionalTo().ifPresent(to -> timestamp.put("to", to.toEpochMilli()).put("include_upper", true));

        if (!timestamp.isEmpty()) {
            terms.add(JsonObject.of("range", JsonObject.of("@timestamp", timestamp)));
        }

        if (!terms.isEmpty()) {
            return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(terms.toArray())));
        }

        return null;
    }
}
