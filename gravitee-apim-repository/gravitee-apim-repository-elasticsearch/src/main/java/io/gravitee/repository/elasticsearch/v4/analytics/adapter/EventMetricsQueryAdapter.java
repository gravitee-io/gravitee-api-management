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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.BUCKETS_PATH;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.BUCKET_SCRIPT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.COMPOSITE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.DATE_HISTOGRAM;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.DERIVATIVE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.EXTENDED_BOUNDS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.FIXED_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.GAP_POLICY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.INSERT_ZEROS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MAX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MIN;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MINIMUM_SHOULD_MATCH;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MIN_DOC_COUNT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MISSING_BUCKET;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SCRIPT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SORT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SOURCES;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.TOP_METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.AGGS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.BOOL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.FILTER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.QUERY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SHOULD;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SIZE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TIMESTAMP;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TRACK_TOTAL_HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BEFORE_START;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BY_DIMENSIONS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.DERIVATIVE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_IN_RANGE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_VALUE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.MAX_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.MILLISECONDS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.NON_NEGATIVE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.PER_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.START_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.EXISTS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.GTE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.LT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.LTE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.RANGE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.TERM;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens.FIELD;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens.TERMS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.repository.log.v4.model.analytics.Aggregation;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds Elasticsearch JSON queries for event-metrics analytics:
 * - VALUE: latest value per composite bucket (top_metrics), summed during response parsing.
 * - DELTA: end - start per composite bucket using two time windows; negatives are clamped during response parsing.
 * - TREND: per-interval derivative (based on max per interval) with non-negative clamping; summed and aligned during response parsing.
 * Rules:
 * - No pagination (composite size is capped to 1000).
 * - Exists filters use OR semantics (at least one requested metric must exist).
 * - Root time range is applied for VALUE and TREND; DELTA uses per-window filters instead.
 * - Composite keys:
 *   - VALUE: gw-id, api-id, org-id, env-id
 *   - DELTA/TREND: gw-id, api-id, org-id, env-id, app-id, plan-id, topic (with missing buckets for app-id/plan-id/topic)
 */
public final class EventMetricsQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EventMetricsQueryAdapter() {}

    public static String toESQuery(HistogramQuery query) {
        Set<AggregationType> types = query.aggregations().stream().map(Aggregation::getType).collect(Collectors.toSet());

        if (types.size() != 1) {
            throw new IllegalArgumentException("Exactly one aggregate function should be specified");
        }

        AggregationType type = types.iterator().next();

        ObjectNode root = MAPPER.createObjectNode();
        root.put(SIZE, 0);
        root.put(TRACK_TOTAL_HITS, false);
        ObjectNode aggregationsNode = root.putObject(AGGS);

        switch (type) {
            case VALUE -> applyLatestValueAggregations(query, aggregationsNode);
            case DELTA -> applyDeltaAggregations(query, aggregationsNode);
            case TREND -> applyTrendAggregations(query, aggregationsNode);
        }

        ArrayNode filters = MAPPER.createArrayNode();
        applyDefaultFilters(query, filters, type);
        applyExistsFilters(query, filters);
        applyOptionalFilters(query, filters);
        finalizeQuery(filters, root);

        return root.toString();
    }

    private static void applyLatestValueAggregations(HistogramQuery query, ObjectNode aggregationsNode) {
        ObjectNode dimensionsNode = aggregationsNode.putObject(BY_DIMENSIONS);
        ObjectNode compositeNode = dimensionsNode.putObject(COMPOSITE);
        compositeNode.put(SIZE, 1000);
        ArrayNode sourcesNode = compositeNode.putArray(SOURCES);
        addDefaultCompositeKeys(sourcesNode);

        ObjectNode bucketAggregationNode = dimensionsNode.putObject(AGGS);
        query
            .aggregations()
            .forEach(agg -> {
                String field = agg.getField();
                ObjectNode latestValueNode = bucketAggregationNode.putObject(LATEST_VALUE_PREFIX + field);
                applyTopMetricsAggregation(latestValueNode, field);
            });
    }

    private static void applyDeltaAggregations(HistogramQuery query, ObjectNode aggregationsNode) {
        ObjectNode bucketAggregations = applyCompositeWithExtras(aggregationsNode, "app-id", "plan-id", "topic");
        long start = query.timeRange().from().toEpochMilli();
        long end = query.timeRange().to().toEpochMilli();
        // before_start_time: gets the lastest doc strictly before 'start'
        ObjectNode startValueNode = bucketAggregations.putObject(BEFORE_START);
        ObjectNode startValueTimeRangeNode = startValueNode.putObject(FILTER).putObject(RANGE).putObject(TIMESTAMP);
        startValueTimeRangeNode.put(LT, start);
        ObjectNode startValueAggregations = startValueNode.putObject(AGGS);
        // end_in_range: gets the lastest doc in [start, end)
        ObjectNode endValueNode = bucketAggregations.putObject(END_IN_RANGE);
        ObjectNode endValueTimeRangeNode = endValueNode.putObject(FILTER).putObject(RANGE).putObject(TIMESTAMP);
        endValueTimeRangeNode.put(GTE, start);
        endValueTimeRangeNode.put(LT, end);
        ObjectNode endValueAggregations = endValueNode.putObject(AGGS);

        query
            .aggregations()
            .forEach(agg -> {
                String field = agg.getField();
                ObjectNode startAgg = startValueAggregations.putObject(START_BUCKET_PREFIX + field);
                applyTopMetricsAggregation(startAgg, field);
                ObjectNode endAgg = endValueAggregations.putObject(END_BUCKET_PREFIX + field);
                applyTopMetricsAggregation(endAgg, field);
            });
    }

    private static void applyTrendAggregations(HistogramQuery query, ObjectNode aggregationsNode) {
        ObjectNode bucketAggregations = applyCompositeWithExtras(aggregationsNode, "app-id", "plan-id", "topic");
        ObjectNode intervalAggregations = applyFixedIntervalAggregations(query, bucketAggregations);
        query.aggregations().forEach(agg -> applyDerivativeAggregations(intervalAggregations, agg.getField()));
    }

    private static void applyDerivativeAggregations(ObjectNode aggregationsNode, String field) {
        String max = MAX_PREFIX + field;
        String derivative = DERIVATIVE_PREFIX + field;
        String nonNegative = NON_NEGATIVE_PREFIX + field;

        aggregationsNode.putObject(max).putObject(MAX).put(FIELD, field);

        ObjectNode derivativeNode = aggregationsNode.putObject(derivative).putObject(DERIVATIVE);
        derivativeNode.put(BUCKETS_PATH, max);
        derivativeNode.put(GAP_POLICY, INSERT_ZEROS);

        ObjectNode scriptNode = aggregationsNode.putObject(nonNegative).putObject(BUCKET_SCRIPT);
        ObjectNode path = scriptNode.putObject(BUCKETS_PATH);
        path.put("d", derivative);
        scriptNode.put(SCRIPT, "params.d != null ? Math.max(params.d, 0) : 0");
    }

    private static ObjectNode applyCompositeWithExtras(ObjectNode aggregationsNode, String... additionalKeys) {
        ObjectNode dimensionsNode = aggregationsNode.putObject(BY_DIMENSIONS);
        ObjectNode compositeNode = dimensionsNode.putObject(COMPOSITE);
        compositeNode.put(SIZE, 1000);
        ArrayNode sourcesNode = compositeNode.putArray(SOURCES);
        addDefaultCompositeKeys(sourcesNode);

        if (additionalKeys != null) {
            Arrays.stream(additionalKeys).forEach(field -> addCompositeKey(sourcesNode, field));
        }

        return dimensionsNode.putObject(AGGS);
    }

    private static void addDefaultCompositeKeys(ArrayNode node) {
        addCompositeKey(node, "gw-id");
        addCompositeKey(node, "api-id");
        addCompositeKey(node, "org-id");
        addCompositeKey(node, "env-id");
    }

    private static void addCompositeKey(ArrayNode node, String fieldName) {
        ObjectNode sourceNode = MAPPER.createObjectNode();
        ObjectNode termsNode = MAPPER.createObjectNode();
        ObjectNode fieldNode = MAPPER.createObjectNode();
        fieldNode.put(FIELD, fieldName);

        if (fieldName.equals("topic") || fieldName.equals("plan-id") || fieldName.equals("app-id")) {
            fieldNode.put(MISSING_BUCKET, true);
        }

        termsNode.set(TERMS, fieldNode);
        sourceNode.set(fieldName, termsNode);
        node.add(sourceNode);
    }

    private static void applyTopMetricsAggregation(ObjectNode container, String field) {
        ObjectNode tm = container.putObject(TOP_METRICS);
        tm.putObject(METRICS).put(FIELD, field);
        tm.putObject(SORT).put(TIMESTAMP, io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.DESC);
    }

    private static ObjectNode applyFixedIntervalAggregations(HistogramQuery query, ObjectNode aggregationsNode) {
        TimeRange timeRange = query.timeRange();
        Duration duration = timeRange
            .interval()
            .orElseThrow(() -> new IllegalArgumentException("Interval is required for TREND aggregations"));
        long from = timeRange.from().toEpochMilli();
        long to = timeRange.to().toEpochMilli();

        ObjectNode intervalNode = aggregationsNode.putObject(PER_INTERVAL);
        ObjectNode histogramNode = intervalNode.putObject(DATE_HISTOGRAM);
        histogramNode.put(FIELD, TIMESTAMP);
        histogramNode.put(FIXED_INTERVAL, duration.toMillis() + MILLISECONDS);
        histogramNode.put(MIN_DOC_COUNT, 0);
        ObjectNode timeBoundsNode = histogramNode.putObject(EXTENDED_BOUNDS);
        timeBoundsNode.put(MIN, from);
        timeBoundsNode.put(MAX, to);

        return intervalNode.putObject(AGGS);
    }

    private static void applyDefaultFilters(HistogramQuery query, ArrayNode filters, AggregationType type) {
        if (query.searchTermId() != null && query.searchTermId().id() != null) {
            applyTermFilter("api-id", query.searchTermId().id(), filters);
        }

        if (type != AggregationType.DELTA) {
            applyTimeRangeFilter(query.timeRange(), filters);
        }
    }

    private static void applyExistsFilters(HistogramQuery query, ArrayNode filters) {
        // OR semantics: group all exists clauses under a bool.should with minimum_should_match=1
        List<String> fields = query.aggregations().stream().map(Aggregation::getField).toList();

        if (fields.isEmpty()) {
            return;
        }

        ArrayNode shouldNode = MAPPER.createArrayNode();

        for (String field : fields) {
            ObjectNode existsNode = MAPPER.createObjectNode();
            existsNode.putObject(EXISTS).put(FIELD, field);
            shouldNode.add(existsNode);
        }

        ObjectNode boolNode = MAPPER.createObjectNode();
        boolNode.set(SHOULD, shouldNode);
        boolNode.put(MINIMUM_SHOULD_MATCH, 1);

        ObjectNode wrapper = MAPPER.createObjectNode();
        wrapper.set(BOOL, boolNode);

        filters.add(wrapper);
    }

    private static void applyOptionalFilters(HistogramQuery query, ArrayNode filters) {
        if (query.terms() == null || query.terms().isEmpty()) {
            return;
        }

        Map<String, Set<String>> groupedTerms = new LinkedHashMap<>();

        query.terms().forEach(term -> groupedTerms.computeIfAbsent(term.key(), k -> new LinkedHashSet<>()).add(term.value()));

        groupedTerms.forEach((field, values) -> applyTermsFilter(field, values, filters));
    }

    private static void applyTermsFilter(String field, java.util.Collection<String> values, ArrayNode filters) {
        ObjectNode node = MAPPER.createObjectNode();
        node.set(TERMS, MAPPER.createObjectNode().set(field, MAPPER.valueToTree(values)));
        filters.add(node);
    }

    private static void applyTermFilter(String field, String value, ArrayNode filters) {
        ObjectNode node = MAPPER.createObjectNode();
        node.set(TERM, MAPPER.createObjectNode().put(field, value));
        filters.add(node);
    }

    private static void applyTimeRangeFilter(TimeRange timeRange, ArrayNode filters) {
        long from = timeRange.from().toEpochMilli();
        long to = timeRange.to().toEpochMilli();

        ObjectNode node = MAPPER.createObjectNode();
        ObjectNode rangeNode = node.putObject(RANGE).putObject(TIMESTAMP);
        rangeNode.put(GTE, from);
        rangeNode.put(LTE, to);
        filters.add(node);
    }

    private static void finalizeQuery(ArrayNode filters, ObjectNode root) {
        ObjectNode query = root.putObject(QUERY);
        ObjectNode bool = query.putObject(BOOL);
        bool.set(FILTER, filters);
    }
}
