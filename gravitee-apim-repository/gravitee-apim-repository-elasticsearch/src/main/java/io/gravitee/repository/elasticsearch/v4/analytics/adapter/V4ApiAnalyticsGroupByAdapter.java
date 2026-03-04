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
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByQuery;
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class V4ApiAnalyticsGroupByAdapter {

    private static final String TERMS_AGG = "by_field";

    public static String buildQuery(ApiAnalyticsGroupByQuery query) {
        String esField = V4ApiAnalyticsFieldMapping.toEsField(query.getField());
        var queryObj = V4ApiAnalyticsStatsAdapter.buildFilter(query.getApiId(), query.getFrom(), query.getTo());
        var terms = new JsonObject().put("field", esField).put("size", query.getSize());
        if (query.getOrder() != null && !query.getOrder().isBlank()) {
            boolean asc = !query.getOrder().startsWith("-");
            String orderField = asc ? query.getOrder() : query.getOrder().substring(1);
            if ("_count".equals(orderField) || "count".equals(orderField)) {
                terms.put("order", JsonObject.of("_count", asc ? "asc" : "desc"));
            }
        }
        var aggs = new JsonObject().put(TERMS_AGG, new JsonObject().put("terms", terms));
        return new JsonObject().put("size", 0).put("query", queryObj).put("aggs", aggs).encode();
    }

    public static Optional<GroupByAggregate> adaptResponse(SearchResponse response) {
        if (response.getAggregations() == null || response.getAggregations().isEmpty()) {
            return Optional.empty();
        }
        Aggregation agg = response.getAggregations().get(TERMS_AGG);
        if (agg == null || agg.getBuckets() == null) {
            return Optional.empty();
        }
        Map<String, Long> values = new LinkedHashMap<>();
        Map<String, Map<String, Object>> metadata = new LinkedHashMap<>();
        for (JsonNode bucket : agg.getBuckets()) {
            String key = bucket.get("key").asText();
            long docCount = bucket.get("doc_count").asLong();
            values.put(key, docCount);
            metadata.put(key, Map.of("name", key));
        }
        return Optional.of(GroupByAggregate.builder().values(values).metadata(metadata).build());
    }
}
