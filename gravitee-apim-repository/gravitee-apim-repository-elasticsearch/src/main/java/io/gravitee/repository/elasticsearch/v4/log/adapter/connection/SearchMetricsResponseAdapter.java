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

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.*;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.connection.RequestV2MetricsV4Fields.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.ConnectionDiagnostic;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

public class SearchMetricsResponseAdapter {

    private SearchMetricsResponseAdapter() {}

    public static LogResponse<Metrics> adapt(SearchResponse response) {
        var hits = response.getSearchHits();
        if (hits == null) {
            return new LogResponse<>(0, List.of());
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

    private static Metrics buildFromSource(String index, String id, JsonNode json) {
        var metrics = Metrics.builder()
            .timestamp(asTextOrNull(json.get(RequestV2MetricsV4Fields.TIMESTAMP)))
            .status(asIntOr(json.get(RequestV2MetricsV4Fields.STATUS), 0))
            .gateway(asTextOrNull(json.get(RequestV2MetricsV4Fields.GATEWAY)))
            .uri(asTextOrNull(json.get(RequestV2MetricsV4Fields.URI)))
            .requestContentLength(asIntOr(json.get(RequestV2MetricsV4Fields.REQUEST_CONTENT_LENGTH), 0))
            .responseContentLength(asIntOr(json.get(RequestV2MetricsV4Fields.RESPONSE_CONTENT_LENGTH), 0))
            .errorKey(asTextOrNull(json.get(ERROR_KEY)))
            .errorComponentName(asTextOrNull(json.get(ERROR_COMPONENT_NAME)))
            .errorComponentType(asTextOrNull(json.get(ERROR_COMPONENT_TYPE)))
            .additionalMetrics(asMapOrNull(json.get(ADDITIONAL_METRICS)));

        if (index.contains(Type.REQUEST.getType())) {
            return metrics
                .requestId(id)
                .applicationId(asTextOrNull(json.get(RequestV2MetricsV4Fields.APPLICATION_ID.v2Request())))
                .apiId(asTextOrNull(json.get(RequestV2MetricsV4Fields.API_ID.v2Request())))
                .planId(asTextOrNull(json.get(RequestV2MetricsV4Fields.PLAN_ID.v2Request())))
                .clientIdentifier(asTextOrNull(json.get(RequestV2MetricsV4Fields.CLIENT_IDENTIFIER.v2Request())))
                .transactionId(asTextOrNull(json.get(RequestV2MetricsV4Fields.TRANSACTION_ID.v2Request())))
                .method(HttpMethod.get(asIntOr(json.get(RequestV2MetricsV4Fields.HTTP_METHOD.v2Request()), 0)))
                .requestEnded(true)
                .entrypointId(null)
                .gatewayResponseTime(asIntOr(json.get(RequestV2MetricsV4Fields.GATEWAY_RESPONSE_TIME.v2Request()), 0))
                .message(asTextOrNull(json.get(RequestV2MetricsV4Fields.MESSAGE.v2Request())))
                .warnings(buildWarnings(json.get("warnings")))
                .build();
        }
        return metrics
            .requestId(json.get(RequestV2MetricsV4Fields.REQUEST_ID.v4Metrics()).asText())
            .applicationId(asTextOrNull(json.get(RequestV2MetricsV4Fields.APPLICATION_ID.v4Metrics())))
            .apiId(asTextOrNull(json.get(RequestV2MetricsV4Fields.API_ID.v4Metrics())))
            .planId(asTextOrNull(json.get(RequestV2MetricsV4Fields.PLAN_ID.v4Metrics())))
            .clientIdentifier(asTextOrNull(json.get(RequestV2MetricsV4Fields.CLIENT_IDENTIFIER.v4Metrics())))
            .transactionId(asTextOrNull(json.get(RequestV2MetricsV4Fields.TRANSACTION_ID.v4Metrics())))
            .method(HttpMethod.get(asIntOr(json.get(RequestV2MetricsV4Fields.HTTP_METHOD.v4Metrics()), 0)))
            .requestEnded(asBooleanOrFalse(json.get(RequestV2MetricsV4Fields.REQUEST_ENDED.v4Metrics())))
            .entrypointId(asTextOrNull(json.get(RequestV2MetricsV4Fields.ENTRYPOINT_ID.v4Metrics())))
            .gatewayResponseTime(asIntOr(json.get(RequestV2MetricsV4Fields.GATEWAY_RESPONSE_TIME.v4Metrics()), 0))
            .endpoint(asTextOrNull(json.get(RequestV2MetricsV4Fields.ENDPOINT.v4Metrics())))
            .message(asTextOrNull(json.get(RequestV2MetricsV4Fields.MESSAGE.v4Metrics())))
            .warnings(buildWarnings(json.get(WARNINGS)))
            .build();
    }

    private static List<ConnectionDiagnostic> buildWarnings(JsonNode json) {
        if (json == null || json.isNull() || !json.isArray()) {
            return null;
        }
        return StreamSupport.stream(json.spliterator(), false)
            .map(SearchMetricsResponseAdapter::buildDiagnostic)
            .filter(Objects::nonNull)
            .toList();
    }

    private static ConnectionDiagnostic buildDiagnostic(JsonNode json) {
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
}
