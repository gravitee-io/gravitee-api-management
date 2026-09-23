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
package io.gravitee.repository.elasticsearch.v4.log.adapter.authz;

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLog;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author GraviteeSource Team
 */
final class AuthzDecisionLogSourceMapper {

    private AuthzDecisionLogSourceMapper() {}

    static AuthzDecisionLog from(JsonNode source) {
        return AuthzDecisionLog.builder()
            .eventId(asTextOrNull(source.get(AuthzDecisionLogFields.EVENT_ID)))
            .timestamp(epochMillis(asTextOrNull(source.get(AuthzDecisionLogFields.TIMESTAMP))))
            .apiId(asTextOrNull(source.get(AuthzDecisionLogFields.API_ID)))
            .organizationId(asTextOrNull(source.get(AuthzDecisionLogFields.ORG_ID)))
            .environmentId(asTextOrNull(source.get(AuthzDecisionLogFields.ENV_ID)))
            .gatewayId(asTextOrNull(source.get(AuthzDecisionLogFields.GW_ID)))
            .requestId(asTextOrNull(source.get(AuthzDecisionLogFields.REQUEST_ID)))
            .status(asTextOrNull(source.get(AuthzDecisionLogFields.STATUS)))
            .caller(asTextOrNull(source.get(AuthzDecisionLogFields.CALLER)))
            .targetPdpId(asTextOrNull(source.get(AuthzDecisionLogFields.TARGET_PDP_ID)))
            .policyGeneration(asTextOrNull(source.get(AuthzDecisionLogFields.POLICY_GENERATION)))
            .decision(asTextOrNull(source.get(AuthzDecisionLogFields.DECISION)))
            .outcome(asTextOrNull(source.get(AuthzDecisionLogFields.OUTCOME)))
            .enforced(asTextOrNull(source.get(AuthzDecisionLogFields.ENFORCED)))
            .indeterminateCause(asTextOrNull(source.get(AuthzDecisionLogFields.INDETERMINATE_CAUSE)))
            .matchedRules(matchedRules(source.get(AuthzDecisionLogFields.MATCHED_RULES)))
            .reasons(asTextList(source.get(AuthzDecisionLogFields.REASONS)))
            .subjectType(asTextOrNull(source.get(AuthzDecisionLogFields.SUBJECT_TYPE)))
            .subjectId(asTextOrNull(source.get(AuthzDecisionLogFields.SUBJECT_ID)))
            .action(asTextOrNull(source.get(AuthzDecisionLogFields.ACTION)))
            .resourceType(asTextOrNull(source.get(AuthzDecisionLogFields.RESOURCE_TYPE)))
            .resourceId(asTextOrNull(source.get(AuthzDecisionLogFields.RESOURCE_ID)))
            .batchId(asTextOrNull(source.get(AuthzDecisionLogFields.BATCH_ID)))
            .batchIndex(asIntOrNull(at(source, AuthzDecisionLogFields.BATCH_INDEX)))
            .batchSize(asIntOrNull(at(source, AuthzDecisionLogFields.BATCH_SIZE)))
            .durationNanos(asLongOrNull(source.get(AuthzDecisionLogFields.DURATION_NANOS)))
            .errorType(asTextOrNull(source.get(AuthzDecisionLogFields.ERROR_TYPE)))
            .build();
    }

    private static JsonNode at(JsonNode source, String dottedPath) {
        return source.at("/" + dottedPath.replace('.', '/'));
    }

    private static List<AuthzDecisionLog.MatchedRule> matchedRules(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        var rules = new ArrayList<AuthzDecisionLog.MatchedRule>(node.size());
        node.forEach(rule -> {
            var name = asTextOrNull(rule.get(AuthzDecisionLogFields.MATCHED_RULE_NAME));
            if (name != null) {
                rules.add(
                    new AuthzDecisionLog.MatchedRule(
                        asTextOrNull(rule.get(AuthzDecisionLogFields.MATCHED_RULE_ID)),
                        name,
                        asTextOrNull(rule.get(AuthzDecisionLogFields.MATCHED_RULE_VERSION)),
                        asTextOrNull(rule.get(AuthzDecisionLogFields.MATCHED_RULE_EFFECT))
                    )
                );
            }
        });
        return rules;
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

    private static Integer asIntOrNull(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode() ? null : node.asInt();
    }
}
