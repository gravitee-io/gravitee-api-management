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
package io.gravitee.gamma.rest.core.tracing.inmemory;

import io.gravitee.gamma.rest.core.tracing.model.PayloadLog;
import io.gravitee.gamma.rest.core.tracing.model.SpanEvent;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.OtelLogPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory {@link OtelLogPort} for domain tests. Seed events / payload logs per traceId; the use case
 * fetches them with {@link #findLogs(String, String, String, Map)} and stitches them onto matching
 * spans by spanId. Unknown trace ids return empty lists (matching the prod adapter's degraded mode).
 */
public class InMemoryOtelLogPort implements OtelLogPort {

    private final Map<String, List<SpanEvent>> eventsByTraceId = new HashMap<>();
    private final Map<String, List<PayloadLog>> payloadsByTraceId = new HashMap<>();

    public void givenEvents(String traceId, List<SpanEvent> events) {
        eventsByTraceId.put(traceId, new ArrayList<>(events));
    }

    public void givenPayloadLogs(String traceId, List<PayloadLog> payloadLogs) {
        payloadsByTraceId.put(traceId, new ArrayList<>(payloadLogs));
    }

    public void reset() {
        eventsByTraceId.clear();
        payloadsByTraceId.clear();
    }

    @Override
    public TraceLogs findLogs(String orgId, String envId, String traceId, Map<String, String> resourceAttributeFilters) {
        return new TraceLogs(eventsByTraceId.getOrDefault(traceId, List.of()), payloadsByTraceId.getOrDefault(traceId, List.of()));
    }
}
