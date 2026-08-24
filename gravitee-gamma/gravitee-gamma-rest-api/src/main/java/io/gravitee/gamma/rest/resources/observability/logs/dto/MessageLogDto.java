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
package io.gravitee.gamma.rest.resources.observability.logs.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.gamma.rest.core.observability.logs.model.MessageLog;
import java.util.List;
import java.util.Map;

/**
 * Wire shape of one message of a connection. Field names mirror what the observability library's
 * message log detail reads, so the UI needs no adapter of its own.
 *
 * @param timestamp ISO-8601, unlike the epoch millis a log entry carries — the library's message
 *                  entry expects a string here.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageLogDto(
    String requestId,
    String apiId,
    String timestamp,
    String clientIdentifier,
    String correlationId,
    String parentCorrelationId,
    String operation,
    MessageDto entrypoint,
    MessageDto endpoint
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MessageDto(
        String id,
        String timestamp,
        String connectorId,
        String payload,
        boolean isError,
        Map<String, List<String>> headers,
        Map<String, String> metadata
    ) {
        static MessageDto from(MessageLog.Message message) {
            if (message == null) {
                return null;
            }
            return new MessageDto(
                message.id(),
                message.timestamp(),
                message.connectorId(),
                message.payload(),
                message.error(),
                message.headers(),
                message.metadata()
            );
        }
    }

    public static MessageLogDto from(MessageLog log) {
        return new MessageLogDto(
            log.requestId(),
            log.apiId(),
            log.timestamp(),
            log.clientIdentifier(),
            log.correlationId(),
            log.parentCorrelationId(),
            log.operation(),
            MessageDto.from(log.entrypoint()),
            MessageDto.from(log.endpoint())
        );
    }
}
