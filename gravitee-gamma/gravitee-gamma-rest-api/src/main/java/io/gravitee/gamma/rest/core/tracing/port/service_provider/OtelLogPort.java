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

import io.gravitee.gamma.rest.core.tracing.model.PayloadLog;
import io.gravitee.gamma.rest.core.tracing.model.SpanEvent;
import java.util.List;
import java.util.Map;

/**
 * Core-side port for reading OTel log records (span events + gateway payload logs). The infra adapter
 * delegates to the platform-level {@code OtelLogRepository} SPI but exposes a blocking, partitioned
 * signature: events vs. payload logs are split client-side here so use cases don't need to know about
 * the {@code event.name} discriminator.
 *
 * @author GraviteeSource Team
 */
public interface OtelLogPort {
    /**
     * Fetches everything in the logs data stream for the given trace, then splits the result on the
     * presence of the OTel {@code event.name} attribute — present → {@link SpanEvent}, absent →
     * {@link PayloadLog}. Returns empty lists when the trace has nothing (or the backing data stream
     * isn't configured — the SPI degrades to empty rather than erroring so the trace's spans still
     * render).
     */
    TraceLogs findLogs(String orgId, String envId, String traceId, Map<String, String> resourceAttributeFilters);

    /** Container for the partitioned result so the port returns both shapes in one call. */
    record TraceLogs(List<SpanEvent> events, List<PayloadLog> payloadLogs) {}
}
