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

import static io.gravitee.repository.log.v4.model.analytics.AggregationType.VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.repository.analytics.query.stats.EventAnalyticsQuery;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import jakarta.validation.constraints.NotNull;

/**
 * Generates Elasticsearch queries to retrieve metric values (e.g. with top_hits)
 * for specified fields, supporting time range and dimension filters.
 *
 * Upcoming:
 * - setDeltaAggregations(): support DELTA aggregation
 * - Terms filtering: enable filters for application, topic, gateway, etc.
 * - query_string: support native Elasticsearch queries
 */
public class TopHitsAggregateQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TopHitsAggregateQueryAdapter() {}

    public static String adapt(@NotNull EventAnalyticsQuery query) {
        // Create root node
        ObjectNode root = MAPPER.createObjectNode();
        root.put("size", 0);
        // Set aggregations e.g. `VALUE`, `DELTA`
        setValueAggregations(query, root);
        ArrayNode filters = MAPPER.createArrayNode();
        // Apply mandatory filters like, api-id, time-range
        applyDefaultFilters(query, filters);
        // Finalise query
        finaliseQuery(filters, root);

        return root.toString();
    }

    private static void setValueAggregations(EventAnalyticsQuery query, ObjectNode root) {
        ObjectNode aggregations = root.putObject("aggs");

        query
            .aggregations()
            .stream()
            .filter(agg -> VALUE.equals(agg.getType()))
            .forEach(agg -> {
                String field = agg.getField();
                String name = field + "_latest";
                // Create a filter aggregation for "exists"
                ObjectNode filterAgg = aggregations.putObject(name);
                ObjectNode filterNode = filterAgg.putObject("filter");
                filterNode.putObject("exists").put("field", field);
                // Nest top_hits aggregation
                ObjectNode aggsNode = filterAgg.putObject("aggs");
                ObjectNode topHitsAggNode = aggsNode.putObject("top_hits");
                ObjectNode topHitsNode = topHitsAggNode.putObject("top_hits");
                topHitsNode.put("size", 1);
                // Set sort node
                ArrayNode sortArray = topHitsNode.putArray("sort");
                ObjectNode sortObj = MAPPER.createObjectNode();
                sortObj.set("@timestamp", MAPPER.createObjectNode().put("order", "desc"));
                sortArray.add(sortObj);
                // Set source fields
                ArrayNode sourceArray = topHitsNode.putArray("_source");
                sourceArray.add(field);
            });
    }

    private static void finaliseQuery(ArrayNode filters, ObjectNode root) {
        ObjectNode boolNode = MAPPER.createObjectNode();
        boolNode.set("filter", filters);

        ObjectNode queryNode = MAPPER.createObjectNode();
        queryNode.set("bool", boolNode);

        root.set("query", queryNode);
    }

    private static void applyTimeRangeFilter(@NotNull TimeRange timeRange, ArrayNode filters) {
        filters.add(TimeRangeAdapter.createRangeFilterNode(timeRange));
    }

    private static void applyDefaultFilters(EventAnalyticsQuery query, ArrayNode filters) {
        applyAPIFilter("term", "api-id", query.apiId(), filters);
        applyTimeRangeFilter(query.timeRange(), filters);
    }

    private static void applyAPIFilter(String term, String field, String value, ArrayNode filters) {
        ObjectNode apiFilter = MAPPER.createObjectNode();
        apiFilter.set(term, MAPPER.createObjectNode().put(field, value));
        filters.add(apiFilter);
    }
}
