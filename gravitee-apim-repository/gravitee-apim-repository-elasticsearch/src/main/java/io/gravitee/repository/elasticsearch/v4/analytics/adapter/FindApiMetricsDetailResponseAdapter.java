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
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asMapOrNull;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.ApiMetricsDetail;
import io.gravitee.repository.log.v4.model.connection.ConnectionDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class FindApiMetricsDetailResponseAdapter {

    public FindApiMetricsDetailResponseAdapter() {}

    public static Optional<ApiMetricsDetail> adaptFirst(SearchResponse response) {
        var hits = response.getSearchHits();
        if (hits == null) {
            return Optional.empty();
        }

        return hits
            .getHits()
            .stream()
            .findFirst()
            .map(h -> buildFromSource(h.getSource(), h.getId()));
    }

    private static ApiMetricsDetail buildFromSource(JsonNode json, String hitId) {
        var requestId = coalesceText(json, "request-id", "id");
        return ApiMetricsDetail.builder()
            .timestamp(asTextOrNull(json.get("@timestamp")))
            .apiId(coalesceText(json, "api-id", "api"))
            .requestId(requestId != null ? requestId : hitId)
            .transactionId(coalesceText(json, "transaction-id", "transaction"))
            .host(asTextOrNull(json.get("host")))
            .applicationId(coalesceText(json, "application-id", "application"))
            .planId(coalesceText(json, "plan-id", "plan"))
            .gateway(asTextOrNull(json.get("gateway")))
            .status(asIntOr(json.get("status"), 0))
            .uri(asTextOrNull(json.get("uri")))
            .requestContentLength(asLongOr(json.get("request-content-length"), 0))
            .responseContentLength(asLongOr(json.get("response-content-length"), 0))
            .remoteAddress(asTextOrNull(json.get("remote-address")))
            .gatewayLatency(coalesceLong(json, "gateway-latency-ms", "proxy-latency"))
            .gatewayResponseTime(coalesceLong(json, "gateway-response-time-ms", "response-time"))
            .endpointResponseTime(coalesceLong(json, "endpoint-response-time-ms", "api-response-time"))
            .method(HttpMethod.get(coalesceInt(json, "http-method", "method")))
            .endpoint(asTextOrNull(json.get("endpoint")))
            .message(asTextOrNull(json.get("error-message")))
            .errorKey(asTextOrNull(json.get("error-key")))
            .errorComponentName(asTextOrNull(json.get("error-component-name")))
            .errorComponentType(asTextOrNull(json.get("error-component-type")))
            .warnings(buildWarnings(json.get("warnings")))
            .additionalMetrics(asMapOrNull(json.get("additional-metrics")))
            .build();
    }

    private static String coalesceText(JsonNode json, String v4Field, String v2Field) {
        var value = asTextOrNull(json.get(v4Field));
        return value != null ? value : asTextOrNull(json.get(v2Field));
    }

    private static long coalesceLong(JsonNode json, String v4Field, String v2Field) {
        var node = json.get(v4Field);
        if (node != null && !node.isNull()) {
            return node.asLong(0);
        }
        return asLongOr(json.get(v2Field), 0);
    }

    private static int coalesceInt(JsonNode json, String v4Field, String v2Field) {
        var node = json.get(v4Field);
        if (node != null && !node.isNull()) {
            return node.asInt(0);
        }
        return asIntOr(json.get(v2Field), 0);
    }

    private static List<ConnectionDiagnostic> buildWarnings(JsonNode json) {
        if (json == null || !json.isArray()) {
            return List.of();
        }
        return StreamSupport.stream(json.spliterator(), false)
            .map(FindApiMetricsDetailResponseAdapter::buildDiagnostic)
            .filter(Objects::nonNull)
            .toList();
    }

    private static ConnectionDiagnostic buildDiagnostic(JsonNode json) {
        if (json == null) {
            return null;
        }
        return ConnectionDiagnostic.builder()
            .componentType(asTextOrNull(json.get("component-type")))
            .componentName(asTextOrNull(json.get("component-name")))
            .key(asTextOrNull(json.get("key")))
            .message(asTextOrNull(json.get("message")))
            .build();
    }
}
