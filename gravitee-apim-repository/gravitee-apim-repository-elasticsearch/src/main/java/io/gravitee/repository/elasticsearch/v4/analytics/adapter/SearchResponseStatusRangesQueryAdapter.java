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

import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchResponseStatusRangesQueryAdapter {

    public static final String FIELD = "field";
    public static final String STATUS_RANGES = "status_ranges";

    public static String adapt(ResponseStatusRangesQuery query) {
        var jsonContent = new HashMap<String, Object>();
        var esQuery = buildElasticQuery(Optional.ofNullable(query).orElse(ResponseStatusRangesQuery.builder().build()));
        jsonContent.put("size", 0);
        jsonContent.put("query", esQuery);
        jsonContent.put("aggs", buildResponseCountPerStatusCodeRangePerEntrypointAggregation());
        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildResponseCountPerStatusCodeRangePerEntrypointAggregation() {
        return JsonObject.of(
            STATUS_RANGES,
            JsonObject.of(
                "range",
                JsonObject.of(
                    FIELD,
                    "status",
                    "ranges",
                    JsonArray.of(
                        JsonObject.of("from", 100.0, "to", 200.0),
                        JsonObject.of("from", 200.0, "to", 300.0),
                        JsonObject.of("from", 300.0, "to", 400.0),
                        JsonObject.of("from", 400.0, "to", 500.0),
                        JsonObject.of("from", 500.0, "to", 600.0)
                    )
                )
            )
        );
    }

    private static JsonObject buildElasticQuery(ResponseStatusRangesQuery query) {
        return JsonObject.of("term", JsonObject.of("api-id", query.getApiId()));
    }
}
