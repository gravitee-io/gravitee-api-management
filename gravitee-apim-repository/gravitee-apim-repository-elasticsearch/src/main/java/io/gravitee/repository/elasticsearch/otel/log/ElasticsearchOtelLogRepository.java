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
package io.gravitee.repository.elasticsearch.otel.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.utils.IndexNameUtils;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.otel.log.api.OtelLogRepository;
import io.gravitee.repository.otel.log.model.OtelLogRecord;
import io.gravitee.repository.otel.log.model.OtelLogSearchCriteria;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch-backed {@link OtelLogRepository} reading log records written by the OpenTelemetry
 * Collector's {@code elasticsearch} exporter in {@code mapping.mode: otel}.
 * <p>
 * Two distinct record shapes share the same logs data stream — span events written by the collector when
 * an exporter forwards an OTel trace payload (the exporter writes the event's {@code event.name} into a
 * top-level {@code event_name} field), and payload logs written by {@code gravitee-reporter-otel} when
 * the gateway captures a request/response payload (no {@code event_name}). The repository returns both
 * shapes uniformly: it doesn't discriminate them at query time, and on the read side it promotes the
 * top-level {@code event_name} field back into the record's {@code attributes} map under the OTel-standard
 * {@code event.name} key so consumers can partition without leaking ES-specific knowledge.
 *
 * @author GraviteeSource Team
 */
public class ElasticsearchOtelLogRepository implements OtelLogRepository {

    /**
     * OTel standard attribute key for an event's name (logs data model "Event" convention). The ES
     * exporter writes it as a top-level {@code event_name} field for indexing convenience; the repository
     * normalises back to the standard key so consumers stay backend-agnostic.
     */
    private static final String EVENT_NAME_ATTRIBUTE = "event.name";

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private final String indexTemplate;
    private final Client client;

    public ElasticsearchOtelLogRepository(String indexTemplate, Client client) {
        this.indexTemplate = indexTemplate;
        this.client = client;
    }

    @Override
    public Single<List<OtelLogRecord>> findLogs(QueryContext queryContext, OtelLogSearchCriteria criteria) {
        String index = resolveIndex(indexTemplate, queryContext);
        String body = buildSearchBody(criteria);
        // Operators may run with traces but not payload logs configured (or vice-versa); a missing data
        // stream raises an error from the client. Translate to empty list so the consumer can stitch the
        // trace anyway.
        return client.search(index, null, body).onErrorReturnItem(new SearchResponse()).map(ElasticsearchOtelLogRepository::parseResponse);
    }

    private String resolveIndex(String template, QueryContext queryContext) {
        // Mirrors the analytics plugin's index-naming convention: IndexNameUtils.format substitutes
        // {orgId} / {envId} with the lowercased values from QueryContext.placeholder(). ES data streams
        // require lowercase names — the OTel collector writes to lowercase too, so a caller orgId of
        // "DEFAULT" resolves to logs-apim.otel-default, not -DEFAULT.
        return IndexNameUtils.format(template, queryContext.placeholder());
    }

    /**
     * Composes a {@code _search} body that filters by every dimension the criteria carries, sorted by
     * {@code @timestamp} ascending so consumers see entries in chronological order. No event_name
     * discriminator — span events and payload logs are returned in the same response.
     */
    static String buildSearchBody(OtelLogSearchCriteria criteria) {
        ObjectNode root = JSON.objectNode();
        root.put("size", criteria.limit());

        ArrayNode filter = JSON.arrayNode();

        if (criteria.traceId() != null) {
            ObjectNode traceIdTerm = JSON.objectNode();
            ObjectNode traceIdInner = JSON.objectNode();
            traceIdInner.put("trace_id", criteria.traceId());
            traceIdTerm.set("term", traceIdInner);
            filter.add(traceIdTerm);
        }

        if (criteria.start() != null || criteria.end() != null) {
            filter.add(buildTimestampRangeFilter(criteria.start(), criteria.end()));
        }

        for (Map.Entry<String, String> entry : criteria.attributeFilters().entrySet()) {
            // The OTel Collector ES exporter (mapping.mode: otel) writes most record attributes flat
            // under attributes.<key>, but the standard event.name attribute is promoted to a top-level
            // event_name field for indexing convenience — mirror that mapping on the query side so a
            // caller filtering on event.name actually hits the indexed field. The response side already
            // normalises event_name back into attributes["event.name"] for the API contract.
            String field = EVENT_NAME_ATTRIBUTE.equals(entry.getKey()) ? "event_name" : "attributes." + entry.getKey();
            filter.add(buildTermFilter(field, entry.getValue()));
        }

        for (Map.Entry<String, String> entry : criteria.resourceAttributeFilters().entrySet()) {
            filter.add(buildTermFilter("resource.attributes." + entry.getKey(), entry.getValue()));
        }

        ObjectNode bool = JSON.objectNode();
        bool.set("filter", filter);

        ObjectNode query = JSON.objectNode();
        query.set("bool", bool);
        root.set("query", query);

        ArrayNode sort = JSON.arrayNode();
        ObjectNode sortField = JSON.objectNode();
        ObjectNode order = JSON.objectNode();
        order.put("order", "asc");
        sortField.set("@timestamp", order);
        sort.add(sortField);
        root.set("sort", sort);

        return root.toString();
    }

    private static ObjectNode buildTermFilter(String field, String value) {
        ObjectNode termFilter = JSON.objectNode();
        ObjectNode term = JSON.objectNode();
        term.put(field, value);
        termFilter.set("term", term);
        return termFilter;
    }

    private static ObjectNode buildTimestampRangeFilter(Instant start, Instant end) {
        // Half-open ISO 8601 range on @timestamp: gte = inclusive lower bound, lt = exclusive upper. ES
        // accepts ISO with offset, and Instant.toString() is ISO-8601 UTC ("Z"), so no formatter needed.
        ObjectNode rangeFilter = JSON.objectNode();
        ObjectNode range = JSON.objectNode();
        ObjectNode bounds = JSON.objectNode();
        if (start != null) {
            bounds.put("gte", start.toString());
        }
        if (end != null) {
            bounds.put("lt", end.toString());
        }
        range.set("@timestamp", bounds);
        rangeFilter.set("range", range);
        return rangeFilter;
    }

    private static List<OtelLogRecord> parseResponse(SearchResponse response) {
        if (
            response == null ||
            response.getSearchHits() == null ||
            response.getSearchHits().getHits() == null ||
            response.getSearchHits().getHits().isEmpty()
        ) {
            return List.of();
        }
        List<OtelLogRecord> records = new ArrayList<>(response.getSearchHits().getHits().size());
        for (SearchHit hit : response.getSearchHits().getHits()) {
            JsonNode source = hit.getSource();
            if (source == null) {
                continue;
            }
            String spanId = textOrNull(source.get("span_id"));
            // Without a span_id the record can't be stitched onto any span — drop rather than
            // synthesise an orphan that would confuse the consumer.
            if (spanId == null) {
                continue;
            }
            String traceId = textOrNull(source.get("trace_id"));
            Instant timestamp = parseInstant(source.path("@timestamp"));
            String severity = textOrNull(source.get("severity_text"));
            String body = readBody(source);
            Map<String, String> attributes = flattenStringMap(source.path("attributes"), "");
            // Promote the ES-side top-level field back into the OTel-standard attribute key so consumers
            // see a backend-agnostic record. Don't overwrite a same-named attribute that was already in
            // the OTel attributes block — the exporter shouldn't double-write, but defensive doesn't hurt.
            String eventName = textOrNull(source.get("event_name"));
            if (eventName != null) {
                attributes.putIfAbsent(EVENT_NAME_ATTRIBUTE, eventName);
            }
            Map<String, String> resourceAttributes = flattenStringMap(source.path("resource").path("attributes"), "");
            records.add(new OtelLogRecord(traceId, spanId, timestamp, severity, body, attributes, resourceAttributes));
        }
        return records;
    }

    /**
     * The OTel ES exporter writes the log {@code body} either as a top-level string or as a nested
     * object {@code body.text} / {@code body.structured} depending on the OTel SDK / collector version.
     * Try the common shapes; return {@code null} when none are populated.
     */
    private static String readBody(JsonNode source) {
        JsonNode body = source.path("body");
        if (body.isTextual()) {
            return body.asText();
        }
        String text = textOrNull(body.path("text"));
        if (text != null) {
            return text;
        }
        // Structured bodies fall back to JSON serialization so consumers can inspect them.
        JsonNode structured = body.path("structured");
        if (!structured.isMissingNode() && !structured.isNull()) {
            return structured.toString();
        }
        return null;
    }

    /**
     * OTel-mode documents store {@code attributes} either as a JSON object (newer collector versions) or
     * as an array of {@code {key, value}} pairs (older versions). This walks both into a flat key→string
     * map; nested objects are dot-flattened (e.g. {@code http.status_code = "200"}).
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

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    /**
     * The OTel collector's elasticsearch exporter (otel mapping mode) writes {@code @timestamp} as
     * {@code "<epoch-millis>.<sub-ms-digits>"} (e.g. {@code "1778515640168.675208"}). Older collectors
     * may emit plain integer epoch ms or ISO 8601; try ISO first, fall back to the decimal split.
     */
    private static Instant parseInstant(JsonNode node) {
        String text = textOrNull(node);
        if (text == null) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {
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
            // Sub-millisecond digits: pad / truncate to 6 chars and interpret as the nano-suffix below
            // the millisecond boundary. "675208" → 675208 ns past the ms; "67" → 670000 ns;
            // "67520823" → 675208 ns.
            String padded = (fractionalMs + "000000").substring(0, 6);
            long extraNanos = Long.parseLong(padded);
            return Instant.ofEpochMilli(epochMillis).plusNanos(extraNanos);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
