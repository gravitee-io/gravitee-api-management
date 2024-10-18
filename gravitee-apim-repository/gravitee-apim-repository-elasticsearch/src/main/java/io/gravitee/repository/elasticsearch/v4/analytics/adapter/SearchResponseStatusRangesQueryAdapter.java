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

import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchResponseStatusRangesQueryAdapter {

    public static final String ENTRYPOINT_ID_AGG = "entrypoint_id_agg";
    public static final String FIELD = "field";
    public static final String STATUS_RANGES = "status_ranges";

    public static String adapt(ResponseStatusQueryCriteria query, boolean isEntrypointIdKeyword) {
        var jsonContent = new HashMap<String, Object>();

        jsonContent.put("query", buildElasticQuery(query));
        jsonContent.put("size", 0);
        jsonContent.put("aggs", buildResponseCountPerStatusCodeRangePerEntrypointAggregation(isEntrypointIdKeyword));
        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildResponseCountPerStatusCodeRangePerEntrypointAggregation(boolean isEntrypointIdKeyword) {
        return JsonObject.of(
            ENTRYPOINT_ID_AGG,
            JsonObject.of(
                "terms",
                JsonObject.of(FIELD, isEntrypointIdKeyword ? "entrypoint-id" : "entrypoint-id.keyword"),
                "aggs",
                JsonObject.of(
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
                )
            )
        );
    }

    private static JsonObject buildElasticQuery(ResponseStatusQueryCriteria queryParams) {
        var filterQuery = new ArrayList<JsonObject>();

        if (queryParams == null || queryParams.getApiIds() == null) {
            log.warn("Null query params or queried API IDs. Empty ranges will be returned");
            filterQuery.add(apiIdsFilterForQuery(List.of()));
        } else {
            filterQuery.add(apiIdsFilterForQuery(queryParams.getApiIds()));
        }

        if (queryParams != null && queryParams.getFrom() != null && queryParams.getTo() != null) {
            filterQuery.add(dateRangeFilterForQuery(queryParams.getFrom(), queryParams.getTo()));
        }

        return JsonObject.of("bool", JsonObject.of("filter", filterQuery));
    }

    private static JsonObject apiIdsFilterForQuery(List<String> apiIds) {
        return JsonObject.of("terms", JsonObject.of("api-id", apiIds));
    }

    private static JsonObject dateRangeFilterForQuery(Long from, Long to) {
        var fromDate = new Date(from);
        var toDate = new Date(to);
        log.info("Query filtering date range from {} to {}", fromDate, toDate);
        return JsonObject.of("range", JsonObject.of("@timestamp", JsonObject.of("gte", fromDate, "lte", toDate)));
    }
}
