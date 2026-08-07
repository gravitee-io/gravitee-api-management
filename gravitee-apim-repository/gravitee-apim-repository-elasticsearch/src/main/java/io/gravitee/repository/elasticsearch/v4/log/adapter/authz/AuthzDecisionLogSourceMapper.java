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
            .timestamp(epochMillis(asTextOrNull(source.get("@timestamp"))))
            .apiId(asTextOrNull(source.get(AuthzDecisionLogFields.API_ID)))
            .organizationId(asTextOrNull(source.get(AuthzDecisionLogFields.ORG_ID)))
            .environmentId(asTextOrNull(source.get(AuthzDecisionLogFields.ENV_ID)))
            .gatewayId(asTextOrNull(source.get(AuthzDecisionLogFields.GW_ID)))
            .requestId(asTextOrNull(source.get(AuthzDecisionLogFields.REQUEST_ID)))
            .operation(asTextOrNull(source.get(AuthzDecisionLogFields.OPERATION)))
            .status(asTextOrNull(source.get(AuthzDecisionLogFields.STATUS)))
            .caller(asTextOrNull(source.get(AuthzDecisionLogFields.CALLER)))
            .targetPdpId(asTextOrNull(source.get(AuthzDecisionLogFields.TARGET_PDP_ID)))
            .policyGeneration(asLongOrNull(source.get(AuthzDecisionLogFields.POLICY_GENERATION)))
            .decision(asTextOrNull(source.get(AuthzDecisionLogFields.DECISION)))
            .matchedPolicyNames(matchedPolicyNames(source.get(AuthzDecisionLogFields.MATCHED_POLICIES)))
            .reasons(asTextList(source.get(AuthzDecisionLogFields.REASONS)))
            .subjectType(asTextOrNull(source.get(AuthzDecisionLogFields.SUBJECT_TYPE)))
            .subjectId(asTextOrNull(source.get(AuthzDecisionLogFields.SUBJECT_ID)))
            .action(asTextOrNull(source.get(AuthzDecisionLogFields.ACTION)))
            .resourceType(asTextOrNull(source.get(AuthzDecisionLogFields.RESOURCE_TYPE)))
            .resourceId(asTextOrNull(source.get(AuthzDecisionLogFields.RESOURCE_ID)))
            .batchId(asTextOrNull(source.get(AuthzDecisionLogFields.BATCH_ID)))
            .batchIndex(asIntOrNull(source.get(AuthzDecisionLogFields.BATCH_INDEX)))
            .batchSize(asIntOrNull(source.get(AuthzDecisionLogFields.BATCH_SIZE)))
            .searchType(asTextOrNull(source.get(AuthzDecisionLogFields.SEARCH_TYPE)))
            .resultCount(asIntOrNull(source.get(AuthzDecisionLogFields.RESULT_COUNT)))
            .durationNanos(asLongOrNull(source.get(AuthzDecisionLogFields.DURATION_NANOS)))
            .build();
    }

    private static List<String> matchedPolicyNames(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        var names = new ArrayList<String>(node.size());
        node.forEach(policy -> {
            var name = asTextOrNull(policy.get(AuthzDecisionLogFields.MATCHED_POLICY_NAME));
            if (name != null) {
                names.add(name);
            }
        });
        return names;
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
        return node == null || node.isNull() ? null : node.asInt();
    }
}
