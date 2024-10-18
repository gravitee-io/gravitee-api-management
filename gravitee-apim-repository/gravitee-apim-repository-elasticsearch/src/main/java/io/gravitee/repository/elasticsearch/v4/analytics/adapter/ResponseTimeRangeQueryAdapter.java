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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.reactivex.rxjava3.core.Maybe;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResponseTimeRangeQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String TIME_FIELD = "@timestamp";
    private static final String RESPONSE_TIME_FIELD = "gateway-response-time-ms";
    private static final String REDUCE_OPERATION = "avg";
    private static final String HISTOGRAM = "by_date";

    public String queryAdapt(ElasticsearchInfo esInfo, ResponseTimeRangeQuery query) {
        return json().put("size", 0).<ObjectNode>set("query", query(query)).set("aggregations", aggregations(esInfo, query)).toString();
    }

    public Maybe<AverageAggregate> responseAdapt(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Maybe.empty();
        }
        final var entrypointsAggregation = aggregations.get(HISTOGRAM);
        if (entrypointsAggregation == null) {
            return Maybe.empty();
        }

        final var averageConnectionDuration = entrypointsAggregation
            .getBuckets()
            .stream()
            .map(json ->
                Map.of(json.get("key_as_string").asText(), json.get(REDUCE_OPERATION + "_" + RESPONSE_TIME_FIELD).get("value").asDouble())
            )
            .reduce(
                new LinkedHashMap<>(),
                (acc, b) -> {
                    acc.putAll(b);
                    return acc;
                }
            );
        double avg = averageConnectionDuration.values().stream().mapToDouble(e -> e).average().orElse(0);

        return Maybe.just(buildFromSource(averageConnectionDuration, avg));
    }

    private static AverageAggregate buildFromSource(Map<String, Double> averageConnectionDuration, double globalAverage) {
        return AverageAggregate.builder().average(globalAverage).averageBy(averageConnectionDuration).build();
    }

    private ObjectNode query(ResponseTimeRangeQuery query) {
        JsonNode termFilter = json().set("term", json().put("api-id", query.apiId()));

        // we just ensure to fetch full bucket interval (a bit too)
        var from = query.from().minus(query.interval());
        var to = query.to().plus(query.interval());

        ObjectNode timestamp = json()
            .put("from", from.toEpochMilli())
            .put("to", to.toEpochMilli())
            .put("include_lower", true)
            .put("include_upper", true);
        JsonNode rangeFilter = json().set("range", json().set(TIME_FIELD, timestamp));

        var bool = json().set("filter", array().add(termFilter).add(rangeFilter));
        return json().set("bool", bool);
    }

    private ObjectNode aggregations(ElasticsearchInfo esInfo, ResponseTimeRangeQuery query) {
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
        ObjectNode agg = json()
            .set(REDUCE_OPERATION + "_" + RESPONSE_TIME_FIELD, json().set(REDUCE_OPERATION, json().put("field", RESPONSE_TIME_FIELD)));

        return json().set(HISTOGRAM, json().<ObjectNode>set("date_histogram", histogram).set("aggregations", agg));
    }

    private ObjectNode json() {
        return MAPPER.createObjectNode();
    }

    private ArrayNode array() {
        return MAPPER.createArrayNode();
    }
}
