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
import io.gravitee.gamma.rest.core.observability.logs.model.LogEntry;
import io.gravitee.gamma.rest.core.observability.logs.model.LogEntryWarning;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Wire shape for one light log row returned by {@code POST /observability/logs/search}. Null
 * fields are omitted from the JSON to keep the payload lean.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LogEntryDto(
    String apiId,
    String apiName,
    String apiType,
    Instant timestamp,
    String requestId,
    String method,
    String clientIdentifier,
    String planId,
    String planName,
    String applicationId,
    String applicationName,
    String transactionId,
    Integer status,
    Boolean requestEnded,
    Integer gatewayResponseTime,
    String gateway,
    String gatewayHostname,
    String uri,
    String endpoint,
    String message,
    String errorKey,
    String errorComponentName,
    String errorComponentType,
    List<WarningDto> warnings,
    Map<String, Object> additionalMetrics,
    String mcpMethod,
    String apiProductId,
    String apiProductName
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WarningDto(String componentType, String componentName, String key, String message) {
        public static WarningDto from(LogEntryWarning w) {
            return new WarningDto(w.componentType(), w.componentName(), w.key(), w.message());
        }
    }

    public static LogEntryDto from(LogEntry entry) {
        return new LogEntryDto(
            entry.apiId(),
            entry.apiName(),
            entry.apiType(),
            entry.timestamp(),
            entry.requestId(),
            entry.method(),
            entry.clientIdentifier(),
            entry.planId(),
            entry.planName(),
            entry.applicationId(),
            entry.applicationName(),
            entry.transactionId(),
            entry.status(),
            entry.requestEnded(),
            entry.gatewayResponseTime(),
            entry.gateway(),
            entry.gatewayHostname(),
            entry.uri(),
            entry.endpoint(),
            entry.message(),
            entry.errorKey(),
            entry.errorComponentName(),
            entry.errorComponentType(),
            entry.warnings() != null ? entry.warnings().stream().map(WarningDto::from).toList() : null,
            entry.additionalMetrics(),
            entry.mcpMethod(),
            entry.apiProductId(),
            entry.apiProductName()
        );
    }
}
