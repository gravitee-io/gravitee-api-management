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
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchRequestResponseTimeAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<String> FILTERED_ENTRYPOINT_TYPES = List.of("http-post", "http-get", "http-proxy");
    private static final String GATEWAY_RESPONSE_TIME_MS_FIELD = "gateway-response-time-ms";
    private static final String MAX_RESPONSE_TIME_AGG = "max_response_time";
    private static final String MIN_RESPONSE_TIME_AGG = "min_response_time";
    private static final String AVG_RESPONSE_TIME_AGG = "avg_response_time";

    public String adaptQuery(RequestResponseTimeQueryCriteria queryCriteria) {
        return json().put("size", 0).<ObjectNode>set("query", buildQuery(queryCriteria)).set("aggs", buildAggregation()).toString();
    }

    private ObjectNode buildQuery(RequestResponseTimeQueryCriteria queryCriteria) {
        var filterQuery = array();

        if (queryCriteria == null || queryCriteria.apiIds() == null) {
            log.warn("Null query params or queried API IDs. Empty ranges will be returned");
            filterQuery.add(apiIdsFilterForQuery(List.of()));
        } else {
            filterQuery.add(apiIdsFilterForQuery(queryCriteria.apiIds()));
        }

        if (queryCriteria != null) {
            filterQuery.add(dateRangeFilterForQuery(queryCriteria.from(), queryCriteria.to()));
        }

        filterQuery.add(entrypointFilterForQuery());

        return json().set("bool", json().<ObjectNode>set("filter", filterQuery));
    }

    private ObjectNode apiIdsFilterForQuery(List<String> apiIds) {
        return json().set("terms", json().set("api-id", toArray(apiIds)));
    }

    private ObjectNode dateRangeFilterForQuery(Long from, Long to) {
        log.info("Top Hits Query: filtering date range from {} to {}", from, to);
        return json().set("range", json().set("@timestamp", json().put("gte", from).put("lte", to)));
    }

    private ObjectNode entrypointFilterForQuery() {
        return json().set("terms", json().set("entrypoint-id", toArray(FILTERED_ENTRYPOINT_TYPES)));
    }

    private ObjectNode buildAggregation() {
        String script =
            "if (doc.containsKey('gateway-response-time-ms')) { return doc.get('gateway-response-time-ms').value; } else if (doc.containsKey('response-time')) { return doc.get('response-time').value; }";
        return json()
            .<ObjectNode>set(
                MAX_RESPONSE_TIME_AGG,
                json().set("max", json().set("script", json().put("lang", "painless").put("source", script)))
            )
            .<ObjectNode>set(
                MIN_RESPONSE_TIME_AGG,
                json().set("min", json().set("script", json().put("lang", "painless").put("source", script)))
            )
            .set(AVG_RESPONSE_TIME_AGG, json().set("avg", json().set("script", json().put("lang", "painless").put("source", script))));
    }

    public RequestResponseTimeAggregate adaptResponse(SearchResponse response, RequestResponseTimeQueryCriteria queryCriteria) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        var requestResponseTimeAggregateBuilder = RequestResponseTimeAggregate.builder();

        if (aggregations == null || aggregations.isEmpty() || queryCriteria == null) {
            return requestResponseTimeAggregateBuilder.build();
        }

        requestResponseTimeAggregateBuilder.responseMinTime(getAggregationResponseValue(aggregations, MIN_RESPONSE_TIME_AGG));
        requestResponseTimeAggregateBuilder.responseMaxTime(getAggregationResponseValue(aggregations, MAX_RESPONSE_TIME_AGG));
        requestResponseTimeAggregateBuilder.responseAvgTime(getAggregationResponseValue(aggregations, AVG_RESPONSE_TIME_AGG));

        if (response.getSearchHits() != null && response.getSearchHits().getTotal() != null) {
            var totalRequestValue = response.getSearchHits().getTotal().getValue();
            requestResponseTimeAggregateBuilder.requestsTotal(totalRequestValue);

            var timeDiffInSeconds = ((queryCriteria.to() - queryCriteria.from()) / 1000);
            double requestsPerSecond = (double) totalRequestValue / timeDiffInSeconds;
            requestResponseTimeAggregateBuilder.requestsPerSecond(requestsPerSecond);
        }

        return requestResponseTimeAggregateBuilder.build();
    }

    private Double getAggregationResponseValue(Map<String, Aggregation> aggregations, String aggregationName) {
        var aggregation = aggregations.get(aggregationName);
        if (aggregation == null || aggregation.getValue() == null) {
            log.error("Aggregation with name {} not found", aggregationName);
            return 0.0;
        }
        return aggregation.getValue().doubleValue();
    }

    private ObjectNode json() {
        return MAPPER.createObjectNode();
    }

    private ArrayNode array() {
        return MAPPER.createArrayNode();
    }

    private ArrayNode toArray(List<String> list) {
        var arrayNode = array();
        list.forEach(arrayNode::add);
        return arrayNode;
    }
}
