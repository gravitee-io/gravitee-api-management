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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.DOC_COUNT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.KEY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SOURCE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.DELTA_BUCKET_SUFFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.START_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.TOP;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.query.events.EventAnalyticsAggregate;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            switch (aggregationType) {
                case VALUE:
                    parseLatestValueBuckets(key, value, result);
                    break;
                case DELTA:
                    parseStartEndValueBuckets(key, value, result);
                    break;
                case TREND:
                    List<JsonNode> buckets = value.getBuckets();
                    parseDeltaBuckets(buckets, result);
                    break;
            }
        });

        return result.isEmpty() ? Optional.empty() : Optional.of(new EventAnalyticsAggregate(result));
    }

    private static void parseLatestValueBuckets(String key, Aggregation value, Map<String, Map<String, List<Long>>> result) {
        List<JsonNode> buckets = value.getBuckets();

        if (buckets == null || buckets.isEmpty()) {
            return;
        }

        Map<String, Long> latestMetricValueMap = new HashMap<>();

        buckets.forEach(bucket ->
            bucket
                .fieldNames()
                .forEachRemaining(fieldName -> {
                    if (KEY.equals(fieldName) || DOC_COUNT.equals(fieldName)) {
                        return;
                    }
                    if (fieldName.startsWith(LATEST_BUCKET_PREFIX)) {
                        String metricField = fieldName.substring(LATEST_BUCKET_PREFIX.length());
                        long fieldValue = readTopMetricsValue(bucket.get(fieldName), metricField);
                        latestMetricValueMap.merge(metricField, fieldValue, Long::sum);
                    }
                })
        );

        if (!latestMetricValueMap.isEmpty()) {
            Map<String, List<Long>> latestValuesAsLists = new HashMap<>();
            latestMetricValueMap.forEach((field, total) -> latestValuesAsLists.put(field, List.of(total)));

            result.put(key, latestValuesAsLists);
        }
    }

    private static void parseStartEndValueBuckets(String key, Aggregation value, Map<String, Map<String, List<Long>>> result) {
        List<JsonNode> buckets = value.getBuckets();

        if (buckets == null || buckets.isEmpty()) {
            return;
        }

        Map<String, Long> deltasByField = new HashMap<>();

        for (JsonNode bucket : buckets) {
            Map<String, Long> starts = new HashMap<>();
            Map<String, Long> ends = new HashMap<>();

            bucket
                .fieldNames()
                .forEachRemaining(fieldName -> {
                    if (KEY.equals(fieldName) || DOC_COUNT.equals(fieldName)) {
                        return;
                    }
                    if (fieldName.startsWith(START_BUCKET_PREFIX)) {
                        String metricField = fieldName.substring(START_BUCKET_PREFIX.length());
                        long fieldValue = readTopMetricsValue(bucket.get(fieldName), metricField);
                        starts.put(metricField, fieldValue);
                    } else if (fieldName.startsWith(END_BUCKET_PREFIX)) {
                        String metricField = fieldName.substring(END_BUCKET_PREFIX.length());
                        long fieldValue = readTopMetricsValue(bucket.get(fieldName), metricField);
                        ends.put(metricField, fieldValue);
                    }
                });

            starts.forEach((metricField, startVal) -> {
                Long endVal = ends.get(metricField);

                if (endVal != null) {
                    deltasByField.merge(metricField, endVal - startVal, Long::sum);
                }
            });
        }

        if (!deltasByField.isEmpty()) {
            Map<String, List<Long>> deltaValuesAsLists = new HashMap<>();
            deltasByField.forEach((field, delta) -> deltaValuesAsLists.put(field, List.of(delta)));
            result.put(key, deltaValuesAsLists);
        }
    }

    private static long readTopMetricsValue(JsonNode topMetricsNode, String metricField) {
        if (topMetricsNode == null || !topMetricsNode.has(TOP)) {
            return 0L;
        }

        JsonNode topArray = topMetricsNode.get(TOP);

        if (topArray == null || !topArray.isArray() || topArray.isEmpty()) {
            return 0L;
        }

        JsonNode first = topArray.get(0);

        if (first == null || !first.has(METRICS)) {
            return 0L;
        }

        JsonNode metrics = first.get(METRICS);
        JsonNode valueNode = metrics.get(metricField);

        return (valueNode != null && valueNode.isNumber()) ? valueNode.asLong() : 0L;
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

    private static void parseDeltaBuckets(List<JsonNode> buckets, Map<String, Map<String, List<Long>>> result) {
        if (buckets == null || buckets.isEmpty()) return;

        buckets.forEach(bucket ->
            bucket
                .fieldNames()
                .forEachRemaining(bucketName -> {
                    // Each bucket with _delta suffix has start_value and end_value sub-aggregations
                    if (bucketName.endsWith(DELTA_BUCKET_SUFFIX)) {
                        // Get the value from the source hit in the start_value node
                        long startValue = parseValueFromBucketSource(bucket, bucketName, START_BUCKET_PREFIX);
                        // Get the value from the source hit in the end_value node
                        long endValue = parseValueFromBucketSource(bucket, bucketName, END_BUCKET_PREFIX);
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
