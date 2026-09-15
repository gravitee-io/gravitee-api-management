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
package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asMapOrNull;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.AgentActivityDecision;
import io.gravitee.repository.log.v4.model.analytics.AgentActivityHop;
import io.gravitee.repository.log.v4.model.analytics.AgentActivityResult;
import io.gravitee.repository.log.v4.model.analytics.AgentActivityRun;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts raw ES hits from {@code v4-metrics} and {@code decisions} into
 * {@link AgentActivityResult}.
 *
 * <p>Two-phase: first read hop hits, then read decision hits keyed by the same request-ids,
 * join in Java, group by {@code conversation-id} when present, derive shared outcomes.
 */
public class AgentActivityResponseAdapter {

    private static final String REQUEST_ID_V4 = "request-id";
    private static final String REQUEST_ID_V2 = "id";
    private static final String CONVERSATION_ID_FIELD = "additional-metrics.keyword_gravitee_conversation-id";
    private static final String ENTRYPOINT_ID_FIELD = "entrypoint-id";
    private static final String APPLICATION_ID_FIELD = "application-id";
    private static final String API_ID_FIELD = "api-id";

    /** Same keys HTTPFieldResolver uses for MCP charts. Slash is part of the metric name. */
    private static final String MCP_TOOL_METRIC = "keyword_mcp-proxy_tools/call";
    private static final String MCP_RESOURCE_METRIC = "keyword_mcp-proxy_resources/read";

    // Decision fields — kebab-case as written to the decisions data stream. CamelCase kept as
    // fallback for older fixtures.
    private static final String DECISION_REQUEST_ID = "request-id";
    private static final String DECISION_REQUEST_ID_CAMEL = "requestId";
    private static final String DECISION_POINT_TYPE = "decision-point-type";
    private static final String DECISION_POINT_TYPE_CAMEL = "decisionPointType";
    private static final String OUTCOME = "outcome";
    private static final String ENFORCED = "enforced";
    private static final String PHASE = "phase";
    private static final String DECIDER_TYPE = "decider-type";
    private static final String DECIDER_TYPE_CAMEL = "deciderType";
    private static final String DECIDER_ID = "decider-id";
    private static final String DECIDER_ID_CAMEL = "deciderId";
    private static final String REASONS = "reasons";
    private static final String DECISION_ID = "event-id";
    private static final String DECISION_ID_CAMEL = "eventId";

    public AgentActivityResult adapt(SearchResponse hopsResponse, SearchResponse decisionsResponse, int page, int size) {
        var hops = parseHops(hopsResponse);

        if (hops == null || hops.isEmpty()) {
            return new AgentActivityResult(List.of(), 0, page, size);
        }

        var decisionsByRequestId = parseDecisions(decisionsResponse);

        for (var hop : hops) {
            var hopDecisions = decisionsByRequestId.getOrDefault(hop.getRequestId(), List.of());
            hop.setDecisions(resolveHitlPhases(hopDecisions));
        }

        var runs = groupIntoRuns(hops);

        // Estimate total (at minimum what we returned on this page)
        var hits = hopsResponse.getSearchHits();
        long totalHits = hits != null && hits.getTotal() != null ? hits.getTotal().getValue() : hops.size();

        return new AgentActivityResult(runs, (int) Math.max(totalHits, runs.size()), page, size);
    }

    private List<AgentActivityRun> groupIntoRuns(List<AgentActivityHop> hops) {
        // Partition hops with a conversation-id vs without
        var withConvId = hops
            .stream()
            .filter(h -> h.getConversationId() != null)
            .collect(Collectors.toList());
        var withoutConvId = hops
            .stream()
            .filter(h -> h.getConversationId() == null)
            .collect(Collectors.toList());

        // Group by conversation-id
        var convGroups = withConvId
            .stream()
            .collect(Collectors.groupingBy(AgentActivityHop::getConversationId, LinkedHashMap::new, Collectors.toList()));

        var runs = new ArrayList<AgentActivityRun>();

        for (var entry : convGroups.entrySet()) {
            runs.add(buildRun(entry.getKey(), entry.getValue()));
        }

        // Hops without conversation-id each stand alone
        for (var hop : withoutConvId) {
            runs.add(buildRun(null, List.of(hop)));
        }

        // Sort by startedAt desc (newest first)
        runs.sort((a, b) -> Long.compare(b.getStartedAt(), a.getStartedAt()));
        return runs;
    }

    private AgentActivityRun buildRun(String conversationId, List<AgentActivityHop> hops) {
        var sorted = hops.stream().sorted(Comparator.comparingLong(AgentActivityHop::getTimestamp)).toList();
        var startedAt = sorted.get(0).getTimestamp();
        var lastEventAt = sorted.get(sorted.size() - 1).getTimestamp();

        var outcome = deriveOutcome(sorted);

        // Asked-by: from the inbound A2A hop (marked by api-id match)
        var askedBy = "Not recorded";

        return AgentActivityRun.builder()
            .conversationId(conversationId)
            .outcome(outcome)
            .outcomeSummary(buildSummary(sorted, outcome))
            .askedBy(askedBy)
            .hops(sorted)
            .startedAt(startedAt)
            .lastEventAt(lastEventAt)
            .build();
    }

    private String deriveOutcome(List<AgentActivityHop> hops) {
        var allDecisions = hops
            .stream()
            .flatMap(h -> h.getDecisions().stream())
            .toList();

        // If any HITL is waiting (unresolved REQUESTED): "waiting"
        if (allDecisions.stream().anyMatch(d -> "human-approval".equals(d.getDecisionPointType()) && "REQUESTED".equals(d.getPhase()))) {
            return "waiting";
        }

        // If anything was denied/stopped
        if (allDecisions.stream().anyMatch(d -> "DENY".equals(d.getOutcome()) || "DENY".equals(d.getEnforced()))) {
            return "stopped";
        }

        // Guardian or HITL changed something
        if (
            allDecisions
                .stream()
                .anyMatch(
                    d ->
                        ("human-approval".equals(d.getDecisionPointType()) && "ALLOW".equals(d.getOutcome())) ||
                        ("guardian".equals(d.getDecisionPointType()) && "TRANSFORM".equals(d.getOutcome()))
                )
        ) {
            return "done-with-changes";
        }

        // All policy permits, no interventions: "done"
        return "done";
    }

    private String buildSummary(List<AgentActivityHop> hops, String outcome) {
        if (hops.isEmpty()) {
            return "No activity";
        }

        // Use the first hop's label as the narrative anchor
        var first = hops.get(0);
        var label = first.getLabel() != null ? first.getLabel() : "Request";

        var intervention = switch (outcome) {
            case "waiting" -> " — waiting for sign-off";
            case "stopped" -> " — blocked";
            case "done-with-changes" -> " — modified";
            case "failed" -> " — failed";
            default -> "";
        };

        return label + intervention;
    }

    /**
     * Collapses HITL REQUESTED + RESOLVED pairs into a single intervention.
     */
    private List<AgentActivityDecision> resolveHitlPhases(List<AgentActivityDecision> decisions) {
        if (decisions.size() <= 1) {
            return decisions;
        }

        var hitlRequested = new ArrayList<AgentActivityDecision>();
        var hitlResolved = new ArrayList<AgentActivityDecision>();
        var others = new ArrayList<AgentActivityDecision>();

        for (var d : decisions) {
            if ("human-approval".equals(d.getDecisionPointType())) {
                if ("REQUESTED".equals(d.getPhase())) {
                    hitlRequested.add(d);
                } else {
                    hitlResolved.add(d);
                }
            } else {
                others.add(d);
            }
        }

        var result = new ArrayList<AgentActivityDecision>();
        result.addAll(others);

        // Pair REQUESTED with RESOLVED by position; unmatched REQUESTED -> "waiting"
        for (int i = 0; i < hitlRequested.size(); i++) {
            var req = hitlRequested.get(i);
            var res = i < hitlResolved.size() ? hitlResolved.get(i) : null;

            if (res != null) {
                // Merged: use RESOLVED outcome + REQUESTED reason
                var merged = AgentActivityDecision.builder()
                    .decisionPointType("human-approval")
                    .outcome(res.getOutcome())
                    .enforced(res.getEnforced())
                    .decider(res.getDecider())
                    .reason(req.getReason() != null ? req.getReason() : res.getReason())
                    .timestamp(res.getTimestamp())
                    .decisionId(res.getDecisionId())
                    .build();
                result.add(merged);
            } else {
                result.add(req); // unresolved -> "waiting" handled by deriveOutcome
            }
        }

        // Extra RESOLVED without matching REQUESTED -> keep
        for (int i = hitlRequested.size(); i < hitlResolved.size(); i++) {
            result.add(hitlResolved.get(i));
        }

        return result;
    }

    // ---- Parsers ----

    private List<AgentActivityHop> parseHops(SearchResponse response) {
        if (response == null) {
            return List.of();
        }

        var hits = response.getSearchHits();
        if (hits == null) {
            return List.of();
        }

        return hits.getHits().stream().map(this::parseHop).filter(Objects::nonNull).toList();
    }

    private AgentActivityHop parseHop(io.gravitee.elasticsearch.model.SearchHit hit) {
        var source = hit.getSource();
        if (source == null) {
            return null;
        }

        var requestId = coalesceText(source, REQUEST_ID_V4, REQUEST_ID_V2);
        if (requestId == null) {
            return null;
        }

        var entrypointId = asTextOrNull(source.get(ENTRYPOINT_ID_FIELD));
        var kind = hopKind(entrypointId);
        var additionalMetrics = asMapOrNull(source.get("additional-metrics"));
        var conversationId = extractConversationId(additionalMetrics);
        long cost = extractCost(kind, additionalMetrics);
        int status = source.has("status") ? source.get("status").asInt(0) : 0;
        var traceId = asTextOrNull(source.get("trace-id"));
        long timestamp = extractTimestamp(source);

        return AgentActivityHop.builder()
            .requestId(requestId)
            .kind(kind)
            .timestamp(timestamp)
            .label(hopLabel(kind, source, additionalMetrics))
            .detail(hopDetail(kind, additionalMetrics))
            .status(status)
            .cost(cost)
            .traceId(traceId)
            .entrypointId(entrypointId)
            .conversationId(conversationId)
            .build();
    }

    private String hopKind(String entrypointId) {
        if (entrypointId == null) {
            return "inbound";
        }
        if (entrypointId.contains("llm")) {
            return "llm-call";
        }
        if (entrypointId.contains("mcp")) {
            return "mcp-call";
        }
        return "inbound"; // A2A
    }

    private String hopLabel(String kind, JsonNode source, Map<String, Object> additionalMetrics) {
        if (additionalMetrics != null) {
            return switch (kind) {
                case "llm-call" -> {
                    var model = additionalMetrics.get("keyword_llm-proxy_model");
                    yield model != null ? "LLM call: " + model : "LLM call";
                }
                case "mcp-call" -> {
                    var tool = additionalMetrics.get(MCP_TOOL_METRIC);
                    yield tool != null ? "MCP tool: " + tool : "MCP tool call";
                }
                default -> {
                    var uri = asTextOrNull(source.get("uri"));
                    yield uri != null ? "Inbound: " + uri : "Inbound request";
                }
            };
        }
        return switch (kind) {
            case "llm-call" -> "LLM call";
            case "mcp-call" -> "MCP tool call";
            default -> {
                var uri = asTextOrNull(source.get("uri"));
                yield uri != null ? "Inbound: " + uri : "Inbound request";
            }
        };
    }

    private String hopDetail(String kind, Map<String, Object> additionalMetrics) {
        if (additionalMetrics == null) {
            return null;
        }
        return switch (kind) {
            case "llm-call" -> {
                var provider = additionalMetrics.get("keyword_llm-proxy_provider");
                yield provider != null ? String.valueOf(provider) : null;
            }
            case "mcp-call" -> {
                var resource = additionalMetrics.get(MCP_RESOURCE_METRIC);
                yield resource != null ? String.valueOf(resource) : null;
            }
            default -> null;
        };
    }

    private String extractConversationId(Map<String, Object> additionalMetrics) {
        if (additionalMetrics == null) {
            return null;
        }
        var convId = additionalMetrics.get("keyword_gravitee_conversation-id");
        return convId != null ? String.valueOf(convId) : null;
    }

    private long extractCost(String kind, Map<String, Object> additionalMetrics) {
        if (additionalMetrics == null) {
            return 0;
        }
        return switch (kind) {
            case "llm-call" -> {
                var sent = toLong(additionalMetrics.get("double_llm-proxy_sent-cost"));
                var received = toLong(additionalMetrics.get("double_llm-proxy_received-cost"));
                yield sent + received;
            }
            case "mcp-call" -> toLong(additionalMetrics.get("double_mcp-proxy_tool-cost"));
            default -> 0;
        };
    }

    private long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private Map<String, List<AgentActivityDecision>> parseDecisions(SearchResponse response) {
        if (response == null) {
            return Map.of();
        }

        var hits = response.getSearchHits();
        if (hits == null) {
            return Map.of();
        }

        return hits
            .getHits()
            .stream()
            .map(this::parseDecision)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(AgentActivityDecision::getRequestId));
    }

    private AgentActivityDecision parseDecision(io.gravitee.elasticsearch.model.SearchHit hit) {
        var source = hit.getSource();
        if (source == null) {
            return null;
        }

        var requestId = coalesceText(source, DECISION_REQUEST_ID, DECISION_REQUEST_ID_CAMEL);
        if (requestId == null) {
            return null;
        }

        long timestamp = extractTimestamp(source);

        return AgentActivityDecision.builder()
            .requestId(requestId)
            .decisionPointType(coalesceText(source, DECISION_POINT_TYPE, DECISION_POINT_TYPE_CAMEL))
            .outcome(asTextOrNull(source.get(OUTCOME)))
            .enforced(asTextOrNull(source.get(ENFORCED)))
            .phase(asTextOrNull(source.get(PHASE)))
            .decider(extractDecider(source))
            .reason(extractReason(source))
            .timestamp(timestamp)
            .decisionId(coalesceText(source, DECISION_ID, DECISION_ID_CAMEL))
            .build();
    }

    private String extractDecider(JsonNode source) {
        var type = coalesceText(source, DECIDER_TYPE, DECIDER_TYPE_CAMEL);
        var id = coalesceText(source, DECIDER_ID, DECIDER_ID_CAMEL);
        if (type != null && id != null) {
            return type + ": " + id;
        }
        if (type != null) {
            return type;
        }
        return id;
    }

    private String extractReason(JsonNode source) {
        var reasonsNode = source.get(REASONS);
        if (reasonsNode != null && reasonsNode.isArray() && !reasonsNode.isEmpty()) {
            var first = reasonsNode.get(0);
            if (first.isTextual()) {
                return first.asText();
            }
            if (first.has("message")) {
                return asTextOrNull(first.get("message"));
            }
            return first.toString();
        }
        return null;
    }

    private String coalesceText(JsonNode json, String v4Field, String v2Field) {
        var value = asTextOrNull(json.get(v4Field));
        return value != null ? value : asTextOrNull(json.get(v2Field));
    }

    /**
     * Metrics {@code @timestamp} is a date in ES. Hits return it as epoch millis or an ISO-8601
     * string. {@link JsonNode#asLong()} on a date string is 0, which dumped every live hop onto
     * 1970-01-01.
     */
    private long extractTimestamp(JsonNode source) {
        var node = source.get("@timestamp");
        if (node == null || node.isNull() || node.isMissingNode()) {
            return 0;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        var text = node.asText();
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            return Instant.parse(text).toEpochMilli();
        } catch (DateTimeParseException ignored) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
