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
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.MAX_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.PER_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.START_PREFIX;
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
import java.util.List;
import java.util.Map;
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

                        if (!fieldName.startsWith(LATEST_PREFIX)) return;

                        // Derive the original metric field from "latest_<field>"
                        String metric = fieldName.substring(LATEST_PREFIX.length());
                        JsonNode topMetricsNode = bucket.get(fieldName);
                        // Require a top hit AND the metric value to be present and numeric
                        Long value = parseTopMetricsNode(topMetricsNode, metric);

                        if (value == null) return;

                        // Sum across composite buckets
                        metricValueMap.merge(metric, value, Long::sum);
                    })
            );

        metricValueMap.forEach((metric, total) -> result.put(metric, List.of(total)));
    }

    private static Long parseTopMetricsNode(JsonNode topMetricsNode, String metric) {
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

        JsonNode valueNode = metricsNode.get(metric);

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

            Map<String, Long> startValuesMap = collectTopMetrics(startValueNode, START_PREFIX);
            Map<String, Long> endValuesMap = collectTopMetrics(endValueNode, END_PREFIX);

            Set<String> fields = new HashSet<>(startValuesMap.keySet());
            fields.addAll(endValuesMap.keySet());

            for (String metric : fields) {
                Long endVal = endValuesMap.get(metric);

                if (endVal == null) continue;

                Long startVal = startValuesMap.get(metric);

                if (startVal == null) {
                    startVal = 0L; // baseline 0 when no start value
                }

                long delta = endVal - startVal;

                if (delta < 0L) {
                    delta = 0L;
                }

                metricValueMap.merge(metric, delta, Long::sum);
            }
        }

        metricValueMap.forEach((metric, delta) -> result.put(metric, List.of(delta)));
    }

    private static void parseTrendQueryResponse(Aggregation aggregation, Map<String, List<Long>> result) {
        List<JsonNode> buckets = aggregation.getBuckets();

        if (buckets == null || buckets.isEmpty()) return;

        JsonNode first = buckets.getFirst();

        if (first == null || !first.has(PER_INTERVAL)) return;

        Set<Long> timeline = new HashSet<>();
        Map<SeriesKey, Long> valuesByKey = new HashMap<>();
        Map<String, Set<String>> metricToIds = new HashMap<>();

        parseCompositeIdBuckets(buckets, timeline, valuesByKey, metricToIds);

        if (timeline.isEmpty() || metricToIds.isEmpty()) return;

        List<Long> sortedTimeline = sortedTimeline(timeline);

        metricToIds.forEach((metric, ids) -> {
            List<List<Long>> deltaSeriesList = buildDeltaSeriesPerId(metric, ids, sortedTimeline, valuesByKey);
            result.put(metric, buildTrendFromDeltaSeries(deltaSeriesList));
        });
    }

    private static void parseCompositeIdBuckets(
        List<JsonNode> buckets,
        Set<Long> timeline,
        Map<SeriesKey, Long> valuesByKey,
        Map<String, Set<String>> metricToIds
    ) {
        for (JsonNode perIdBucket : buckets) {
            JsonNode perInterval = perIdBucket.get(PER_INTERVAL);
            JsonNode intervals = (perInterval == null) ? null : perInterval.get(BUCKETS);

            if (intervals == null || !intervals.isArray()) continue;

            String id = (perIdBucket.get(KEY) != null) ? perIdBucket.get(KEY).toString() : perIdBucket.toString();

            for (JsonNode interval : intervals) {
                Long timestamp = parseTimestamp(interval);

                if (timestamp == null) continue;

                timeline.add(timestamp);

                interval
                    .fieldNames()
                    .forEachRemaining(fieldName -> {
                        if (isMetaField(fieldName) || !fieldName.startsWith(MAX_PREFIX)) return;

                        String metric = fieldName.substring(MAX_PREFIX.length());
                        Long maxValue = parseMaxValueNode(interval.get(fieldName));

                        if (maxValue == null) return;

                        valuesByKey.put(new SeriesKey(metric, id, timestamp), maxValue);
                        metricToIds.computeIfAbsent(metric, k -> new HashSet<>()).add(id);
                    });
            }
        }
    }

    private static List<Long> sortedTimeline(Set<Long> timeline) {
        List<Long> sorted = new ArrayList<>(timeline);
        sorted.sort(Long::compare);

        return sorted;
    }

    private static List<List<Long>> buildDeltaSeriesPerId(
        String metric,
        Set<String> ids,
        List<Long> timeline,
        Map<SeriesKey, Long> valuesByKey
    ) {
        List<List<Long>> deltaSeriesList = new ArrayList<>(ids.size());

        for (String id : ids) {
            List<Long> deltaSeries = new ArrayList<>(timeline.size());
            Long previous = null;

            for (Long ts : timeline) {
                Long current = valuesByKey.get(new SeriesKey(metric, id, ts));

                if (current == null) {
                    deltaSeries.add(null); // data gap
                } else if (previous == null) {
                    deltaSeries.add(0L); // first observation baseline
                    previous = current;
                } else {
                    // reset-aware delta
                    deltaSeries.add(current >= previous ? current - previous : current);
                    previous = current;
                }
            }

            deltaSeriesList.add(deltaSeries);
        }

        return deltaSeriesList;
    }

    private static List<Long> buildTrendFromDeltaSeries(List<List<Long>> deltaSeriesList) {
        if (deltaSeriesList.isEmpty()) return List.of();

        int dataPoints = deltaSeriesList.getFirst().size();
        List<Long> trend = new ArrayList<>(dataPoints);

        for (int i = 0; i < dataPoints; i++) {
            long sum = 0L;
            boolean hasValue = false;

            for (List<Long> series : deltaSeriesList) {
                Long delta = series.get(i);

                if (delta != null) {
                    sum += delta;
                    hasValue = true;
                }
            }

            trend.add(hasValue ? sum : null);
        }

        return trend;
    }

    private static boolean isMetaField(final String name) {
        return KEY.equals(name) || DOC_COUNT.equals(name);
    }

    private static Long parseTimestamp(JsonNode node) {
        if (node == null) {
            return null;
        }

        JsonNode keyNode = node.get(KEY);

        return (keyNode != null && keyNode.isNumber()) ? keyNode.asLong() : null;
    }

    private static Long parseMaxValueNode(final JsonNode node) {
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
                Long value = parseTopMetricsNode(parent.get(fieldName), metric);

                if (value != null) {
                    values.put(metric, value);
                }
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

        List<AggregationType> types = query
            .aggregations()
            .stream()
            .map(io.gravitee.repository.log.v4.model.analytics.Aggregation::getType)
            .distinct()
            .toList();

        return (types.size() == 1) ? types.getFirst() : null;
    }

    private record SeriesKey(String metric, String id, long ts) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (!(o instanceof SeriesKey(String metric1, String id1, long ts1))) return false;

            return ts == ts1 && metric.equals(metric1) && id.equals(id1);
        }

        @Override
        public int hashCode() {
            int result = metric.hashCode();
            result = 31 * result + id.hashCode();
            result = 31 * result + Long.hashCode(ts);

            return result;
        }
    }
}
