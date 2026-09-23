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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLog;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthzDecisionLogSourceMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void carries_identity_request_verdict_and_effect_of_a_decision_report() {
        var decision = map(
            """
            {
              "@timestamp": "2026-08-05T12:03:00.123+02:00",
              "event-id": "evt-1",
              "api-id": "api-1",
              "org-id": "org-1",
              "env-id": "env-1",
              "gw-id": "gateway-1",
              "request-id": "req-1",
              "phase": "RESOLVED",
              "decision-point-type": "authz",
              "decision-point-id": "pdp-1",
              "decision-point-version": "7",
              "caller": "pep",
              "status": "success",
              "outcome": "DENY",
              "enforced": "DENY",
              "verdict": "FORBID",
              "matched-rules": [
                { "id": "p1", "name": "deny-writers", "version": "2026-09-23T10:00:00Z", "effect": "FORBID" },
                { "id": "p2", "name": "deny-admins", "effect": "FORBID" }
              ],
              "reasons": ["Forbidden by policy 'deny-writers'"],
              "subject-type": "User",
              "subject-id": "alice",
              "action": "write",
              "resource-type": "Document",
              "resource-id": "doc-1",
              "duration-nanos": 4200
            }
            """
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.eventId()).isEqualTo("evt-1");
            soft.assertThat(decision.timestamp()).isEqualTo(1785924180123L);
            soft.assertThat(decision.apiId()).isEqualTo("api-1");
            soft.assertThat(decision.organizationId()).isEqualTo("org-1");
            soft.assertThat(decision.environmentId()).isEqualTo("env-1");
            soft.assertThat(decision.gatewayId()).isEqualTo("gateway-1");
            soft.assertThat(decision.requestId()).isEqualTo("req-1");
            soft.assertThat(decision.status()).isEqualTo("success");
            soft.assertThat(decision.caller()).isEqualTo("pep");
            soft.assertThat(decision.targetPdpId()).isEqualTo("pdp-1");
            soft.assertThat(decision.policyGeneration()).isEqualTo("7");
            soft.assertThat(decision.decision()).isEqualTo("FORBID");
            soft.assertThat(decision.outcome()).isEqualTo("DENY");
            soft.assertThat(decision.enforced()).isEqualTo("DENY");
            soft.assertThat(decision.indeterminateCause()).isNull();
            soft
                .assertThat(decision.matchedRules())
                .containsExactly(
                    new AuthzDecisionLog.MatchedRule("p1", "deny-writers", "2026-09-23T10:00:00Z", "FORBID"),
                    new AuthzDecisionLog.MatchedRule("p2", "deny-admins", null, "FORBID")
                );
            soft.assertThat(decision.reasons()).containsExactly("Forbidden by policy 'deny-writers'");
            soft.assertThat(decision.subjectType()).isEqualTo("User");
            soft.assertThat(decision.subjectId()).isEqualTo("alice");
            soft.assertThat(decision.action()).isEqualTo("write");
            soft.assertThat(decision.resourceType()).isEqualTo("Document");
            soft.assertThat(decision.resourceId()).isEqualTo("doc-1");
            soft.assertThat(decision.durationNanos()).isEqualTo(4200L);
        });
    }

    @Test
    void carries_the_cause_and_the_error_of_a_decision_the_pdp_could_not_take() {
        var decision = map(
            """
            {
              "event-id": "evt-2",
              "status": "error",
              "outcome": "INDETERMINATE",
              "enforced": "DENY",
              "indeterminate-cause": "NOT_READY",
              "error-type": "NoSnapshotException"
            }
            """
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.status()).isEqualTo("error");
            soft.assertThat(decision.outcome()).isEqualTo("INDETERMINATE");
            soft.assertThat(decision.enforced()).isEqualTo("DENY");
            soft.assertThat(decision.indeterminateCause()).isEqualTo("NOT_READY");
            soft.assertThat(decision.errorType()).isEqualTo("NoSnapshotException");
            soft.assertThat(decision.decision()).isNull();
        });
    }

    @Test
    void reads_the_batch_position_from_the_additional_metrics() {
        var decision = map(
            """
            {
              "event-id": "evt-3",
              "batch-id": "batch-1",
              "additional-metrics": { "int_authz_batch-index": 1, "int_authz_batch-size": 2 }
            }
            """
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.batchId()).isEqualTo("batch-1");
            soft.assertThat(decision.batchIndex()).isEqualTo(1);
            soft.assertThat(decision.batchSize()).isEqualTo(2);
        });
    }

    @Test
    void leaves_absent_fields_null_and_absent_lists_empty() {
        var decision = map(
            """
            { "event-id": "evt-4" }
            """
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.timestamp()).isNull();
            soft.assertThat(decision.decision()).isNull();
            soft.assertThat(decision.policyGeneration()).isNull();
            soft.assertThat(decision.batchIndex()).isNull();
            soft.assertThat(decision.batchSize()).isNull();
            soft.assertThat(decision.matchedRules()).isEmpty();
            soft.assertThat(decision.reasons()).isEmpty();
        });
    }

    @Test
    void drops_a_matched_rule_that_carries_no_name_rather_than_reporting_a_blank_row() {
        var decision = map(
            """
            {
              "event-id": "evt-5",
              "matched-rules": [{ "id": "p1" }, { "id": "p2", "name": "allow-admins" }]
            }
            """
        );

        assertThat(decision.matchedRules()).extracting(AuthzDecisionLog.MatchedRule::name).containsExactly("allow-admins");
    }

    @Test
    void reports_no_timestamp_rather_than_failing_the_page_when_the_stamp_is_unreadable() {
        var decision = map(
            """
            { "event-id": "evt-6", "@timestamp": "not-a-date" }
            """
        );

        assertThat(decision.timestamp()).isNull();
        assertThat(decision.eventId()).isEqualTo("evt-6");
    }

    @SneakyThrows
    private static AuthzDecisionLog map(String source) {
        return AuthzDecisionLogSourceMapper.from(MAPPER.readTree(source));
    }
}
