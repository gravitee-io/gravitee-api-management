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
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsStatsQuery;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class V4ApiAnalyticsStatsAdapter {

    private static final String STATS_AGG = "stats_agg";

    public static String buildQuery(ApiAnalyticsStatsQuery query) {
        String esField = V4ApiAnalyticsFieldMapping.toEsField(query.getField());
        var queryObj = buildFilter(query);
        var aggs = new JsonObject().put(STATS_AGG, new JsonObject().put("stats", new JsonObject().put("field", esField)));
        return new JsonObject().put("size", 0).put("query", queryObj).put("aggs", aggs).encode();
    }

    public static Optional<StatsAggregate> adaptResponse(SearchResponse response) {
        if (response.getAggregations() == null || response.getAggregations().isEmpty()) {
            return Optional.empty();
        }
        Aggregation agg = response.getAggregations().get(STATS_AGG);
        if (agg == null) {
            return Optional.empty();
        }
        long count = agg.getCount() != null ? agg.getCount().longValue() : 0L;
        double min = agg.getMin() != null ? agg.getMin().doubleValue() : 0.0;
        double max = agg.getMax() != null ? agg.getMax().doubleValue() : 0.0;
        double avg = agg.getAvg() != null ? agg.getAvg().doubleValue() : 0.0;
        double sum = agg.getSum() != null ? agg.getSum().doubleValue() : 0.0;
        return Optional.of(StatsAggregate.builder().count(count).min(min).max(max).avg(avg).sum(sum).build());
    }

    static JsonObject buildFilter(ApiAnalyticsStatsQuery query) {
        return buildFilter(query.getApiId(), query.getFrom(), query.getTo());
    }

    /** Shared filter for v4-metrics: api-id + time range. */
    public static JsonObject buildFilter(String apiId, java.time.Instant from, java.time.Instant to) {
        var must = new ArrayList<JsonObject>();
        must.add(JsonObject.of("term", JsonObject.of("api-id", apiId)));
        must.add(JsonObject.of("range", JsonObject.of("@timestamp", JsonObject.of("gte", from.toEpochMilli(), "lte", to.toEpochMilli()))));
        return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(must.toArray())));
    }
}
