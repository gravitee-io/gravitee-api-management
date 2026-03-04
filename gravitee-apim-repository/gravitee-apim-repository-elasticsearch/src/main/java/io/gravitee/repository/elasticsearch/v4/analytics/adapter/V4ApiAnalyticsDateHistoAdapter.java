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
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsDateHistoQuery;
import io.gravitee.repository.log.v4.model.analytics.DateHistoAggregate;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class V4ApiAnalyticsDateHistoAdapter {

    private static final String BY_DATE = "by_date";
    private static final String BY_FIELD = "by_field";
    private static final String TIME_FIELD = "@timestamp";

    public static String buildQuery(ApiAnalyticsDateHistoQuery query, ElasticsearchInfo esInfo) {
        String esField = V4ApiAnalyticsFieldMapping.toEsField(query.getField());
        var queryObj = V4ApiAnalyticsStatsAdapter.buildFilter(query.getApiId(), query.getFrom(), query.getTo());
        String intervalKey = esInfo.getVersion().canUseDateHistogramFixedInterval() ? "fixed_interval" : "interval";
        var dateHisto = new JsonObject()
            .put("field", TIME_FIELD)
            .put(intervalKey, query.getInterval() + "ms")
            .put("min_doc_count", 0)
            .put("extended_bounds", new JsonObject().put("min", query.getFrom().toEpochMilli()).put("max", query.getTo().toEpochMilli()));
        var byField = new JsonObject().put("terms", new JsonObject().put("field", esField));
        var aggs = new JsonObject()
            .put(BY_DATE, new JsonObject().put("date_histogram", dateHisto).put("aggregations", new JsonObject().put(BY_FIELD, byField)));
        return new JsonObject().put("size", 0).put("query", queryObj).put("aggs", aggs).encode();
    }

    public static Optional<DateHistoAggregate> adaptResponse(SearchResponse response) {
        if (response.getAggregations() == null || response.getAggregations().isEmpty()) {
            return Optional.empty();
        }
        Aggregation byDateAgg = response.getAggregations().get(BY_DATE);
        if (byDateAgg == null || byDateAgg.getBuckets() == null || byDateAgg.getBuckets().isEmpty()) {
            return Optional.empty();
        }
        TreeMap<Long, Map<String, Long>> timeToFieldCounts = new TreeMap<>();
        TreeSet<String> fieldValues = new TreeSet<>();
        for (JsonNode dateBucket : byDateAgg.getBuckets()) {
            long keyAsDate = dateBucket.get("key").asLong();
            Map<String, Long> fieldCounts = new LinkedHashMap<>();
            JsonNode byFieldAgg = dateBucket.get(BY_FIELD);
            if (byFieldAgg != null && byFieldAgg.has("buckets")) {
                for (JsonNode b : byFieldAgg.get("buckets")) {
                    String fieldVal = b.get("key").asText();
                    long count = b.get("doc_count").asLong();
                    fieldCounts.put(fieldVal, count);
                    fieldValues.add(fieldVal);
                }
            }
            timeToFieldCounts.put(keyAsDate, fieldCounts);
        }
        List<Long> timestamp = new ArrayList<>(timeToFieldCounts.keySet());
        List<DateHistoAggregate.DateHistoValue> values = new ArrayList<>();
        for (String fieldVal : fieldValues) {
            List<Long> buckets = new ArrayList<>();
            for (Long ts : timestamp) {
                buckets.add(timeToFieldCounts.get(ts).getOrDefault(fieldVal, 0L));
            }
            values.add(
                DateHistoAggregate.DateHistoValue.builder().field(fieldVal).buckets(buckets).metadata(Map.of("name", fieldVal)).build()
            );
        }
        return Optional.of(DateHistoAggregate.builder().timestamp(timestamp).values(values).build());
    }
}
