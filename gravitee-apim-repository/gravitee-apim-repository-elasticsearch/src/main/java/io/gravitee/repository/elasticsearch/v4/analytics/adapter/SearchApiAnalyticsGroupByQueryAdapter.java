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

import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByOrder;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchApiAnalyticsGroupByQueryAdapter {

    private static final int DEFAULT_SIZE = 10;

    public static String adapt(ApiAnalyticsGroupByQuery query) {
        var jsonContent = new HashMap<String, Object>();
        jsonContent.put("size", 0);

        var esQuery = buildElasticQuery(query);
        if (esQuery != null) {
            jsonContent.put("query", esQuery);
        }

        var field = query != null && query.field().isPresent() ? query.field().get().esFieldName() : null;
        var size = query != null && query.size().isPresent() ? query.size().get() : DEFAULT_SIZE;

        JsonObject order = null;
        var orderEnum = query != null ? query.order().orElse(null) : null;
        if (orderEnum != null) {
            order =
                switch (orderEnum) {
                    case COUNT_DESC -> JsonObject.of("_count", "desc");
                    case COUNT_ASC -> JsonObject.of("_count", "asc");
                };
        }

        var terms = JsonObject.of("field", field).put("size", size);
        if (order != null) {
            terms.put("order", order);
        }
        jsonContent.put("aggs", JsonObject.of("group_by", JsonObject.of("terms", terms)));

        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildElasticQuery(ApiAnalyticsGroupByQuery query) {
        if (query == null) {
            return null;
        }

        var filter = new ArrayList<JsonObject>();

        query
            .apiId()
            .ifPresent(apiId -> {
                var should = new ArrayList<JsonObject>();
                should.add(JsonObject.of("term", JsonObject.of("api-id", apiId)));
                should.add(JsonObject.of("term", JsonObject.of("api", apiId)));
                filter.add(JsonObject.of("bool", JsonObject.of("should", JsonArray.of(should.toArray()))));
            });

        var timestamp = new JsonObject();
        query.from().ifPresent(from -> timestamp.put("from", from.toEpochMilli()).put("include_lower", true));
        query.to().ifPresent(to -> timestamp.put("to", to.toEpochMilli()).put("include_upper", true));
        if (!timestamp.isEmpty()) {
            filter.add(JsonObject.of("range", JsonObject.of("@timestamp", timestamp)));
        }

        if (filter.isEmpty()) {
            return null;
        }

        return JsonObject.of("bool", JsonObject.of("filter", JsonArray.of(filter.toArray())));
    }
}
