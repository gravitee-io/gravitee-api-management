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

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchRequestResponseTimeAdapter {

    private static final List<String> FILTERED_ENTRYPOINT_TYPES = List.of("http-post", "http-get", "http-proxy");
    private static final String GATEWAY_RESPONSE_TIME_MS_FIELD = "gateway-response-time-ms";
    private static final String MAX_RESPONSE_TIME_AGG = "max_response_time";
    private static final String MIN_RESPONSE_TIME_AGG = "min_response_time";
    private static final String AVG_RESPONSE_TIME_AGG = "avg_response_time";

    public static String adaptQuery(RequestResponseTimeQueryCriteria queryCriteria) {
        var jsonContent = new HashMap<String, Object>();

        jsonContent.put("size", 0);
        jsonContent.put("query", buildQuery(queryCriteria));
        jsonContent.put("aggs", buildAggregation());
        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildQuery(RequestResponseTimeQueryCriteria queryCriteria) {
        var filterQuery = new ArrayList<JsonObject>();

        if (queryCriteria == null || queryCriteria.getApiIds() == null) {
            log.warn("Null query params or queried API IDs. Empty ranges will be returned");
            filterQuery.add(apiIdsFilterForQuery(List.of()));
        } else {
            filterQuery.add(apiIdsFilterForQuery(queryCriteria.getApiIds()));
        }

        if (queryCriteria != null) {
            filterQuery.add(dateRangeFilterForQuery(queryCriteria.getFrom(), queryCriteria.getTo()));
        }

        filterQuery.add(entrypointFilterForQuery());

        return JsonObject.of("bool", JsonObject.of("filter", filterQuery));
    }

    private static JsonObject apiIdsFilterForQuery(List<String> apiIds) {
        return JsonObject.of("terms", JsonObject.of("api-id", apiIds));
    }

    private static JsonObject dateRangeFilterForQuery(Long from, Long to) {
        log.info("Top Hits Query: filtering date range from {} to {}", from, to);
        return JsonObject.of("range", JsonObject.of("@timestamp", JsonObject.of("gte", from, "lte", to)));
    }

    private static JsonObject entrypointFilterForQuery() {
        return JsonObject.of("terms", JsonObject.of("entrypoint-id", FILTERED_ENTRYPOINT_TYPES));
    }

    private static JsonObject buildAggregation() {
        return JsonObject.of(
            MAX_RESPONSE_TIME_AGG,
            JsonObject.of("max", JsonObject.of("field", GATEWAY_RESPONSE_TIME_MS_FIELD)),
            MIN_RESPONSE_TIME_AGG,
            JsonObject.of("min", JsonObject.of("field", GATEWAY_RESPONSE_TIME_MS_FIELD)),
            AVG_RESPONSE_TIME_AGG,
            JsonObject.of("avg", JsonObject.of("field", GATEWAY_RESPONSE_TIME_MS_FIELD))
        );
    }

    public static RequestResponseTimeAggregate adaptResponse(SearchResponse response, RequestResponseTimeQueryCriteria queryCriteria) {
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

            var timeDiffInSeconds = ((queryCriteria.getTo() - queryCriteria.getFrom()) / 1000);
            double requestsPerSecond = (double) totalRequestValue / timeDiffInSeconds;
            requestResponseTimeAggregateBuilder.requestsPerSecond(requestsPerSecond);
        }

        return requestResponseTimeAggregateBuilder.build();
    }

    private static Double getAggregationResponseValue(Map<String, Aggregation> aggregations, String aggregationName) {
        var aggregation = aggregations.get(aggregationName);
        if (aggregation == null || aggregation.getValue() == null) {
            log.error("Aggregation with name {} not found", aggregationName);
            return 0.0;
        }
        return aggregation.getValue().doubleValue();
    }
}
