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
import io.gravitee.repository.log.v4.model.analytics.DateHistogramAggregate;
import io.gravitee.repository.log.v4.model.analytics.DateHistogramQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchDateHistogramAdapter {

    private static final String BY_DATE_AGG = "by_date";
    private static final String BY_FIELD_AGG = "by_field";
    private static final String TIME_FIELD = "@timestamp";

    public static String adaptQuery(DateHistogramQuery query) {
        var jsonContent = new JsonObject();
        jsonContent.put("size", 0);

        var esQuery = buildElasticQuery(query);
        if (esQuery != null) {
            jsonContent.put("query", esQuery);
        }

        jsonContent.put("aggs", buildAggregations(query));
        return jsonContent.encode();
    }

    public static Optional<DateHistogramAggregate> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }

        var byDateAgg = aggregations.get(BY_DATE_AGG);
        if (byDateAgg == null || byDateAgg.getBuckets() == null) {
            return Optional.empty();
        }

        // TreeMap to keep timestamps ordered
        var treeMap = new TreeMap<Long, Map<String, Long>>();
        var fieldValues = new TreeSet<String>();

        for (var dateBucket : byDateAgg.getBuckets()) {
            var timestamp = dateBucket.get("key").asLong();
            var fieldCounts = new HashMap<String, Long>();

            var byFieldNode = dateBucket.get(BY_FIELD_AGG);
            if (byFieldNode != null && byFieldNode.get("buckets") != null) {
                for (var fieldBucket : byFieldNode.get("buckets")) {
                    var key = fieldBucket.get("key").asText();
                    var count = fieldBucket.get("doc_count").asLong();
                    fieldCounts.put(key, count);
                    fieldValues.add(key);
                }
            }
            treeMap.put(timestamp, fieldCounts);
        }

        // Build ordered timestamp list and value arrays
        var timestamps = new ArrayList<Long>(treeMap.keySet());
        var values = new LinkedHashMap<String, List<Long>>();
        for (var fieldValue : fieldValues) {
            values.put(fieldValue, new ArrayList<>());
        }
        for (var entry : treeMap.entrySet()) {
            var fieldCounts = entry.getValue();
            for (var fieldValue : fieldValues) {
                values.get(fieldValue).add(fieldCounts.getOrDefault(fieldValue, 0L));
            }
        }

        return Optional.of(DateHistogramAggregate.builder().timestamps(timestamps).values(values).build());
    }

    private static JsonObject buildAggregations(DateHistogramQuery query) {
        var histogram = JsonObject.of(
            "field",
            TIME_FIELD,
            "fixed_interval",
            query.interval().toMillis() + "ms",
            "min_doc_count",
            0,
            "extended_bounds",
            JsonObject.of("min", query.from().toEpochMilli(), "max", query.to().toEpochMilli())
        );

        var subAgg = JsonObject.of(BY_FIELD_AGG, JsonObject.of("terms", JsonObject.of("field", query.field())));

        return JsonObject.of(BY_DATE_AGG, JsonObject.of("date_histogram", histogram, "aggs", subAgg));
    }

    private static JsonObject buildElasticQuery(DateHistogramQuery query) {
        var terms = new ArrayList<JsonObject>();

        if (query.apiId() != null) {
            terms.add(JsonObject.of("term", JsonObject.of("api-id", query.apiId())));
        }

        var timestamp = new JsonObject();
        if (query.from() != null) {
            timestamp.put("from", query.from().toEpochMilli()).put("include_lower", true);
        }
        if (query.to() != null) {
            timestamp.put("to", query.to().toEpochMilli()).put("include_upper", true);
        }

        if (!timestamp.isEmpty()) {
            terms.add(JsonObject.of("range", JsonObject.of("@timestamp", timestamp)));
        }

        if (!terms.isEmpty()) {
            return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(terms.toArray())));
        }

        return null;
    }
}
