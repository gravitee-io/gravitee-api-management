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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.StatsQueryCriteria;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Builds an ES {@code stats} aggregation query and parses its response (US-03).
 *
 * <p>ES query shape:</p>
 * <pre>{@code
 * {
 *   "size": 0,
 *   "query": { "bool": { "must": [ {"term": {"api-id": "<id>"}},
 *                                   {"range": {"@timestamp": {"gte": <from>, "lte": <to>}}} ] } },
 *   "aggs": { "stats_agg": { "stats": { "field": "<field>" } } }
 * }
 * }</pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchStatsAdapter {

    static final String STATS_AGG = "stats_agg";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String adaptQuery(StatsQueryCriteria criteria) {
        return json()
            .put("size", 0)
            .<ObjectNode>set("query", buildQuery(criteria))
            .set("aggs", json().set(STATS_AGG, json().set("stats", json().put("field", criteria.field()))))
            .toString();
    }

    public static Optional<StatsAggregate> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.of(StatsAggregate.builder().build()); // zero-doc case — return 0s (AC 2)
        }
        final Aggregation statsAgg = aggregations.get(STATS_AGG);
        if (statsAgg == null) {
            return Optional.of(StatsAggregate.builder().build());
        }

        long count = statsAgg.getCount() != null ? statsAgg.getCount().longValue() : 0L;
        double min = count > 0 && statsAgg.getMin() != null ? statsAgg.getMin().doubleValue() : 0.0;
        double max = count > 0 && statsAgg.getMax() != null ? statsAgg.getMax().doubleValue() : 0.0;
        double avg = count > 0 && statsAgg.getAvg() != null ? statsAgg.getAvg().doubleValue() : 0.0;
        double sum = count > 0 && statsAgg.getSum() != null ? statsAgg.getSum().doubleValue() : 0.0;

        return Optional.of(StatsAggregate.builder().count(count).min(min).max(max).avg(avg).sum(sum).build());
    }

    private static ObjectNode buildQuery(StatsQueryCriteria criteria) {
        var must = MAPPER.createArrayNode();
        must.add(json().set("term", json().put("api-id", criteria.apiId())));
        if (criteria.from() != null && criteria.to() != null) {
            must.add(
                json()
                    .set(
                        "range",
                        json().set("@timestamp", json().put("gte", criteria.from().toEpochMilli()).put("lte", criteria.to().toEpochMilli()))
                    )
            );
        }
        return json().set("bool", json().set("must", must));
    }

    private static ObjectNode json() {
        return MAPPER.createObjectNode();
    }
}
