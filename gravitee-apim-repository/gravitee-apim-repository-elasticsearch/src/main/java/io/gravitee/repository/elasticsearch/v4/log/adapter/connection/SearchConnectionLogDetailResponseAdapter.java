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
package io.gravitee.repository.elasticsearch.v4.log.adapter.connection;

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asBooleanOrFalse;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asIntOr;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.elasticsearch.utils.JsonNodeUtils;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SearchConnectionLogDetailResponseAdapter {

    private SearchConnectionLogDetailResponseAdapter() {}

    public static Optional<ConnectionLogDetail> adapt(SearchResponse response) {
        var hits = response.getSearchHits();
        if (hits == null) {
            return Optional.empty();
        }

        return hits.getHits().stream().findFirst().map(SearchHit::getSource).map(SearchConnectionLogDetailResponseAdapter::buildFromSource);
    }

    private static ConnectionLogDetail buildFromSource(JsonNode json) {
        return ConnectionLogDetail.builder()
            .requestId(json.get("request-id").asText())
            .timestamp(asTextOrNull(json.get("@timestamp")))
            .apiId(asTextOrNull(json.get("api-id")))
            .clientIdentifier(asTextOrNull(json.get("client-identifier")))
            .requestEnded(asBooleanOrFalse(json.get("request-ended")))
            .entrypointRequest(buildRequest(json.get("entrypoint-request")))
            .endpointRequest(buildRequest(json.get("endpoint-request")))
            .entrypointResponse(buildResponse(json.get("entrypoint-response")))
            .endpointResponse(buildResponse(json.get("endpoint-response")))
            .build();
    }

    private static ConnectionLogDetail.Request buildRequest(JsonNode json) {
        return null != json
            ? ConnectionLogDetail.Request.builder()
                .uri(asTextOrNull(json.get("uri")))
                .method(asTextOrNull(json.get("method")))
                .headers(buildHeaders(json.get("headers")))
                .body(asTextOrNull(json.get("body")))
                .build()
            : null;
    }

    private static ConnectionLogDetail.Response buildResponse(JsonNode json) {
        return null != json
            ? ConnectionLogDetail.Response.builder()
                .status(asIntOr(json.get("status"), 0))
                .headers(buildHeaders(json.get("headers")))
                .body(asTextOrNull(json.get("body")))
                .build()
            : null;
    }

    private static Map<String, List<String>> buildHeaders(JsonNode headers) {
        if (headers == null) {
            return Map.of();
        }
        return headers
            .properties()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, entry ->
                    StreamSupport.stream(entry.getValue().spliterator(), false).map(JsonNodeUtils::asTextOrNull).toList()
                )
            );
    }
}
