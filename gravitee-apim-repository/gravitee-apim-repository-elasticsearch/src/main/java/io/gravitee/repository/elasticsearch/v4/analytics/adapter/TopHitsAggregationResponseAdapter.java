/*
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

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.DELTA_BUCKET_SUFFIX;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.END_VALUE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.HITS;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.SOURCE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.START_VALUE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.TOP_HITS;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.query.events.EventAnalyticsAggregate;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts Elasticsearch metric aggregation responses to support VALUE, DELTA, TREND aggregations,
 * and maps them to {@link EventAnalyticsAggregate}.
 *
 * <p>Examples:
 * <ul>
 *   <li>VALUE: <pre>
 *   {"downstream-active-connections_latest": {"top_hits": {"hits": {...}}}}
 *   </pre></li>
 *   <li>DELTA counters: <pre>
 *   {"downstream-publish-messages-total_delta": {
 *         "start_value": {"hits": {"hits": [{"_source": {"downstream-publish-messages-total": 0 }}]}},
 *         "end_value": {"hits": {"hits": [{"_source": {"downstream-publish-messages-total": 121}}]}}
 *   }}</pre></li>
 * </ul>
 */
public class TopHitsAggregationResponseAdapter {

    private TopHitsAggregationResponseAdapter() {}

    public static Optional<EventAnalyticsAggregate> adapt(SearchResponse response, HistogramQuery query) {
        if (response == null || response.getTimedOut()) {
            return Optional.empty();
        }
        Map<String, Aggregation> aggregations = response.getAggregations();

        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }

        AggregationType aggregationType = getAggregationType(query);

        if (aggregationType == null) {
            return Optional.empty();
        }

        Map<String, Map<String, List<Long>>> result = new HashMap<>();

        aggregations.forEach((key, value) -> {
            Map<String, Aggregation> subAggs;

            switch (aggregationType) {
                case VALUE:
                    subAggs = value.getAggregations();
                    parseValueAggregations(key, subAggs, result);
                    break;
                case DELTA:
                    subAggs = value.getAggregations();
                    parseDeltaAggregations(key, subAggs, result);
                    break;
                case TREND:
                    List<JsonNode> buckets = value.getBuckets();
                    parseDeltaBuckets(buckets, result);
                    break;
            }
        });

        return result.isEmpty() ? Optional.empty() : Optional.of(new EventAnalyticsAggregate(result));
    }

    private static AggregationType getAggregationType(HistogramQuery query) {
        return (query == null)
            ? null
            : query
                .aggregations()
                .stream()
                .map(io.gravitee.repository.log.v4.model.analytics.Aggregation::getType)
                .distinct()
                .findFirst()
                .orElse(null);
    }

    private static void parseValueAggregations(String key, Map<String, Aggregation> subAggs, Map<String, Map<String, List<Long>>> result) {
        // VALUE aggregation: expects top_hits sub-aggregations
        Aggregation topHitsAgg = subAggs.get(TOP_HITS);

        if (
            topHitsAgg != null &&
            topHitsAgg.getHits() != null &&
            topHitsAgg.getHits().getHits() != null &&
            !topHitsAgg.getHits().getHits().isEmpty()
        ) {
            // Extract field and values from the _source of the first hit
            SearchHit topHit = topHitsAgg.getHits().getHits().getFirst();

            if (topHit != null && topHit.getSource() != null) {
                JsonNode source = topHit.getSource();
                source.fieldNames().forEachRemaining(field -> result.put(key, Map.of(field, List.of(source.get(field).asLong()))));
            }
        }
    }

    private static void parseDeltaAggregations(String key, Map<String, Aggregation> subAggs, Map<String, Map<String, List<Long>>> result) {
        // DELTA aggregation: expects start_value and end_value sub-aggregations
        Aggregation startAgg = subAggs.get(START_VALUE);
        Aggregation endAgg = subAggs.get(END_VALUE);
        Long start = null;
        Long end = null;
        String fieldName = null;
        // Extract field and values from the _source of the first hit in each
        if (startAgg.getHits() != null && startAgg.getHits().getHits() != null && !startAgg.getHits().getHits().isEmpty()) {
            SearchHit hit = startAgg.getHits().getHits().getFirst();

            if (hit != null && hit.getSource() != null && hit.getSource().fieldNames().hasNext()) {
                fieldName = hit.getSource().fieldNames().next();
                start = hit.getSource().get(fieldName).asLong();
            }
        }

        if (endAgg.getHits() != null && endAgg.getHits().getHits() != null && !endAgg.getHits().getHits().isEmpty()) {
            SearchHit hit = endAgg.getHits().getHits().getFirst();

            if (hit != null && hit.getSource() != null && hit.getSource().fieldNames().hasNext()) {
                if (fieldName == null) fieldName = hit.getSource().fieldNames().next();
                end = hit.getSource().get(fieldName).asLong();
            }
        }

        if (fieldName != null && start != null && end != null) {
            result.put(key, Map.of(fieldName, List.of(end - start)));
        }
    }

    private static void parseDeltaBuckets(List<JsonNode> buckets, Map<String, Map<String, List<Long>>> result) {
        if (buckets == null || buckets.isEmpty()) return;

        buckets.forEach(bucket ->
            bucket
                .fieldNames()
                .forEachRemaining(bucketName -> {
                    // Each bucket with _delta suffix has start_value and end_value sub-aggregations
                    if (bucketName.endsWith(DELTA_BUCKET_SUFFIX)) {
                        // Get the value from the source hit in the start_value node
                        long startValue = parseValueFromBucketSource(bucket, bucketName, START_VALUE);
                        // Get the value from the source hit in the end_value node
                        long endValue = parseValueFromBucketSource(bucket, bucketName, END_VALUE);
                        addTrendValue(result, bucketName, (endValue - startValue));
                    }
                })
        );
    }

    private static long parseValueFromBucketSource(JsonNode bucket, String bucketName, String nodeName) {
        JsonNode valueNode = bucket.get(bucketName).get(nodeName);
        JsonNode hits = valueNode.get(HITS);
        JsonNode sourceNode;

        if (valueNode.has(HITS) && hits.has(HITS) && !hits.get(HITS).isEmpty()) {
            sourceNode = hits.get(HITS).get(0).get(SOURCE);
            String sourceField = sourceNode.fieldNames().next();
            return sourceNode.get(sourceField).asLong();
        }

        return 0;
    }

    private static void addTrendValue(Map<String, Map<String, List<Long>>> result, String bucketName, long value) {
        String fieldName = bucketName.substring(0, bucketName.length() - DELTA_BUCKET_SUFFIX.length());

        if (!result.containsKey(bucketName)) {
            Map<String, List<Long>> values = new HashMap<>();
            values.put(fieldName, new ArrayList<>());
            result.put(bucketName, values);
        }

        result.get(bucketName).get(fieldName).add(value);
    }
}
