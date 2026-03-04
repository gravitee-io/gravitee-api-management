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

import io.gravitee.repository.log.v4.model.analytics.GroupByQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchGroupByQueryAdapter {

    private static final String GROUP_BY_AGG = "group_by_field";

    public static String adapt(GroupByQuery query) {
        var jsonContent = new HashMap<String, Object>();
        jsonContent.put("size", 0);

        var esQuery = buildElasticQuery(query);
        if (esQuery != null) {
            jsonContent.put("query", esQuery);
        }

        var termsAgg = new HashMap<String, Object>();
        termsAgg.put("field", query.field());
        termsAgg.put("size", query.size());

        var aggs = new HashMap<String, Object>();
        aggs.put(GROUP_BY_AGG, JsonObject.of("terms", new JsonObject(termsAgg)));
        jsonContent.put("aggs", aggs);

        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildElasticQuery(GroupByQuery query) {
        if (query == null) {
            return null;
        }

        var terms = new ArrayList<JsonObject>();

        if (query.apiId() != null) {
            terms.add(JsonObject.of("term", JsonObject.of("api-id", query.apiId())));
        }

        var timestamp = new JsonObject();
        if (query.from() != null) {
            timestamp.put("from", query.from().toEpochMilli()).put("include_lower", true);
        }
        if (query.to() != null) {
            timestamp.put("to", query.to().toEpochMilli()).put("include_upper", true);
        }

        if (!timestamp.isEmpty()) {
            terms.add(JsonObject.of("range", JsonObject.of("@timestamp", timestamp)));
        }

        if (!terms.isEmpty()) {
            return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(terms.toArray())));
        }

        return null;
    }
}
