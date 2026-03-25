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

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchStatsAdapter {

    private static final String MIN_AGG = "field_min";
    private static final String MAX_AGG = "field_max";
    private static final String AVG_AGG = "field_avg";
    private static final String SUM_AGG = "field_sum";

    public static String adaptQuery(StatsQuery query) {
        var jsonContent = new HashMap<String, Object>();
        jsonContent.put("size", 0);

        var esQuery = buildElasticQuery(query);
        if (esQuery != null) {
            jsonContent.put("query", esQuery);
        }

        jsonContent.put("aggs", buildAggregations(query.field()));
        return new JsonObject(jsonContent).encode();
    }

    public static Optional<StatsAggregate> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }

        long count = 0;
        if (response.getSearchHits() != null && response.getSearchHits().getTotal() != null) {
            count = response.getSearchHits().getTotal().getValue();
        }

        return Optional.of(
            StatsAggregate
                .builder()
                .count(count)
                .min(getAggregationValue(aggregations, MIN_AGG))
                .max(getAggregationValue(aggregations, MAX_AGG))
                .avg(getAggregationValue(aggregations, AVG_AGG))
                .sum(getAggregationValue(aggregations, SUM_AGG))
                .build()
        );
    }

    private static double getAggregationValue(Map<String, Aggregation> aggregations, String aggName) {
        var aggregation = aggregations.get(aggName);
        if (aggregation == null || aggregation.getValue() == null) {
            return 0.0;
        }
        return aggregation.getValue().doubleValue();
    }

    private static JsonObject buildAggregations(String field) {
        return JsonObject.of(
            MIN_AGG,
            JsonObject.of("min", JsonObject.of("field", field)),
            MAX_AGG,
            JsonObject.of("max", JsonObject.of("field", field)),
            AVG_AGG,
            JsonObject.of("avg", JsonObject.of("field", field)),
            SUM_AGG,
            JsonObject.of("sum", JsonObject.of("field", field))
        );
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
