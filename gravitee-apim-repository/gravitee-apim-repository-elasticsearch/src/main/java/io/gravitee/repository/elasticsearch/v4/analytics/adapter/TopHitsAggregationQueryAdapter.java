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
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MIN_DOC_COUNT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SCRIPT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SORT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SOURCES;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.TOP_METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.AGGS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.BOOL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.FILTER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.QUERY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SIZE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TIMESTAMP;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TRACK_TOTAL_HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BEFORE_START;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BY_DIMENSIONS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.DERIVATIVE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_IN_RANGE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_BUCKET_PREFIX;
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
 * - DELTA: end - start per composite bucket via two time windows, summed during response parsing.
 * - TREND: per-interval derivative with non-negative clamping; summed and aligned during response parsing.
 *
 * Strict rules:
 * - No pagination (composite size capped, handled elsewhere if needed).
 * - Exists filters are ANDed (one exists per requested metric).
 * - Root time range is applied only for VALUE and TREND (not for DELTA).
 * - Composite keys:
 *   - VALUE: gw-id, api-id, org-id, env-id
 *   - DELTA/TREND: VALUE keys + app-id, plan-id, topic
 */
public final class TopHitsAggregationQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TopHitsAggregationQueryAdapter() {}

    public static String adapt(HistogramQuery query) {
        Set<AggregationType> types = query.aggregations().stream().map(Aggregation::getType).collect(Collectors.toSet());

        if (types.size() != 1) {
            throw new IllegalArgumentException("Exactly one aggregate function should be specified");
        }

        AggregationType type = types.iterator().next();

        ObjectNode root = MAPPER.createObjectNode();
        root.put(SIZE, 0);
        root.put(TRACK_TOTAL_HITS, false);
        ObjectNode aggsRoot = root.putObject(AGGS);

        switch (type) {
            case VALUE -> buildValueAggs(query, aggsRoot);
            case DELTA -> buildDeltaAggs(query, aggsRoot);
            case TREND -> buildTrendAggs(query, aggsRoot);
        }

        ArrayNode filters = MAPPER.createArrayNode();
        applyDefaultFilters(query, filters, type);
        applyExistsFilters(query, filters);
        applyOptionalFilters(query, filters);
        finalizeQuery(filters, root);

        return root.toString();
    }

    // ---------------- VALUE ----------------

    private static void buildValueAggs(HistogramQuery query, ObjectNode aggsRoot) {
        ObjectNode byDimensions = aggsRoot.putObject(BY_DIMENSIONS);
        ObjectNode composite = byDimensions.putObject(COMPOSITE);
        composite.put(SIZE, 1000);
        ArrayNode sources = composite.putArray(SOURCES);
        addDefaultCompositeKeys(sources);

        ObjectNode perBucketAggs = byDimensions.putObject(AGGS);
        query
            .aggregations()
            .forEach(agg -> {
                String field = agg.getField();
                ObjectNode latest = perBucketAggs.putObject(LATEST_BUCKET_PREFIX + field);
                applyTopMetrics(latest, field);
            });
    }

    // ---------------- DELTA ----------------

    private static void buildDeltaAggs(HistogramQuery query, ObjectNode aggsRoot) {
        ObjectNode perBucketAggs = createCompositeWithExtras(aggsRoot, "app-id", "plan-id", "topic");

        long start = query.timeRange().from().toEpochMilli();
        long end = query.timeRange().to().toEpochMilli();

        // before_start_time: last doc strictly before 'start'
        ObjectNode beforeStart = perBucketAggs.putObject(BEFORE_START);
        ObjectNode beforeStartTs = beforeStart.putObject(FILTER).putObject(RANGE).putObject(TIMESTAMP);
        beforeStartTs.put(LT, start);
        ObjectNode beforeStartAggs = beforeStart.putObject(AGGS);

        // end_in_range: last doc in [start, end)
        ObjectNode endInRange = perBucketAggs.putObject(END_IN_RANGE);
        ObjectNode endInRangeTs = endInRange.putObject(FILTER).putObject(RANGE).putObject(TIMESTAMP);
        endInRangeTs.put(GTE, start);
        endInRangeTs.put(LT, end);
        ObjectNode endInRangeAggs = endInRange.putObject(AGGS);

        query
            .aggregations()
            .forEach(agg -> {
                String field = agg.getField();

                ObjectNode startAgg = beforeStartAggs.putObject(START_BUCKET_PREFIX + field);
                applyTopMetrics(startAgg, field);

                ObjectNode endAgg = endInRangeAggs.putObject(END_BUCKET_PREFIX + field);
                applyTopMetrics(endAgg, field);
            });
    }

    // ---------------- TREND ----------------

    private static void buildTrendAggs(HistogramQuery query, ObjectNode aggsRoot) {
        ObjectNode perBucketAggs = createCompositeWithExtras(aggsRoot, "app-id", "plan-id", "topic");
        ObjectNode perIntervalAggs = applyFixedIntervalHistogram(query, perBucketAggs);
        query.aggregations().forEach(agg -> addTrendSeries(perIntervalAggs, agg.getField()));
    }

    private static void addTrendSeries(ObjectNode perIntervalAggs, String field) {
        String maxName = MAX_PREFIX + field;
        String derivativeName = DERIVATIVE_PREFIX + field;
        String nonNegativeName = NON_NEGATIVE_PREFIX + field;

        perIntervalAggs.putObject(maxName).putObject(MAX).put(FIELD, field);

        ObjectNode derivative = perIntervalAggs.putObject(derivativeName).putObject(DERIVATIVE);
        derivative.put(BUCKETS_PATH, maxName);
        derivative.put(GAP_POLICY, INSERT_ZEROS);

        ObjectNode script = perIntervalAggs.putObject(nonNegativeName).putObject(BUCKET_SCRIPT);
        ObjectNode path = script.putObject(BUCKETS_PATH);
        path.put("d", derivativeName);
        script.put(SCRIPT, "params.d != null ? Math.max(params.d, 0) : 0");
    }

    // ---------------- Composite helpers ----------------

    private static ObjectNode createCompositeWithExtras(ObjectNode aggsRoot, String... extras) {
        ObjectNode byDimensions = aggsRoot.putObject(BY_DIMENSIONS);
        ObjectNode composite = byDimensions.putObject(COMPOSITE);
        composite.put(SIZE, 1000);
        ArrayNode sources = composite.putArray(SOURCES);

        addDefaultCompositeKeys(sources);
        if (extras != null) {
            Arrays.stream(extras).forEach(field -> addCompositeKey(sources, field));
        }

        return byDimensions.putObject(AGGS);
    }

    private static void addDefaultCompositeKeys(ArrayNode sources) {
        addCompositeKey(sources, "gw-id");
        addCompositeKey(sources, "api-id");
        addCompositeKey(sources, "org-id");
        addCompositeKey(sources, "env-id");
    }

    private static void addCompositeKey(ArrayNode sources, String fieldName) {
        ObjectNode sourceWrapper = MAPPER.createObjectNode();
        ObjectNode inner = MAPPER.createObjectNode();
        ObjectNode termsBody = MAPPER.createObjectNode();
        termsBody.put(FIELD, fieldName);
        inner.set(TERMS, termsBody);
        sourceWrapper.set(fieldName, inner);
        sources.add(sourceWrapper);
    }

    // ---------------- top_metrics and histogram ----------------

    private static void applyTopMetrics(ObjectNode container, String field) {
        ObjectNode tm = container.putObject(TOP_METRICS);
        tm.putObject(METRICS).put(FIELD, field);
        tm.putObject(SORT).put(TIMESTAMP, io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.DESC);
    }

    private static ObjectNode applyFixedIntervalHistogram(HistogramQuery query, ObjectNode aggsRoot) {
        TimeRange tr = query.timeRange();
        Duration iv = tr.interval().orElseThrow(() -> new IllegalArgumentException("Interval is required for TREND aggregations"));
        long from = tr.from().toEpochMilli();
        long to = tr.to().toEpochMilli();

        ObjectNode perInterval = aggsRoot.putObject(PER_INTERVAL);
        ObjectNode dh = perInterval.putObject(DATE_HISTOGRAM);
        dh.put(FIELD, TIMESTAMP);
        dh.put(FIXED_INTERVAL, iv.toMillis() + MILLISECONDS);
        dh.put(MIN_DOC_COUNT, 0);
        ObjectNode bounds = dh.putObject(EXTENDED_BOUNDS);
        bounds.put(MIN, from);
        bounds.put(MAX, to);
        return perInterval.putObject(AGGS);
    }

    // ---------------- Filters ----------------

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

        ArrayNode should = MAPPER.createArrayNode();
        for (String field : fields) {
            ObjectNode existsNode = MAPPER.createObjectNode();
            existsNode.putObject(EXISTS).put(FIELD, field);
            should.add(existsNode);
        }

        ObjectNode boolNode = MAPPER.createObjectNode();
        boolNode.set("should", should);
        boolNode.put("minimum_should_match", 1);

        ObjectNode wrapper = MAPPER.createObjectNode();
        wrapper.set("bool", boolNode);

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
        ObjectNode ts = node.putObject(RANGE).putObject(TIMESTAMP);
        ts.put(GTE, from);
        ts.put(LTE, to);
        filters.add(node);
    }

    private static void finalizeQuery(ArrayNode filters, ObjectNode root) {
        ObjectNode query = root.putObject(QUERY);
        ObjectNode bool = query.putObject(BOOL);
        bool.set(FILTER, filters);
    }
}
