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
package io.gravitee.gamma.rest.core.tracing.port.service_provider;

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.tracing.model.Span;
import io.gravitee.gamma.rest.core.tracing.model.Trace;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core-side port for reading traces. The infra adapter delegates to the platform-level
 * {@code TracingRepository} SPI but exposes a blocking, framework-free signature so use cases can stay
 * unaware of RxJava / repository internals.
 *
 * @author GraviteeSource Team
 */
public interface TracingPort {
    /**
     * Lists traces matching the criteria, ordered by start time descending, paginated.
     * {@code resourceAttributeFilters} is the scope envelope ({@code gravitee.module},
     * {@code gravitee.env.id}, …) — the impl emits each entry as a resource-attribute filter against
     * the tracing backend so traces from another env / module are excluded server-side.
     */
    Page<Trace> searchTraces(
        String orgId,
        String envId,
        Map<String, String> resourceAttributeFilters,
        Map<String, String> attributeFilters,
        Instant start,
        Instant end,
        int page,
        int perPage
    );

    /**
     * Returns the spans for a single trace, or {@link Optional#empty()} if the trace doesn't exist (or the
     * resource scope rejected it). Span events / payload logs are NOT fetched here — they live behind
     * {@link OtelLogPort#findLogs} and the use case stitches them on the spans.
     */
    Optional<List<Span>> getTraceSpans(String orgId, String envId, String traceId, Map<String, String> resourceAttributeFilters);
}
