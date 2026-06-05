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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.gamma.rest.core.tracing.model.PayloadLog;
import io.gravitee.gamma.rest.core.tracing.model.SpanEvent;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.OtelLogPort;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.otel.log.api.OtelLogRepository;
import io.gravitee.repository.otel.log.model.OtelLogRecord;
import io.gravitee.repository.otel.log.model.OtelLogSearchCriteria;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Wraps the platform-level {@code OtelLogRepository} SPI for the core layer. Partitions the unified log
 * stream into span events vs. payload logs on the OTel-standard {@code event.name} attribute — present
 * means span event (the OTel logs data model "Event" convention), absent means payload log written by
 * {@code gravitee-reporter-otel}. The SPI bean is always present in the parent context (real impl or
 * the no-op fallback from {@code gravitee-apim-repository-noop} via {@code type=none}), so no optional
 * injection is needed here.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class OtelLogPortAdapter implements OtelLogPort {

    /** OTel logs data model attribute key that flags a log record as a span event. */
    private static final String EVENT_NAME_ATTRIBUTE = "event.name";

    private final OtelLogRepository otelLogRepository;

    @Override
    public TraceLogs findLogs(String orgId, String envId, String traceId, Map<String, String> attributeFilters) {
        QueryContext queryContext = new QueryContext(orgId, envId);
        // Pin to this trace + apply the per-record scope; let the SPI's default record cap (5000)
        // bound the result. Time range isn't useful at the trace-detail granularity. The caller
        // (GetTraceDetailUseCase) decides which keys go where — env / api / … land in the record-
        // attribute slot here because that's where gravitee-reporter-otel stamps them.
        OtelLogSearchCriteria criteria = new OtelLogSearchCriteria(traceId, attributeFilters, Map.of(), null, null, null);

        List<SpanEvent> events = new ArrayList<>();
        List<PayloadLog> payloadLogs = new ArrayList<>();
        for (OtelLogRecord record : otelLogRepository.findLogs(queryContext, criteria).blockingGet()) {
            String eventName = record.attributes().get(EVENT_NAME_ATTRIBUTE);
            if (eventName != null) {
                events.add(new SpanEvent(record.spanId(), eventName, record.timestamp(), record.attributes()));
            } else {
                payloadLogs.add(new PayloadLog(record.spanId(), record.timestamp(), record.severity(), record.body(), record.attributes()));
            }
        }
        return new TraceLogs(events, payloadLogs);
    }
}
