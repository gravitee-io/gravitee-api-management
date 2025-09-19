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
package io.gravitee.repository.elasticsearch.v4.log.adapter.message;

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.repository.elasticsearch.utils.JsonNodeUtils;
import io.gravitee.repository.log.v4.model.message.AggregatedMessageLog;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchMessageLogResponseAdapter {

    private SearchMessageLogResponseAdapter() {}

    public static List<AggregatedMessageLog> adapt(SearchHits hits) {
        if (hits == null || hits.getHits() == null) {
            return Collections.emptyList();
        }

        return hits
            .getHits()
            .stream()
            .map(hit -> {
                var json = hit.getSource();
                var connectorType = asTextOrNull(json.get("connector-type"));

                if ("entrypoint".equals(connectorType)) {
                    return aggregateFromEntrypointOnly(json);
                }
                return aggregateFromEndpointOnly(json);
            })
            .toList();
    }

    public static List<AggregatedMessageLog> adapt(SearchHits entrypointHits, SearchHits endpointHits) {
        return entrypointHits
            .getHits()
            .stream()
            .map(entrypointHit -> {
                var entrypoint = entrypointHit.getSource();
                return endpointHits
                    .getHits()
                    .stream()
                    .filter(endpointHit -> entrypoint.get("correlation-id").equals(endpointHit.getSource().get("correlation-id")))
                    .findFirst()
                    .map(endpointHit -> aggregateFromEntrypointAndEndpoint(entrypoint, endpointHit.getSource()))
                    .orElseGet(() -> {
                        log.warn(
                            "No endpoint found for entrypoint [correlation-id={}] of log [_id={}]",
                            entrypoint.get("correlation-id"),
                            entrypointHit.getId()
                        );
                        return aggregateFromEntrypointOnly(entrypoint);
                    });
            })
            .toList();
    }

    private static AggregatedMessageLog aggregateFromEntrypointOnly(JsonNode entrypoint) {
        return AggregatedMessageLog.builder()
            .apiId(asTextOrNull(entrypoint.get("api-id")))
            .requestId(asTextOrNull(entrypoint.get("request-id")))
            .timestamp(asTextOrNull(entrypoint.get("@timestamp")))
            .clientIdentifier(asTextOrNull(entrypoint.get("client-identifier")))
            .correlationId(asTextOrNull(entrypoint.get("correlation-id")))
            .operation(asTextOrNull(entrypoint.get("operation")))
            .entrypoint(adaptMessage(entrypoint))
            .build();
    }

    private static AggregatedMessageLog aggregateFromEndpointOnly(JsonNode endpoint) {
        return AggregatedMessageLog.builder()
            .apiId(asTextOrNull(endpoint.get("api-id")))
            .requestId(asTextOrNull(endpoint.get("request-id")))
            .timestamp(asTextOrNull(endpoint.get("@timestamp")))
            .clientIdentifier(asTextOrNull(endpoint.get("client-identifier")))
            .correlationId(asTextOrNull(endpoint.get("correlation-id")))
            .operation(asTextOrNull(endpoint.get("operation")))
            .endpoint(adaptMessage(endpoint))
            .build();
    }

    private static AggregatedMessageLog aggregateFromEntrypointAndEndpoint(JsonNode entrypoint, JsonNode endpoint) {
        var operation = asTextOrNull(entrypoint.get("operation"));
        var timestamp = asTextOrNull(entrypoint.get("@timestamp"));
        if ("subscribe".equals(operation)) {
            timestamp = asTextOrNull(endpoint.get("@timestamp"));
        }

        return AggregatedMessageLog.builder()
            .apiId(asTextOrNull(entrypoint.get("api-id")))
            .requestId(asTextOrNull(entrypoint.get("request-id")))
            .timestamp(timestamp)
            .clientIdentifier(asTextOrNull(entrypoint.get("client-identifier")))
            .correlationId(asTextOrNull(entrypoint.get("correlation-id")))
            .operation(operation)
            .entrypoint(adaptMessage(entrypoint))
            .endpoint(adaptMessage(endpoint))
            .build();
    }

    private static AggregatedMessageLog.Message adaptMessage(JsonNode json) {
        var messageJson = json.get("message");
        var isError = messageJson.get("isError") != null && messageJson.get("isError").asBoolean();
        var headersJson = messageJson.get("headers");
        var messageHeaders = null != headersJson
            ? headersJson
                .properties()
                .stream()
                .collect(
                    Collectors.toMap(Map.Entry::getKey, entry ->
                        StreamSupport.stream(entry.getValue().spliterator(), false).map(JsonNodeUtils::asTextOrNull).toList()
                    )
                )
            : null;

        var metadataJson = messageJson.get("metadata");
        var messageMetadata = null != metadataJson
            ? metadataJson.properties().stream().collect(Collectors.toMap(Map.Entry::getKey, value -> asTextOrNull(value.getValue())))
            : null;

        return AggregatedMessageLog.Message.builder()
            .connectorId(asTextOrNull(json.get("connector-id")))
            .timestamp(asTextOrNull(json.get("@timestamp")))
            .id(asTextOrNull(messageJson.get("id")))
            .isError(isError)
            .payload(asTextOrNull(messageJson.get("payload")))
            .headers(messageHeaders)
            .metadata(messageMetadata)
            .build();
    }
}
