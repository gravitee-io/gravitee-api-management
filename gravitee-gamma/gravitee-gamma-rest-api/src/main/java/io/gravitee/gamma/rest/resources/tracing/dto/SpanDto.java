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
package io.gravitee.gamma.rest.resources.tracing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.gamma.rest.core.tracing.model.Span;
import java.util.List;
import java.util.Map;

/**
 * Span in the detail view, carrying its already-stitched events and payload logs. Renders directly
 * into the trace timeline UI without additional client-side joining.
 *
 * <p>{@code traceId} is duplicated on every span — same value as the envelope's {@code traceId} —
 * because every OTel transport (OTLP proto, ES OTel-mode docs, Tempo span JSON) carries it per-span,
 * and the lib's canonical {@code TraceSpan} type expects it. The repetition cost is negligible
 * (~32 bytes per span); the alternative (lib adapter injecting from envelope) fights the OTel model.
 *
 * <p>{@code endTime} is intentionally not emitted: it's derivable as
 * {@code startTimeEpochMs + durationNanos / 1_000_000}. {@code durationNanos} is kept at nanosecond
 * precision because fast spans (sub-millisecond service-mesh ops, validation hooks) are common and
 * ms precision would round them all to 0.
 *
 * <p>{@code status} and {@code kind} are emitted lowercase (e.g. {@code "ok"}, {@code "server"}) —
 * matches the canonical {@code @gravitee/gamma-lib-observability TraceSpan} unions.
 * {@code parentSpanId} is omitted from the JSON when null (lib's field is optional; absence marks
 * the trace root).
 *
 * <p>{@code attributes} on the wire are string-only ({@code Map<String, String>}) — the OTel
 * collector ES exporter writes typed values (numbers, booleans, arrays) but our adapter currently
 * stringifies them at read time. The lib's {@code AttributeValue} union is broader; the lossy
 * mapping is documented in {@code TRACING_API.md} and tracked as a follow-up.
 */
public record SpanDto(
    String traceId,
    String spanId,
    @JsonInclude(JsonInclude.Include.NON_NULL) String parentSpanId,
    String operationName,
    String serviceName,
    long startTimeEpochMs,
    long durationNanos,
    String status,
    String kind,
    Map<String, String> attributes,
    List<SpanEventDto> events,
    List<PayloadLogDto> payloadLogs
) {
    public static SpanDto from(String traceId, Span span) {
        return new SpanDto(
            traceId,
            span.spanId(),
            span.parentSpanId(),
            span.operationName(),
            span.serviceName(),
            span.startTime() != null ? span.startTime().toEpochMilli() : 0L,
            span.durationNanos(),
            span.status().name().toLowerCase(),
            span.kind().name().toLowerCase(),
            span.attributes(),
            span.events().stream().map(SpanEventDto::from).toList(),
            span.payloadLogs().stream().map(PayloadLogDto::from).toList()
        );
    }
}
