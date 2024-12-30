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
import io.gravitee.repository.healthcheck.v4.model.ApiFieldPeriod;
import io.gravitee.repository.healthcheck.v4.model.AvailabilityResponse;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AvailabilityQueryMapper implements QueryResponseAdapter<ApiFieldPeriod, AvailabilityResponse> {

    private static final String BY_FIELD_AGGS = "by_field";
    private static final String RESULTS_AGGS = "results";

    @Override
    public JsonNode adaptQuery(ApiFieldPeriod query) {
        return json().put("size", 0).<ObjectNode>set("query", query(query)).set("aggregations", aggregations(query));
    }

    private ObjectNode query(ApiFieldPeriod query) {
        JsonNode termFilter = json().set("term", json().put("api", query.apiId()));

        ObjectNode timestamp = json()
            .put("from", query.from().toEpochMilli())
            .put("to", query.to().toEpochMilli())
            .put("include_lower", true)
            .put("include_upper", true);
        JsonNode rangeFilter = json().set("range", json().set("@timestamp", timestamp));

        return json().set("bool", json().set("filter", array().add(termFilter).add(rangeFilter)));
    }

    private ObjectNode aggregations(ApiFieldPeriod query) {
        var deep = json().set(RESULTS_AGGS, json().set("terms", json().put("field", "available").set("include", array().add(true))));
        return json().set(BY_FIELD_AGGS, json().<ObjectNode>set("terms", json().put("field", query.field())).set("aggregations", deep));
    }

    @Override
    public Maybe<AvailabilityResponse> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Maybe.empty();
        }

        final var entrypointsAggregation = aggregations.get(BY_FIELD_AGGS);
        if (entrypointsAggregation == null) {
            return Maybe.empty();
        }

        // Did it this way because of 10.000 hits results per query limit for ElasticSearchQuery
        // https://www.elastic.co/guide/en/app-search/8.12/limits.html
        var total = entrypointsAggregation.getBuckets().stream().map(jsonNode -> jsonNode.get("doc_count").asLong()).reduce(0L, Long::sum);

        final var byFieldValue = entrypointsAggregation
            .getBuckets()
            .stream()
            .collect(Collectors.toMap(json -> json.get("key").asText(), AvailabilityQueryMapper::getByField));

        long sum = byFieldValue.values().stream().mapToLong(ByField::success).sum();

        var collect = byFieldValue.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().round()));

        return Maybe.just(new AvailabilityResponse(AvailabilityQueryMapper.round(sum, total), collect));
    }

    private static ByField getByField(JsonNode json) {
        return new ByField(
            json.get("key").asText(),
            json.get("doc_count").asLong(),
            json.at("/%s/buckets/0/doc_count".formatted(RESULTS_AGGS)).asLong(0)
        );
    }

    private record ByField(String name, long total, long success) {
        public float round() {
            return AvailabilityQueryMapper.round(success(), total());
        }
    }

    private static float round(long num, long denum) {
        int precision = 4;
        var value = ((Double) Math.pow(10, precision)).floatValue();
        return Math.round(num * value / denum) / value;
    }
}
