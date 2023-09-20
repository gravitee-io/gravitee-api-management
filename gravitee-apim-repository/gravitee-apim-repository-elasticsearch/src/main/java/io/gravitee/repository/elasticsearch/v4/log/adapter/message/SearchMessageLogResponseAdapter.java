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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.message.MessageLog;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SearchMessageLogResponseAdapter {

    private SearchMessageLogResponseAdapter() {}

    public static LogResponse<MessageLog> adapt(SearchResponse response) {
        var hits = response.getSearchHits();
        if (hits == null) {
            return new LogResponse<>(0, List.of());
        }

        return new LogResponse<>(
            (int) hits.getTotal().getValue(),
            hits.getHits().stream().map(h -> buildFromSource(h.getSource())).toList()
        );
    }

    private static MessageLog buildFromSource(JsonNode json) {
        final JsonNode messageJson = json.get("message");
        return MessageLog
            .builder()
            .apiId(json.get("api-id").asText())
            .requestId(json.get("request-id").asText())
            .timestamp(json.get("@timestamp").asText())
            .clientIdentifier(json.get("client-identifier").asText())
            .correlationId(json.get("correlation-id").asText())
            .operation(json.get("operation").asText())
            .connectorId(json.get("connector-id").asText())
            .connectorType(json.get("connector-type").asText())
            .message(
                MessageLog.Message
                    .builder()
                    .id(messageJson.get("id").asText())
                    .isError(messageJson.get("isError") != null && messageJson.get("isError").asBoolean())
                    .payload(messageJson.get("payload").asText())
                    .headers(
                        messageJson
                            .get("headers")
                            .properties()
                            .stream()
                            .collect(
                                Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry ->
                                        StreamSupport
                                            .stream(entry.getValue().spliterator(), false)
                                            .map(JsonNode::asText)
                                            .collect(Collectors.toList())
                                )
                            )
                    )
                    .metadata(
                        messageJson
                            .get("metadata")
                            .properties()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, value -> value.getValue().asText()))
                    )
                    .build()
            )
            .build();
    }
}
