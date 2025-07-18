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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.StatsQuery;
import java.util.Optional;

public class StatsQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private String field;

    public String adapt(StatsQuery query) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("size", 0);

        // Query
        ObjectNode queryNode = createQueryNode(query);
        root.set("query", queryNode);

        // Aggregations
        ObjectNode aggs = createAggregationsNode(query);
        root.set("aggregations", aggs);

        // Set field for use in adaptResponse
        this.field = query.field();

        return root.toString();
    }

    private ObjectNode createQueryNode(StatsQuery query) {
        var filterArray = MAPPER.createArrayNode();

        // Root (always "api-id" field)
        ObjectNode termNode = MAPPER.createObjectNode();
        termNode.set("term", MAPPER.createObjectNode().put("api-id", query.apiId()));
        filterArray.add(termNode);

        // Time range
        ObjectNode rangeNode = MAPPER.createObjectNode();
        ObjectNode tsRange = MAPPER.createObjectNode();
        tsRange.put("from", query.timeRange().from());
        tsRange.put("to", query.timeRange().to());
        tsRange.put("include_lower", true);
        tsRange.put("include_upper", true);
        rangeNode.set("@timestamp", tsRange);
        filterArray.add(MAPPER.createObjectNode().set("range", rangeNode));

        ObjectNode bool = MAPPER.createObjectNode();
        bool.set("filter", filterArray);

        ObjectNode queryNode = MAPPER.createObjectNode();
        queryNode.set("bool", bool);
        return queryNode;
    }

    private ObjectNode createAggregationsNode(StatsQuery query) {
        ObjectNode aggs = MAPPER.createObjectNode();
        ObjectNode byField = MAPPER.createObjectNode();

        ObjectNode statsAgg = MAPPER.createObjectNode();
        statsAgg.put("field", query.field());
        byField.set("stats", statsAgg);

        aggs.set("by_" + query.field(), byField);
        return aggs;
    }

    public Optional<StatsAggregate> adaptResponse(SearchResponse response) {
        if (response == null || response.getAggregations() == null) {
            return Optional.empty();
        }

        String aggName = "by_" + this.field;
        var agg = response.getAggregations().get(aggName);

        if (agg == null) {
            return Optional.empty();
        }

        if (agg.getCount() == null || agg.getSum() == null || agg.getAvg() == null || agg.getMin() == null || agg.getMax() == null) {
            return Optional.empty();
        }

        long count = agg.getCount().longValue();
        float sum = agg.getSum();
        float avg = agg.getAvg();
        float min = agg.getMin();
        float max = agg.getMax();

        return Optional.of(new StatsAggregate(this.field, count, sum, avg, min, max));
    }
}
