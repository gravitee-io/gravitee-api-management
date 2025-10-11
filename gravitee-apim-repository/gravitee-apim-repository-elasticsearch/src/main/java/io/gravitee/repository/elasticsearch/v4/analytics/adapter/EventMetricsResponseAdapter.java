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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.BUCKETS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.DOC_COUNT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.KEY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_VALUE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.MAX_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.PER_INTERVAL;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class EventMetricsResponseAdapter {

    private EventMetricsResponseAdapter() {}

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

        Map<String, List<Long>> result = new HashMap<>();

        aggregations.forEach((key, value) -> {
            switch (aggregationType) {
                case VALUE:
                    parseLatestValueBuckets(value, result);
                    break;
                case DELTA:
                    parseStartAndEndValueBuckets(value, result);
                    break;
                case TREND:
                    parseTrendQueryResponse(value, result);
                    break;
            }
        });

        return result.isEmpty() ? Optional.empty() : Optional.of(new EventAnalyticsAggregate(result));
    }

    private static void parseLatestValueBuckets(Aggregation agg, Map<String, List<Long>> result) {
        List<JsonNode> buckets = agg.getBuckets();

        if (buckets == null || buckets.isEmpty()) {
            return;
        }

        Map<String, Long> metricValueMap = new HashMap<>();

        buckets
            .stream()
            .filter(bucket -> bucket != null && bucket.fieldNames().hasNext())
            .forEach(bucket ->
                bucket
                    .fieldNames()
                    .forEachRemaining(fieldName -> {
                        // Ignore non-top_metrics fields
                        if (KEY.equals(fieldName) || DOC_COUNT.equals(fieldName)) return;

                        if (!fieldName.startsWith(LATEST_VALUE_PREFIX)) return;

                        // Derive the original metric field from "latest_<field>"
                        final String metricField = fieldName.substring(LATEST_VALUE_PREFIX.length());
                        final JsonNode topMetricsNode = bucket.get(fieldName);

                        // Require a top hit AND the metric value to be present and numeric
                        Long value = extractTopMetricValue(topMetricsNode, metricField);

                        if (value == null) return;

                        // Sum across composite buckets
                        metricValueMap.merge(metricField, value, Long::sum);
                    })
            );

        metricValueMap.forEach((field, total) -> result.put(field, List.of(total)));
    }

    private static Long extractTopMetricValue(JsonNode topMetricsNode, String metricField) {
        if (isTopHitMissing(topMetricsNode)) {
            return null;
        }

        JsonNode topArray = topMetricsNode.get(TOP);
        JsonNode first = (topArray != null && topArray.isArray() && !topArray.isEmpty()) ? topArray.get(0) : null;

        if (first == null || !first.has(METRICS)) {
            return null;
        }

        JsonNode metricsNode = first.get(METRICS);

        if (metricsNode == null) {
            return null;
        }

        JsonNode valueNode = metricsNode.get(metricField);

        if (valueNode == null || !valueNode.isNumber()) {
            return null;
        }

        return valueNode.asLong();
    }

    private static void parseStartAndEndValueBuckets(Aggregation agg, Map<String, List<Long>> result) {
        List<JsonNode> buckets = agg.getBuckets();

        if (buckets == null || buckets.isEmpty()) return;

        Map<String, Long> metricValueMap = new HashMap<>();

        for (JsonNode bucket : buckets) {
            JsonNode startValueNode = bucket.get(io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BEFORE_START);
            JsonNode endValueNode = bucket.get(io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_IN_RANGE);

            Map<String, Long> startValuesMap = collectTopMetrics(startValueNode, START_BUCKET_PREFIX);
            Map<String, Long> endValuesMap = collectTopMetrics(endValueNode, END_BUCKET_PREFIX);

            Set<String> fields = new HashSet<>(startValuesMap.keySet());
            fields.addAll(endValuesMap.keySet());

            for (String field : fields) {
                Long endVal = endValuesMap.get(field);

                if (endVal == null) continue;

                Long startVal = startValuesMap.get(field);

                if (startVal == null) {
                    startVal = 0L; // baseline 0 when no start value
                }

                long delta = endVal - startVal;

                if (delta < 0L) {
                    delta = 0L;
                }

                metricValueMap.merge(field, delta, Long::sum);
            }
        }

        metricValueMap.forEach((field, delta) -> result.put(field, List.of(delta)));
    }

    private static void parseTrendQueryResponse(Aggregation aggregation, Map<String, List<Long>> result) {
        List<JsonNode> buckets = aggregation.getBuckets();

        if (buckets == null || buckets.isEmpty()) {
            return;
        }

        boolean hasComposite = buckets.getFirst().has(PER_INTERVAL);
        Set<Long> timelineValues = new LinkedHashSet<>();
        Map<String, Map<Long, Long>> maxValueByTimestampByField = new HashMap<>();

        if (hasComposite) {
            for (JsonNode dimensionBucket : buckets) {
                JsonNode perInterval = dimensionBucket.get(PER_INTERVAL);
                JsonNode intervals = (perInterval == null) ? null : perInterval.get(BUCKETS);

                if (intervals == null || !intervals.isArray()) {
                    continue;
                }

                intervals.forEach(interval -> processPerIntervalBucket(interval, timelineValues, maxValueByTimestampByField));
            }
        }

        if (timelineValues.isEmpty() || maxValueByTimestampByField.isEmpty()) {
            return;
        }

        // Build a sorted timeline and project each field's sparse deltas onto it
        List<Long> sortedTimeline = new ArrayList<>(timelineValues);
        sortedTimeline.sort(Long::compare);
        Map<String, List<Long>> maxValuesByField = new HashMap<>();

        maxValueByTimestampByField.forEach((field, maxValueByTimestamp) -> {
            List<Long> series = new ArrayList<>(sortedTimeline.size());

            for (Long ts : sortedTimeline) {
                series.add(maxValueByTimestamp.getOrDefault(ts, null));
            }

            maxValuesByField.put(field, series);
        });

        maxValuesByField.forEach((field, maxValues) -> {
            List<Long> trend = new ArrayList<>();
            Long previousMax = maxValues.stream().filter(Objects::nonNull).findFirst().orElse(null);

            for (Long currentMax : maxValues) {
                if (currentMax == null) {
                    trend.add(null);
                } else {
                    trend.add(previousMax > currentMax ? currentMax : Long.valueOf(currentMax - previousMax));
                    previousMax = currentMax;
                }
            }

            result.put(field, trend);
        });
    }

    private static void processPerIntervalBucket(
        JsonNode interval,
        Set<Long> timelineValues,
        Map<String, Map<Long, Long>> maxValueByTimestampByField
    ) {
        Long timestamp = parseBucketTimestamp(interval);

        if (timestamp == null) {
            return;
        }

        timelineValues.add(timestamp);
        interval
            .fieldNames()
            .forEachRemaining(fieldName -> {
                if (isMetaField(fieldName) || !fieldName.startsWith(MAX_PREFIX)) {
                    return;
                }

                String metricName = fieldName.substring(MAX_PREFIX.length());
                Long currentMax = parseMaxFieldValue(interval.get(fieldName));

                if (currentMax == null) {
                    return;
                }

                maxValueByTimestampByField.computeIfAbsent(metricName, __ -> new HashMap<>()).merge(timestamp, currentMax, Long::sum);
            });
    }

    private static boolean isMetaField(final String name) {
        return KEY.equals(name) || DOC_COUNT.equals(name);
    }

    private static Long parseBucketTimestamp(JsonNode node) {
        if (node == null) {
            return null;
        }

        JsonNode keyNode = node.get(KEY);

        return (keyNode != null && keyNode.isNumber()) ? keyNode.asLong() : null;
    }

    private static Long parseMaxFieldValue(final JsonNode node) {
        if (node == null) {
            return null;
        }

        JsonNode valueNode = node.get("value");

        return (valueNode == null || !valueNode.isNumber()) ? null : valueNode.asLong();
    }

    private static Map<String, Long> collectTopMetrics(JsonNode parent, String prefix) {
        Map<String, Long> values = new HashMap<>();

        if (parent == null) return values;

        parent
            .fieldNames()
            .forEachRemaining(fieldName -> {
                if (KEY.equals(fieldName) || DOC_COUNT.equals(fieldName)) return;

                if (!fieldName.startsWith(prefix)) return;

                String metric = fieldName.substring(prefix.length());
                JsonNode topMetricsNode = parent.get(fieldName);

                if (isTopHitMissing(topMetricsNode)) return;

                JsonNode topArray = topMetricsNode.get(TOP);
                JsonNode first = (topArray != null && topArray.isArray() && !topArray.isEmpty()) ? topArray.get(0) : null;

                if (first == null || !first.has(METRICS)) return;

                JsonNode metricsNode = first.get(METRICS);
                JsonNode valueNode = metricsNode.get(metric);

                if (valueNode == null || !valueNode.isNumber()) return;

                values.put(metric, valueNode.asLong());
            });

        return values;
    }

    private static boolean isTopHitMissing(JsonNode topMetricsNode) {
        if (topMetricsNode == null) return true;

        JsonNode topArray = topMetricsNode.get(TOP);

        return topArray == null || !topArray.isArray() || topArray.isEmpty();
    }

    private static AggregationType getAggregationType(HistogramQuery query) {
        if (query == null || query.aggregations() == null || query.aggregations().isEmpty()) {
            return null;
        }

        var types = query
            .aggregations()
            .stream()
            .map(io.gravitee.repository.log.v4.model.analytics.Aggregation::getType)
            .distinct()
            .toList();

        return (types.size() == 1) ? types.getFirst() : null;
    }
}
