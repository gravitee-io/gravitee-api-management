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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramAggregate;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SearchHistogramQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> ENTRYPOINT_IDS = List.of("http-post", "http-get", "http-proxy");
    private static final Map<AggregationType, String> AGGREGATION_PREFIX = Map.of(
        AggregationType.FIELD,
        "by_",
        AggregationType.AVG,
        "avg_",
        AggregationType.MAX,
        "max_",
        AggregationType.MIN,
        "min"
    );
    private static final Map<AggregationType, String> AGGREGATION_NAME = Map.of(
        AggregationType.FIELD,
        "terms",
        AggregationType.AVG,
        "avg",
        AggregationType.MAX,
        "max",
        AggregationType.MIN,
        "min"
    );
    private final Map<String, String> aggFieldMap = new LinkedHashMap<>();
    private final Map<String, AggregationType> aggTypeMap = new LinkedHashMap<>();

    public String adapt(HistogramQuery query) {
        aggFieldMap.clear();

        ObjectNode root = MAPPER.createObjectNode();
        root.put("size", 0);

        // Query
        ObjectNode queryNode = createQueryNode(query);
        root.set("query", queryNode);

        // Aggregations
        ObjectNode aggs = createAggregationsNode(query);
        root.set("aggregations", aggs);

        return root.toString();
    }

    private ObjectNode createQueryNode(HistogramQuery query) {
        var filterArray = MAPPER.createArrayNode();
        filterArray.add(createApiFilterNode(query));
        filterArray.add(createTimeRangeFilterNode(query));

        ObjectNode bool = MAPPER.createObjectNode();
        bool.set("filter", filterArray);

        ObjectNode queryNode = MAPPER.createObjectNode();
        queryNode.set("bool", bool);
        return queryNode;
    }

    private ObjectNode createApiFilterNode(HistogramQuery query) {
        var apiIds = List.of(query.getApiId());
        var mustArray = MAPPER.createArrayNode();
        var apiTerms = MAPPER.createObjectNode();
        apiTerms.set("terms", MAPPER.createObjectNode().set("api-id", MAPPER.valueToTree(apiIds)));
        mustArray.add(apiTerms);

        var entrypointIdTerms = MAPPER.createObjectNode();
        entrypointIdTerms.set("terms", MAPPER.createObjectNode().set("entrypoint-id", MAPPER.valueToTree(ENTRYPOINT_IDS)));
        mustArray.add(entrypointIdTerms);

        var boolMust = MAPPER.createObjectNode();
        boolMust.set("must", mustArray);

        var shouldArray = MAPPER.createArrayNode();
        shouldArray.add(MAPPER.createObjectNode().set("bool", boolMust));

        var boolShould = MAPPER.createObjectNode();
        boolShould.put("minimum_should_match", 1);
        boolShould.set("should", shouldArray);

        return MAPPER.createObjectNode().set("bool", boolShould);
    }

    private ObjectNode createTimeRangeFilterNode(HistogramQuery query) {
        ObjectNode range = MAPPER.createObjectNode();
        range.put("from", query.getFrom().toEpochMilli());
        range.put("to", query.getTo().toEpochMilli());
        range.put("include_lower", true);
        range.put("include_upper", true);

        ObjectNode rangeQuery = MAPPER.createObjectNode();
        rangeQuery.set("@timestamp", range);

        return MAPPER.createObjectNode().set("range", rangeQuery);
    }

    private ObjectNode createAggregationsNode(HistogramQuery query) {
        ObjectNode aggs = MAPPER.createObjectNode();
        ObjectNode byDate = createTimestampAggregationNode(query);
        aggs.set("by_date", byDate);
        return aggs;
    }

    private ObjectNode createTimestampAggregationNode(HistogramQuery query) {
        ObjectNode byDate = MAPPER.createObjectNode();
        ObjectNode dateHistogram = MAPPER.createObjectNode();
        dateHistogram.put("field", "@timestamp");
        dateHistogram.put("fixed_interval", query.getInterval().toMillis() + "ms");
        dateHistogram.put("min_doc_count", 0);
        ObjectNode extendedBounds = MAPPER.createObjectNode();
        extendedBounds.put("min", query.getFrom().toEpochMilli());
        extendedBounds.put("max", query.getTo().toEpochMilli());
        dateHistogram.set("extended_bounds", extendedBounds);
        byDate.set("date_histogram", dateHistogram);

        aggFieldMap.put("by_date", "@timestamp");

        ObjectNode subAggs = createChildAggregationNodes(query);
        byDate.set("aggregations", subAggs);

        return byDate;
    }

    private ObjectNode createChildAggregationNodes(HistogramQuery query) {
        ObjectNode subAggs = MAPPER.createObjectNode();
        for (io.gravitee.repository.log.v4.model.analytics.Aggregation agg : query.getAggregations()) {
            String aggName = AGGREGATION_PREFIX.get(agg.getType()) + agg.getField();
            String aggType = AGGREGATION_NAME.get(agg.getType());
            ObjectNode subAgg = MAPPER.createObjectNode();
            ObjectNode subAggAgg = MAPPER.createObjectNode();
            subAggAgg.put("field", agg.getField());
            subAgg.set(aggType, subAggAgg);
            subAggs.set(aggName, subAgg);
            aggFieldMap.put(aggName, agg.getField());
            aggTypeMap.put(aggName, agg.getType());
        }
        return subAggs;
    }

    public List<HistogramAggregate<?>> adaptResponse(SearchResponse response) {
        if (response == null || response.getAggregations() == null) {
            return Collections.emptyList();
        }
        Aggregation histogramAgg = response.getAggregations().get("by_date");
        if (histogramAgg == null || histogramAgg.getBuckets() == null) {
            return Collections.emptyList();
        }
        return parseHistogramAggregates(histogramAgg);
    }

    private List<HistogramAggregate<?>> parseHistogramAggregates(Aggregation histogramAgg) {
        List<HistogramAggregate<?>> result = new ArrayList<>();
        var histogramSize = histogramAgg.getBuckets().size();
        for (var aggType : aggTypeMap.entrySet()) {
            if (aggType.getValue() == AggregationType.FIELD) {
                result.add(parseFieldHistogramAggregate(histogramAgg, aggType.getKey(), aggFieldMap.get(aggType.getKey()), histogramSize));
            } else {
                result.add(parseMetricHistogramAggregate(histogramAgg, aggType.getKey(), aggFieldMap.get(aggType.getKey()), histogramSize));
            }
        }
        return result;
    }

    private HistogramAggregate<Long> parseFieldHistogramAggregate(
        Aggregation histogramAgg,
        String aggName,
        String fieldName,
        int histogramSize
    ) {
        var values = new HashMap<String, List<Long>>();
        List<JsonNode> buckets = histogramAgg.getBuckets();
        for (int i = 0; i < buckets.size(); i++) {
            var bucket = buckets.get(i);
            if (bucket.has(aggName)) {
                var aggBuckets = bucket.get(aggName).get("buckets");
                for (var aggBucket : aggBuckets) {
                    var key = aggBucket.get("key").asText();
                    if (!values.containsKey(key)) {
                        values.put(key, new ArrayList<>(Collections.nCopies(histogramSize, 0L)));
                    }
                    values.get(key).set(i, aggBucket.get("doc_count").asLong());
                }
            }
        }
        return new HistogramAggregate<>(aggName, fieldName, values);
    }

    private HistogramAggregate<Double> parseMetricHistogramAggregate(
        Aggregation histogramAgg,
        String aggName,
        String fieldName,
        int histogramSize
    ) {
        var values = new HashMap<String, List<Double>>();
        values.put(aggName, new ArrayList<>(Collections.nCopies(histogramSize, 0.0d)));
        List<JsonNode> buckets = histogramAgg.getBuckets();
        for (int i = 0; i < buckets.size(); i++) {
            var bucket = buckets.get(i);
            if (bucket.has(aggName)) {
                var aggValue = bucket.get(aggName).get("value");
                if (!aggValue.isNull()) {
                    values.get(aggName).set(i, aggValue.asDouble());
                }
            }
        }
        return new HistogramAggregate<>(aggName, fieldName, values);
    }
}
