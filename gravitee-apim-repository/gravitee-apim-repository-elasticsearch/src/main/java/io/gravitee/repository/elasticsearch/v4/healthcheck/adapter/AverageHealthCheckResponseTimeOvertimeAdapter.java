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
package io.gravitee.repository.elasticsearch.v4.healthcheck.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeOvertime;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeOvertimeQuery;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class AverageHealthCheckResponseTimeOvertimeAdapter
    implements QueryResponseAdapter<AverageHealthCheckResponseTimeOvertimeQuery, AverageHealthCheckResponseTimeOvertime> {

    private static final String TIME_FIELD = "@timestamp";
    public static final String AGGREGATION_BY_DATE = "by_date";
    public static final String AGGREGATION_AVG_RESPONSE_TIME = "avg_response-time";

    public JsonNode adaptQuery(AverageHealthCheckResponseTimeOvertimeQuery query, ElasticsearchInfo esInfo) {
        return json().put("size", 0).<ObjectNode>set("query", query(query)).set("aggregations", aggregations(query, esInfo));
    }

    public Maybe<AverageHealthCheckResponseTimeOvertime> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Maybe.empty();
        }
        final var entrypointsAggregation = aggregations.get(AGGREGATION_BY_DATE);
        if (entrypointsAggregation == null) {
            return Maybe.empty();
        }

        final var buckets = entrypointsAggregation
            .getBuckets()
            .stream()
            .map(json ->
                Map.of(json.get("key_as_string").asText(), Math.round(json.get(AGGREGATION_AVG_RESPONSE_TIME).get("value").asDouble()))
            )
            .reduce(
                new LinkedHashMap<>(),
                (acc, b) -> {
                    acc.putAll(b);
                    return acc;
                }
            );

        return Maybe.just(new AverageHealthCheckResponseTimeOvertime(buckets));
    }

    private ObjectNode query(AverageHealthCheckResponseTimeOvertimeQuery query) {
        JsonNode termFilter = json().set("term", json().put("api", query.apiId()));
        // we just ensure to fetch full bucket interval
        Instant from = query.from().minus(query.interval());
        Instant to = query.to().plus(query.interval());

        ObjectNode timestamp = json()
            .put("from", from.toEpochMilli())
            .put("to", to.toEpochMilli())
            .put("include_lower", true)
            .put("include_upper", true);
        JsonNode rangeFilter = json().set("range", json().set(TIME_FIELD, timestamp));

        var bool = json().set("filter", array().add(termFilter).add(rangeFilter));
        return json().set("bool", bool);
    }

    private ObjectNode aggregations(AverageHealthCheckResponseTimeOvertimeQuery query, ElasticsearchInfo esInfo) {
        String intervalFieldName = esInfo.getVersion().canUseDateHistogramFixedInterval() ? "fixed_interval" : "interval";
        ObjectNode histogram = json()
            .put("field", TIME_FIELD)
            .put(intervalFieldName, query.interval().toMillis() + "ms")
            .put("min_doc_count", 0)
            .set(
                "extended_bounds",
                json()
                    // we don't need to align it on interval ES make it for us
                    .put("min", query.from().toEpochMilli())
                    .put("max", query.to().toEpochMilli())
            );
        ObjectNode agg = json().set(AGGREGATION_AVG_RESPONSE_TIME, json().set("avg", json().put("field", "response-time")));

        return json().set(AGGREGATION_BY_DATE, json().<ObjectNode>set("date_histogram", histogram).set("aggregations", agg));
    }
}
