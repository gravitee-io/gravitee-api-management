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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.DATE_HISTOGRAM;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.EXTENDED_BOUNDS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.FIXED_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MAX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MIN;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MIN_DOC_COUNT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.ORDER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SORT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SUM;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.TOP_HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.AGGS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.BOOL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.FILTER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.QUERY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SIZE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SOURCE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TIMESTAMP;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TRACK_TOTAL_HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.FILTERED_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.MILLISECONDS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.PER_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.TOTAL_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.GTE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.LTE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.RANGE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.TERM;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.DESC;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for constructing Elasticsearch queries based on provided histogram queries.
 * This adapter is designed to convert a {@link HistogramQuery} into a JSON Elasticsearch query string.
 * It supports various aggregation types such as VALUE, DELTA, TREND, and TREND_RATE.
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
            case VALUE -> applyValueAggregations(query, aggregationsNode);
            case DELTA -> applyDeltaAggregations(query, aggregationsNode);
            case TREND, TREND_RATE -> applyTrendAggregations(query, aggregationsNode);
        }

        ArrayNode filters = MAPPER.createArrayNode();
        applyOptionalFilters(query, filters);
        applySearchTermId(query, filters);
        applyTimeRangeFilter(query.timeRange(), filters);

        finalizeQuery(filters, root);

        return root.toString();
    }

    public static String getDocType(String field) {
        Map<String, String> docTypes = new LinkedHashMap<>();
        docTypes.put("downstream-active-connections", "api");
        docTypes.put("upstream-active-connections", "api");
        docTypes.put("downstream-authentication-failures-count-increment", "api");

        docTypes.put("upstream-authenticated-connections", "application");
        docTypes.put("downstream-authenticated-connections", "application");
        docTypes.put("downstream-authentication-successes-count-increment", "application");
        docTypes.put("upstream-authentication-successes-count-increment", "application");
        docTypes.put("upstream-authentication-failures-count-increment", "application");

        docTypes.put("downstream-publish-messages-count-increment", "topic");
        docTypes.put("upstream-publish-messages-count-increment", "topic");
        docTypes.put("downstream-publish-message-bytes-increment", "topic");
        docTypes.put("upstream-publish-message-bytes-increment", "topic");
        docTypes.put("upstream-subscribe-messages-count-increment", "topic");
        docTypes.put("downstream-subscribe-messages-count-increment", "topic");
        docTypes.put("upstream-subscribe-message-bytes-increment", "topic");
        docTypes.put("downstream-subscribe-message-bytes-increment", "topic");

        return docTypes.get(field);
    }

    private static void applyDeltaAggregations(HistogramQuery query, ObjectNode aggregationsNode) {
        query
            .aggregations()
            .forEach(agg -> {
                String field = agg.getField();
                var filteredNode = aggregationsNode.putObject(FILTERED_PREFIX + field);
                filteredNode.putObject(FILTER).putObject(TERM).put("doc-type", getDocType(field));
                var filteredAgg = filteredNode.putObject(AGGS);
                filteredAgg.putObject(TOTAL_PREFIX + field).putObject(SUM).put(FIELD, field);
            });
    }

    private static void applyValueAggregations(HistogramQuery query, ObjectNode aggregationsNode) {
        query
            .aggregations()
            .forEach(agg -> {
                String field = agg.getField();
                var filteredNode = aggregationsNode.putObject(FILTERED_PREFIX + field);
                filteredNode.putObject(FILTER).putObject(TERM).put("doc-type", getDocType(field));
                var filteredAgg = filteredNode.putObject(AGGS);

                var latestTopHitts = filteredAgg.putObject(LATEST_PREFIX + field).putObject(TOP_HITS);
                var sort = latestTopHitts.putArray(SORT);
                sort.addObject().putObject(TIMESTAMP).put(ORDER, DESC);

                latestTopHitts.put(SIZE, 1);
                latestTopHitts.putObject(SOURCE).putArray("includes").add(field);
            });
    }

    private static void applyTrendAggregations(HistogramQuery query, ObjectNode aggregationsNode) {
        var histogramAgg = applyFixedIntervalAggregations(query, aggregationsNode);
        query
            .aggregations()
            .forEach(agg -> {
                String field = agg.getField();
                histogramAgg.putObject(TOTAL_PREFIX + field).putObject(SUM).put(FIELD, field);
            });
    }

    private static ObjectNode applyFixedIntervalAggregations(HistogramQuery query, ObjectNode aggregationsNode) {
        TimeRange timeRange = query.timeRange();
        Duration interval = timeRange
            .interval()
            .orElseThrow(() -> new IllegalArgumentException("Interval is required for TREND aggregations"));
        long from = timeRange.from().toEpochMilli();
        long to = timeRange.to().toEpochMilli();
        long intervalMillis = interval.toMillis();

        ObjectNode intervalNode = aggregationsNode.putObject(PER_INTERVAL);
        ObjectNode histogramNode = intervalNode.putObject(DATE_HISTOGRAM);
        histogramNode.put(FIELD, TIMESTAMP);
        histogramNode.put(FIXED_INTERVAL, intervalMillis + MILLISECONDS);
        histogramNode.put(MIN_DOC_COUNT, 0);
        ObjectNode timeBoundsNode = histogramNode.putObject(EXTENDED_BOUNDS);
        timeBoundsNode.put(MIN, from);
        timeBoundsNode.put(MAX, to);

        return intervalNode.putObject(AGGS);
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

    private static void applySearchTermId(HistogramQuery query, ArrayNode filters) {
        if (query.searchTermId() != null && query.searchTermId().id() != null) {
            applyTermFilter("api-id", query.searchTermId().id(), filters);
        }
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
