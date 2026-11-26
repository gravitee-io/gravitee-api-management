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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.log.v4.model.guardrail.GuardrailDecisionLog;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardrailDecisionLogSourceMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void reads_a_decision_that_redacted_a_response() {
        var decision = map(
            """
            {
              "@timestamp": "2026-11-23T10:15:30.250+01:00",
              "gw-id": "gw-1", "org-id": "org-1", "env-id": "env-1", "api-id": "api-1",
              "event-id": "evt-1", "phase": "RESOLVED",
              "decision-point-type": "guardrail", "decision-point-id": "guardrail-a", "decision-point-version": "3",
              "checkpoint": "response.llm-proxy", "caller": "Guardrail",
              "subject-type": "user", "subject-id": "alice", "actor-type": "agent", "actor-id": "app-1",
              "outcome": "TRANSFORM", "enforced": "TRANSFORM", "transformed": true, "transformation-type": "REDACT", "verdict": "REDACT",
              "reasons": ["Redact PII: REDACT — 1 passage"],
              "matched-rules": [
                { "id": "EMAIL", "name": "EMAIL", "effect": "TRANSFORM",
                  "annotations": { "guardrail": "Redact PII", "location": "$.choices[0].message.content" } }
              ],
              "request-id": "req-1", "trace-id": "trace-1", "status": "success", "duration-nanos": 9100000
            }
            """
        );

        assertThat(decision).isEqualTo(
            GuardrailDecisionLog.builder()
                .eventId("evt-1")
                .timestamp(OffsetDateTime.parse("2026-11-23T10:15:30.250+01:00").toInstant().toEpochMilli())
                .apiId("api-1")
                .requestId("req-1")
                .guardrailId("guardrail-a")
                .revision("3")
                .checkpoint("response.llm-proxy")
                .outcome("TRANSFORM")
                .enforced("TRANSFORM")
                .transformationType("REDACT")
                .verdict("REDACT")
                .steps(List.of(new GuardrailDecisionLog.Step("Redact PII", "REDACT", "1 passage")))
                .findings(List.of(new GuardrailDecisionLog.Finding("Redact PII", "EMAIL", "$.choices[0].message.content", "TRANSFORM")))
                .durationNanos(9_100_000L)
                .build()
        );
    }

    @Test
    void reads_a_decision_of_a_guardrail_that_is_not_deployed() {
        var decision = map(
            """
            {
              "@timestamp": "2026-11-23T10:15:30.250+01:00",
              "event-id": "evt-2", "decision-point-id": "guardrail-b",
              "outcome": "INDETERMINATE", "enforced": "DENY", "indeterminate-cause": "NOT_READY",
              "status": "error", "error-type": "GUARDRAIL_NOT_DEPLOYED"
            }
            """
        );

        assertThat(decision.revision()).isNull();
        assertThat(decision.outcome()).isEqualTo("INDETERMINATE");
        assertThat(decision.enforced()).isEqualTo("DENY");
        assertThat(decision.transformationType()).isNull();
        assertThat(decision.durationNanos()).isNull();
        assertThat(decision.steps()).isEmpty();
        assertThat(decision.findings()).isEmpty();
    }

    /** The gateway writes one line per guardrail policy: "<policy>: <verdict>", then " — <reasons>" when it gave any. */
    @Test
    void reads_the_guardrail_policy_sequence_from_the_lines_the_gateway_writes() {
        var decision = map(
            """
            { "reasons": ["Topic check: CONTINUE", "Block credentials: BLOCK — Blocked by guardrail; llm-rule matched: messages[0].user"] }
            """
        );

        assertThat(decision.steps()).containsExactly(
            new GuardrailDecisionLog.Step("Topic check", "CONTINUE", null),
            new GuardrailDecisionLog.Step("Block credentials", "BLOCK", "Blocked by guardrail; llm-rule matched: messages[0].user")
        );
    }

    /** A name its findings carry is matched whole, even when it holds ": " itself. */
    @Test
    void keeps_a_guardrail_policy_name_holding_a_colon_whole_when_its_findings_name_it() {
        var decision = map(
            """
            {
              "reasons": ["PII: email: REDACT — 1 passage"],
              "matched-rules": [ { "id": "EMAIL", "name": "EMAIL", "effect": "TRANSFORM", "annotations": { "guardrail": "PII: email" } } ]
            }
            """
        );

        assertThat(decision.steps()).containsExactly(new GuardrailDecisionLog.Step("PII: email", "REDACT", "1 passage"));
    }

    @Test
    void keeps_a_line_it_cannot_split_as_the_guardrail_policy_name() {
        assertThat(map("{ \"reasons\": [\"unexpected\"] }").steps()).containsExactly(
            new GuardrailDecisionLog.Step("unexpected", null, null)
        );
    }

    @Test
    void reads_a_finding_without_annotations() {
        var decision = map(
            """
            { "matched-rules": [ { "id": "EMAIL", "name": "EMAIL", "effect": "DENY" } ] }
            """
        );

        assertThat(decision.findings()).containsExactly(new GuardrailDecisionLog.Finding(null, "EMAIL", null, "DENY"));
    }

    @Test
    void reads_a_finding_of_a_guardrail_policy_making_a_single_check() {
        var decision = map(
            """
            { "matched-rules": [ { "id": "", "effect": "DENY", "annotations": { "guardrail": "Block credentials", "location": "messages[0].user" } } ] }
            """
        );

        assertThat(decision.findings()).containsExactly(
            new GuardrailDecisionLog.Finding("Block credentials", null, "messages[0].user", "DENY")
        );
    }

    @Test
    void leaves_the_time_empty_when_it_cannot_be_read() {
        assertThat(map("{ \"@timestamp\": \"yesterday\" }").timestamp()).isNull();
    }

    @SneakyThrows
    private static GuardrailDecisionLog map(String source) {
        return GuardrailDecisionLogSourceMapper.from(MAPPER.readTree(source));
    }
}
