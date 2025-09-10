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

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asIntOr;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asLongOr;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.ApiMetricsDetail;
import java.util.Optional;

public class FindApiMetricsDetailResponseAdapter {

    public FindApiMetricsDetailResponseAdapter() {}

    public static Optional<ApiMetricsDetail> adaptFirst(SearchResponse response) {
        var hits = response.getSearchHits();
        if (hits == null) {
            return Optional.empty();
        }

        return hits.getHits().stream().findFirst().map(h -> buildFromSource(h.getSource()));
    }

    private static ApiMetricsDetail buildFromSource(JsonNode json) {
        return ApiMetricsDetail
            .builder()
            .timestamp(asTextOrNull(json.get("@timestamp")))
            .apiId(asTextOrNull(json.get("api-id")))
            .requestId(asTextOrNull(json.get("request-id")))
            .transactionId(asTextOrNull(json.get("transaction-id")))
            .host(asTextOrNull(json.get("host")))
            .applicationId(asTextOrNull(json.get("application-id")))
            .planId(asTextOrNull(json.get("plan-id")))
            .gateway(asTextOrNull(json.get("gateway")))
            .status(asIntOr(json.get("status"), 0))
            .uri(asTextOrNull(json.get("uri")))
            .requestContentLength(asLongOr(json.get("request-content-length"), 0))
            .responseContentLength(asLongOr(json.get("response-content-length"), 0))
            .remoteAddress(asTextOrNull(json.get("remote-address")))
            .gatewayLatency(asLongOr(json.get("gateway-latency-ms"), 0))
            .gatewayResponseTime(asLongOr(json.get("gateway-response-time-ms"), 0))
            .endpointResponseTime(asLongOr(json.get("endpoint-response-time-ms"), 0))
            .method(HttpMethod.get(asIntOr(json.get("http-method"), 0)))
            .endpoint(asTextOrNull(json.get("endpoint")))
            .build();
    }
}
