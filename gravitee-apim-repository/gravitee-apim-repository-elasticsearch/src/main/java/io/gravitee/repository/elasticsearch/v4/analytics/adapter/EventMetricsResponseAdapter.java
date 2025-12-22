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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.DOC_COUNT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.FILTERED_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.TOTAL_PREFIX;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.query.events.EventAnalyticsAggregate;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        Map<String, List<Double>> result = new HashMap<>();

        aggregations.forEach((key, value) -> {
            switch (aggregationType) {
                case VALUE:
                    parseTopHitValues(key, value, result);
                    break;
                case DELTA:
                    parseTotalValues(key, value, result);
                    break;
                case TREND:
                    parseTrendQueryResponse(value, result);
                    break;
                case TREND_RATE:
                    parseTrendRateQueryResponse(value, result, query);
                    break;
            }
        });

        return result.isEmpty() ? Optional.empty() : Optional.of(new EventAnalyticsAggregate(result));
    }

    private static void parseTotalValues(String key, Aggregation agg, Map<String, List<Double>> result) {
        if (!key.startsWith(FILTERED_PREFIX)) return;

        String metricKey = key.substring(FILTERED_PREFIX.length());

        var resultMetric = result.computeIfAbsent(metricKey, k -> new ArrayList<>());

        var metricAgg = agg.getAggregations().get(TOTAL_PREFIX + metricKey);

        resultMetric.add((metricAgg == null || metricAgg.getValue() == null) ? null : metricAgg.getValue().doubleValue());
    }

    private static void parseTopHitValues(String key, Aggregation agg, Map<String, List<Double>> result) {
        if (!key.startsWith(FILTERED_PREFIX)) return;

        String metricKey = key.substring(FILTERED_PREFIX.length());

        var resultMetric = result.computeIfAbsent(metricKey, k -> new ArrayList<>());

        var metricAgg = agg.getAggregations().get(LATEST_PREFIX + metricKey);

        if (metricAgg.getHits() == null || metricAgg.getHits().getHits() == null || metricAgg.getHits().getHits().isEmpty()) {
            resultMetric.add(null);
            return;
        }
        var hit = metricAgg.getHits().getHits().getFirst();

        if (hit == null || hit.getSource() == null || hit.getSource().get(metricKey) == null) {
            resultMetric.add(null);
            return;
        }
        var metricHit = hit.getSource().get(metricKey);

        resultMetric.add((metricHit == null) ? null : metricHit.doubleValue());
    }

    private static void parseTrendQueryResponse(Aggregation aggregation, Map<String, List<Double>> result) {
        List<JsonNode> buckets = aggregation.getBuckets();
        if (buckets == null || buckets.isEmpty()) return;

        for (JsonNode bucket : buckets) {
            var docCount = bucket.get(DOC_COUNT).asLong();
            bucket
                .fieldNames()
                .forEachRemaining(fieldName -> {
                    if (!fieldName.startsWith(TOTAL_PREFIX)) return;

                    String metricKey = fieldName.substring(TOTAL_PREFIX.length());
                    var value = parseValueNode(bucket.get(fieldName));
                    var resultMetric = result.computeIfAbsent(metricKey, k -> new ArrayList<>());

                    if (docCount == 0L || value == null) {
                        resultMetric.add(null);
                    } else {
                        resultMetric.add(value);
                    }
                });
        }
    }

    private static void parseTrendRateQueryResponse(Aggregation aggregation, Map<String, List<Double>> result, HistogramQuery query) {
        List<JsonNode> buckets = aggregation.getBuckets();
        if (buckets == null || buckets.isEmpty()) return;

        Optional<Duration> interval = query.timeRange().interval();
        if (interval.isEmpty()) return;
        long trendIntervalMs = interval.get().toMillis();

        for (JsonNode bucket : buckets) {
            var docCount = bucket.get(DOC_COUNT).asLong();
            bucket
                .fieldNames()
                .forEachRemaining(fieldName -> {
                    if (!fieldName.startsWith(TOTAL_PREFIX)) return;

                    String metricKey = fieldName.substring(TOTAL_PREFIX.length());
                    var value = parseValueNode(bucket.get(fieldName));
                    var resultMetric = result.computeIfAbsent(metricKey, k -> new ArrayList<>());

                    if (docCount == 0L || value == null) {
                        resultMetric.add(null);
                    } else {
                        double factor = 1000d;
                        double perSecond = (value * 1000d) / trendIntervalMs;
                        resultMetric.add(Math.round((perSecond) * factor) / factor);
                    }
                });
        }
    }

    private static Double parseValueNode(final JsonNode node) {
        if (node == null) {
            return null;
        }

        JsonNode valueNode = node.get("value");

        return (valueNode == null || !valueNode.isNumber()) ? null : valueNode.asDouble();
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
}
