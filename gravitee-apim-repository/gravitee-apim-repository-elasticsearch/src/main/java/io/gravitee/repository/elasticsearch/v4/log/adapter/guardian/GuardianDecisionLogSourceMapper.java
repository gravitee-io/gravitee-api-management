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
package io.gravitee.repository.elasticsearch.v4.log.adapter.guardian;

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asMapOrNull;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionSourceValues.booleanOrNull;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionSourceValues.epochMillis;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionSourceValues.longOrNull;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionSourceValues.textList;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionReportFields;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author GraviteeSource Team
 */
final class GuardianDecisionLogSourceMapper {

    private GuardianDecisionLogSourceMapper() {}

    static GuardianDecisionLog from(JsonNode source) {
        return GuardianDecisionLog.builder()
            .eventId(text(source, DecisionReportFields.EVENT_ID))
            .timestamp(epochMillis(text(source, DecisionReportFields.TIMESTAMP)))
            .organizationId(text(source, DecisionReportFields.ORG_ID))
            .environmentId(text(source, DecisionReportFields.ENV_ID))
            .gatewayId(text(source, DecisionReportFields.GW_ID))
            .apiId(text(source, DecisionReportFields.API_ID))
            .planId(text(source, DecisionReportFields.PLAN_ID))
            .applicationId(text(source, DecisionReportFields.APP_ID))
            .guardianId(text(source, DecisionReportFields.DECISION_POINT_ID))
            .guardianRevision(text(source, DecisionReportFields.DECISION_POINT_VERSION))
            .checkpoint(text(source, DecisionReportFields.CHECKPOINT))
            .caller(text(source, DecisionReportFields.CALLER))
            .requestId(text(source, DecisionReportFields.REQUEST_ID))
            .traceId(text(source, DecisionReportFields.TRACE_ID))
            .outcome(text(source, DecisionReportFields.OUTCOME))
            .enforced(text(source, DecisionReportFields.ENFORCED))
            .verdict(text(source, DecisionReportFields.VERDICT))
            .indeterminateCause(text(source, DecisionReportFields.INDETERMINATE_CAUSE))
            .errorType(text(source, DecisionReportFields.ERROR_TYPE))
            .status(text(source, DecisionReportFields.STATUS))
            .reasons(textList(source.get(DecisionReportFields.REASONS)))
            .matchedRules(matchedRules(source.get(DecisionReportFields.MATCHED_RULES)))
            .transformed(booleanOrNull(source.get(DecisionReportFields.TRANSFORMED)))
            .transformationType(text(source, DecisionReportFields.TRANSFORMATION_TYPE))
            .durationNanos(longOrNull(source.get(DecisionReportFields.DURATION_NANOS)))
            .subjectType(text(source, DecisionReportFields.SUBJECT_TYPE))
            .subjectId(text(source, DecisionReportFields.SUBJECT_ID))
            .actorType(text(source, DecisionReportFields.ACTOR_TYPE))
            .actorId(text(source, DecisionReportFields.ACTOR_ID))
            .additionalMetrics(asMapOrNull(source.get(DecisionReportFields.ADDITIONAL_METRICS)))
            .build();
    }

    private static String text(JsonNode source, String field) {
        return asTextOrNull(source.get(field));
    }

    /** Unlike an authz rule, a guardrail finding has no name: its annotations say which guardrail matched where. */
    private static List<GuardianDecisionLog.MatchedRule> matchedRules(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        var rules = new ArrayList<GuardianDecisionLog.MatchedRule>(node.size());
        node.forEach(rule ->
            rules.add(
                new GuardianDecisionLog.MatchedRule(
                    // the reporter template writes a null rule id as an empty string
                    blankAsNull(text(rule, DecisionReportFields.MATCHED_RULE_ID)),
                    text(rule, DecisionReportFields.MATCHED_RULE_NAME),
                    text(rule, DecisionReportFields.MATCHED_RULE_VERSION),
                    text(rule, DecisionReportFields.MATCHED_RULE_EFFECT),
                    annotations(rule.get(DecisionReportFields.MATCHED_RULE_ANNOTATIONS))
                )
            )
        );
        return rules;
    }

    private static Map<String, String> annotations(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, String> annotations = new LinkedHashMap<>();
        node.properties().forEach(entry -> annotations.put(entry.getKey(), entry.getValue().asText()));
        return annotations;
    }

    private static String blankAsNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
