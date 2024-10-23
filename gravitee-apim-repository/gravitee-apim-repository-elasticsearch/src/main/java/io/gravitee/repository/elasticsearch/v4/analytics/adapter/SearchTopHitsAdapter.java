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
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchTopHitsAdapter {

    public static final String API_TOP_HITS_COUNT = "api_top_hits_count";
    public static final String FIELD = "field";
    public static final String HITS_COUNT = "hits_count";
    public static final String API_ID_FIELD = "api-id";

    public static String adaptQuery(TopHitsQueryCriteria queryCriteria) {
        var jsonContent = new HashMap<String, Object>();

        jsonContent.put("size", 0);
        jsonContent.put("query", buildQuery(queryCriteria));
        jsonContent.put("aggs", buildAggregation());
        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildQuery(TopHitsQueryCriteria queryCriteria) {
        var filterQuery = new ArrayList<JsonObject>();

        if (queryCriteria == null || queryCriteria.getApiIds() == null) {
            log.warn("Null query params or queried API IDs. Empty ranges will be returned");
            filterQuery.add(apiIdsFilterForQuery(List.of()));
        } else {
            filterQuery.add(apiIdsFilterForQuery(queryCriteria.getApiIds()));
        }

        if (queryCriteria != null) {
            filterQuery.add(dateRangeFilterForQuery(queryCriteria.getFrom(), queryCriteria.getTo()));
        }

        return JsonObject.of("bool", JsonObject.of("filter", filterQuery));
    }

    private static JsonObject apiIdsFilterForQuery(List<String> apiIds) {
        return JsonObject.of("terms", JsonObject.of("api-id", apiIds));
    }

    private static JsonObject dateRangeFilterForQuery(Long from, Long to) {
        log.info("Top Hits Query: filtering date range from {} to {}", from, to);
        return JsonObject.of("range", JsonObject.of("@timestamp", JsonObject.of("gte", from, "lte", to)));
    }

    private static JsonObject buildAggregation() {
        return JsonObject.of(
            API_TOP_HITS_COUNT,
            JsonObject.of(
                "terms",
                JsonObject.of(FIELD, API_ID_FIELD),
                "aggs",
                JsonObject.of(HITS_COUNT, JsonObject.of("value_count", JsonObject.of(FIELD, API_ID_FIELD)))
            )
        );
    }

    public static Optional<TopHitsAggregate> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();

        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }

        final var topHitsAggregation = aggregations.get(API_TOP_HITS_COUNT);
        if (topHitsAggregation == null) {
            return Optional.empty();
        }

        final Map<String, Long> result = topHitsAggregation
            .getBuckets()
            .stream()
            .collect(
                Collectors.toMap(jsonNode -> jsonNode.get("key").asText(), jsonNode -> jsonNode.get(HITS_COUNT).get("value").asLong())
            );
        return Optional.of(TopHitsAggregate.builder().topHitsCounts(result).build());
    }
}
