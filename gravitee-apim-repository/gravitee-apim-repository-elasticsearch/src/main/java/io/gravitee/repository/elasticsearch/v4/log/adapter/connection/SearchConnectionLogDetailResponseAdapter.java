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
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.elasticsearch.utils.JsonNodeUtils;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.ConnectionDiagnostic;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SearchConnectionLogDetailResponseAdapter {

    private SearchConnectionLogDetailResponseAdapter() {}

    public static Optional<ConnectionLogDetail> adaptFirst(SearchResponse response) {
        var hits = response.getSearchHits();
        if (hits == null) {
            return Optional.empty();
        }

        return hits
            .getHits()
            .stream()
            .findFirst()
            .map(h -> buildFromSource(h.getIndex(), h.getId(), h.getSource()));
    }

    public static LogResponse<ConnectionLogDetail> adapt(SearchResponse response) {
        var hits = response.getSearchHits();
        if (hits == null) {
            return new LogResponse<>(0, Collections.emptyList());
        }

        return new LogResponse<>(
            (int) hits.getTotal().getValue(),
            hits
                .getHits()
                .stream()
                .map(h -> buildFromSource(h.getIndex(), h.getId(), h.getSource()))
                .toList()
        );
    }

    private static ConnectionLogDetail buildFromSource(String index, String id, JsonNode json) {
        var connectionLogDetail = ConnectionLogDetail.builder().timestamp(asTextOrNull(json.get("@timestamp")));

        if (index.contains(Type.V4_LOG.getType())) {
            return connectionLogDetail
                .requestId(json.get("request-id").asText())
                .apiId(asTextOrNull(json.get("api-id")))
                .clientIdentifier(asTextOrNull(json.get("client-identifier")))
                .requestEnded(asBooleanOrFalse(json.get("request-ended")))
                .entrypointRequest(buildRequest(json.get("entrypoint-request")))
                .endpointRequest(buildRequest(json.get("endpoint-request")))
                .entrypointResponse(buildResponse(json.get("entrypoint-response")))
                .endpointResponse(buildResponse(json.get("endpoint-response")))
                .message(asTextOrNull(json.get("message")))
                .errorKey(asTextOrNull(json.get("error-key")))
                .errorComponentName(asTextOrNull(json.get("error-component-name")))
                .errorComponentType(asTextOrNull(json.get("error-component-type")))
                .warnings(buildWarnings(json.get("warnings")))
                .build();
        }
        return connectionLogDetail
            .requestId(id)
            .apiId(asTextOrNull(json.get("api")))
            .clientIdentifier(null)
            .requestEnded(true)
            .entrypointRequest(buildRequest(json.get("client-request")))
            .endpointRequest(buildRequest(json.get("proxy-request")))
            .entrypointResponse(buildResponse(json.get("client-response")))
            .endpointResponse(buildResponse(json.get("proxy-response")))
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

    private static ConnectionDiagnostic buildFailure(JsonNode json) {
        if (json == null || json.isNull()) {
            return null;
        }
        return ConnectionDiagnostic.builder()
            .componentType(asTextOrNull(json.get("component-type")))
            .componentName(asTextOrNull(json.get("component-name")))
            .key(asTextOrNull(json.get("key")))
            .message(asTextOrNull(json.get("message")))
            .build();
    }

    private static List<ConnectionDiagnostic> buildWarnings(JsonNode json) {
        if (json == null || json.isNull() || !json.isArray()) {
            return null;
        }
        return StreamSupport.stream(json.spliterator(), false)
            .map(SearchConnectionLogDetailResponseAdapter::buildFailure)
            .filter(Objects::nonNull)
            .toList();
    }
}
