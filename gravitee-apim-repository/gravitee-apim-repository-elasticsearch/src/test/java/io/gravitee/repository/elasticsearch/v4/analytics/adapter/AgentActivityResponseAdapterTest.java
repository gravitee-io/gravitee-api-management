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
package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AgentActivityResponseAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentActivityResponseAdapter adapter = new AgentActivityResponseAdapter();

    @Nested
    class EmptyAndNull {

        @Test
        void should_return_empty_result_when_no_hop_hits() {
            var result = adapter.adapt(new SearchResponse(), null, 0, 25);
            assertThat(result.runs()).isEmpty();
            assertThat(result.total()).isZero();
        }
    }

    @Nested
    class HopParsing {

        @Test
        @SneakyThrows
        void should_parse_inbound_a2a_hop() {
            var hopJson =
                "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000, \"entrypoint-id\": \"a2a-agent\", \"uri\": \"/agent/card\", \"status\": 200, \"trace-id\": \"abc123\", \"additional-metrics\": { \"keyword_gravitee_conversation-id\": \"conv-1\" } }";
            var result = adapter.adapt(hopResponse(hopJson), null, 0, 25);
            assertThat(result.runs()).hasSize(1);
            var hop = result.runs().get(0).getHops().get(0);
            assertThat(hop.getRequestId()).isEqualTo("req-1");
            assertThat(hop.getKind()).isEqualTo("inbound");
            assertThat(hop.getTimestamp()).isEqualTo(1700000000000L);
            assertThat(hop.getStatus()).isEqualTo(200);
            assertThat(hop.getTraceId()).isEqualTo("abc123");
            assertThat(hop.getConversationId()).isEqualTo("conv-1");
            assertThat(hop.getCost()).isZero();
        }

        @Test
        @SneakyThrows
        void should_parse_iso_timestamp_from_elasticsearch_source() {
            var hopJson = "{ \"request-id\": \"req-iso\", \"@timestamp\": \"2026-09-14T16:07:42.060Z\", \"entrypoint-id\": \"a2a-proxy\" }";
            var result = adapter.adapt(hopResponse(hopJson), null, 0, 25);
            assertThat(result.runs().get(0).getHops().get(0).getTimestamp()).isEqualTo(1789402062060L);
        }

        @Test
        @SneakyThrows
        void should_parse_llm_call_hop() {
            var hopJson =
                "{ \"request-id\": \"req-llm\", \"@timestamp\": 1700000001000, \"entrypoint-id\": \"llm-proxy\", \"additional-metrics\": { \"keyword_gravitee_conversation-id\": \"conv-1\", \"keyword_llm-proxy_model\": \"gpt-4o\", \"keyword_llm-proxy_provider\": \"openai\", \"double_llm-proxy_sent-cost\": 1500, \"double_llm-proxy_received-cost\": 500 } }";
            var result = adapter.adapt(hopResponse(hopJson), null, 0, 25);
            var hop = result.runs().get(0).getHops().get(0);
            assertThat(hop.getKind()).isEqualTo("llm-call");
            assertThat(hop.getLabel()).isEqualTo("LLM call: gpt-4o");
            assertThat(hop.getDetail()).isEqualTo("openai");
            assertThat(hop.getCost()).isEqualTo(2000L);
        }

        @Test
        @SneakyThrows
        void should_parse_mcp_call_hop() {
            var hopJson =
                "{ \"request-id\": \"req-mcp\", \"@timestamp\": 1700000002000, \"entrypoint-id\": \"mcp-proxy\", \"additional-metrics\": { \"keyword_mcp-proxy_tools/call\": \"list_prs\", \"keyword_mcp-proxy_resources/read\": \"github\", \"double_mcp-proxy_tool-cost\": 750 } }";
            var result = adapter.adapt(hopResponse(hopJson), null, 0, 25);
            var hop = result.runs().get(0).getHops().get(0);
            assertThat(hop.getKind()).isEqualTo("mcp-call");
            assertThat(hop.getLabel()).isEqualTo("MCP tool: list_prs");
            assertThat(hop.getDetail()).isEqualTo("github");
            assertThat(hop.getCost()).isEqualTo(750L);
        }

        @Test
        @SneakyThrows
        void should_fallback_to_id_field() {
            var hopJson = "{ \"id\": \"req-v2-fallback\", \"@timestamp\": 1700000000000 }";
            var result = adapter.adapt(hopResponse(hopJson), null, 0, 25);
            assertThat(result.runs()).hasSize(1);
            assertThat(result.runs().get(0).getHops().get(0).getRequestId()).isEqualTo("req-v2-fallback");
        }

        @Test
        @SneakyThrows
        void should_skip_hit_without_request_id() {
            var hopJson = "{ \"@timestamp\": 1700000000000 }";
            var result = adapter.adapt(hopResponse(hopJson), null, 0, 25);
            assertThat(result.runs()).isEmpty();
        }
    }

    @Nested
    class DecisionJoin {

        @Test
        @SneakyThrows
        void should_join_decisions_onto_hops_by_request_id() {
            var hopJson = "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000 }";
            var decisionJson =
                "{ \"requestId\": \"req-1\", \"@timestamp\": 1700000000500, \"decisionPointType\": \"authz\", \"outcome\": \"ALLOW\", \"enforced\": \"ALLOW\", \"phase\": \"RESOLVED\", \"eventId\": \"dec-1\", \"reasons\": [\"Permitted\"] }";
            var result = adapter.adapt(hopResponse(hopJson), decisionResponse(decisionJson), 0, 25);
            assertThat(result.runs()).hasSize(1);
            var decisions = result.runs().get(0).getHops().get(0).getDecisions();
            assertThat(decisions).hasSize(1);
            assertThat(decisions.get(0).getDecisionPointType()).isEqualTo("authz");
            assertThat(decisions.get(0).getOutcome()).isEqualTo("ALLOW");
            assertThat(decisions.get(0).getReason()).isEqualTo("Permitted");
        }

        @Test
        @SneakyThrows
        void should_join_kebab_case_decision_fields_from_decisions_stream() {
            var hopJson = "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000 }";
            var decisionJson =
                "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000500, \"decision-point-type\": \"human-approval\", \"outcome\": \"PENDING\", \"enforced\": \"SUSPEND\", \"phase\": \"REQUESTED\", \"event-id\": \"hitl-req\", \"reasons\": [\"Needs manager approval\"] }";
            var result = adapter.adapt(hopResponse(hopJson), decisionResponse(decisionJson), 0, 25);
            var decisions = result.runs().get(0).getHops().get(0).getDecisions();
            assertThat(decisions).hasSize(1);
            assertThat(decisions.get(0).getDecisionPointType()).isEqualTo("human-approval");
            assertThat(decisions.get(0).getPhase()).isEqualTo("REQUESTED");
            assertThat(result.runs().get(0).getOutcome()).isEqualTo("waiting");
        }

        @Test
        @SneakyThrows
        void should_join_kebab_case_guardian_decision() {
            var hopJson = "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000 }";
            var decisionJson =
                "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000500, \"decision-point-type\": \"guardian\", \"outcome\": \"DENY\", \"enforced\": \"DENY\", \"phase\": \"RESOLVED\", \"event-id\": \"g-1\" }";
            var result = adapter.adapt(hopResponse(hopJson), decisionResponse(decisionJson), 0, 25);
            var decision = result.runs().get(0).getHops().get(0).getDecisions().get(0);
            assertThat(decision.getDecisionPointType()).isEqualTo("guardian");
            assertThat(decision.getOutcome()).isEqualTo("DENY");
            assertThat(result.runs().get(0).getOutcome()).isEqualTo("stopped");
        }
    }

    @Nested
    class HitlPhaseCollapse {

        @Test
        @SneakyThrows
        void should_collapse_requested_and_resolved_into_one_intervention() {
            var hopJson = "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000 }";
            var requested =
                "{ \"requestId\": \"req-1\", \"@timestamp\": 1700000001000, \"decisionPointType\": \"human-approval\", \"outcome\": \"PENDING\", \"phase\": \"REQUESTED\", \"eventId\": \"hitl-req\", \"reasons\": [\"Needs manager approval\"] }";
            var resolved =
                "{ \"requestId\": \"req-1\", \"@timestamp\": 1700000002000, \"decisionPointType\": \"human-approval\", \"outcome\": \"ALLOW\", \"enforced\": \"ALLOW\", \"phase\": \"RESOLVED\", \"eventId\": \"hitl-res\", \"deciderId\": \"manager-1\", \"deciderType\": \"person\", \"reasons\": [\"Approved\"] }";
            var result = adapter.adapt(hopResponse(hopJson), decisionResponse(requested, resolved), 0, 25);
            var decisions = result.runs().get(0).getHops().get(0).getDecisions();
            assertThat(decisions).hasSize(1);
            assertThat(decisions.get(0).getDecisionPointType()).isEqualTo("human-approval");
            assertThat(decisions.get(0).getOutcome()).isEqualTo("ALLOW");
            assertThat(decisions.get(0).getDecider()).isEqualTo("person: manager-1");
            assertThat(decisions.get(0).getReason()).isEqualTo("Needs manager approval");
            assertThat(decisions.get(0).getDecisionId()).isEqualTo("hitl-res");
        }

        @Test
        @SneakyThrows
        void should_leave_unmatched_requested_marking_outcome_waiting() {
            var hopJson = "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000 }";
            var requested =
                "{ \"requestId\": \"req-1\", \"@timestamp\": 1700000001000, \"decisionPointType\": \"human-approval\", \"outcome\": \"PENDING\", \"phase\": \"REQUESTED\", \"eventId\": \"hitl-req\" }";
            var result = adapter.adapt(hopResponse(hopJson), decisionResponse(requested), 0, 25);
            var decisions = result.runs().get(0).getHops().get(0).getDecisions();
            assertThat(decisions).hasSize(1);
            assertThat(result.runs().get(0).getOutcome()).isEqualTo("waiting");
        }
    }

    @Nested
    class OutcomeDerivation {

        @Test
        @SneakyThrows
        void should_derive_done_when_only_policy_allows() {
            var hopJson = "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000 }";
            var decJson =
                "{ \"requestId\": \"req-1\", \"@timestamp\": 1700000001000, \"decisionPointType\": \"authz\", \"outcome\": \"ALLOW\", \"phase\": \"RESOLVED\", \"eventId\": \"dec-1\" }";
            var result = adapter.adapt(hopResponse(hopJson), decisionResponse(decJson), 0, 25);
            assertThat(result.runs().get(0).getOutcome()).isEqualTo("done");
        }

        @Test
        @SneakyThrows
        void should_derive_stopped_when_any_deny() {
            var hopJson = "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000 }";
            var decJson =
                "{ \"requestId\": \"req-1\", \"@timestamp\": 1700000001000, \"decisionPointType\": \"authz\", \"outcome\": \"DENY\", \"phase\": \"RESOLVED\", \"eventId\": \"dec-1\" }";
            var result = adapter.adapt(hopResponse(hopJson), decisionResponse(decJson), 0, 25);
            assertThat(result.runs().get(0).getOutcome()).isEqualTo("stopped");
        }

        @Test
        @SneakyThrows
        void should_derive_done_with_changes_when_human_approval_allowed() {
            var hopJson = "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000 }";
            var decJson =
                "{ \"requestId\": \"req-1\", \"@timestamp\": 1700000001000, \"decisionPointType\": \"human-approval\", \"outcome\": \"ALLOW\", \"phase\": \"RESOLVED\", \"eventId\": \"dec-1\" }";
            var result = adapter.adapt(hopResponse(hopJson), decisionResponse(decJson), 0, 25);
            assertThat(result.runs().get(0).getOutcome()).isEqualTo("done-with-changes");
        }

        @Test
        @SneakyThrows
        void should_derive_done_when_no_decisions() {
            var hopJson = "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000 }";
            var result = adapter.adapt(hopResponse(hopJson), null, 0, 25);
            assertThat(result.runs().get(0).getOutcome()).isEqualTo("done");
        }
    }

    @Nested
    class ConversationGrouping {

        @Test
        @SneakyThrows
        void should_group_hops_with_same_conversation_id_into_one_run() {
            var hop1 =
                "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000, \"additional-metrics\": { \"keyword_gravitee_conversation-id\": \"conv-1\" } }";
            var hop2 =
                "{ \"request-id\": \"req-2\", \"@timestamp\": 1700000001000, \"additional-metrics\": { \"keyword_gravitee_conversation-id\": \"conv-1\" } }";
            var result = adapter.adapt(hopResponse(hop1, hop2), null, 0, 25);
            assertThat(result.runs()).hasSize(1);
            assertThat(result.runs().get(0).getConversationId()).isEqualTo("conv-1");
            assertThat(result.runs().get(0).getHops()).hasSize(2);
            assertThat(result.runs().get(0).getStartedAt()).isEqualTo(1700000000000L);
            assertThat(result.runs().get(0).getLastEventAt()).isEqualTo(1700000001000L);
        }

        @Test
        @SneakyThrows
        void should_create_separate_runs_for_hops_without_conversation_id() {
            var hop1 = "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000 }";
            var hop2 = "{ \"request-id\": \"req-2\", \"@timestamp\": 1700000001000 }";
            var result = adapter.adapt(hopResponse(hop1, hop2), null, 0, 25);
            assertThat(result.runs()).hasSize(2);
            assertThat(result.runs().get(0).getConversationId()).isNull();
            assertThat(result.runs().get(1).getConversationId()).isNull();
        }

        @Test
        @SneakyThrows
        void should_sort_runs_newest_first() {
            var hop1 = "{ \"request-id\": \"req-1\", \"@timestamp\": 1700000000000 }";
            var hop2 = "{ \"request-id\": \"req-2\", \"@timestamp\": 1700000002000 }";
            var result = adapter.adapt(hopResponse(hop1, hop2), null, 0, 25);
            assertThat(result.runs()).hasSize(2);
            assertThat(result.runs().get(0).getStartedAt()).isEqualTo(1700000002000L);
        }
    }

    // ---- helpers ----

    @SafeVarargs
    @SneakyThrows
    private SearchResponse hopResponse(String... sources) {
        return hitsResponse(sources);
    }

    @SafeVarargs
    @SneakyThrows
    private SearchResponse decisionResponse(String... sources) {
        return hitsResponse(sources);
    }

    @SafeVarargs
    @SneakyThrows
    private SearchResponse hitsResponse(String... sources) {
        var response = new SearchResponse();
        var hits = new SearchHits();
        var hitList = new SearchHit[sources.length];
        for (int i = 0; i < sources.length; i++) {
            var hit = new SearchHit();
            hit.setSource(objectMapper.readTree(sources[i]));
            hitList[i] = hit;
        }
        hits.setHits(List.of(hitList));
        hits.setTotal(new TotalHits(sources.length));
        response.setSearchHits(hits);
        return response;
    }
}
