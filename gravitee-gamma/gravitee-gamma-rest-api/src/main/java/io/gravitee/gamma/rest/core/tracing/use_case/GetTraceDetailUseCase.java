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
package io.gravitee.gamma.rest.core.tracing.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.tracing.TraceScopeFilters;
import io.gravitee.gamma.rest.core.tracing.model.PayloadLog;
import io.gravitee.gamma.rest.core.tracing.model.Span;
import io.gravitee.gamma.rest.core.tracing.model.SpanEvent;
import io.gravitee.gamma.rest.core.tracing.model.SpanStatus;
import io.gravitee.gamma.rest.core.tracing.model.TraceDetail;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.OtelLogPort;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TracingPort;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

/**
 * Fetches a trace's spans from the tracing repository, fetches its OTel log records from the log
 * repository, then stitches events / payload logs onto the matching spans by {@code spanId}. Returns
 * an empty Optional if the trace doesn't exist (or the env-scope rejected it).
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetTraceDetailUseCase {

    private final TracingPort tracingPort;
    private final OtelLogPort otelLogPort;

    public record Input(
        String organizationId,
        String environmentId,
        // Required — same reasoning as SearchTracesUseCase.Input.apiId. Also defends against trace-id
        // guessing: a user with access to API A can't fetch trace details from API B by knowing its id,
        // because the resource-attribute filter on gravitee.api.id rejects spans from the other API
        // server-side (the endpoint returns 404, collapsing "doesn't exist" and "wrong scope").
        String apiId,
        String traceId
    ) {}

    public record Output(Optional<TraceDetail> trace) {}

    public Output execute(Input input) {
        // Same scope envelope (env + api) goes to both signals, but each port interprets it
        // differently — the per-API tracer emits Gravitee attributes on the OTel resource, while
        // gravitee-reporter-otel stamps them on each log record's attributes map (only
        // gravitee.org.id is on the per-org Logger's resource).
        Map<String, String> traceResourceFilters = TraceScopeFilters.forApi(input.environmentId, input.apiId);
        Map<String, String> logAttributeFilters = TraceScopeFilters.forApi(input.environmentId, input.apiId);

        Optional<List<Span>> spansOpt = tracingPort.getTraceSpans(
            input.organizationId,
            input.environmentId,
            input.traceId,
            traceResourceFilters
        );
        if (spansOpt.isEmpty()) {
            return new Output(Optional.empty());
        }
        OtelLogPort.TraceLogs logs = otelLogPort.findLogs(input.organizationId, input.environmentId, input.traceId, logAttributeFilters);

        // Group by spanId once, then look up per span — O(events + spans) instead of nested-loop O(n*m).
        Map<String, List<SpanEvent>> eventsBySpan = logs.events().stream().collect(Collectors.groupingBy(SpanEvent::spanId));
        Map<String, List<PayloadLog>> payloadsBySpan = logs.payloadLogs().stream().collect(Collectors.groupingBy(PayloadLog::spanId));

        List<Span> stitchedSpans = new ArrayList<>(spansOpt.get().size());
        for (Span span : spansOpt.get()) {
            stitchedSpans.add(
                new Span(
                    span.spanId(),
                    span.parentSpanId(),
                    span.operationName(),
                    span.serviceName(),
                    span.startTime(),
                    span.durationNanos(),
                    span.status(),
                    span.kind(),
                    span.attributes(),
                    sortByTimestamp(eventsBySpan.getOrDefault(span.spanId(), List.of()), SpanEvent::timestamp),
                    sortByTimestamp(payloadsBySpan.getOrDefault(span.spanId(), List.of()), PayloadLog::timestamp)
                )
            );
        }

        Span rootSpan = pickRootSpan(stitchedSpans);
        // Trace-level status uses the same UNSET/OK/ERROR vocabulary as a span: any ERROR wins; else fall back to
        // the root span's own status. Keeps the wire status field useful for "is this trace healthy?" filtering
        // without forcing the consumer to walk every span.
        SpanStatus traceStatus;
        if (stitchedSpans.stream().anyMatch(s -> s.status() == SpanStatus.ERROR)) {
            traceStatus = SpanStatus.ERROR;
        } else {
            traceStatus = rootSpan != null ? rootSpan.status() : SpanStatus.UNSET;
        }
        TraceDetail detail = new TraceDetail(
            input.traceId,
            rootSpan != null ? rootSpan.startTime() : null,
            rootSpan != null ? rootSpan.durationNanos() : 0L,
            rootSpan != null ? rootSpan.serviceName() : null,
            rootSpan != null ? rootSpan.operationName() : null,
            traceStatus,
            stitchedSpans.size(),
            stitchedSpans
        );
        return new Output(Optional.of(detail));
    }

    /**
     * Root span = the one without a parent. Falls back to the earliest-starting span if no clear root
     * exists (e.g. the trace's root span wasn't ingested) so the summary fields still resolve.
     */
    private static Span pickRootSpan(List<Span> spans) {
        return spans
            .stream()
            .filter(s -> s.parentSpanId() == null || s.parentSpanId().isEmpty())
            .findFirst()
            .orElseGet(() ->
                spans.stream().min(Comparator.comparing(Span::startTime, Comparator.nullsLast(Comparator.naturalOrder()))).orElse(null)
            );
    }

    private static <T> List<T> sortByTimestamp(List<T> items, java.util.function.Function<T, java.time.Instant> timestampOf) {
        return items.stream().sorted(Comparator.comparing(timestampOf, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }
}
