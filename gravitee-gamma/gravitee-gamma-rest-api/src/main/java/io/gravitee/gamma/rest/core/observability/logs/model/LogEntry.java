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
 * Light log row from the {@code v4-metrics} index — carries per-request metadata but no HTTP
 * bodies/headers (those live in the heavy detail endpoint, GMA-424). Fields are enriched with
 * display names (api, plan, application, gateway, apiProduct) so the table can render without
 * additional round-trips.
 *
 * @author GraviteeSource Team
 */
@Builder(toBuilder = true)
public record LogEntry(
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
    List<LogEntryWarning> warnings,
    Map<String, Object> additionalMetrics,
    String mcpMethod,
    String apiProductId,
    String apiProductName
) {}
