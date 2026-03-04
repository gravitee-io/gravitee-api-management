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
import io.gravitee.repository.log.v4.model.analytics.StatsQueryCriteria;
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
public class SearchStatsAdapter {

    private static final String FIELD = "field";
    private static final String STATS_COUNT = "stats_count";
    private static final String STATS_MIN = "stats_min";
    private static final String STATS_MAX = "stats_max";
    private static final String STATS_AVG = "stats_avg";
    private static final String STATS_SUM = "stats_sum";

    public static String adaptQuery(StatsQueryCriteria query) {
        var jsonContent = new HashMap<String, Object>();
        var safeQuery = Optional.ofNullable(query).orElse(new StatsQueryCriteria());

        jsonContent.put("size", 0);
        jsonContent.put("query", buildElasticQuery(safeQuery));
        jsonContent.put("aggs", buildStatsAggregate(safeQuery));
        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildStatsAggregate(StatsQueryCriteria query) {
        var field = query.field().orElse("status");
        return JsonObject.of(
            STATS_COUNT,
            JsonObject.of("value_count", JsonObject.of(FIELD, field)),
            STATS_MIN,
            JsonObject.of("min", JsonObject.of(FIELD, field)),
            STATS_MAX,
            JsonObject.of("max", JsonObject.of(FIELD, field)),
            STATS_AVG,
            JsonObject.of("avg", JsonObject.of(FIELD, field)),
            STATS_SUM,
            JsonObject.of("sum", JsonObject.of(FIELD, field))
        );
    }

    private static JsonObject buildElasticQuery(StatsQueryCriteria query) {
        var terms = new ArrayList<JsonObject>();
        query.apiId().ifPresent(apiId -> terms.add(JsonObject.of("term", JsonObject.of("api-id", apiId))));

        var timestamp = new JsonObject();
        query.from().ifPresent(from -> timestamp.put("from", from.toEpochMilli()).put("include_lower", true));
        query.to().ifPresent(to -> timestamp.put("to", to.toEpochMilli()).put("include_upper", true));

        if (!timestamp.isEmpty()) {
            terms.add(JsonObject.of("range", JsonObject.of("@timestamp", timestamp)));
        }

        if (terms.isEmpty()) {
            return JsonObject.of("match_all", JsonObject.of());
        }

        return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(terms.toArray())));
    }

    public static Optional<StatsAggregate> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
            StatsAggregate
                .builder()
                .count(asLong(aggregations.get(STATS_COUNT)))
                .min(asDouble(aggregations.get(STATS_MIN)))
                .max(asDouble(aggregations.get(STATS_MAX)))
                .avg(asDouble(aggregations.get(STATS_AVG)))
                .sum(asDouble(aggregations.get(STATS_SUM)))
                .build()
        );
    }

    private static long asLong(Aggregation aggregation) {
        if (aggregation == null || aggregation.getValue() == null) {
            return 0L;
        }
        return aggregation.getValue().longValue();
    }

    private static double asDouble(Aggregation aggregation) {
        if (aggregation == null || aggregation.getValue() == null) {
            return 0D;
        }
        var value = aggregation.getValue().doubleValue();
        return Double.isFinite(value) ? value : 0D;
    }
}
