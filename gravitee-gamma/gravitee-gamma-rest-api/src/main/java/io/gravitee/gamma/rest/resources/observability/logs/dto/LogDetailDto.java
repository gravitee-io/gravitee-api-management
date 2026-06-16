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
import io.gravitee.gamma.rest.core.observability.logs.model.HttpPayload;
import io.gravitee.gamma.rest.core.observability.logs.model.LogDetail;
import io.gravitee.gamma.rest.core.observability.logs.model.LogEntryWarning;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Wire shape for the merged log detail returned by {@code GET /observability/logs/{requestId}}.
 * Null fields are omitted to keep the payload lean.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LogDetailDto(
    String requestId,
    String apiId,
    String transactionId,
    String clientIdentifier,
    Instant timestamp,
    Boolean requestEnded,
    String method,
    String uri,
    Integer status,
    String endpoint,
    String host,
    String planId,
    String planName,
    String applicationId,
    String applicationName,
    String gateway,
    String gatewayHostname,
    String gatewayIp,
    String remoteAddress,
    Long requestContentLength,
    Long responseContentLength,
    Long gatewayLatency,
    Long gatewayResponseTime,
    Long endpointResponseTime,
    String message,
    String errorKey,
    String errorComponentName,
    String errorComponentType,
    List<LogEntryDto.WarningDto> warnings,
    Map<String, Object> additionalMetrics,
    HttpPayloadDto entrypointRequest,
    HttpPayloadDto entrypointResponse,
    HttpPayloadDto endpointRequest,
    HttpPayloadDto endpointResponse
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HttpPayloadDto(String method, String uri, Integer status, Map<String, List<String>> headers, String body) {
        public static HttpPayloadDto from(HttpPayload payload) {
            if (payload == null) {
                return null;
            }
            return new HttpPayloadDto(payload.method(), payload.uri(), payload.status(), payload.headers(), payload.body());
        }
    }

    public static LogDetailDto from(LogDetail detail) {
        return new LogDetailDto(
            detail.requestId(),
            detail.apiId(),
            detail.transactionId(),
            detail.clientIdentifier(),
            detail.timestamp(),
            detail.requestEnded(),
            detail.method(),
            detail.uri(),
            detail.status(),
            detail.endpoint(),
            detail.host(),
            detail.planId(),
            detail.planName(),
            detail.applicationId(),
            detail.applicationName(),
            detail.gateway(),
            detail.gatewayHostname(),
            detail.gatewayIp(),
            detail.remoteAddress(),
            detail.requestContentLength(),
            detail.responseContentLength(),
            detail.gatewayLatency(),
            detail.gatewayResponseTime(),
            detail.endpointResponseTime(),
            detail.message(),
            detail.errorKey(),
            detail.errorComponentName(),
            detail.errorComponentType(),
            detail.warnings() != null ? detail.warnings().stream().map(LogEntryDto.WarningDto::from).toList() : null,
            detail.additionalMetrics(),
            HttpPayloadDto.from(detail.entrypointRequest()),
            HttpPayloadDto.from(detail.entrypointResponse()),
            HttpPayloadDto.from(detail.endpointRequest()),
            HttpPayloadDto.from(detail.endpointResponse())
        );
    }
}
