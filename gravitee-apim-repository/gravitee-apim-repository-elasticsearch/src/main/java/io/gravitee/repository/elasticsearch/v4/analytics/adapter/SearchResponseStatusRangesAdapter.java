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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchResponseStatusRangesAdapter {

    public static final String BY_ENTRYPOINT_ID_AGG = "entrypoint_id_agg";
    public static final String FIELD = "field";
    public static final String STATUS_RANGES = "status_ranges";
    static final String ALL_APIS_STATUS_RANGES = "all_apis_status_ranges";

    public static String adaptQuery(ResponseStatusQueryCriteria query, boolean isEntrypointIdKeyword) {
        var jsonContent = new HashMap<String, Object>();

        jsonContent.put("query", buildElasticQuery(query));
        jsonContent.put("size", 0);
        jsonContent.put("aggs", buildResponseCountPerStatusCodeRangePerEntrypointAggregation(isEntrypointIdKeyword));
        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildElasticQuery(ResponseStatusQueryCriteria queryParams) {
        var filterQuery = new ArrayList<JsonObject>();

        if (queryParams == null || queryParams.apiIds() == null) {
            log.warn("Null query params or queried API IDs. Empty ranges will be returned");
            filterQuery.add(apiIdsFilterForQuery(List.of()));
        } else {
            filterQuery.add(apiIdsFilterForQuery(queryParams.apiIds()));
        }

        if (queryParams != null && queryParams.from() != null && queryParams.to() != null) {
            filterQuery.add(dateRangeFilterForQuery(queryParams.from(), queryParams.to()));
        }

        return JsonObject.of("bool", JsonObject.of("filter", filterQuery));
    }

    private static JsonObject apiIdsFilterForQuery(List<String> apiIds) {
        var terms = new ArrayList<JsonObject>();
        terms.add(JsonObject.of("terms", JsonObject.of("api-id", apiIds)));
        terms.add(JsonObject.of("terms", JsonObject.of("api", apiIds)));
        return buildShould(terms);
    }

    private static JsonObject buildShould(List<JsonObject> terms) {
        return JsonObject.of("bool", JsonObject.of("should", JsonArray.of(terms.toArray())));
    }

    private static JsonObject dateRangeFilterForQuery(Long from, Long to) {
        var fromDate = new Date(from);
        var toDate = new Date(to);
        log.info("Query filtering date range from {} to {}", fromDate, toDate);
        return JsonObject.of("range", JsonObject.of("@timestamp", JsonObject.of("gte", fromDate, "lte", toDate)));
    }

    private static JsonObject buildResponseCountPerStatusCodeRangePerEntrypointAggregation(boolean isEntrypointIdKeyword) {
        return JsonObject.of(
            BY_ENTRYPOINT_ID_AGG,
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
            ),
            ALL_APIS_STATUS_RANGES,
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

    public static Optional<ResponseStatusRangesAggregate> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }
        final var byEntrypointsAggregation = aggregations.get(BY_ENTRYPOINT_ID_AGG);
        if (byEntrypointsAggregation == null) {
            return Optional.empty();
        }

        final Map<String, Map<String, Long>> statusRangesByEntrypoints = byEntrypointsAggregation
            .getBuckets()
            .stream()
            .collect(
                Collectors.toMap(jsonNode -> jsonNode.get("key").asText(), jsonNode -> processStatusRanges(jsonNode.get(STATUS_RANGES)))
            );

        final var allApisStatusRangesAggregation = aggregations.get(ALL_APIS_STATUS_RANGES);
        if (allApisStatusRangesAggregation == null) {
            return Optional.empty();
        }

        //        allApisStatusRangesAggregation.getBuckets().stream().flatMap(bucket -> bucket.get("bucket")).forEach(System.out::println);

        final Map<String, Long> allApisStatusRanges = allApisStatusRangesAggregation
            .getBuckets()
            .stream()
            .collect(Collectors.toMap(jsonNode -> jsonNode.get("key").asText(), jsonNode -> jsonNode.get("doc_count").asLong()));

        return Optional.of(
            ResponseStatusRangesAggregate
                .builder()
                .statusRangesCountByEntrypoint(statusRangesByEntrypoints)
                .ranges(allApisStatusRanges)
                .build()
        );
    }

    private static Map<String, Long> processStatusRanges(JsonNode jsonNode) {
        if (jsonNode == null) {
            return Collections.emptyMap();
        }

        final var buckets = jsonNode.get("buckets");
        if (buckets == null || buckets.isEmpty()) {
            return Collections.emptyMap();
        }

        final var result = new HashMap<String, Long>();
        for (final var bucket : buckets) {
            final var count = bucket.get("doc_count").asLong();
            result.put(bucket.get("key").asText(), count);
        }

        return result;
    }
}
