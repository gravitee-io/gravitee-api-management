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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLog;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLog.MatchedRule;
import java.util.Map;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianDecisionLogSourceMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void carries_every_field_of_a_denial_the_gateway_reported() {
        var decision = map(
            """
            {
              "@timestamp": "2026-11-25T13:28:13.200Z",
              "gw-id": "gw-1",
              "org-id": "org-1",
              "env-id": "env-1",
              "api-id": "api-1",
              "plan-id": "plan-1",
              "app-id": "app-1",
              "event-id": "evt-1",
              "phase": "RESOLVED",
              "decision-point-type": "guardian",
              "decision-point-id": "guardian-1",
              "decision-point-version": "1795613284993",
              "checkpoint": "request.llm-proxy",
              "caller": "Number check",
              "subject-type": "user",
              "subject-id": "alice",
              "actor-type": "agent",
              "actor-id": "app-1",
              "outcome": "DENY",
              "enforced": "DENY",
              "verdict": "GUARDRAIL_BLOCKED",
              "reasons": ["zz: BLOCK — Blocked by guardrail; llm-rule matched: messages[0].user"],
              "matched-rules": [
                { "id": "", "effect": "DENY", "annotations": { "guardrail": "zz", "location": "messages[0].user" } }
              ],
              "request-id": "req-1",
              "trace-id": "trace-1",
              "status": "success",
              "duration-nanos": 7794875
            }
            """
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.eventId()).isEqualTo("evt-1");
            soft.assertThat(decision.timestamp()).isEqualTo(1795613293200L);
            soft.assertThat(decision.gatewayId()).isEqualTo("gw-1");
            soft.assertThat(decision.organizationId()).isEqualTo("org-1");
            soft.assertThat(decision.environmentId()).isEqualTo("env-1");
            soft.assertThat(decision.apiId()).isEqualTo("api-1");
            soft.assertThat(decision.planId()).isEqualTo("plan-1");
            soft.assertThat(decision.applicationId()).isEqualTo("app-1");
            soft.assertThat(decision.guardianId()).isEqualTo("guardian-1");
            soft.assertThat(decision.guardianRevision()).isEqualTo("1795613284993");
            soft.assertThat(decision.checkpoint()).isEqualTo("request.llm-proxy");
            soft.assertThat(decision.caller()).isEqualTo("Number check");
            soft.assertThat(decision.subjectType()).isEqualTo("user");
            soft.assertThat(decision.subjectId()).isEqualTo("alice");
            soft.assertThat(decision.actorType()).isEqualTo("agent");
            soft.assertThat(decision.actorId()).isEqualTo("app-1");
            soft.assertThat(decision.outcome()).isEqualTo("DENY");
            soft.assertThat(decision.enforced()).isEqualTo("DENY");
            soft.assertThat(decision.verdict()).isEqualTo("GUARDRAIL_BLOCKED");
            soft.assertThat(decision.reasons()).containsExactly("zz: BLOCK — Blocked by guardrail; llm-rule matched: messages[0].user");
            soft.assertThat(decision.requestId()).isEqualTo("req-1");
            soft.assertThat(decision.traceId()).isEqualTo("trace-1");
            soft.assertThat(decision.status()).isEqualTo("success");
            soft.assertThat(decision.durationNanos()).isEqualTo(7794875L);
            soft.assertThat(decision.transformed()).isNull();
            soft.assertThat(decision.transformationType()).isNull();
            soft.assertThat(decision.indeterminateCause()).isNull();
            soft.assertThat(decision.errorType()).isNull();
        });
    }

    @Test
    void keeps_a_finding_without_rule_name_and_reads_the_empty_rule_id_the_template_writes_as_absent() {
        var decision = map(
            """
            {
              "event-id": "evt-1",
              "matched-rules": [
                { "id": "", "effect": "DENY", "annotations": { "guardrail": "zz", "location": "messages[0].user" } },
                { "id": "rule-1", "name": "rule-1", "version": "2", "effect": "TRANSFORM" }
              ]
            }
            """
        );

        assertThat(decision.matchedRules()).containsExactly(
            new MatchedRule(null, null, null, "DENY", Map.of("guardrail", "zz", "location", "messages[0].user")),
            new MatchedRule("rule-1", "rule-1", "2", "TRANSFORM", Map.of())
        );
    }

    @Test
    void reads_a_transformation() {
        var decision = map(
            """
            {
              "event-id": "evt-2",
              "checkpoint": "response.llm-proxy",
              "outcome": "TRANSFORM",
              "enforced": "TRANSFORM",
              "transformed": true,
              "transformation-type": "REDACT"
            }
            """
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.outcome()).isEqualTo("TRANSFORM");
            soft.assertThat(decision.transformed()).isTrue();
            soft.assertThat(decision.transformationType()).isEqualTo("REDACT");
        });
    }

    @Test
    void reads_why_a_guardian_could_not_decide() {
        var decision = map(
            """
            {
              "event-id": "evt-3",
              "decision-point-id": "guardian-1",
              "outcome": "INDETERMINATE",
              "enforced": "DENY",
              "indeterminate-cause": "NOT_READY",
              "status": "error",
              "error-type": "GUARDIAN_NOT_DEPLOYED"
            }
            """
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.outcome()).isEqualTo("INDETERMINATE");
            soft.assertThat(decision.indeterminateCause()).isEqualTo("NOT_READY");
            soft.assertThat(decision.status()).isEqualTo("error");
            soft.assertThat(decision.errorType()).isEqualTo("GUARDIAN_NOT_DEPLOYED");
            soft.assertThat(decision.guardianRevision()).isNull();
        });
    }

    @Test
    void leaves_absent_fields_null_and_absent_lists_empty() {
        var decision = map(
            """
            { "event-id": "evt-4", "@timestamp": "not-a-date" }
            """
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.timestamp()).isNull();
            soft.assertThat(decision.durationNanos()).isNull();
            soft.assertThat(decision.additionalMetrics()).isNull();
            soft.assertThat(decision.reasons()).isEmpty();
            soft.assertThat(decision.matchedRules()).isEmpty();
        });
    }

    @Test
    void reads_the_additional_metrics_as_a_map() {
        var decision = map(
            """
            { "event-id": "evt-5", "additional-metrics": { "keyword_custom_tag": "a", "long_custom_count": 2 } }
            """
        );

        assertThat(decision.additionalMetrics()).containsExactlyInAnyOrderEntriesOf(
            Map.of("keyword_custom_tag", "a", "long_custom_count", 2)
        );
    }

    @SneakyThrows
    private static GuardianDecisionLog map(String source) {
        return GuardianDecisionLogSourceMapper.from(MAPPER.readTree(source));
    }
}
