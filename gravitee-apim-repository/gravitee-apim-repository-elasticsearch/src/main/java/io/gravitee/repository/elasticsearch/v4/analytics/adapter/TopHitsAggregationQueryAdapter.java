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

import static io.gravitee.repository.log.v4.model.analytics.AggregationType.DELTA;
import static io.gravitee.repository.log.v4.model.analytics.AggregationType.VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.repository.analytics.query.events.EventAnalyticsQuery;
import io.gravitee.repository.log.v4.model.analytics.Aggregation;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
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
    public static final String END_VALUE = "end_value";
    public static final String START_VALUE = "start_value";
    public static final String SORT = "sort";
    public static final String SOURCE = "_source";
    public static final String TIMESTAMP = "@timestamp";
    public static final String AGGS = "aggs";
    public static final String DELTA_BUCKET_SUFFIX = "_delta";
    public static final String LATEST_BUCKET_SUFFIX = "_latest";
    public static final String FILTER = "filter";

    private TopHitsAggregationQueryAdapter() {}

    public static String adapt(@NotNull EventAnalyticsQuery query) {
        // Create root node
        ObjectNode root = MAPPER.createObjectNode();
        root.put("size", 0);
        // Set aggregations e.g. `VALUE`, `DELTA`
        AggregationType aggregationType = identifyAggregateFunction(query);

        switch (aggregationType) {
            case VALUE -> setValueAggregations(query, root);
            case DELTA -> setDeltaAggregations(query, root);
            default -> throw new IllegalArgumentException("Unsupported aggregation type: " + aggregationType);
        }
        // Add filters node
        ArrayNode filters = MAPPER.createArrayNode();
        // Apply mandatory filters like, api-id, time-range
        applyDefaultFilters(query, filters);
        // Finalise query
        finaliseQuery(filters, root);

        return root.toString();
    }

    private static AggregationType identifyAggregateFunction(EventAnalyticsQuery query) {
        Set<AggregationType> aggregations = query.aggregations().stream().map(Aggregation::getType).collect(Collectors.toSet());
        throwIfMultipleAggregateFunctions(aggregations);

        return aggregations.iterator().next();
    }

    private static void throwIfMultipleAggregateFunctions(Set<AggregationType> types) {
        if (types.isEmpty()) {
            throw new IllegalArgumentException("At least one aggregate function must be applied to query the data");
        } else if (types.size() > 1) {
            throw new IllegalArgumentException("Only one aggregate function must be applied to query the data");
        }
    }

    private static void setValueAggregations(EventAnalyticsQuery query, ObjectNode root) {
        ObjectNode aggregations = root.putObject(AGGS);

        query
            .aggregations()
            .stream()
            .filter(agg -> VALUE.equals(agg.getType()))
            .forEach(agg -> {
                String field = agg.getField();
                String name = field + LATEST_BUCKET_SUFFIX;
                // Create a filter aggregation for "exists"
                ObjectNode filterAgg = aggregations.putObject(name);
                ObjectNode filterNode = filterAgg.putObject(FILTER);
                filterNode.putObject("exists").put("field", field);
                // Nest top_hits aggregation
                ObjectNode aggsNode = filterAgg.putObject(AGGS);
                ObjectNode topHitsAggNode = aggsNode.putObject(TOP_HITS);
                ObjectNode topHitsNode = topHitsAggNode.putObject(TOP_HITS);
                topHitsNode.put("size", 1);
                // Set sort node
                ArrayNode sortArray = topHitsNode.putArray(SORT);
                ObjectNode sortObj = MAPPER.createObjectNode();
                sortObj.set(TIMESTAMP, MAPPER.createObjectNode().put("order", "desc"));
                sortArray.add(sortObj);
                // Set source fields
                ArrayNode sourceArray = topHitsNode.putArray(SOURCE);
                sourceArray.add(field);
            });
    }

    private static void setDeltaAggregations(EventAnalyticsQuery query, ObjectNode root) {
        ObjectNode aggregations = root.putObject(AGGS);

        query
            .aggregations()
            .stream()
            .filter(agg -> DELTA.equals(agg.getType()))
            .forEach(agg -> {
                String field = agg.getField();
                String name = field + DELTA_BUCKET_SUFFIX;
                ObjectNode filterAgg = aggregations.putObject(name);
                ObjectNode filterNode = filterAgg.putObject(FILTER);
                filterNode.putObject("exists").put("field", field);

                ObjectNode aggsNode = filterAgg.putObject(AGGS);

                // Earliest value via top_hits sorted ascending (min)
                ObjectNode minHitsAgg = aggsNode.putObject(START_VALUE);
                ObjectNode minHits = minHitsAgg.putObject(TOP_HITS);
                minHits.put("size", 1);
                ArrayNode minSort = minHits.putArray(SORT);
                ObjectNode minSortObj = MAPPER.createObjectNode();
                minSortObj.set(TIMESTAMP, MAPPER.createObjectNode().put("order", "asc"));
                minSort.add(minSortObj);
                ArrayNode minSource = minHits.putArray(SOURCE);
                minSource.add(field);

                // Latest value via top_hits sorted descending (max)
                ObjectNode maxHitsAgg = aggsNode.putObject(END_VALUE);
                ObjectNode maxHits = maxHitsAgg.putObject(TOP_HITS);
                maxHits.put("size", 1);
                ArrayNode maxSort = maxHits.putArray(SORT);
                ObjectNode maxSortObj = MAPPER.createObjectNode();
                maxSortObj.set(TIMESTAMP, MAPPER.createObjectNode().put("order", "desc"));
                maxSort.add(maxSortObj);
                ArrayNode maxSource = maxHits.putArray(SOURCE);
                maxSource.add(field);
            });
    }

    private static void finaliseQuery(ArrayNode filters, ObjectNode root) {
        ObjectNode boolNode = MAPPER.createObjectNode();
        boolNode.set(FILTER, filters);

        ObjectNode queryNode = MAPPER.createObjectNode();
        queryNode.set("bool", boolNode);

        root.set("query", queryNode);
    }

    private static void applyTimeRangeFilter(@NotNull TimeRange timeRange, ArrayNode filters) {
        filters.add(TimeRangeAdapter.createRangeFilterNode(timeRange));
    }

    private static void applyDefaultFilters(EventAnalyticsQuery query, ArrayNode filters) {
        applyFilter("term", "api-id", query.apiId(), filters);
        applyTimeRangeFilter(query.timeRange(), filters);
    }

    private static void applyFilter(String term, String field, String value, ArrayNode filters) {
        ObjectNode apiFilter = MAPPER.createObjectNode();
        apiFilter.set(term, MAPPER.createObjectNode().put(field, value));
        filters.add(apiFilter);
    }
}
