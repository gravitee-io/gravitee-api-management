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
package io.gravitee.apim.infra.query_service.tracing;

import io.gravitee.apim.core.tracing.model.Trace;
import io.gravitee.apim.core.tracing.model.TraceEvent;
import io.gravitee.apim.core.tracing.model.TraceSearchCriteria;
import io.gravitee.apim.core.tracing.model.TraceSpan;
import io.gravitee.apim.core.tracing.query_service.TracingQueryService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TempoTracingQueryService implements TracingQueryService {

    private final TempoHttpClient tempoClient;

    public TempoTracingQueryService(@Value("${tracing.tempo.url:http://localhost:3200}") String tempoBaseUrl) {
        this.tempoClient = new TempoHttpClient(tempoBaseUrl);
    }

    @Override
    public List<Trace> searchTraces(TraceSearchCriteria criteria) {
        String traceQL = buildTraceQL(criteria.tags(), false);
        String errorTraceQL = buildTraceQL(criteria.tags(), true);
        Long start = criteria.start();
        Long end = criteria.end();
        if (start == null && end != null) {
            start = end - 7 * 24 * 3600L;
        }

        TempoSearchResponse response = tempoClient.searchTracesTraceQL(traceQL, criteria.limit(), start, end);
        // Second pass: find trace IDs with at least one ERROR span in the same window; used to flag the list rows.
        Set<String> errorTraceIds = new HashSet<>();
        try {
            TempoSearchResponse errorResponse = tempoClient.searchTracesTraceQL(errorTraceQL, criteria.limit(), start, end);
            if (errorResponse.traces() != null) {
                for (TempoSearchResponse.TraceResult r : errorResponse.traces()) {
                    errorTraceIds.add(r.traceID());
                }
            }
        } catch (RuntimeException ignored) {
            // Older Tempo builds may not support `status = error`; fall back to unknown error state rather than failing the list.
        }

        List<Trace> traces = new ArrayList<>();
        if (response.traces() != null) {
            for (TempoSearchResponse.TraceResult result : response.traces()) {
                traces.add(
                    new Trace(
                        result.traceID(),
                        Instant.ofEpochMilli(result.startTimeUnixNano() / 1_000_000),
                        result.durationMs() * 1_000_000,
                        result.rootServiceName(),
                        deriveRootOperation(result),
                        errorTraceIds.contains(result.traceID()),
                        List.of()
                    )
                );
            }
        }
        return traces;
    }

    /**
     * Prefer the inbound HTTP SERVER span's {@code http.target} (+ {@code http.method}) over the raw {@code rootTraceName} returned by
     * Tempo. Some OTel instrumentations build the span name from {@code http.route}, which can be mutated by policies such as
     * http-callout and therefore surface the wrong path (e.g. "POST /callout2" instead of "POST /proxy"). Requires the TraceQL
     * {@code select(span.http.target, span.http.method)} projection so the attributes ride along with each search result.
     */
    private String deriveRootOperation(TempoSearchResponse.TraceResult result) {
        if (result.spanSet() != null && result.spanSet().spans() != null) {
            for (TempoSearchResponse.Span span : result.spanSet().spans()) {
                String target = null;
                String method = null;
                if (span.attributes() != null) {
                    for (TempoSearchResponse.KeyValue kv : span.attributes()) {
                        if ("http.target".equals(kv.key()) && kv.value() != null) target = kv.value().asString();
                        else if ("http.method".equals(kv.key()) && kv.value() != null) method = kv.value().asString();
                    }
                }
                if (target != null) {
                    return method != null ? method + " " + target : target;
                }
            }
        }
        return result.rootTraceName();
    }

    @Override
    public Optional<Trace> getTrace(String traceId) {
        TempoTraceResponse response = tempoClient.getTrace(traceId);
        if (response == null || response.batches() == null || response.batches().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(convertToTrace(traceId, response));
    }

    private Trace convertToTrace(String traceId, TempoTraceResponse response) {
        List<TraceSpan> allSpans = new ArrayList<>();
        String rootService = null;
        String rootOperation = null;
        Instant startTime = null;
        long maxEndTime = 0;

        for (TempoTraceResponse.ResourceSpans batch : response.batches()) {
            String serviceName = extractServiceName(batch.resource());
            if (batch.scopeSpans() == null) continue;

            for (TempoTraceResponse.ScopeSpans scopeSpans : batch.scopeSpans()) {
                if (scopeSpans.spans() == null) continue;

                for (TempoTraceResponse.Span span : scopeSpans.spans()) {
                    TraceSpan traceSpan = convertSpan(span, serviceName);
                    allSpans.add(traceSpan);

                    if (span.parentSpanId() == null || span.parentSpanId().isEmpty()) {
                        rootService = serviceName;
                        rootOperation = span.name();
                        startTime = traceSpan.startTime();
                    }

                    long endNanos = Long.parseLong(span.endTimeUnixNano());
                    if (endNanos > maxEndTime) {
                        maxEndTime = endNanos;
                    }
                }
            }
        }

        long durationNanos = 0;
        if (startTime != null) {
            long startNanos = startTime.getEpochSecond() * 1_000_000_000 + startTime.getNano();
            durationNanos = maxEndTime - startNanos;
        }

        boolean hasError = allSpans.stream().anyMatch(s -> "ERROR".equals(s.attributes().get("otel.status_code")));

        // Prefer http.target (+ http.method) from the inbound server span over the raw span name, which some instrumentations
        // derive from http.route and that gets mutated by http-callout-style policies.
        TraceSpan serverSpan = allSpans
            .stream()
            .filter(s ->
                s.attributes().containsKey("http.method") &&
                !s.attributes().containsKey("http.url") &&
                !s.attributes().containsKey("Invoker") &&
                (s.attributes().containsKey("client.address") ||
                    s.attributes().containsKey("net.host.name") ||
                    s.attributes().containsKey("http.target"))
            )
            .min((a, b) -> Long.compare(a.startTime().toEpochMilli(), b.startTime().toEpochMilli()))
            .orElse(null);
        if (serverSpan != null) {
            String target = serverSpan.attributes().get("http.target");
            String method = serverSpan.attributes().get("http.method");
            if (target != null) {
                rootOperation = method != null ? method + " " + target : target;
            }
        }

        return new Trace(traceId, startTime, durationNanos, rootService, rootOperation, hasError, allSpans);
    }

    private TraceSpan convertSpan(TempoTraceResponse.Span span, String serviceName) {
        Map<String, String> attrs = new HashMap<>();
        if (span.attributes() != null) {
            for (TempoTraceResponse.KeyValue kv : span.attributes()) {
                String value = kv.value() != null ? kv.value().asString() : null;
                if (value != null) {
                    attrs.put(kv.key(), value);
                }
            }
        }

        // OTLP status codes: 0=UNSET, 1=OK, 2=ERROR. Tempo serialises them either as the numeric index ("0"/"1"/"2") or as the proto
        // enum name ("STATUS_CODE_UNSET"/"STATUS_CODE_OK"/"STATUS_CODE_ERROR") depending on the version, so both forms are normalised.
        if (span.status() != null && span.status().code() != null) {
            String statusCode = switch (span.status().code()) {
                case "0", "STATUS_CODE_UNSET" -> "UNSET";
                case "1", "STATUS_CODE_OK" -> "OK";
                case "2", "STATUS_CODE_ERROR" -> "ERROR";
                default -> span.status().code();
            };
            attrs.put("otel.status_code", statusCode);
        }

        long startNanos = Long.parseLong(span.startTimeUnixNano());
        long endNanos = Long.parseLong(span.endTimeUnixNano());
        long durationNanos = endNanos - startNanos;

        Instant startTime = Instant.ofEpochSecond(startNanos / 1_000_000_000, startNanos % 1_000_000_000);

        List<TraceEvent> events = convertEvents(span.events());

        return TraceSpan.of(
            span.traceId(),
            span.spanId(),
            span.parentSpanId(),
            span.name(),
            serviceName,
            startTime,
            durationNanos,
            attrs,
            events
        );
    }

    private List<TraceEvent> convertEvents(List<TempoTraceResponse.Event> rawEvents) {
        if (rawEvents == null || rawEvents.isEmpty()) {
            return List.of();
        }
        List<TraceEvent> events = new ArrayList<>(rawEvents.size());
        for (TempoTraceResponse.Event rawEvent : rawEvents) {
            Map<String, String> eventAttrs = new LinkedHashMap<>();
            if (rawEvent.attributes() != null) {
                for (TempoTraceResponse.KeyValue kv : rawEvent.attributes()) {
                    String value = kv.value() != null ? kv.value().asString() : null;
                    if (value != null) {
                        eventAttrs.put(kv.key(), value);
                    }
                }
            }
            Instant eventTime = null;
            if (rawEvent.timeUnixNano() != null) {
                long nanos = Long.parseLong(rawEvent.timeUnixNano());
                eventTime = Instant.ofEpochSecond(nanos / 1_000_000_000, nanos % 1_000_000_000);
            }
            events.add(new TraceEvent(rawEvent.name(), eventTime, eventAttrs));
        }
        return events;
    }

    /**
     * Turn a logfmt-style tag filter into TraceQL. {@code service.*} and {@code telemetry.*} attributes go on the resource, everything
     * else on the span. Multiple keys are joined with {@code &&}. When {@code errorsOnly} is true, a {@code status = error} intrinsic is
     * added so only traces with at least one errored span match.
     */
    private String buildTraceQL(String tags, boolean errorsOnly) {
        StringBuilder sb = new StringBuilder("{ ");
        boolean hasTag = tags != null && !tags.isBlank();
        if (hasTag) {
            String[] parts = tags.split("\\s*&&\\s*");
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(" && ");
                String[] kv = parts[i].split("=", 2);
                String key = kv[0].trim();
                String prefix = isResourceAttribute(key) ? "resource." : "span.";
                sb.append(prefix).append(key).append(" = \"").append(kv[1].trim()).append("\"");
            }
        }
        if (errorsOnly) {
            if (hasTag) sb.append(" && ");
            sb.append("status = error");
        }
        // select(...) projects span attributes onto each trace result's spanSet so the list can show http.method + http.target
        // instead of rootTraceName (which is derived from http.route and can be corrupted by callout-style policies).
        sb.append(" } | select(span.http.target, span.http.method) with (most_recent=true)");
        return sb.toString();
    }

    private boolean isResourceAttribute(String key) {
        return key.startsWith("service.") || key.startsWith("telemetry.");
    }

    private String extractServiceName(TempoTraceResponse.Resource resource) {
        if (resource != null && resource.attributes() != null) {
            for (TempoTraceResponse.KeyValue kv : resource.attributes()) {
                if ("service.name".equals(kv.key()) && kv.value() != null) {
                    return kv.value().asString();
                }
            }
        }
        return "unknown";
    }
}
