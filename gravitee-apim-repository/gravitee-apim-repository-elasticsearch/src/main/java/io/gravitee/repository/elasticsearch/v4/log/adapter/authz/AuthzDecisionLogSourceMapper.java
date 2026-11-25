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

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asMapOrNull;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionSourceValues.epochMillis;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionSourceValues.intOrNull;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionSourceValues.longOrNull;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionSourceValues.textList;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLog;
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
            .reasons(textList(source.get(AuthzDecisionLogFields.REASONS)))
            .subjectType(asTextOrNull(source.get(AuthzDecisionLogFields.SUBJECT_TYPE)))
            .subjectId(asTextOrNull(source.get(AuthzDecisionLogFields.SUBJECT_ID)))
            .action(asTextOrNull(source.get(AuthzDecisionLogFields.ACTION)))
            .resourceType(asTextOrNull(source.get(AuthzDecisionLogFields.RESOURCE_TYPE)))
            .resourceId(asTextOrNull(source.get(AuthzDecisionLogFields.RESOURCE_ID)))
            .batchId(asTextOrNull(source.get(AuthzDecisionLogFields.BATCH_ID)))
            .batchIndex(intOrNull(at(source, AuthzDecisionLogFields.BATCH_INDEX)))
            .batchSize(intOrNull(at(source, AuthzDecisionLogFields.BATCH_SIZE)))
            .durationNanos(longOrNull(source.get(AuthzDecisionLogFields.DURATION_NANOS)))
            .errorType(asTextOrNull(source.get(AuthzDecisionLogFields.ERROR_TYPE)))
            .additionalMetrics(asMapOrNull(source.get(AuthzDecisionLogFields.ADDITIONAL_METRICS)))
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
}
