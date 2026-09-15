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
package io.gravitee.repository.elasticsearch.v4.log.adapter.decision;

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.log.v4.model.decision.DecisionLog;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads a {@code decisions} document into the flat record.
 *
 * <p>Every field is read the same way whichever family wrote the document: one that a family never
 * writes is simply absent from {@code _source} and comes back {@code null}. That is the whole cost of a
 * shared record, and it is paid here rather than by branching on {@code decision-point-type} — a branch
 * would have to be extended for every new kind of point, and would drop the fields of a point this
 * version has never heard of.
 *
 * @author GraviteeSource Team
 */
final class DecisionLogSourceMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> ADDITIONAL_METRICS = new TypeReference<>() {};

    private DecisionLogSourceMapper() {}

    static DecisionLog from(JsonNode source) {
        return DecisionLog.builder()
            .timestamp(epochMillis(asTextOrNull(source.get(DecisionLogFields.TIMESTAMP))))
            .gatewayId(asTextOrNull(source.get(DecisionLogFields.GW_ID)))
            .organizationId(asTextOrNull(source.get(DecisionLogFields.ORG_ID)))
            .environmentId(asTextOrNull(source.get(DecisionLogFields.ENV_ID)))
            .apiId(asTextOrNull(source.get(DecisionLogFields.API_ID)))
            .planId(asTextOrNull(source.get(DecisionLogFields.PLAN_ID)))
            .applicationId(asTextOrNull(source.get(DecisionLogFields.APP_ID)))
            .eventId(asTextOrNull(source.get(DecisionLogFields.EVENT_ID)))
            .caseId(asTextOrNull(source.get(DecisionLogFields.CASE_ID)))
            .batchId(asTextOrNull(source.get(DecisionLogFields.BATCH_ID)))
            .phase(asTextOrNull(source.get(DecisionLogFields.PHASE)))
            .decisionPointType(asTextOrNull(source.get(DecisionLogFields.DECISION_POINT_TYPE)))
            .decisionPointId(asTextOrNull(source.get(DecisionLogFields.DECISION_POINT_ID)))
            .decisionPointVersion(asTextOrNull(source.get(DecisionLogFields.DECISION_POINT_VERSION)))
            .checkpoint(asTextOrNull(source.get(DecisionLogFields.CHECKPOINT)))
            .caller(asTextOrNull(source.get(DecisionLogFields.CALLER)))
            .subjectType(asTextOrNull(source.get(DecisionLogFields.SUBJECT_TYPE)))
            .subjectId(asTextOrNull(source.get(DecisionLogFields.SUBJECT_ID)))
            .actorType(asTextOrNull(source.get(DecisionLogFields.ACTOR_TYPE)))
            .actorId(asTextOrNull(source.get(DecisionLogFields.ACTOR_ID)))
            .action(asTextOrNull(source.get(DecisionLogFields.ACTION)))
            .resourceType(asTextOrNull(source.get(DecisionLogFields.RESOURCE_TYPE)))
            .resourceId(asTextOrNull(source.get(DecisionLogFields.RESOURCE_ID)))
            .argsHash(asTextOrNull(source.get(DecisionLogFields.ARGS_HASH)))
            .outcome(asTextOrNull(source.get(DecisionLogFields.OUTCOME)))
            .enforced(asTextOrNull(source.get(DecisionLogFields.ENFORCED)))
            .verdict(asTextOrNull(source.get(DecisionLogFields.VERDICT)))
            .indeterminateCause(asTextOrNull(source.get(DecisionLogFields.INDETERMINATE_CAUSE)))
            .confidence(asDoubleOrNull(source.get(DecisionLogFields.CONFIDENCE)))
            .reasons(asTextList(source.get(DecisionLogFields.REASONS)))
            .matchedRules(matchedRules(source.get(DecisionLogFields.MATCHED_RULES)))
            .transformed(asBooleanOrNull(source.get(DecisionLogFields.TRANSFORMED)))
            .transformationType(asTextOrNull(source.get(DecisionLogFields.TRANSFORMATION_TYPE)))
            .requiredApprover(asTextOrNull(source.get(DecisionLogFields.REQUIRED_APPROVER)))
            .deciderType(asTextOrNull(source.get(DecisionLogFields.DECIDER_TYPE)))
            .deciderId(asTextOrNull(source.get(DecisionLogFields.DECIDER_ID)))
            .channel(asTextOrNull(source.get(DecisionLogFields.CHANNEL)))
            .requestId(asTextOrNull(source.get(DecisionLogFields.REQUEST_ID)))
            .traceId(asTextOrNull(source.get(DecisionLogFields.TRACE_ID)))
            .conversationId(asTextOrNull(source.get(DecisionLogFields.CONVERSATION_ID)))
            .missionId(asTextOrNull(source.get(DecisionLogFields.MISSION_ID)))
            .status(asTextOrNull(source.get(DecisionLogFields.STATUS)))
            .errorType(asTextOrNull(source.get(DecisionLogFields.ERROR_TYPE)))
            .durationNanos(asLongOrNull(source.get(DecisionLogFields.DURATION_NANOS)))
            .waitedNanos(asLongOrNull(source.get(DecisionLogFields.WAITED_NANOS)))
            .additionalMetrics(additionalMetrics(source.get(DecisionLogFields.ADDITIONAL_METRICS)))
            .build();
    }

    /** A rule with no id cannot be pointed at, so it is dropped rather than reported as a blank row. */
    private static List<DecisionLog.MatchedRule> matchedRules(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        var rules = new ArrayList<DecisionLog.MatchedRule>(node.size());
        node.forEach(rule -> {
            var id = asTextOrNull(rule.get(DecisionLogFields.MATCHED_RULE_ID));
            if (id != null) {
                rules.add(
                    new DecisionLog.MatchedRule(
                        id,
                        asTextOrNull(rule.get(DecisionLogFields.MATCHED_RULE_NAME)),
                        asTextOrNull(rule.get(DecisionLogFields.MATCHED_RULE_EFFECT))
                    )
                );
            }
        });
        return rules;
    }

    /**
     * Point-specific figures, whatever the index's dynamic templates typed them as. Read as a map rather
     * than named one by one: their whole purpose is to travel from a plugin to a reader without anything
     * in between being taught their names.
     */
    private static Map<String, Object> additionalMetrics(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        try {
            return MAPPER.convertValue(node, ADDITIONAL_METRICS);
        } catch (IllegalArgumentException e) {
            return Map.of();
        }
    }

    private static List<String> asTextList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        var values = new ArrayList<String>(node.size());
        node.forEach(value -> {
            if (!value.isNull()) {
                values.add(value.asText());
            }
        });
        return values;
    }

    private static Long epochMillis(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            // the reporter writes an offset date-time ("…+02:00"), not a bare instant
            return OffsetDateTime.parse(timestamp).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Long asLongOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asLong();
    }

    private static Double asDoubleOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asDouble();
    }

    private static Boolean asBooleanOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asBoolean();
    }
}
