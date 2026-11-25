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
package io.gravitee.repository.elasticsearch.v4.log.adapter.guardrail;

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asLongOrNull;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextList;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.epochMillisOrNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.repository.log.v4.model.guardrail.GuardrailDecisionLog;
import java.util.ArrayList;
import java.util.List;

/**
 * @author GraviteeSource Team
 */
final class GuardrailDecisionLogSourceMapper {

    private GuardrailDecisionLogSourceMapper() {}

    static GuardrailDecisionLog from(JsonNode source) {
        return GuardrailDecisionLog.builder()
            .eventId(asTextOrNull(source.get(GuardrailDecisionLogFields.EVENT_ID)))
            .timestamp(epochMillisOrNull(asTextOrNull(source.get(GuardrailDecisionLogFields.TIMESTAMP))))
            .apiId(asTextOrNull(source.get(GuardrailDecisionLogFields.API_ID)))
            .requestId(asTextOrNull(source.get(GuardrailDecisionLogFields.REQUEST_ID)))
            .guardrailId(asTextOrNull(source.get(GuardrailDecisionLogFields.GUARDRAIL_ID)))
            .revision(asTextOrNull(source.get(GuardrailDecisionLogFields.REVISION)))
            .checkpoint(asTextOrNull(source.get(GuardrailDecisionLogFields.CHECKPOINT)))
            .outcome(asTextOrNull(source.get(GuardrailDecisionLogFields.OUTCOME)))
            .enforced(asTextOrNull(source.get(GuardrailDecisionLogFields.ENFORCED)))
            .transformationType(asTextOrNull(source.get(GuardrailDecisionLogFields.TRANSFORMATION_TYPE)))
            .verdict(asTextOrNull(source.get(GuardrailDecisionLogFields.VERDICT)))
            .reasons(asTextList(source.get(GuardrailDecisionLogFields.REASONS)))
            .findings(findings(source.get(GuardrailDecisionLogFields.MATCHED_RULES)))
            .durationNanos(asLongOrNull(source.get(GuardrailDecisionLogFields.DURATION_NANOS)))
            .build();
    }

    /**
     * A finding is a matched rule and where the gateway says it matched: the guardrail policy and the location are annotations.
     * A guardrail policy making a single check names no rule, so a finding without a name is kept, its rule left empty.
     */
    private static List<GuardrailDecisionLog.Finding> findings(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        var findings = new ArrayList<GuardrailDecisionLog.Finding>(node.size());
        node.forEach(rule -> {
            if (rule.isObject()) {
                var annotations = rule.path(GuardrailDecisionLogFields.MATCHED_RULE_ANNOTATIONS);
                findings.add(
                    new GuardrailDecisionLog.Finding(
                        asTextOrNull(annotations.get(GuardrailDecisionLogFields.ANNOTATION_GUARDRAIL)),
                        asTextOrNull(rule.get(GuardrailDecisionLogFields.MATCHED_RULE_NAME)),
                        asTextOrNull(annotations.get(GuardrailDecisionLogFields.ANNOTATION_LOCATION)),
                        asTextOrNull(rule.get(GuardrailDecisionLogFields.MATCHED_RULE_EFFECT))
                    )
                );
            }
        });
        return findings;
    }
}
