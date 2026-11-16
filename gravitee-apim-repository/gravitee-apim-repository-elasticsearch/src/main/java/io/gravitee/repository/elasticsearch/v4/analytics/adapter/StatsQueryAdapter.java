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
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.elasticsearch.v4.shared.ConnectionPhaseClause;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.StatsQuery;
import java.util.Map;
import java.util.Optional;

public class StatsQueryAdapter {

    private static final String COUNTED_AGG = "counted";
    private static final String MEASURED_AGG = "measured";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private String field;
    private long seconds; // duration in seconds

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

        // Calculate seconds for time range
        this.seconds = query.timeRange().seconds();

        return root.toString();
    }

    private ObjectNode createQueryNode(StatsQuery query) {
        var filterArray = MAPPER.createArrayNode();

        // Query string support (as in stats.ftl)
        query
            .query()
            .ifPresent(q -> {
                if (!q.trim().isEmpty()) {
                    ObjectNode queryStringNode = MAPPER.createObjectNode();
                    ObjectNode queryString = MAPPER.createObjectNode();
                    queryString.put("query", q);
                    queryStringNode.set("query_string", queryString);
                    filterArray.add(queryStringNode);
                }
            });

        var searchTermId = query.searchTermId();
        var shouldArray = MAPPER.createArrayNode();
        shouldArray.add(
            MAPPER.createObjectNode().set("term", MAPPER.createObjectNode().put(searchTermId.searchTerm().getField(), searchTermId.id()))
        );
        shouldArray.add(MAPPER.createObjectNode().set("term", MAPPER.createObjectNode().put("api", searchTermId.id())));
        filterArray.add(MAPPER.createObjectNode().set("bool", MAPPER.createObjectNode().set("should", shouldArray)));

        // Time range
        filterArray.add(TimeRangeAdapter.toRangeNode(query.timeRange()));

        ObjectNode bool = MAPPER.createObjectNode();
        bool.set("filter", filterArray);

        ObjectNode queryNode = MAPPER.createObjectNode();
        queryNode.set("bool", bool);
        return queryNode;
    }

    /**
     * The count reads every connection as it opens, leaving its end out. The values are only known once a connection
     * has ended: they leave the opening out, so the average is that of the connections measured, not the sum over the
     * count.
     */
    private ObjectNode createAggregationsNode(StatsQuery query) {
        ObjectNode aggs = MAPPER.createObjectNode();
        aggs.set(COUNTED_AGG, filtered(ConnectionPhaseClause.notEnd().mapTo(Map.class), statsOf(query.field())));
        aggs.set(MEASURED_AGG, filtered(ConnectionPhaseClause.notOpening().mapTo(Map.class), statsOf(query.field())));
        return aggs;
    }

    private static ObjectNode filtered(Map<?, ?> filter, ObjectNode aggregations) {
        ObjectNode node = MAPPER.createObjectNode();
        node.set("filter", MAPPER.valueToTree(filter));
        node.set("aggregations", aggregations);
        return node;
    }

    private static ObjectNode statsOf(String field) {
        ObjectNode byField = MAPPER.createObjectNode();
        byField.set("stats", MAPPER.createObjectNode().put("field", field));
        return MAPPER.createObjectNode().set("by_" + field, byField);
    }

    public Optional<StatsAggregate> adaptResponse(SearchResponse response) {
        if (response == null || response.getAggregations() == null) {
            return Optional.empty();
        }

        String aggName = "by_" + this.field;
        var agg = nested(response, COUNTED_AGG, aggName);
        var measured = nested(response, MEASURED_AGG, aggName);

        if (agg == null) {
            return Optional.empty();
        }

        if (
            agg.getCount() == null ||
            measured == null ||
            measured.getSum() == null ||
            measured.getAvg() == null ||
            measured.getMin() == null ||
            measured.getMax() == null
        ) {
            return Optional.empty();
        }

        float count = agg.getCount().longValue();

        // StatsAggregate is part of the repository API and still exposes floats; narrowing here
        // keeps this adapter unchanged rather than rippling a type change through its consumers.
        float sum = measured.getSum().floatValue();
        float avg = measured.getAvg().floatValue();
        float min = measured.getMin().floatValue();
        float max = measured.getMax().floatValue();

        float rps = count / this.seconds;
        float rpm = rps * 60L;
        float rph = rpm * 60L;

        return Optional.of(new StatsAggregate(this.field, count, sum, avg, min, max, rps, rpm, rph));
    }

    private static Aggregation nested(SearchResponse response, String filterName, String aggName) {
        var filter = response.getAggregations().get(filterName);
        return filter == null ? null : filter.getAggregations().get(aggName);
    }
}
