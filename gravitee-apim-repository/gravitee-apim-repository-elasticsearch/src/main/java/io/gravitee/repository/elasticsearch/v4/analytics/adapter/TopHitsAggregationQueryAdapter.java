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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.COMPOSITE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.DATE_HISTOGRAM;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.EXTENDED_BOUNDS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.FIXED_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MAX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MIN;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MIN_DOC_COUNT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SORT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SOURCES;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.TOP_HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.TOP_METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.AGGS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.BOOL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.FILTER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.QUERY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SIZE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SOURCE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TIMESTAMP;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TRACK_TOTAL_HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BY_DIMENSIONS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.DELTA_BUCKET_SUFFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_VALUE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.MILLISECONDS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.PER_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.START_VALUE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.EXISTS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.TERM;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.ASC;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.DESC;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.ORDER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens.FIELD;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens.TERMS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.repository.log.v4.model.analytics.Aggregation;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TopHitsAggregationQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TopHitsAggregationQueryAdapter() {}

    public static String adapt(HistogramQuery query) {
        Set<AggregationType> aggregationTypes = query.aggregations().stream().map(Aggregation::getType).collect(Collectors.toSet());

        if (aggregationTypes.size() != 1) {
            throw new IllegalArgumentException("Exactly one aggregate function should be specified");
        }

        AggregationType aggregationType = aggregationTypes.iterator().next();
        // Create root node
        ObjectNode root = MAPPER.createObjectNode();
        root.put(SIZE, 0);
        root.put(TRACK_TOTAL_HITS, false);
        ObjectNode aggregations = root.putObject(AGGS);
        // Set aggregations
        switch (aggregationType) {
            case VALUE:
                applyLatestValueByDimensionsAggregations(query, aggregations);
                break;
            case DELTA:
                applyDeltaAggregations(query, aggregations);
                break;
            case TREND:
                ObjectNode node = applyFixedIntervalAggregation(query, aggregations);
                applyDeltaAggregations(query, node);
                break;
        }
        // Add filters node
        ArrayNode filters = MAPPER.createArrayNode();
        // Apply mandatory filters like, api-id, time-range
        applyDefaultFilters(query, filters);
        // Apply optional filters like, application, topic, gateway, etc.
        applyOptionalFilters(query, filters);
        // Finalise query
        finaliseQuery(filters, root);

        return root.toString();
    }

    public static void applyLatestValueByDimensionsAggregations(HistogramQuery query, ObjectNode aggregations) {
        // aggs.by_dimensions.composite
        ObjectNode byDimensions = aggregations.putObject(BY_DIMENSIONS);
        ObjectNode composite = byDimensions.putObject(COMPOSITE);
        composite.put(SIZE, 1000);

        ArrayNode sources = composite.putArray(SOURCES);
        addCompositeTermsSource(sources, "gw-id");
        addCompositeTermsSource(sources, "app-id");
        addCompositeTermsSource(sources, "plan-id");
        addCompositeTermsSource(sources, "org-id");
        addCompositeTermsSource(sources, "env-id");

        // Sub-aggregations: one top_metrics per requested metric field, named "latest_" + field
        ObjectNode dimensionAggs = byDimensions.putObject(AGGS);
        query.aggregations().forEach(agg -> addLatestTopMetricsAgg(dimensionAggs, agg.getField()));
    }

    // Helper to append a composite source {"<fieldName>": {"terms": {"field": "<fieldName>"}}}
    private static void addCompositeTermsSource(ArrayNode sources, String fieldName) {
        ObjectNode oneSource = MAPPER.createObjectNode();
        ObjectNode wrapper = MAPPER.createObjectNode();
        ObjectNode body = MAPPER.createObjectNode();
        body.put(FIELD, fieldName);
        wrapper.set(TERMS, body);
        oneSource.set(fieldName, wrapper);
        sources.add(oneSource);
    }

    // Helper to append a "latest_<field>" top_metrics sub-aggregation with sort by @timestamp desc
    private static void addLatestTopMetricsAgg(ObjectNode aggsNode, String field) {
        String aggName = LATEST_PREFIX + field;
        ObjectNode latest = aggsNode.putObject(aggName);
        ObjectNode topMetrics = latest.putObject(TOP_METRICS);
        ObjectNode metricsNode = MAPPER.createObjectNode();
        metricsNode.put(FIELD, field);
        topMetrics.set(METRICS, metricsNode);
        ObjectNode sortNode = MAPPER.createObjectNode();
        sortNode.put(TIMESTAMP, DESC);
        topMetrics.set(SORT, sortNode);
    }

    private static void applyDeltaAggregations(HistogramQuery query, ObjectNode aggregations) {
        query
            .aggregations()
            .forEach(agg -> {
                String field = agg.getField();
                String name = field + DELTA_BUCKET_SUFFIX;
                ObjectNode filterAgg = applyExistsFilter(aggregations, name, field);
                ObjectNode aggsNode = filterAgg.putObject(AGGS);
                // Earliest value via top_hits sorted ascending (min)
                applyTopHitsAggregation(aggsNode, START_VALUE, ASC, field);
                // Latest value via top_hits sorted descending (max)
                applyTopHitsAggregation(aggsNode, END_VALUE, DESC, field);
            });
    }

    private static ObjectNode applyFixedIntervalAggregation(HistogramQuery query, ObjectNode aggregations) {
        String fixedInterval = query
            .timeRange()
            .interval()
            .map(duration -> duration.toMillis() + MILLISECONDS)
            .orElseThrow(() -> new IllegalArgumentException("Time interval is mandatory to calculate the trend analytics"));

        ObjectNode intervalAgg = aggregations.putObject(PER_INTERVAL);
        ObjectNode histogram = intervalAgg.putObject(DATE_HISTOGRAM);
        histogram.put(FIELD, TIMESTAMP);
        histogram.put(FIXED_INTERVAL, fixedInterval);
        histogram.put(MIN_DOC_COUNT, 0);
        ObjectNode extendedBounds = MAPPER.createObjectNode();
        extendedBounds.put(MIN, query.timeRange().from().toEpochMilli());
        extendedBounds.put(MAX, query.timeRange().to().toEpochMilli());
        histogram.set(EXTENDED_BOUNDS, extendedBounds);

        return intervalAgg.putObject(AGGS);
    }

    private static void applyTopHitsAggregation(ObjectNode aggsNode, String aggName, String order, String field) {
        ObjectNode aggNode = aggsNode.putObject(aggName);
        ObjectNode hitsAgg = aggNode.putObject(TOP_HITS);
        hitsAgg.put(SIZE, 1);
        ArrayNode sortNode = hitsAgg.putArray(SORT);
        ObjectNode sortObj = MAPPER.createObjectNode();
        sortObj.set(TIMESTAMP, MAPPER.createObjectNode().put(ORDER, order));
        sortNode.add(sortObj);
        ArrayNode sourceNode = hitsAgg.putArray(SOURCE);
        sourceNode.add(field);
    }

    private static ObjectNode applyExistsFilter(ObjectNode aggregations, String name, String field) {
        ObjectNode filterAgg = aggregations.putObject(name);
        ObjectNode filterNode = filterAgg.putObject(FILTER);
        filterNode.putObject(EXISTS).put(FIELD, field);

        return filterAgg;
    }

    private static void applyDefaultFilters(HistogramQuery query, ArrayNode filters) {
        applyTermFilter("api-id", query.searchTermId().id(), filters);
        applyTimeRangeFilter(query.timeRange(), filters);
    }

    private static void applyOptionalFilters(HistogramQuery query, ArrayNode filters) {
        if (query.terms() == null || query.terms().isEmpty()) {
            return;
        }

        Map<String, Set<String>> groupedTerms = new LinkedHashMap<>();
        query.terms().forEach(term -> groupedTerms.computeIfAbsent(term.key(), k -> new LinkedHashSet<>()).add(term.value()));

        groupedTerms.forEach((field, values) -> applyTermsFilter(field, values, filters));
    }

    private static void applyTermsFilter(String field, Set<String> values, ArrayNode filters) {
        ObjectNode termsNode = MAPPER.createObjectNode();
        ArrayNode valuesArray = MAPPER.createArrayNode();
        values.forEach(valuesArray::add);

        ObjectNode fieldNode = MAPPER.createObjectNode();
        fieldNode.set(field, valuesArray);

        termsNode.set(TERMS, fieldNode);
        filters.add(termsNode);
    }

    private static void applyTermFilter(String field, String value, ArrayNode filters) {
        ObjectNode apiFilter = MAPPER.createObjectNode();
        apiFilter.set(TERM, MAPPER.createObjectNode().put(field, value));
        filters.add(apiFilter);
    }

    private static void applyTimeRangeFilter(@NotNull TimeRange timeRange, ArrayNode filters) {
        filters.add(TimeRangeAdapter.createRangeFilterNode(timeRange));
    }

    private static void finaliseQuery(ArrayNode filters, ObjectNode root) {
        ObjectNode boolNode = MAPPER.createObjectNode();
        boolNode.set(FILTER, filters);

        ObjectNode queryNode = MAPPER.createObjectNode();
        queryNode.set(BOOL, boolNode);

        root.set(QUERY, queryNode);
    }
}
