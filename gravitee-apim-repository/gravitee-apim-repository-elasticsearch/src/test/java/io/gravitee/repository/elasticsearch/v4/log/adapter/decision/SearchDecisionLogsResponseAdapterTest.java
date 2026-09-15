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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import io.gravitee.repository.log.v4.model.decision.DecisionLog;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchDecisionLogsResponseAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returns_an_empty_response_when_the_search_had_no_hits() {
        var response = SearchDecisionLogsResponseAdapter.adapt(new SearchResponse());

        assertThat(response.data()).isEmpty();
        assertThat(response.total()).isZero();
    }

    @Test
    @SneakyThrows
    void carries_every_field_the_document_holds_onto_the_flat_record() {
        var response = responseOf(
            """
            {
              "@timestamp": "2026-08-05T12:03:00.123+02:00",
              "doc-type": "decision",
              "gw-id": "gateway-1",
              "org-id": "org-1",
              "env-id": "env-1",
              "api-id": "api-1",
              "plan-id": "plan-1",
              "app-id": "app-1",
              "event-id": "dec-1",
              "case-id": "case-1",
              "batch-id": "batch-1",
              "phase": "RESOLVED",
              "decision-point-type": "guardian",
              "decision-point-id": "prompt-guardian",
              "decision-point-version": "gpt-4o-2024-11",
              "checkpoint": "response.tool_calls",
              "caller": "pep",
              "subject-type": "User",
              "subject-id": "alice",
              "actor-type": "AgentIdentity",
              "actor-id": "booking-agent",
              "action": "flight_booking",
              "resource-type": "MCPTool",
              "resource-id": "travel.flight_booking",
              "args-hash": "sha256:9c41f2a8e7b3",
              "outcome": "INDETERMINATE",
              "enforced": "ALLOW",
              "verdict": "unsure",
              "indeterminate-cause": "TIMEOUT",
              "confidence": 0.94,
              "reasons": ["Guardian did not answer within 250ms"],
              "matched-rules": [{ "id": "p1#0", "name": "spend-threshold", "effect": "FORBID" }],
              "transformed": true,
              "transformation-type": "REDACT",
              "required-approver": "group:finance",
              "decider-type": "User",
              "decider-id": "frank",
              "channel": "slack",
              "request-id": "req-1",
              "trace-id": "trace-1",
              "conversation-id": "conv-1",
              "mission-id": "mission-1",
              "status": "error",
              "error-type": "TimeoutException",
              "duration-nanos": 4416541,
              "waited-nanos": 60000000000
            }
            """,
            3L
        );

        var result = SearchDecisionLogsResponseAdapter.adapt(response);

        assertThat(result.total()).isEqualTo(3L);
        assertThat(result.data()).hasSize(1);
        var decision = result.data().getFirst();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.timestamp()).isEqualTo(1785924180123L);
            soft.assertThat(decision.gatewayId()).isEqualTo("gateway-1");
            soft.assertThat(decision.organizationId()).isEqualTo("org-1");
            soft.assertThat(decision.environmentId()).isEqualTo("env-1");
            soft.assertThat(decision.apiId()).isEqualTo("api-1");
            soft.assertThat(decision.planId()).isEqualTo("plan-1");
            soft.assertThat(decision.applicationId()).isEqualTo("app-1");
            soft.assertThat(decision.eventId()).isEqualTo("dec-1");
            soft.assertThat(decision.caseId()).isEqualTo("case-1");
            soft.assertThat(decision.batchId()).isEqualTo("batch-1");
            soft.assertThat(decision.phase()).isEqualTo("RESOLVED");
            soft.assertThat(decision.decisionPointType()).isEqualTo("guardian");
            soft.assertThat(decision.decisionPointId()).isEqualTo("prompt-guardian");
            soft.assertThat(decision.decisionPointVersion()).isEqualTo("gpt-4o-2024-11");
            soft.assertThat(decision.checkpoint()).isEqualTo("response.tool_calls");
            soft.assertThat(decision.caller()).isEqualTo("pep");
            soft.assertThat(decision.subjectType()).isEqualTo("User");
            soft.assertThat(decision.subjectId()).isEqualTo("alice");
            soft.assertThat(decision.actorType()).isEqualTo("AgentIdentity");
            soft.assertThat(decision.actorId()).isEqualTo("booking-agent");
            soft.assertThat(decision.action()).isEqualTo("flight_booking");
            soft.assertThat(decision.resourceType()).isEqualTo("MCPTool");
            soft.assertThat(decision.resourceId()).isEqualTo("travel.flight_booking");
            soft.assertThat(decision.argsHash()).isEqualTo("sha256:9c41f2a8e7b3");
            soft.assertThat(decision.outcome()).isEqualTo("INDETERMINATE");
            soft.assertThat(decision.enforced()).isEqualTo("ALLOW");
            soft.assertThat(decision.verdict()).isEqualTo("unsure");
            soft.assertThat(decision.indeterminateCause()).isEqualTo("TIMEOUT");
            soft.assertThat(decision.confidence()).isEqualTo(0.94d);
            soft.assertThat(decision.reasons()).containsExactly("Guardian did not answer within 250ms");
            soft.assertThat(decision.matchedRules()).containsExactly(new DecisionLog.MatchedRule("p1#0", "spend-threshold", "FORBID"));
            soft.assertThat(decision.transformed()).isTrue();
            soft.assertThat(decision.transformationType()).isEqualTo("REDACT");
            soft.assertThat(decision.requiredApprover()).isEqualTo("group:finance");
            soft.assertThat(decision.deciderType()).isEqualTo("User");
            soft.assertThat(decision.deciderId()).isEqualTo("frank");
            soft.assertThat(decision.channel()).isEqualTo("slack");
            soft.assertThat(decision.requestId()).isEqualTo("req-1");
            soft.assertThat(decision.traceId()).isEqualTo("trace-1");
            soft.assertThat(decision.conversationId()).isEqualTo("conv-1");
            soft.assertThat(decision.missionId()).isEqualTo("mission-1");
            soft.assertThat(decision.status()).isEqualTo("error");
            soft.assertThat(decision.errorType()).isEqualTo("TimeoutException");
            soft.assertThat(decision.durationNanos()).isEqualTo(4416541L);
            soft.assertThat(decision.waitedNanos()).isEqualTo(60000000000L);
        });
    }

    @Test
    @SneakyThrows
    void carries_the_point_specific_figures_the_dynamic_template_typed() {
        var response = responseOf(
            """
            {
              "event-id": "dec-2",
              "additional-metrics": {
                "keyword_agent_type": "HOSTED_DELEGATED",
                "double_guardian_cost": 0.5,
                "int_batch_index": 2,
                "bool_fail_open": true,
                "keyword_tool_names": ["book", "cancel"]
              }
            }
            """,
            1L
        );

        var decision = SearchDecisionLogsResponseAdapter.adapt(response).data().getFirst();

        assertThat(decision.additionalMetrics()).contains(
            entry("keyword_agent_type", "HOSTED_DELEGATED"),
            entry("double_guardian_cost", 0.5d),
            entry("int_batch_index", 2),
            entry("bool_fail_open", true),
            entry("keyword_tool_names", List.of("book", "cancel"))
        );
    }

    @Test
    @SneakyThrows
    void leaves_a_field_the_family_does_not_write_null_rather_than_failing() {
        var response = responseOf(
            """
            { "event-id": "dec-3", "outcome": "ALLOW", "enforced": "ALLOW" }
            """,
            1L
        );

        var decision = SearchDecisionLogsResponseAdapter.adapt(response).data().getFirst();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.outcome()).isEqualTo("ALLOW");
            // An ALLOW reaches a conclusion, so it carries no cause for not reaching one.
            soft.assertThat(decision.indeterminateCause()).isNull();
            soft.assertThat(decision.timestamp()).isNull();
            soft.assertThat(decision.caseId()).isNull();
            soft.assertThat(decision.confidence()).isNull();
            soft.assertThat(decision.transformed()).isNull();
            soft.assertThat(decision.durationNanos()).isNull();
            soft.assertThat(decision.waitedNanos()).isNull();
            // The human-approval fields a guardian record never writes.
            soft.assertThat(decision.requiredApprover()).isNull();
            soft.assertThat(decision.deciderId()).isNull();
            soft.assertThat(decision.reasons()).isEmpty();
            soft.assertThat(decision.matchedRules()).isEmpty();
            soft.assertThat(decision.additionalMetrics()).isEmpty();
        });
    }

    @Test
    @SneakyThrows
    void reports_no_timestamp_rather_than_failing_the_page_when_the_stamp_is_unreadable() {
        var response = responseOf(
            """
            { "event-id": "dec-5", "@timestamp": "not-a-date" }
            """,
            1L
        );

        var decision = SearchDecisionLogsResponseAdapter.adapt(response).data().getFirst();

        assertThat(decision.timestamp()).isNull();
        assertThat(decision.eventId()).isEqualTo("dec-5");
    }

    @Test
    @SneakyThrows
    void drops_a_matched_rule_that_carries_no_id_rather_than_reporting_a_null_row() {
        var response = responseOf(
            """
            {
              "event-id": "dec-4",
              "matched-rules": [{ "name": "orphan" }, { "id": "r2", "name": "spend-threshold", "effect": "FORBID" }]
            }
            """,
            1L
        );

        var decision = SearchDecisionLogsResponseAdapter.adapt(response).data().getFirst();

        assertThat(decision.matchedRules()).containsExactly(new DecisionLog.MatchedRule("r2", "spend-threshold", "FORBID"));
    }

    @Test
    void finds_no_decision_when_the_lookup_came_back_without_hits() {
        assertThat(SearchDecisionLogsResponseAdapter.adaptFirst(new SearchResponse())).isEmpty();
    }

    @Test
    void finds_no_decision_when_the_hit_list_is_absent_or_empty() {
        var withoutHitList = new SearchResponse();
        withoutHitList.setSearchHits(new SearchHits());

        var withEmptyHitList = new SearchResponse();
        var emptyHits = new SearchHits();
        emptyHits.setHits(List.of());
        emptyHits.setTotal(new TotalHits(0));
        withEmptyHitList.setSearchHits(emptyHits);

        assertThat(SearchDecisionLogsResponseAdapter.adaptFirst(withoutHitList)).isEmpty();
        assertThat(SearchDecisionLogsResponseAdapter.adaptFirst(withEmptyHitList)).isEmpty();
    }

    @Test
    @SneakyThrows
    void reads_the_one_decision_a_by_id_lookup_returns() {
        var response = responseOf(
            """
            {
              "event-id": "dec-9",
              "api-id": "api-9",
              "phase": "REQUESTED",
              "decision-point-type": "human-approval",
              "outcome": "PENDING",
              "enforced": "SUSPEND"
            }
            """,
            1L
        );

        var decision = SearchDecisionLogsResponseAdapter.adaptFirst(response);

        assertThat(decision).isPresent();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.get().eventId()).isEqualTo("dec-9");
            soft.assertThat(decision.get().apiId()).isEqualTo("api-9");
            // A by-id lookup reads whatever record that id names, open decisions included.
            soft.assertThat(decision.get().phase()).isEqualTo("REQUESTED");
            soft.assertThat(decision.get().outcome()).isEqualTo("PENDING");
            soft.assertThat(decision.get().enforced()).isEqualTo("SUSPEND");
        });
    }

    @SneakyThrows
    private SearchResponse responseOf(String source, long total) {
        var response = new SearchResponse();
        var hits = new SearchHits();
        var hit = new SearchHit();
        hit.setSource(objectMapper.readTree(source));
        hits.setHits(List.of(hit));
        hits.setTotal(new TotalHits(total));
        response.setSearchHits(hits);
        return response;
    }
}
