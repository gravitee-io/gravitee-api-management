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
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GroupByQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> ENTRYPOINT_IDS = List.of("http-post", "http-get", "http-proxy");

    private String aggName;
    private String fieldName;

    public String adapt(GroupByQuery query) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("size", 0);

        // Query
        ObjectNode queryNode = createQueryNode(query);
        root.set("query", queryNode);

        // Aggregations
        ObjectNode aggs = createAggregationsNode(query);
        root.set("aggregations", aggs);

        // Set instance fields for later use in adaptResponse
        if (query.groups() != null && !query.groups().isEmpty()) {
            this.aggName = "by_" + query.field() + "_range";
        } else {
            this.aggName = "by_" + query.field();
        }
        this.fieldName = query.field();

        return root.toString();
    }

    private ObjectNode createQueryNode(GroupByQuery query) {
        var filterArray = MAPPER.createArrayNode();

        // Terms
        ObjectNode boolShould = MAPPER.createObjectNode();
        var shouldArray = MAPPER.createArrayNode();
        ObjectNode termsNode = MAPPER.createObjectNode();
        termsNode.set("terms", MAPPER.createObjectNode().set("entrypoint-id", MAPPER.valueToTree(ENTRYPOINT_IDS)));
        shouldArray.add(termsNode);
        boolShould.set("should", shouldArray);
        filterArray.add(MAPPER.createObjectNode().set("bool", boolShould));

        ObjectNode termNode = MAPPER.createObjectNode();
        termNode.set("term", MAPPER.createObjectNode().put("api-id", query.apiId()));
        filterArray.add(termNode);

        // Add query_string if query.query() is present
        if (query.query() != null && !query.query().isEmpty()) {
            ObjectNode queryStringNode = MAPPER.createObjectNode();
            queryStringNode.set("query_string", MAPPER.createObjectNode().put("query", query.query()));
            filterArray.add(queryStringNode);
        }

        // Time range (from/to are never null)
        ObjectNode rangeNode = MAPPER.createObjectNode();
        ObjectNode tsRange = MAPPER.createObjectNode();
        tsRange.put("from", query.from().toEpochMilli());
        tsRange.put("to", query.to().toEpochMilli());
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

    private ObjectNode createAggregationsNode(GroupByQuery query) {
        ObjectNode aggs = MAPPER.createObjectNode();

        if (query.groups() != null && !query.groups().isEmpty()) {
            ObjectNode byRange = MAPPER.createObjectNode();
            ObjectNode rangeAgg = MAPPER.createObjectNode();
            rangeAgg.put("field", query.field());
            var rangesArray = MAPPER.createArrayNode();
            for (GroupByQuery.Group group : query.groups()) {
                ObjectNode rangeObj = MAPPER.createObjectNode();
                rangeObj.put("from", group.from());
                rangeObj.put("to", group.to());
                rangesArray.add(rangeObj);
            }
            rangeAgg.set("ranges", rangesArray);
            byRange.set("range", rangeAgg);
            aggs.set("by_" + query.field() + "_range", byRange);
        } else {
            ObjectNode byTerms = MAPPER.createObjectNode();
            ObjectNode termsAgg = MAPPER.createObjectNode();
            termsAgg.put("field", query.field());
            termsAgg.put("size", 1000);
            if (query.order() != null) {
                ObjectNode orderNode = MAPPER.createObjectNode();
                orderNode.put(query.order().field(), query.order().order() ? "asc" : "desc");
                termsAgg.set("order", orderNode);

                if (query.order().type() != null && query.order().type().equalsIgnoreCase("AVG")) {
                    ObjectNode aggregationsNode = MAPPER.createObjectNode();
                    ObjectNode avgAgg = MAPPER.createObjectNode();
                    ObjectNode avgField = MAPPER.createObjectNode();
                    avgField.put("field", query.order().field());
                    avgAgg.set("avg", avgField);
                    aggregationsNode.set(query.order().field(), avgAgg);
                    byTerms.set("aggregations", aggregationsNode);
                }
            }
            byTerms.set("terms", termsAgg);
            aggs.set("by_" + query.field(), byTerms);
        }

        return aggs;
    }

    public Optional<GroupByAggregate> adaptResponse(SearchResponse response) {
        if (response == null || response.getAggregations() == null) {
            return Optional.empty();
        }
        Aggregation agg = response.getAggregations().get(this.aggName);
        if (agg == null || agg.getBuckets() == null) {
            return Optional.empty();
        }
        Map<String, Long> values = new HashMap<>();
        List<String> order = new ArrayList<>(agg.getBuckets().size());
        agg
            .getBuckets()
            .forEach(bucket -> {
                String key = bucket.get("key").asText();
                long count = bucket.get("doc_count").asLong();
                values.put(key, count);
                order.add(key);
            });
        return Optional.of(new GroupByAggregate(this.aggName, this.fieldName, values, order));
    }
}
