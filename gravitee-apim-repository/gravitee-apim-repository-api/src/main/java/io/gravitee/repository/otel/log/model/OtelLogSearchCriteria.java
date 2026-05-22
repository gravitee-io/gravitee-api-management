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
package io.gravitee.repository.otel.log.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter passed to {@link io.gravitee.repository.otel.log.api.OtelLogRepository#findLogs}. Mirrors
 * {@link io.gravitee.repository.tracing.model.TraceSearchCriteria} so the two OTel SPIs share the same
 * shape — the dimensions are the ones the OTel logs data model itself exposes.
 * <ul>
 *   <li>{@code traceId} — distinguished top-level field in the OTel logs data model (not an attribute);
 *       optional so the SPI serves both "fetch all logs for trace X" and "browse logs for API Y over
 *       time" use cases.</li>
 *   <li>{@code attributeFilters} — per-record attribute filters ({@code LogRecord.attributes.*}). Use for
 *       attributes set on the individual log event: {@code event.name} (span events),
 *       {@code http.status_code} (payload logs), …</li>
 *   <li>{@code resourceAttributeFilters} — resource-attribute filters ({@code Resource.attributes.*}).
 *       Use for properties set once per producer process: service identity, environment, deployment
 *       module, our {@code gravitee.api.id} / {@code gravitee.env.id} / {@code gravitee.module}.</li>
 *   <li>{@code start} / {@code end} — half-open {@code @timestamp} window, both optional.</li>
 *   <li>{@code limit} — hard cap on records returned. Defaulted to 5000 because a single
 *       verbose-tracing trace can emit hundreds of span events plus request+response payloads per
 *       acceptor; 5000 covers it without paging while still bounding worst case.</li>
 * </ul>
 * Separating record-vs-resource attributes avoids the impl having to guess (by prefix or hardcoded key
 * list) where each entry belongs, and lines up with how OTel backends actually store attributes.
 *
 * @author GraviteeSource Team
 */
public record OtelLogSearchCriteria(
    String traceId,
    Map<String, String> attributeFilters,
    Map<String, String> resourceAttributeFilters,
    Instant start,
    Instant end,
    Integer limit
) {
    public OtelLogSearchCriteria {
        if (limit == null || limit <= 0) {
            limit = 5000;
        }
        if (attributeFilters == null) {
            attributeFilters = new HashMap<>();
        }
        if (resourceAttributeFilters == null) {
            resourceAttributeFilters = new HashMap<>();
        }
    }
}
