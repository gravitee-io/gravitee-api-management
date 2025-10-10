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
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.DERIVATIVE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_VALUE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.MAX_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.NON_NEGATIVE_PREFIX;
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

        // Flat output: <field> -> List<Long>
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

    // VALUE: sum latest_* values across buckets per field, write as singleton list
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

        // Emit only metrics that were actually observed at least once
        metricValueMap.forEach((field, total) -> result.put(field, List.of(total)));
    }

    // Extracts the numeric value for a metric from a top_metrics node if and only if:
    // - a top hit exists (non-empty "top" array)
    // - the first hit contains "metrics" and a numeric value for the requested field
    // Returns null otherwise (caller should skip merging).
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

    // DELTA:
    // - Compute (end - start) per field across buckets, write as singleton list
    // - Prefer END_IN_RANGE (query-time end window)
    // - If start is missing but end exists, baseline is 0.
    private static void parseStartAndEndValueBuckets(Aggregation agg, Map<String, List<Long>> result) {
        List<JsonNode> buckets = agg.getBuckets();

        if (buckets == null || buckets.isEmpty()) return;

        Map<String, Long> metricValueMap = new HashMap<>();

        for (JsonNode bucket : buckets) {
            JsonNode startValueNode = bucket.get(io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BEFORE_START);
            // Prefer end_in_range, but fall back to before_end_time if present (legacy/fixtures).
            JsonNode endValueNode = bucket.get(io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_IN_RANGE);

            if (endValueNode == null) {
                endValueNode = bucket.get(io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BEFORE_END);
            }

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

    // TREND: Reset-aware deltas from max_<field> per interval; align timeline; sum across dimensions
    private static void parseTrendQueryResponse(Aggregation aggregation, Map<String, List<Long>> result) {
        List<JsonNode> buckets = aggregation.getBuckets();

        if (buckets == null || buckets.isEmpty()) {
            return;
        }

        boolean hasComposite = buckets.getFirst().has(PER_INTERVAL);
        // Global timeline across all dimensions/intervals
        Set<Long> timelineValues = new LinkedHashSet<>();
        // field -> (timestamp -> summed delta)
        Map<String, Map<Long, Long>> fieldDeltaByTimeStampMap = new HashMap<>();

        if (hasComposite) {
            // Composite: each outer bucket contains PER_INTERVAL aggregation
            for (JsonNode dimensionBucket : buckets) {
                JsonNode perInterval = dimensionBucket.get(PER_INTERVAL);
                JsonNode intervals = (perInterval == null) ? null : perInterval.get(BUCKETS);

                if (intervals == null || !intervals.isArray()) {
                    continue;
                }
                // Last observed max per field for applied composite keys
                final Map<String, Long> latestMaxByFieldMap = new HashMap<>();
                intervals.forEach(interval ->
                    processPerIntervalBucket(interval, latestMaxByFieldMap, timelineValues, fieldDeltaByTimeStampMap)
                );
            }
        } else {
            // Flat histogram (no composite): treat top-level buckets as intervals
            Map<String, Long> latestMaxByFieldMap = new HashMap<>();

            for (JsonNode interval : buckets) {
                processPerIntervalBucket(interval, latestMaxByFieldMap, timelineValues, fieldDeltaByTimeStampMap);
            }
        }

        if (timelineValues.isEmpty() || fieldDeltaByTimeStampMap.isEmpty()) {
            return;
        }

        // Build a sorted timeline and project each field's sparse deltas onto it
        List<Long> sortedTimeline = new ArrayList<>(timelineValues);
        // Sort timestamps
        sortedTimeline.sort(Long::compare);

        Map<String, List<Long>> aa = new HashMap<>();

        fieldDeltaByTimeStampMap.forEach((field, deltaByTimestamp) -> {
            List<Long> series = new ArrayList<>(sortedTimeline.size());
            // Get value of field at each timestamp and add to trend
            for (Long ts : sortedTimeline) {
                series.add(deltaByTimestamp.getOrDefault(ts, null));
            }

            aa.put(field, series);
        });

        aa.forEach((field, maxSeries) -> {
            List<Long> series = new ArrayList<>();

            Long previousValue = maxSeries.stream().filter(Objects::nonNull).findFirst().orElse(null);

            for (Long maxSeriesValue : maxSeries) {

                if (maxSeriesValue == null) {
                    series.add(null);
                } else {

                    if(previousValue > maxSeriesValue) {
                        series.add(maxSeriesValue);
                    } else {
                        series.add(maxSeriesValue - previousValue);
                    }

                    previousValue = maxSeriesValue;
                }
            }

            result.put(field, series);
        });



    }

    private static void processPerIntervalBucket(
        JsonNode interval,
        Map<String, Long> lastestMaxByFieldMap,
        Set<Long> timelineValues,
        Map<String, Map<Long, Long>> fieldDeltaByTimeStampMap
    ) {
        Long timestamp = parseBucketTimestamp(interval);

        if (timestamp == null) {
            return;
        }

        timelineValues.add(timestamp);

        interval
            .fieldNames()
            .forEachRemaining(fieldName -> {
                // Skip meta fields and keep only max_<field> metrics
                if (isMetaField(fieldName) || !fieldName.startsWith(MAX_PREFIX)) {
                    return;
                }

                String metricName = fieldName.substring(MAX_PREFIX.length());
                Long currentMax = parseMaxFieldValue(interval.get(fieldName));
                // No observation for this field at this interval -> no delta written

                // prevuiysValue 0 0 0 0 1 1 0 0 0 0
                // 0 null null null 1 null 0 null  null 0

//                Long previousMax = lastestMaxByFieldMap.get(metricName);
//                long delta;
                if (currentMax == null) {
//                    fieldDeltaByTimeStampMap.computeIfAbsent(metricName, __ -> new HashMap<>()).getOrDefault(timestamp, null);
                    return;
                }
//
//                if (previousMax == null) {
//                    // First observation for this field in this dimension: delta = 0 (not a gap)
//                    delta = 0L;
//                } else if (currentMax >= previousMax) {
//                    // Normal monotonic increase
//                    delta = currentMax - previousMax;
//                } else {
//                    // Counter reset detected: use current value as the delta
//                    delta = currentMax;
//                }

//                lastestMaxByFieldMap.put(metricName, currentMax);



                fieldDeltaByTimeStampMap.computeIfAbsent(metricName, __ -> new HashMap<>()).merge(timestamp, currentMax, Long::sum);
            });
    }

    private static boolean isMetaField(final String name) {
        return KEY.equals(name) || DOC_COUNT.equals(name);
    }

    private static Long parseTopMetricsResponse(final JsonNode topMetricsNode, final String fieldName) {
        if (topMetricsNode == null) {
            return null;
        }

        JsonNode top = topMetricsNode.get(TOP);

        if (top == null || !top.isArray() || top.isEmpty()) {
            return null;
        }

        JsonNode first = top.get(0);

        if (first == null) {
            return null;
        }

        JsonNode metrics = first.get(METRICS);

        if (metrics == null) {
            return null;
        }

        final JsonNode value = metrics.get(fieldName);

        return (value != null && value.isNumber()) ? value.asLong() : null;
    }

    private static void mergeTopMetricsResponse(Map<String, Long> result, JsonNode node, String bucketPrefix) {
        if (node == null) {
            return;
        }

        node
            .fieldNames()
            .forEachRemaining(fieldName -> {
                if (isMetaField(fieldName) || !fieldName.startsWith(bucketPrefix)) {
                    return;
                }

                String metricField = fieldName.substring(bucketPrefix.length());
                Long value = parseTopMetricsResponse(node.get(fieldName), metricField);

                if (value != null) {
                    result.merge(metricField, value, Long::sum);
                }
            });
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

    // Collector that sums values per timestamp across dimension buckets (no doc_count filtering)
    private static void collectTrendValues(Iterable<JsonNode> intervalBuckets, Map<String, Map<Long, Long>> valueByTimestampPerFiled) {
        if (intervalBuckets == null) return;

        intervalBuckets.forEach(b -> parseIntervalBucket(b, valueByTimestampPerFiled));
    }

    private static void parseIntervalBucket(JsonNode intervalBucket, Map<String, Map<Long, Long>> valueByTimestampPerFiled) {
        if (intervalBucket == null || !intervalBucket.hasNonNull(KEY) || !intervalBucket.get(KEY).isNumber()) {
            return;
        }

        long timestamp = intervalBucket.get(KEY).asLong();
        Map<String, Long> contributions = new HashMap<>();

        intervalBucket
            .fieldNames()
            .forEachRemaining(fieldName -> {

                if(fieldName == null) return;

                if(fieldName.startsWith("max_")) {

                Long value = extractNonNegativeValue(intervalBucket.get(fieldName));

                if(value == null) {
                    contributions.put(fieldName, null);
                }



                }

                // get max_downstream-publish-messages-total
                // if null return null to diff
                // if not null
                // add diff = value - previous value to contributions for this timestamp
                //



//                if (KEY.equals(fieldName) || DOC_COUNT.equals(fieldName)) return;
//
//                boolean isNonNeg = fieldName.startsWith(NON_NEGATIVE_PREFIX);
//                boolean isDerivative = !isNonNeg && fieldName.startsWith(DERIVATIVE_PREFIX);
//
//                if (!isNonNeg && !isDerivative) return;
//
//                String metricName = isNonNeg
//                    ? fieldName.substring(NON_NEGATIVE_PREFIX.length())
//                    : fieldName.substring(DERIVATIVE_PREFIX.length());
//
//                Long value = extractNonNegativeValue(intervalBucket.get(fieldName));
//
//                if (value == null) return;
//
//                if (isNonNeg) {
//                    contributions.put(metricName, value); // prefer non_negative
//                } else {
//                    contributions.putIfAbsent(metricName, value); // fallback to derivative if no non_negative
//                }
            });

        if (!contributions.isEmpty()) {
            contributions.forEach((field, value) ->
                valueByTimestampPerFiled.computeIfAbsent(field, k -> new HashMap<>()).merge(timestamp, value, Long::sum)
            );
        }
    }

    // Read a pipeline agg node like { "value": number } as non-negative rounded long.
    // Returns null if "value" is missing or non-numeric.
    private static Long extractNonNegativeValue(JsonNode aggNode) {
        if (aggNode == null) return null;

        JsonNode valueNode = aggNode.get("value");

        if (valueNode == null || !valueNode.isNumber()) {
            return null;
        }

        long v = Math.round(valueNode.asDouble());

        return Math.max(v, 0L);
    }

    // Collects top_metrics values under the given parent filter agg node for fields with the given prefix.
    // STRICT rule: include a metric only when a top hit exists AND the metric value is present and numeric.
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

    // Helper to know if a top_metrics agg actually returned a hit even if the value is 0
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
