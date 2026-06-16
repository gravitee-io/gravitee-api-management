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
package io.gravitee.gamma.rest.core.observability.logs.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * Merged log detail combining enriched metadata from the {@code v4-metrics} index and HTTP
 * payloads (headers + body) from the {@code v4-log} index. Returned by
 * {@code GET /observability/logs/{requestId}?apiId=}.
 *
 * <p>Fields mirror the union of today's two per-API management-v2 detail endpoints
 * ({@code /apis/{apiId}/logs/{requestId}} and {@code /apis/{apiId}/analytics/{requestId}}) so the
 * frontend can drop the dual-fetch workaround and load the full record in a single lazy call.
 *
 * @author GraviteeSource Team
 */
@Builder
public record LogDetail(
    // Identifiers
    String requestId,
    String apiId,
    String transactionId,
    String clientIdentifier,
    Instant timestamp,
    Boolean requestEnded,
    // Enriched metadata (v4-metrics)
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
    List<LogEntryWarning> warnings,
    Map<String, Object> additionalMetrics,
    // HTTP payloads (v4-log)
    HttpPayload entrypointRequest,
    HttpPayload entrypointResponse,
    HttpPayload endpointRequest,
    HttpPayload endpointResponse
) {}
