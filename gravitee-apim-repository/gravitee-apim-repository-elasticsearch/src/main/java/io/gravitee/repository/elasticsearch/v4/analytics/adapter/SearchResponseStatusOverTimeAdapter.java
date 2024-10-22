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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public class SearchResponseStatusOverTimeAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String AGGREGATION_BY_DATE = "by_date";
    public static final String AGGREGATION_BY_STATUS = "by_status";
    private static final String TIME_FIELD = "@timestamp";

    public String adaptQuery(ResponseStatusOverTimeQuery query, ElasticsearchInfo esInfo) {
        return json().put("size", 0).<ObjectNode>set("query", query(query)).set("aggregations", aggregations(query, esInfo)).toString();
    }

    public ResponseStatusOverTimeAggregate adaptResponse(SearchResponse response) {
        var treeMap = new TreeMap<Long, Map<String, Long>>();
        var statusValues = new TreeSet<String>();
        var byDate = response.getAggregations().get(SearchResponseStatusOverTimeAdapter.AGGREGATION_BY_DATE);
        for (var dateBucket : byDate.getBuckets()) {
            var keyAsDate = dateBucket.get("key").asLong();
            var statusCount = new HashMap<String, Long>();

            for (var statusBucket : dateBucket.get(SearchResponseStatusOverTimeAdapter.AGGREGATION_BY_STATUS).get("buckets")) {
                var status = statusBucket.get("key").asText();
                var count = statusBucket.get("doc_count").asLong();
                statusCount.put(status, count);
                statusValues.add(status);
            }
            treeMap.put(keyAsDate, statusCount);
        }

        var result = new HashMap<String, List<Long>>();

        // Initialize the result map with empty lists for each status returned by ES
        for (var status : statusValues) {
            result.put(status, new ArrayList<>());
        }

        for (var entry : treeMap.entrySet()) {
            var statusCount = entry.getValue();
            result.forEach((key, value) -> {
                value.add(statusCount.getOrDefault(key, 0L));
            });
        }

        return new ResponseStatusOverTimeAggregate(result);
    }

    private ObjectNode query(ResponseStatusOverTimeQuery query) {
        JsonNode termFilter = json().set("term", json().put("api-id", query.apiId()));

        // we just ensure to fetch full bucket interval (a bit too)
        Instant from = query.from().minus(query.interval());
        Instant to = query.to().plus(query.interval());

        ObjectNode timestamp = json()
            .put("from", from.toEpochMilli())
            .put("to", to.toEpochMilli())
            .put("include_lower", true)
            .put("include_upper", true);
        JsonNode rangeFilter = json().set("range", json().set(TIME_FIELD, timestamp));

        var bool = json().set("filter", array().add(termFilter).add(rangeFilter));
        return json().set("bool", bool);
    }

    private ObjectNode aggregations(ResponseStatusOverTimeQuery query, ElasticsearchInfo esInfo) {
        String intervalFieldName = esInfo.getVersion().canUseDateHistogramFixedInterval() ? "fixed_interval" : "interval";
        ObjectNode histogram = json()
            .put("field", TIME_FIELD)
            .put(intervalFieldName, query.interval().toMillis() + "ms")
            .put("min_doc_count", 0)
            .set(
                "extended_bounds",
                json()
                    // we don't need to align it on interval ES make it for us
                    .put("min", query.from().toEpochMilli())
                    .put("max", query.to().toEpochMilli())
            );
        ObjectNode agg = json().set(AGGREGATION_BY_STATUS, json().set("terms", json().put("field", "status")));

        return json().set(AGGREGATION_BY_DATE, json().<ObjectNode>set("date_histogram", histogram).set("aggregations", agg));
    }

    private ObjectNode json() {
        return MAPPER.createObjectNode();
    }

    private ArrayNode array() {
        return MAPPER.createArrayNode();
    }
}
