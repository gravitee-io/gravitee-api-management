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

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchDateHistoQueryAdapter.BY_DATE_AGG;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchDateHistoQueryAdapter.BY_STATUS_AGG;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchDateHistoQueryAdapter.METRIC_AVG_AGG;

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.DateHistoAggregate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchDateHistoResponseAdapter {

    public static Optional<DateHistoAggregate> adapt(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }

        final var byDateAggregation = aggregations.get(BY_DATE_AGG);
        if (byDateAggregation == null || byDateAggregation.getBuckets() == null || byDateAggregation.getBuckets().isEmpty()) {
            return Optional.empty();
        }

        var dateBuckets = byDateAggregation.getBuckets();

        // Detect mode: status terms or metric avg
        var firstBucket = dateBuckets.get(0);
        boolean isStatusMode = firstBucket.has(BY_STATUS_AGG);

        if (isStatusMode) {
            return adaptStatusMode(dateBuckets);
        } else {
            return adaptMetricMode(dateBuckets);
        }
    }

    private static Optional<DateHistoAggregate> adaptStatusMode(List<com.fasterxml.jackson.databind.JsonNode> dateBuckets) {
        var treeMap = new TreeMap<Long, Map<String, Long>>();
        var statusValues = new TreeSet<String>();

        for (var dateBucket : dateBuckets) {
            var keyAsDate = dateBucket.get("key").asLong();
            var statusCount = new HashMap<String, Long>();

            for (var statusBucket : dateBucket.get(BY_STATUS_AGG).get("buckets")) {
                var status = statusBucket.get("key").asText();
                var count = statusBucket.get("doc_count").asLong();
                statusCount.put(status, count);
                statusValues.add(status);
            }
            treeMap.put(keyAsDate, statusCount);
        }

        var timestamps = new ArrayList<Long>(treeMap.keySet());
        var result = new HashMap<String, List<Long>>();
        for (var status : statusValues) {
            result.put(status, new ArrayList<>());
        }
        for (var entry : treeMap.entrySet()) {
            var statusCount = entry.getValue();
            result.forEach((key, value) -> value.add(statusCount.getOrDefault(key, 0L)));
        }

        return Optional.of(DateHistoAggregate.builder().timestamps(timestamps).buckets(result).build());
    }

    private static Optional<DateHistoAggregate> adaptMetricMode(List<com.fasterxml.jackson.databind.JsonNode> dateBuckets) {
        var timestamps = new ArrayList<Long>();
        var values = new ArrayList<Long>();

        for (var dateBucket : dateBuckets) {
            timestamps.add(dateBucket.get("key").asLong());
            var avgNode = dateBucket.get(METRIC_AVG_AGG);
            var avgValue = (avgNode != null && !avgNode.get("value").isNull()) ? avgNode.get("value").asLong() : 0L;
            values.add(avgValue);
        }

        // Use a placeholder key for the single metric series — the field name is not in the response so we use "_metric"
        return Optional.of(DateHistoAggregate.builder().timestamps(timestamps).buckets(Map.of("_metric", values)).build());
    }
}
