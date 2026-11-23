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

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asLongOrNull;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextList;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.epochMillisOrNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLog;
import java.util.ArrayList;
import java.util.List;

/**
 * @author GraviteeSource Team
 */
final class GuardianDecisionLogSourceMapper {

    private GuardianDecisionLogSourceMapper() {}

    static GuardianDecisionLog from(JsonNode source) {
        return GuardianDecisionLog.builder()
            .eventId(asTextOrNull(source.get(GuardianDecisionLogFields.EVENT_ID)))
            .timestamp(epochMillisOrNull(asTextOrNull(source.get(GuardianDecisionLogFields.TIMESTAMP))))
            .apiId(asTextOrNull(source.get(GuardianDecisionLogFields.API_ID)))
            .requestId(asTextOrNull(source.get(GuardianDecisionLogFields.REQUEST_ID)))
            .guardianId(asTextOrNull(source.get(GuardianDecisionLogFields.GUARDIAN_ID)))
            .revision(asTextOrNull(source.get(GuardianDecisionLogFields.REVISION)))
            .checkpoint(asTextOrNull(source.get(GuardianDecisionLogFields.CHECKPOINT)))
            .outcome(asTextOrNull(source.get(GuardianDecisionLogFields.OUTCOME)))
            .enforced(asTextOrNull(source.get(GuardianDecisionLogFields.ENFORCED)))
            .transformationType(asTextOrNull(source.get(GuardianDecisionLogFields.TRANSFORMATION_TYPE)))
            .verdict(asTextOrNull(source.get(GuardianDecisionLogFields.VERDICT)))
            .reasons(asTextList(source.get(GuardianDecisionLogFields.REASONS)))
            .findings(findings(source.get(GuardianDecisionLogFields.MATCHED_RULES)))
            .durationNanos(asLongOrNull(source.get(GuardianDecisionLogFields.DURATION_NANOS)))
            .build();
    }

    /**
     * A finding is a matched rule and where the gateway says it matched: the guardrail and the location are annotations.
     * A guardrail making a single check names no rule, so a finding without a name is kept, its rule left empty.
     */
    private static List<GuardianDecisionLog.Finding> findings(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        var findings = new ArrayList<GuardianDecisionLog.Finding>(node.size());
        node.forEach(rule -> {
            if (rule.isObject()) {
                var annotations = rule.path(GuardianDecisionLogFields.MATCHED_RULE_ANNOTATIONS);
                findings.add(
                    new GuardianDecisionLog.Finding(
                        asTextOrNull(annotations.get(GuardianDecisionLogFields.ANNOTATION_GUARDRAIL)),
                        asTextOrNull(rule.get(GuardianDecisionLogFields.MATCHED_RULE_NAME)),
                        asTextOrNull(annotations.get(GuardianDecisionLogFields.ANNOTATION_LOCATION)),
                        asTextOrNull(rule.get(GuardianDecisionLogFields.MATCHED_RULE_EFFECT))
                    )
                );
            }
        });
        return findings;
    }
}
