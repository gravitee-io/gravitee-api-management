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
import io.gravitee.repository.log.v4.model.analytics.TopFailedAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopFailedQueryCriteria;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchTopFailedApisAdapter {

    private static final String FAILED_REQUESTS = "failed_requests";
    private static final String FAILED_REQUESTS_COUNT = "failed_requests_count";
    private static final String FAILED_REQUESTS_RATIO = "failed_requests_ratio";
    private static final String STATUS_FIELD = "status";
    private static final List<String> API_ID_FIELDS = List.of("api-id", "api");

    public static String adaptQuery(TopFailedQueryCriteria queryCriteria) {
        var jsonContent = new HashMap<String, Object>();

        jsonContent.put("size", 0);
        jsonContent.put("query", buildQuery(queryCriteria));
        jsonContent.put("aggs", buildAggregations());
        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildQuery(TopFailedQueryCriteria queryCriteria) {
        var filterQuery = new ArrayList<JsonObject>();

        if (queryCriteria == null || queryCriteria.apiIds() == null) {
            log.warn("Null query params or queried API IDs. Empty ranges will be returned");
            filterQuery.add(apiIdsFilterForQuery(List.of()));
        } else {
            filterQuery.add(apiIdsFilterForQuery(queryCriteria.apiIds()));
        }

        if (queryCriteria != null) {
            filterQuery.add(dateRangeFilterForQuery(queryCriteria.from(), queryCriteria.to()));
        }

        return JsonObject.of("bool", JsonObject.of("filter", filterQuery));
    }

    private static JsonObject apiIdsFilterForQuery(List<String> apiIds) {
        var terms = new ArrayList<JsonObject>();
        API_ID_FIELDS.forEach(apiIdField -> {
            terms.add(JsonObject.of("terms", JsonObject.of(apiIdField, apiIds)));
        });
        return buildShould(terms);
    }

    private static JsonObject buildShould(List<JsonObject> terms) {
        return JsonObject.of("bool", JsonObject.of("should", JsonArray.of(terms.toArray())));
    }

    private static JsonObject dateRangeFilterForQuery(Long from, Long to) {
        log.info("Top Hits Query: filtering date range from {} to {}", from, to);
        return JsonObject.of("range", JsonObject.of("@timestamp", JsonObject.of("gte", from, "lte", to)));
    }

    private static Object buildAggregations() {
        return API_ID_FIELDS.stream().reduce(JsonObject.of(), (a, b) -> a.mergeIn(buildAggregationForField(b)), JsonObject::mergeIn);
    }

    private static JsonObject buildAggregationForField(String field) {
        return JsonObject.of(
            "failed_apis_agg_" + field,
            JsonObject.of(
                "terms",
                JsonObject.of("field", field),
                "aggs",
                JsonObject.of(
                    "total_requests",
                    JsonObject.of("value_count", JsonObject.of("field", field)),
                    FAILED_REQUESTS,
                    JsonObject.of(
                        "filter",
                        JsonObject.of("range", JsonObject.of(STATUS_FIELD, JsonObject.of("gte", 500, "lt", 600))),
                        "aggs",
                        JsonObject.of(FAILED_REQUESTS_COUNT, JsonObject.of("value_count", JsonObject.of("field", STATUS_FIELD)))
                    ),
                    FAILED_REQUESTS_RATIO,
                    JsonObject.of(
                        "bucket_script",
                        JsonObject.of(
                            "buckets_path",
                            JsonObject.of(
                                "failed_count",
                                String.join(">", FAILED_REQUESTS, FAILED_REQUESTS_COUNT),
                                "total_count",
                                "total_requests"
                            ),
                            "script",
                            "params.failed_count / params.total_count"
                        )
                    )
                )
            )
        );
    }

    public static Optional<TopFailedAggregate> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();

        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }

        final var result = aggregations
            .values()
            .stream()
            .filter(aggregation -> aggregation.getBuckets() != null)
            .flatMap(aggregation -> aggregation.getBuckets().stream())
            .filter(bucket -> bucket.get(FAILED_REQUESTS).get(FAILED_REQUESTS_COUNT).get("value").asLong() > 0)
            .collect(
                Collectors.toMap(
                    bucket -> bucket.get("key").asText(),
                    bucket -> {
                        final var failedRequestsCount = bucket.get(FAILED_REQUESTS).get(FAILED_REQUESTS_COUNT).get("value").asLong();
                        final var failedRequestsRatio = bucket.get(FAILED_REQUESTS_RATIO).get("value").asDouble();
                        return new TopFailedAggregate.FailedApiInfo(failedRequestsCount, failedRequestsRatio);
                    }
                )
            );

        return Optional.of(TopFailedAggregate.builder().failedApis(result).build());
    }
}
