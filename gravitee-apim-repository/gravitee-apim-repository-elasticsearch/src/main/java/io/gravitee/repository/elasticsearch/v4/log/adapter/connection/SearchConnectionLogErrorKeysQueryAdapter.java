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
import java.util.List;

public class SearchConnectionLogErrorKeysQueryAdapter {

    private SearchConnectionLogErrorKeysQueryAdapter() {}

    private static final String TIMESTAMP_RANGE_START = "now-7d";

    public static String adapt(SearchConnectionLogErrorKeysQuery query) {
        return JsonObject.of("size", 0, "query", buildQuery(query.apiId()), "aggs", buildAggregation(query.maxBuckets())).encode();
    }

    private static JsonObject buildQuery(String apiId) {
        return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(buildApiIdFilter(apiId), buildTimestampRangeFilter())));
    }

    /**
     * Matches the API ID against both the V2 {@code api} field and the V4 {@code api-id} field.
     * Follows the same {@code bool/should} pattern used throughout this adapter package.
     */
    private static JsonObject buildApiIdFilter(String apiId) {
        return buildShould(
            List.of(
                JsonObject.of("term", JsonObject.of(RequestV2MetricsV4Fields.API_ID.v2Request(), apiId)),
                JsonObject.of("term", JsonObject.of(RequestV2MetricsV4Fields.API_ID.v4Metrics(), apiId))
            )
        );
    }

    private static JsonObject buildTimestampRangeFilter() {
        return JsonObject.of("range", JsonObject.of("@timestamp", JsonObject.of("gte", TIMESTAMP_RANGE_START, "lte", "now")));
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
