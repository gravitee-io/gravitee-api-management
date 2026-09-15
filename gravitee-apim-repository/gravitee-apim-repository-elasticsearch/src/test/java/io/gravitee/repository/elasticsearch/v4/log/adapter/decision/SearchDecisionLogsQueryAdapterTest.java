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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.log.v4.model.decision.DecisionLogQuery;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchDecisionLogsQueryAdapterTest {

    @Test
    void builds_a_paginated_query_pinned_to_one_decision_point_family_and_to_settled_records() {
        var query = DecisionLogQuery.builder().decisionPointType("guardian").from(1000L).to(2000L).page(2).size(10).build();

        var result = SearchDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result).isEqualTo(
            """
            {
              "query": {
                "bool": {
                  "filter": [
                    { "term": { "decision-point-type": "guardian" } },
                    { "term": { "phase": "RESOLVED" } },
                    { "range": { "@timestamp": { "gte": 1000, "lte": 2000 } } }
                  ]
                }
              },
              "from": 10,
              "size": 10,
              "track_total_hits": true,
              "sort": [
                { "@timestamp": { "order": "desc" } },
                { "event-id": { "order": "asc", "unmapped_type": "keyword" } }
              ]
            }
            """
        );
    }

    @Test
    void pins_the_family_so_the_shared_index_cannot_leak_another_kind_of_decision() {
        var result = SearchDecisionLogsQueryAdapter.adapt(DecisionLogQuery.builder().decisionPointType("human-approval").build());

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"term\": { \"decision-point-type\": \"human-approval\" } }"));
    }

    @Test
    void pins_the_phase_so_a_settled_consultation_is_not_read_twice() {
        var result = SearchDecisionLogsQueryAdapter.adapt(DecisionLogQuery.builder().decisionPointType("guardian").build());

        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"term\": { \"phase\": \"RESOLVED\" } }"));
    }

    @Test
    void breaks_timestamp_ties_on_event_id_so_paging_cannot_repeat_or_skip_a_decision() {
        var result = SearchDecisionLogsQueryAdapter.adapt(DecisionLogQuery.builder().decisionPointType("guardian").build());

        assertThatJson(result)
            .inPath("$.sort[1]")
            .isEqualTo(
                """
                { "event-id": { "order": "asc", "unmapped_type": "keyword" } }
                """
            );
    }

    @Test
    void omits_the_timestamp_range_when_no_bound_is_given() {
        var result = SearchDecisionLogsQueryAdapter.adapt(DecisionLogQuery.builder().decisionPointType("guardian").build());

        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(2);
        assertThatJson(result).inPath("$.from").isEqualTo(0);
        assertThatJson(result).inPath("$.size").isEqualTo(20);
    }

    @Test
    void keeps_an_open_ended_range_when_only_one_bound_is_given() {
        var result = SearchDecisionLogsQueryAdapter.adapt(DecisionLogQuery.builder().decisionPointType("guardian").from(1000L).build());

        assertThatJson(result)
            .inPath("$.query.bool.filter[2].range.@timestamp")
            .isEqualTo(
                """
                { "gte": 1000 }
                """
            );
    }

    @Test
    void omits_every_optional_clause_when_none_is_requested() {
        var result = SearchDecisionLogsQueryAdapter.adapt(DecisionLogQuery.builder().decisionPointType("guardian").build());

        // Family and phase only: an empty terms clause would match nothing and silently empty the table.
        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(2);
    }

    @Test
    void narrows_on_the_scope_the_caller_is_allowed_to_read() {
        var query = DecisionLogQuery.builder()
            .decisionPointType("guardian")
            .apiIds(Set.of("api-1"))
            .applicationIds(Set.of("app-1"))
            .planIds(Set.of("plan-1"))
            .build();

        var result = SearchDecisionLogsQueryAdapter.adapt(query);

        // By content, not by array position: clause order carries no meaning inside a bool filter, so
        // positional assertions would break on a harmless reorder and pass on a wrong field name.
        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"terms\": { \"api-id\": [ \"api-1\" ] } }"));
        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"terms\": { \"app-id\": [ \"app-1\" ] } }"));
        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"terms\": { \"plan-id\": [ \"plan-1\" ] } }"));
    }

    @Test
    void narrows_on_what_the_point_concluded_and_on_what_was_enforced() {
        var query = DecisionLogQuery.builder()
            .decisionPointType("guardian")
            .outcomes(Set.of("INDETERMINATE"))
            .enforcements(Set.of("ALLOW"))
            .verdicts(Set.of("prompt-injection"))
            .statuses(Set.of("error"))
            .build();

        var result = SearchDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"terms\": { \"outcome\": [ \"INDETERMINATE\" ] } }"));
        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"terms\": { \"enforced\": [ \"ALLOW\" ] } }"));
        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"terms\": { \"verdict\": [ \"prompt-injection\" ] } }"));
        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"terms\": { \"status\": [ \"error\" ] } }"));
    }

    @Test
    void narrows_on_the_point_the_subject_and_the_correlation_keys() {
        var query = DecisionLogQuery.builder()
            .decisionPointType("guardian")
            .decisionPointIds(Set.of("prompt-guardian"))
            .checkpoints(Set.of("response.tool_calls"))
            .callers(Set.of("pep"))
            .subjectIds(Set.of("alice"))
            .actorIds(Set.of("booking-agent"))
            .actions(Set.of("flight_booking"))
            .resourceIds(Set.of("travel.flight_booking"))
            .caseIds(Set.of("case-1"))
            .requestIds(Set.of("req-1"))
            .traceIds(Set.of("trace-1"))
            .build();

        var result = SearchDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"terms\": { \"decision-point-id\": [ \"prompt-guardian\" ] } }"))
            .contains(json("{ \"terms\": { \"checkpoint\": [ \"response.tool_calls\" ] } }"))
            .contains(json("{ \"terms\": { \"caller\": [ \"pep\" ] } }"))
            .contains(json("{ \"terms\": { \"subject-id\": [ \"alice\" ] } }"))
            .contains(json("{ \"terms\": { \"actor-id\": [ \"booking-agent\" ] } }"))
            .contains(json("{ \"terms\": { \"action\": [ \"flight_booking\" ] } }"))
            .contains(json("{ \"terms\": { \"resource-id\": [ \"travel.flight_booking\" ] } }"))
            .contains(json("{ \"terms\": { \"case-id\": [ \"case-1\" ] } }"))
            .contains(json("{ \"terms\": { \"request-id\": [ \"req-1\" ] } }"))
            .contains(json("{ \"terms\": { \"trace-id\": [ \"trace-1\" ] } }"));
    }

    @Test
    void matches_a_reason_on_a_fragment_rather_than_the_whole_sentence() {
        var query = DecisionLogQuery.builder().decisionPointType("guardian").reasonContains("did not answer").build();

        var result = SearchDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"wildcard\": { \"reasons\": { \"value\": \"*did not answer*\", \"case_insensitive\": true } } }"));
    }

    @Test
    void escapes_wildcard_syntax_so_a_reason_needle_cannot_widen_the_search() {
        var query = DecisionLogQuery.builder().decisionPointType("guardian").reasonContains("a*b?c").build();

        var result = SearchDecisionLogsQueryAdapter.adapt(query);

        // Unescaped, "*" would match every reason while the caller still sees an active filter.
        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"wildcard\": { \"reasons\": { \"value\": \"*a\\\\*b\\\\?c*\", \"case_insensitive\": true } } }"));
    }

    @Test
    void rejects_a_query_that_would_read_across_every_kind_of_decision_point() {
        assertThatThrownBy(() -> DecisionLogQuery.builder().build().validate())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("decisionPointType");

        assertThatThrownBy(() -> DecisionLogQuery.builder().decisionPointType("  ").build().validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("decisionPointType");
    }

    @Test
    void rejects_an_inverted_time_range() {
        var query = DecisionLogQuery.builder().decisionPointType("guardian").from(2000L).to(1000L).build();

        assertThatThrownBy(query::validate).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("from must be <= to");
    }

    @Test
    void rejects_a_page_larger_than_the_maximum() {
        var query = DecisionLogQuery.builder().decisionPointType("guardian").size(DecisionLogQuery.MAX_SIZE + 1).build();

        assertThatThrownBy(query::validate).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("size must be <= 1000");
    }

    @Test
    void accepts_a_page_at_the_maximum() {
        var query = DecisionLogQuery.builder().decisionPointType("guardian").size(DecisionLogQuery.MAX_SIZE).build();

        assertThatCode(query::validate).doesNotThrowAnyException();
    }
}
