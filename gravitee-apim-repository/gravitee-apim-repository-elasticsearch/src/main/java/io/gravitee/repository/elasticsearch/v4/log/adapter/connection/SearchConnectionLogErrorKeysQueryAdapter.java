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

import io.gravitee.repository.log.v4.model.connection.SearchConnectionLogErrorKeysQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collection;
import java.util.List;

public class SearchConnectionLogErrorKeysQueryAdapter {

    private SearchConnectionLogErrorKeysQueryAdapter() {}

    public static String adapt(SearchConnectionLogErrorKeysQuery query) {
        return JsonObject.of(
            "size",
            0,
            "query",
            buildQuery(query.apiIds(), query.from(), query.to()),
            "aggs",
            buildAggregation(query.maxBuckets())
        ).encode();
    }

    private static JsonObject buildQuery(Collection<String> apiIds, Long from, Long to) {
        var mustClauses = new JsonArray();
        mustClauses.add(buildApiIdsFilter(apiIds));
        mustClauses.add(buildTimestampRangeFilter(from, to));
        return JsonObject.of("bool", JsonObject.of("must", mustClauses));
    }

    private static JsonObject buildApiIdsFilter(Collection<String> apiIds) {
        var ids = new JsonArray(List.copyOf(apiIds));
        return buildShould(
            List.of(
                JsonObject.of("terms", JsonObject.of(RequestV2MetricsV4Fields.API_ID.v2Request(), ids)),
                JsonObject.of("terms", JsonObject.of(RequestV2MetricsV4Fields.API_ID.v4Metrics(), ids))
            )
        );
    }

    private static JsonObject buildTimestampRangeFilter(Long from, Long to) {
        long now = System.currentTimeMillis();
        var range = new JsonObject();
        range.put("gte", from != null ? from : now - 24 * 60 * 60 * 1000L);
        range.put("lte", to != null ? to : now);
        return JsonObject.of("range", JsonObject.of("@timestamp", range));
    }

    private static JsonObject buildAggregation(int maxBuckets) {
        return JsonObject.of(
            "error_keys",
            JsonObject.of("terms", JsonObject.of("field", RequestV2MetricsV4Fields.ERROR_KEY, "size", maxBuckets))
        );
    }

    private static JsonObject buildShould(List<JsonObject> clauses) {
        return JsonObject.of("bool", JsonObject.of("should", JsonArray.of(clauses.toArray())));
    }
}
