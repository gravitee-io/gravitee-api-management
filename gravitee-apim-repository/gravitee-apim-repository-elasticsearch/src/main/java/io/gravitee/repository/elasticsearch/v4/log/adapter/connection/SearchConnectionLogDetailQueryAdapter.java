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
package io.gravitee.repository.elasticsearch.v4.log.adapter.connection;

import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetailQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;

public class SearchConnectionLogDetailQueryAdapter {

    private SearchConnectionLogDetailQueryAdapter() {}

    public static String adapt(ConnectionLogDetailQuery query) {
        var jsonContent = new HashMap<String, Object>();

        jsonContent.put("from", (query.getPage() - 1) * query.getSize());
        jsonContent.put("size", query.getSize());

        var esQuery = buildElasticQuery(query.getFilter());
        if (esQuery != null) {
            jsonContent.put("query", esQuery);
        }

        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildElasticQuery(ConnectionLogDetailQuery.Filter filter) {
        if (filter == null) {
            return null;
        }

        var mustFilterList = new ArrayList<JsonObject>();

        addApisFilter(filter, mustFilterList);

        addRequestIdsFilter(filter, mustFilterList);

        if (!mustFilterList.isEmpty()) {
            return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(mustFilterList.toArray())));
        }
        return null;

        }
    private static void addApisFilter(ConnectionLogDetailQuery.Filter filter, ArrayList<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getApiIds())) {
            mustFilterList.add(buildV2AndV4Terms("api", "api-id", filter.getApiIds()));
        }
        }
    private static void addRequestIdsFilter(ConnectionLogDetailQuery.Filter filter, ArrayList<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getRequestIds())) {
            mustFilterList.add(buildV2AndV4Terms("_id", "request-id", filter.getRequestIds()));
        }
    }

    private static JsonObject buildV2AndV4Terms(String v2Field, String v4Field, Collection<?> value) {
        var terms = new ArrayList<JsonObject>();
        terms.add(JsonObject.of("terms", JsonObject.of(v2Field, value.toArray())));
        terms.add(JsonObject.of("terms", JsonObject.of(v4Field, value.toArray())));
        return buildShould(terms);
    }

    private static JsonObject buildV2AndV4Matches(String v2Field, String v4Field, Collection<?> value) {
        var matches = new ArrayList<JsonObject>();
        value.forEach(v -> {
            matches.add(JsonObject.of("match", JsonObject.of(v2Field, v)));
            matches.add(JsonObject.of("match", JsonObject.of(v4Field, v)));
        });
        return buildShould(matches);
    }

    private static JsonObject buildShould(List<JsonObject> terms) {
        return JsonObject.of("bool", JsonObject.of("should", JsonArray.of(terms.toArray())));
    }
}
