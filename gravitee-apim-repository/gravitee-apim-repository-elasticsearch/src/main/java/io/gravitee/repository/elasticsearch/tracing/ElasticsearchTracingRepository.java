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
package io.gravitee.repository.elasticsearch.tracing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.utils.IndexNameUtils;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.tracing.api.TracingRepository;
import io.gravitee.repository.tracing.model.Trace;
import io.gravitee.repository.tracing.model.TraceSearchCriteria;
import io.gravitee.repository.tracing.model.TraceSpan;
import io.gravitee.repository.tracing.model.TraceSpanEvent;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;

/**
 * Elasticsearch-backed {@link TracingRepository} reading spans written by the OpenTelemetry Collector's
 * {@code elasticsearch} exporter in {@code mapping.mode: otel} (snake_case fields: {@code trace_id},
 * {@code span_id}, {@code parent_span_id}, {@code @timestamp}, {@code duration}, {@code status.code},
 * {@code attributes.*}, {@code resource.attributes.*}, {@code events[]}).
 * <p>
 * Tenancy: the configured index template (default {@code traces-apim.otel-{orgId}}) is resolved against the caller's
 * {@link QueryContext} before each search — operators are expected to point the OTel collector at the matching
 * per-tenant index/data stream.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class ElasticsearchTracingRepository implements TracingRepository {

    private static final int MAX_SPANS_PER_TRACE = 1000;

    /**
     * Hard cap on the trace-id terms aggregation size. ES {@code search.max_buckets} defaults to 10 000 and a
     * caller-supplied {@code criteria.limit()} larger than that returns a {@code circuit_breaking_exception} from
     * the coordinator. 200 keeps the UI's listing fast while staying well under the bucket limit.
     */
    private static final int MAX_SEARCH_RESULTS = 200;

    /** Span-attribute key set by both backends so callers don't have to know about backend-specific status fields. */
    private static final String OTEL_STATUS_CODE_ATTR = "otel.status_code";

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private final String indexTemplate;
    private final Client client;

    public ElasticsearchTracingRepository(String indexTemplate, Client client) {
        this.indexTemplate = indexTemplate;
        this.client = client;
    }

    @Override
    public Single<List<Trace>> searchTraces(QueryContext queryContext, TraceSearchCriteria criteria) {
        String index = resolveIndex(indexTemplate, queryContext);
        String body = buildSearchTracesBody(criteria);
        return client.search(index, null, body).map(ElasticsearchTracingRepository::parseSearchTracesResponse);
    }

    @Override
    public Maybe<Trace> getTrace(QueryContext queryContext, String traceId, Map<String, String> resourceAttributeFilters) {
        Map<String, String> safeFilters = resourceAttributeFilters == null ? Map.of() : resourceAttributeFilters;
        String index = resolveIndex(indexTemplate, queryContext);
        String body = buildGetTraceBody(traceId, safeFilters);
        // Span events live in a separate OTel "logs" data stream (the collector elasticsearch exporter forces
        // {@code data_stream.type = logs} for events regardless of the pipeline they arrived on). Fetching and
        // stitching them is the LogsRepository's job — handled in a follow-up PR; this repo only returns spans.
        return client
            .search(index, null, body)
            .flatMapMaybe(response -> {
                Trace trace = parseGetTraceResponse(traceId, response);
                return trace == null ? Maybe.empty() : Maybe.just(trace);
            });
    }

    private String resolveIndex(String template, QueryContext queryContext) {
        // Mirrors the analytics plugin's index-naming convention: IndexNameUtils.format substitutes
        // {orgId} / {envId} with the lowercased values from QueryContext.placeholder(). ES data streams
        // require lowercase names — the OTel collector writes to lowercase too, so a caller orgId of
        // "DEFAULT" resolves to traces-apim.otel-default, not -DEFAULT.
        return IndexNameUtils.format(template, queryContext.placeholder());
    }

    /**
     * Composes a {@code _search} body that:
     * <ul>
     *   <li>filters by time window + attribute filters + resource-attribute filters (env, module, …);</li>
     *   <li>aggregates by {@code trace_id} (terms agg) ordered by min start time desc, sized to the criteria limit;</li>
     *   <li>nests a {@code top_hits} sub-agg sorted parent-first to grab the root span's summary fields,
     *       a {@code min(@timestamp)} for the trace start, and a {@code filter} agg counting error-status spans.</li>
     * </ul>
     */
    static String buildSearchTracesBody(TraceSearchCriteria criteria) {
        ObjectNode root = JSON.objectNode();
        root.put("size", 0);
        root.set("query", buildQuery(criteria));

        ObjectNode aggs = JSON.objectNode();
        ObjectNode tracesAgg = JSON.objectNode();
        ObjectNode termsAgg = JSON.objectNode();
        termsAgg.put("field", "trace_id");
        // Bound caller-supplied limit so a runaway value can't trip ES's max_buckets circuit breaker.
        termsAgg.put("size", Math.min(criteria.limit(), MAX_SEARCH_RESULTS));
        ObjectNode order = JSON.objectNode();
        order.put("trace_start", "desc");
        termsAgg.set("order", order);
        tracesAgg.set("terms", termsAgg);

        ObjectNode subAggs = JSON.objectNode();
        subAggs.set("trace_start", minAgg("@timestamp"));
        subAggs.set("root_span", rootSpanTopHitsAgg());
        subAggs.set("error_count", errorFilterAgg());
        tracesAgg.set("aggs", subAggs);

        aggs.set("traces", tracesAgg);
        root.set("aggs", aggs);
        return root.toString();
    }

    /**
     * Composes a {@code _search} body for fetch-by-id. The caller-declared {@code resourceAttributeFilters} are
     * emitted as resource-side {@code term} clauses so a caller in env A doesn't see a trace from env B even if
     * they know its id — defence in depth on top of the per-tenant index resolved upstream. An empty map means
     * "no resource scoping" (matches whatever document carries that trace id in the resolved index).
     */
    static String buildGetTraceBody(String traceId, Map<String, String> resourceAttributeFilters) {
        ObjectNode root = JSON.objectNode();
        root.put("size", MAX_SPANS_PER_TRACE);
        root.set("query", buildGetTraceQuery(traceId, resourceAttributeFilters));

        ArrayNode sort = JSON.arrayNode();
        ObjectNode sortField = JSON.objectNode();
        ObjectNode order = JSON.objectNode();
        order.put("order", "asc");
        sortField.set("@timestamp", order);
        sort.add(sortField);
        root.set("sort", sort);
        return root.toString();
    }

    private static ObjectNode buildGetTraceQuery(String traceId, Map<String, String> resourceAttributeFilters) {
        if (resourceAttributeFilters.isEmpty()) {
            ObjectNode query = JSON.objectNode();
            ObjectNode term = JSON.objectNode();
            term.put("trace_id", traceId);
            query.set("term", term);
            return query;
        }
        ArrayNode filters = JSON.arrayNode();
        ObjectNode traceIdTerm = JSON.objectNode();
        ObjectNode traceIdInner = JSON.objectNode();
        traceIdInner.put("trace_id", traceId);
        traceIdTerm.set("term", traceIdInner);
        filters.add(traceIdTerm);
        for (Map.Entry<String, String> entry : resourceAttributeFilters.entrySet()) {
            filters.add(buildResourceTermFilter(entry.getKey(), entry.getValue()));
        }
        ObjectNode bool = JSON.objectNode();
        bool.set("filter", filters);
        ObjectNode query = JSON.objectNode();
        query.set("bool", bool);
        return query;
    }

    private static ObjectNode buildQuery(TraceSearchCriteria criteria) {
        ObjectNode bool = JSON.objectNode();
        ArrayNode filter = JSON.arrayNode();

        if (criteria.start() != null || criteria.end() != null) {
            ObjectNode rangeFilter = JSON.objectNode();
            ObjectNode range = JSON.objectNode();
            ObjectNode timestampRange = JSON.objectNode();
            if (criteria.start() != null) {
                timestampRange.put("gte", criteria.start().toString());
            }
            if (criteria.end() != null) {
                timestampRange.put("lte", criteria.end().toString());
            }
            range.set("@timestamp", timestampRange);
            rangeFilter.set("range", range);
            filter.add(rangeFilter);
        }

        for (Map.Entry<String, String> tag : criteria.attributeFilters().entrySet()) {
            filter.add(buildTagTermFilter(tag.getKey(), tag.getValue()));
        }

        // Caller-declared resource-attribute filters are emitted on resource.attributes.<key> unconditionally —
        // no prefix heuristics or hardcoded key lists. Anything the caller says is a resource filter, is one.
        for (Map.Entry<String, String> entry : criteria.resourceAttributeFilters().entrySet()) {
            filter.add(buildResourceTermFilter(entry.getKey(), entry.getValue()));
        }

        if (filter.isEmpty()) {
            ObjectNode matchAll = JSON.objectNode();
            matchAll.set("match_all", JSON.objectNode());
            return matchAll;
        }
        bool.set("filter", filter);
        ObjectNode query = JSON.objectNode();
        query.set("bool", bool);
        return query;
    }

    private static ObjectNode buildResourceTermFilter(String key, String value) {
        ObjectNode termFilter = JSON.objectNode();
        ObjectNode term = JSON.objectNode();
        term.put("resource.attributes." + key, value);
        termFilter.set("term", term);
        return termFilter;
    }

    /**
     * Span-attribute filter — always targets {@code attributes.<key>}, no key-prefix heuristics. Callers that
     * need to match on {@code resource.attributes.*} (service identity, env, module, …) must declare those via
     * {@link TraceSearchCriteria#resourceAttributeFilters()}; mis-routing a {@code service.name} filter into the
     * span-attribute namespace would silently return zero results.
     */
    private static ObjectNode buildTagTermFilter(String key, String value) {
        ObjectNode termFilter = JSON.objectNode();
        ObjectNode term = JSON.objectNode();
        term.put("attributes." + key, value);
        termFilter.set("term", term);
        return termFilter;
    }

    private static ObjectNode minAgg(String field) {
        ObjectNode agg = JSON.objectNode();
        ObjectNode min = JSON.objectNode();
        min.put("field", field);
        agg.set("min", min);
        return agg;
    }

    private static ObjectNode rootSpanTopHitsAgg() {
        ObjectNode agg = JSON.objectNode();
        ObjectNode topHits = JSON.objectNode();
        topHits.put("size", 1);
        // Spans without a parent_span_id sort first, then by earliest @timestamp — covers the common case where a
        // trace has exactly one root and the rare case where a missing parent_span_id is omitted entirely.
        ArrayNode sort = JSON.arrayNode();
        ObjectNode parentSort = JSON.objectNode();
        ObjectNode parentSortOrder = JSON.objectNode();
        parentSortOrder.put("order", "asc");
        parentSortOrder.put("missing", "_first");
        parentSort.set("parent_span_id", parentSortOrder);
        sort.add(parentSort);
        ObjectNode timestampSort = JSON.objectNode();
        ObjectNode timestampSortOrder = JSON.objectNode();
        timestampSortOrder.put("order", "asc");
        timestampSort.set("@timestamp", timestampSortOrder);
        sort.add(timestampSort);
        topHits.set("sort", sort);
        agg.set("top_hits", topHits);
        return agg;
    }

    private static ObjectNode errorFilterAgg() {
        ObjectNode agg = JSON.objectNode();
        ObjectNode filter = JSON.objectNode();
        ObjectNode terms = JSON.objectNode();
        ArrayNode values = JSON.arrayNode();
        // The OTel ES exporter has shipped the status field as the short ("Error"), the proto-enum
        // ("STATUS_CODE_ERROR"), or the numeric ("2") form across versions; accept all three so the bucket
        // count stays correct regardless of which collector build the operator runs. Mirrors the per-span
        // normalisation in normalizeStatusCode.
        values.add("Error");
        values.add("STATUS_CODE_ERROR");
        values.add("2");
        terms.set("status.code", values);
        filter.set("terms", terms);
        agg.set("filter", filter);
        return agg;
    }

    private static List<Trace> parseSearchTracesResponse(SearchResponse response) {
        if (response.getAggregations() == null) {
            return List.of();
        }
        Aggregation tracesAgg = response.getAggregations().get("traces");
        if (tracesAgg == null || tracesAgg.getBuckets() == null) {
            return List.of();
        }

        List<Trace> traces = new ArrayList<>(tracesAgg.getBuckets().size());
        for (JsonNode bucket : tracesAgg.getBuckets()) {
            Trace trace = parseTraceBucket(bucket);
            if (trace != null) {
                traces.add(trace);
            }
        }
        return traces;
    }

    private static Trace parseTraceBucket(JsonNode bucket) {
        String traceId = textOrNull(bucket.get("key"));
        if (traceId == null) {
            return null;
        }

        Instant traceStart = parseInstant(bucket.path("trace_start").path("value_as_string"));

        JsonNode rootHits = bucket.path("root_span").path("hits").path("hits");
        if (!rootHits.isArray() || rootHits.isEmpty()) {
            return null;
        }
        JsonNode rootSource = rootHits.get(0).path("_source");

        String rootService = readResourceServiceName(rootSource);
        String rootOperation = textOrNull(rootSource.get("name"));
        long durationNanos = rootSource.path("duration").asLong(0L);

        long errorCount = bucket.path("error_count").path("doc_count").asLong(0L);
        // Trace-level rollup: any ERROR span wins; else fall back to the root span's normalized status (OK / UNSET).
        // Missing status on the root → UNSET, mirroring the per-span fallback in normalizeStatusCode.
        String status = errorCount > 0L ? "ERROR" : statusOrUnset(normalizeStatusCode(rootSource));
        // doc_count on a terms-agg bucket equals the number of documents (= spans) for that trace_id within the search
        // window. Exposed to the caller so a listing row can render "N spans" without a second round-trip.
        int spanCount = (int) bucket.path("doc_count").asLong(0L);

        return new Trace(traceId, traceStart, durationNanos, rootService, rootOperation, status, spanCount, List.of());
    }

    private static Trace parseGetTraceResponse(String traceId, SearchResponse response) {
        if (
            response.getSearchHits() == null || response.getSearchHits().getHits() == null || response.getSearchHits().getHits().isEmpty()
        ) {
            return null;
        }

        List<TraceSpan> spans = new ArrayList<>(response.getSearchHits().getHits().size());
        boolean anyError = false;
        TraceSpan rootSpan = null;
        for (SearchHit hit : response.getSearchHits().getHits()) {
            JsonNode source = hit.getSource();
            if (source == null) {
                continue;
            }
            TraceSpan span = parseSpan(source);
            spans.add(span);
            if (rootSpan == null && (span.parentSpanId() == null || span.parentSpanId().isEmpty())) {
                rootSpan = span;
            }
            if ("ERROR".equals(span.attributes().get(OTEL_STATUS_CODE_ATTR))) {
                anyError = true;
            }
        }

        if (rootSpan == null) {
            // No span has an empty parent — fall back to the earliest by start time so the trace summary still resolves.
            rootSpan = spans
                .stream()
                .min(Comparator.comparing(TraceSpan::startTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        }

        Instant traceStart = rootSpan != null ? rootSpan.startTime() : null;
        long durationNanos = rootSpan != null ? rootSpan.durationNanos() : 0L;
        String rootService = rootSpan != null ? rootSpan.serviceName() : null;
        String rootOperation = rootSpan != null ? rootSpan.operationName() : null;
        // Same trace-level rollup as the list path: any ERROR span wins; else root span's status drives OK / UNSET.
        String rootStatus = rootSpan != null ? rootSpan.attributes().get(OTEL_STATUS_CODE_ATTR) : null;
        String status = anyError ? "ERROR" : statusOrUnset(rootStatus);

        return new Trace(traceId, traceStart, durationNanos, rootService, rootOperation, status, spans.size(), spans);
    }

    private static String statusOrUnset(String raw) {
        return (raw == null || raw.isEmpty()) ? "UNSET" : raw;
    }

    private static TraceSpan parseSpan(JsonNode source) {
        String traceId = textOrNull(source.get("trace_id"));
        String spanId = textOrNull(source.get("span_id"));
        String parentSpanId = textOrNull(source.get("parent_span_id"));
        String operationName = textOrNull(source.get("name"));
        String serviceName = readResourceServiceName(source);
        Instant startTime = parseInstant(source.path("@timestamp"));
        long durationNanos = source.path("duration").asLong(0L);
        Map<String, String> attributes = flattenStringMap(source.path("attributes"), "");
        // Mirror the node-side Tempo impl: copy the OTLP status into a stable span attribute (UNSET/OK/ERROR) so a
        // UI / contract caller doesn't have to know about backend-specific status fields. The OTel collector
        // exporter writes status.code as either the short form ("Error") or the proto enum ("STATUS_CODE_ERROR"),
        // and either as a flat dotted key or a nested object — normalizeStatusCode handles both shapes.
        String statusCode = normalizeStatusCode(source);
        if (statusCode != null) {
            attributes.put(OTEL_STATUS_CODE_ATTR, statusCode);
        }
        List<TraceSpanEvent> events = parseEvents(source.path("events"));
        return TraceSpan.of(traceId, spanId, parentSpanId, operationName, serviceName, startTime, durationNanos, attributes, events);
    }

    /**
     * Reads the OTLP status code from an ES document in any of the formats the collector might write:
     * <ul>
     *   <li>nested object: {@code "status": {"code": "Error"}} (OTel mapping mode, recent versions)</li>
     *   <li>flat dotted key: {@code "status.code": "Error"} (some older / flattened mappings)</li>
     * </ul>
     * The value is normalised to the proto-enum-suffix form ({@code UNSET}/{@code OK}/{@code ERROR}) regardless of
     * whether the source carried the short form ({@code Error}) or the long form ({@code STATUS_CODE_ERROR}).
     * Returns {@code null} when the field is absent — callers should treat that as "unset / unknown".
     */
    private static String normalizeStatusCode(JsonNode source) {
        String raw = textOrNull(source.path("status").path("code"));
        if (raw == null) {
            raw = textOrNull(source.get("status.code"));
        }
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case "Error", "STATUS_CODE_ERROR", "2" -> "ERROR";
            case "Ok", "STATUS_CODE_OK", "1" -> "OK";
            case "Unset", "STATUS_CODE_UNSET", "0" -> "UNSET";
            default -> raw;
        };
    }

    private static List<TraceSpanEvent> parseEvents(JsonNode eventsNode) {
        if (!eventsNode.isArray() || eventsNode.isEmpty()) {
            // ArrayList (not List.of()) so a future LogsRepository pass can append events fetched from the
            // separate OTel logs data stream — the collector in mapping.mode: otel writes span events there
            // rather than inline, so the inline array is usually empty.
            return new ArrayList<>();
        }
        List<TraceSpanEvent> events = new ArrayList<>(eventsNode.size());
        for (JsonNode eventNode : eventsNode) {
            String name = textOrNull(eventNode.get("name"));
            // OTel mode writes the event time under "timestamp"; some collector versions also emit "time".
            Instant time = parseInstant(eventNode.path("timestamp"));
            if (time == null) {
                time = parseInstant(eventNode.path("time"));
            }
            Map<String, String> attributes = flattenStringMap(eventNode.path("attributes"), "");
            events.add(new TraceSpanEvent(name, time, attributes));
        }
        return events;
    }

    /**
     * OTel-mode documents store {@code attributes} either as a JSON object (newer collector versions) or as an
     * array of {@code {key, value}} pairs (older versions). This walks both into a flat key→string map; nested
     * objects are dot-flattened (e.g. {@code http.status_code = "200"}).
     */
    private static Map<String, String> flattenStringMap(JsonNode node, String prefix) {
        Map<String, String> result = new HashMap<>();
        if (node == null || node.isMissingNode() || node.isNull()) {
            return result;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                JsonNode value = entry.getValue();
                if (value.isObject()) {
                    result.putAll(flattenStringMap(value, key));
                } else if (!value.isNull()) {
                    result.put(key, value.asText());
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                JsonNode keyNode = element.get("key");
                JsonNode valueNode = element.get("value");
                if (keyNode == null || valueNode == null) {
                    continue;
                }
                String key = prefix.isEmpty() ? keyNode.asText() : prefix + "." + keyNode.asText();
                // OTLP/JSON shape: { "key": "...", "value": { "stringValue": "..." } | { "intValue": ... } | ... }
                JsonNode unwrapped = unwrapOtlpValue(valueNode);
                if (unwrapped != null && !unwrapped.isNull()) {
                    result.put(key, unwrapped.asText());
                }
            }
        }
        return result;
    }

    private static JsonNode unwrapOtlpValue(JsonNode valueNode) {
        if (valueNode.isObject()) {
            for (String typedField : List.of("stringValue", "intValue", "doubleValue", "boolValue")) {
                JsonNode typed = valueNode.get(typedField);
                if (typed != null && !typed.isNull()) {
                    return typed;
                }
            }
            return null;
        }
        return valueNode;
    }

    /**
     * The OTel collector's elasticsearch exporter (mapping.mode: otel) writes resource attributes as a flat map of
     * dotted keys, so {@code service.name} arrives at {@code resource.attributes["service.name"]} — i.e. the dot is
     * part of the JSON key, not a nesting boundary. Older nested-object encodings ({@code resource.attributes.service.name})
     * are still around in some pipelines, so check both shapes.
     */
    private static String readResourceServiceName(JsonNode source) {
        JsonNode flat = source.path("resource").path("attributes").get("service.name");
        if (flat != null && !flat.isMissingNode() && !flat.isNull()) {
            return flat.asText();
        }
        return textOrNull(source.path("resource").path("attributes").path("service").path("name"));
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private static Instant parseInstant(JsonNode node) {
        String text = textOrNull(node);
        if (text == null) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {
            // The OTel collector's elasticsearch exporter (otel mapping mode) writes @timestamp as
            // "<epoch-millis>.<sub-ms-digits>" (e.g. "1778515640168.675208") — epoch ms with sub-millisecond
            // fractional digits, NOT an ISO 8601 instant. Older collectors emit plain integer epoch ms. Split on
            // the decimal point and parse each side as Long so we keep full precision (Double would lose digits
            // past ~15 significant figures, drifting the resulting Instant by tens of nanoseconds).
            return parseEpochMillisDecimalString(text);
        }
    }

    private static Instant parseEpochMillisDecimalString(String text) {
        try {
            int dot = text.indexOf('.');
            if (dot < 0) {
                return Instant.ofEpochMilli(Long.parseLong(text));
            }
            long epochMillis = Long.parseLong(text.substring(0, dot));
            String fractionalMs = text.substring(dot + 1);
            // Sub-millisecond digits: pad / truncate to 6 chars and interpret as the nano-suffix below the ms.
            // "675208" → 675208 ns past the millisecond boundary; "67" → 670000 ns; "67520823" → 675208 ns.
            String padded = (fractionalMs + "000000").substring(0, 6);
            long extraNanos = Long.parseLong(padded);
            return Instant.ofEpochMilli(epochMillis).plusNanos(extraNanos);
        } catch (NumberFormatException ex) {
            // Silently returning null would let spans render with null startTime — the trace then sorts wrong
            // and the root-span fallback kicks in without any signal. Warn so operators can diagnose by grep.
            log.warn("Unexpected @timestamp format, span timestamp will be null: '{}'", text);
            return null;
        }
    }
}
