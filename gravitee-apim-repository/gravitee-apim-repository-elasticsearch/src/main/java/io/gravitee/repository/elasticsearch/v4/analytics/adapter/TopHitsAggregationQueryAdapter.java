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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.repository.log.v4.model.analytics.Aggregation;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates Elasticsearch queries to retrieve values (e.g. with top_hits)
 * for specified metrics, supporting time range and dimension filters.
 * Upcoming:
 * - Terms filtering: enable filters for application, topic, gateway, etc.
 * - query_string: support native Elasticsearch queries
 */
public class TopHitsAggregationQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String TOP_HITS = "top_hits";
    public static final String SORT = "sort";
    public static final String SOURCE = "_source";
    public static final String TIMESTAMP = "@timestamp";
    public static final String AGGS = "aggs";
    public static final String FILTER = "filter";
    public static final String ORDER = "order";
    public static final String SIZE = "size";
    public static final String FIELD = "field";
    public static final String EXISTS = "exists";
    public static final String ASC = "asc";
    public static final String DESC = "desc";
    public static final String TERM = "term";
    public static final String FIXED_INTERVAL = "fixed_interval";
    public static final String DATE_HISTOGRAM = "date_histogram";
    public static final String START_VALUE = "start_value";
    public static final String END_VALUE = "end_value";
    public static final String PER_INTERVAL = "per_interval";
    public static final String MIN_DOC_COUNT = "min_doc_count";
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String EXTENDED_BOUNDS = "extended_bounds";
    public static final String HITS = "hits";
    public static final String DELTA_BUCKET_SUFFIX = "_delta";
    public static final String LATEST = "_latest";

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
        ObjectNode aggregations = root.putObject(AGGS);
        // Set aggregations
        switch (aggregationType) {
            case VALUE:
                applyValueAggregations(query, aggregations);
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
        // Finalise query
        finaliseQuery(filters, root);

        return root.toString();
    }

    private static void applyValueAggregations(HistogramQuery query, ObjectNode aggregations) {
        query
            .aggregations()
            .forEach(agg -> {
                String field = agg.getField();
                String name = field + LATEST;
                // Create a filter aggregation for "exists"
                ObjectNode filterAgg = applyExistsFilter(aggregations, name, field);
                // Nest top_hits aggregation
                ObjectNode aggsNode = filterAgg.putObject(AGGS);
                applyTopHitsAggregation(aggsNode, TOP_HITS, DESC, field);
            });
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
            .map(duration -> duration.toMillis() + "ms")
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
        queryNode.set("bool", boolNode);

        root.set("query", queryNode);
    }
}
