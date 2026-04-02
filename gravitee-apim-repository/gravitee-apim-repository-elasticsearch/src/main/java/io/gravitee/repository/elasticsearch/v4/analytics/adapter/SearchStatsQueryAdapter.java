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
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.StatsQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchStatsQueryAdapter {

    // Single ES stats aggregation — returns count, min, max, avg, sum in one round trip.
    static final String STATS_AGG = "stats_agg";

    public static String adaptQuery(StatsQuery query) {
        var jsonContent = new HashMap<String, Object>();
        jsonContent.put("size", 0);
        var esQuery = buildElasticQuery(query);
        if (esQuery != null) {
            jsonContent.put("query", esQuery);
        }
        jsonContent.put("aggs", buildStatsAggregations(query.field()));
        return new JsonObject(jsonContent).encode();
    }

    public static Optional<StatsAggregate> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }

        var statsAgg = aggregations.get(STATS_AGG);
        if (statsAgg == null) {
            return Optional.empty();
        }

        return Optional.of(
            StatsAggregate
                .builder()
                .count(statsAgg.getCount() != null ? statsAgg.getCount().longValue() : 0L)
                .min(statsAgg.getMin() != null ? statsAgg.getMin().doubleValue() : null)
                .max(statsAgg.getMax() != null ? statsAgg.getMax().doubleValue() : null)
                .avg(statsAgg.getAvg() != null ? statsAgg.getAvg().doubleValue() : null)
                .sum(statsAgg.getSum() != null ? statsAgg.getSum().doubleValue() : null)
                .build()
        );
    }

    private static JsonObject buildStatsAggregations(String field) {
        return JsonObject.of(STATS_AGG, JsonObject.of("stats", JsonObject.of("field", field)));
    }

    private static JsonObject buildElasticQuery(StatsQuery query) {
        if (query == null) {
            return null;
        }

        var terms = new ArrayList<JsonObject>();
        query.apiId().ifPresent(apiId -> terms.add(JsonObject.of("term", JsonObject.of("api-id", apiId))));

        var timestamp = new JsonObject();
        query.from().ifPresent(from -> timestamp.put("from", from.toEpochMilli()).put("include_lower", true));
        query.to().ifPresent(to -> timestamp.put("to", to.toEpochMilli()).put("include_upper", true));

        if (!timestamp.isEmpty()) {
            terms.add(JsonObject.of("range", JsonObject.of("@timestamp", timestamp)));
        }

        if (!terms.isEmpty()) {
            return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(terms.toArray())));
        }

        return null;
    }
}
