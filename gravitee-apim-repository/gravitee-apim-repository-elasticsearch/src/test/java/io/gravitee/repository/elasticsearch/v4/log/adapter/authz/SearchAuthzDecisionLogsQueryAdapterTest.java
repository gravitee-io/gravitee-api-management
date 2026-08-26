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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLogQuery;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchAuthzDecisionLogsQueryAdapterTest {

    @Test
    void builds_a_paginated_query_filtered_by_doc_type_api_ids_and_timestamp_range() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).from(1000L).to(2000L).page(2).size(10).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result).isEqualTo(
            """
            {
              "query": {
                "bool": {
                  "filter": [
                    { "terms": { "api-id": ["api-1"] } },
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
    void breaks_timestamp_ties_on_event_id_so_paging_cannot_repeat_or_skip_a_decision() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

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
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(1);
        assertThatJson(result).inPath("$.from").isEqualTo(0);
        assertThatJson(result).inPath("$.size").isEqualTo(20);
    }

    @Test
    void keeps_an_open_ended_range_when_only_one_bound_is_given() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).from(1000L).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result)
            .inPath("$.query.bool.filter[1].range.@timestamp")
            .isEqualTo(
                """
                { "gte": 1000 }
                """
            );
    }

    @Test
    void narrows_on_the_requested_decisions() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).decisions(Set.of("FORBID")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result)
            .inPath("$.query.bool.filter[1].terms.decision")
            .isEqualTo(
                """
                [ "FORBID" ]
                """
            );
    }

    @Test
    void narrows_on_subject_action_resource_and_caller() {
        var query = AuthzDecisionLogQuery.builder()
            .apiIds(Set.of("api-1"))
            .subjectIds(Set.of("alice"))
            .actions(Set.of("read"))
            .resourceIds(Set.of("doc-1"))
            .callers(Set.of("pep", "authzen"))
            .build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result)
            .inPath("$.query.bool.filter[1].terms.subject-id")
            .isEqualTo(
                """
                [ "alice" ]
                """
            );
        assertThatJson(result)
            .inPath("$.query.bool.filter[2].terms.action")
            .isEqualTo(
                """
                [ "read" ]
                """
            );
        assertThatJson(result)
            .inPath("$.query.bool.filter[3].terms.resource-id")
            .isEqualTo(
                """
                [ "doc-1" ]
                """
            );
        assertThatJson(result).inPath("$.query.bool.filter[4].terms.caller").isArray().hasSize(2);
    }

    @Test
    void omits_every_optional_clause_when_none_is_requested() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        // api-id only: an empty terms clause would match nothing and silently empty the table.
        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(1);
    }

    @Test
    void omits_the_decision_clause_when_none_is_requested() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(1);
    }

    @Test
    void rejects_a_query_that_would_read_across_every_api() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of()).build();

        assertThatThrownBy(query::validate).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("apiIds");
    }

    @Test
    void rejects_an_inverted_time_range() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).from(2000L).to(1000L).build();

        assertThatThrownBy(query::validate).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("from must be <= to");
    }

    @Test
    void rejects_a_page_larger_than_the_maximum() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).size(AuthzDecisionLogQuery.MAX_SIZE + 1).build();

        assertThatThrownBy(query::validate).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("size must be <= 1000");
    }

    @Test
    void accepts_a_page_at_the_maximum() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).size(AuthzDecisionLogQuery.MAX_SIZE).build();

        assertThatCode(query::validate).doesNotThrowAnyException();
    }

    @Test
    void narrows_on_status_operation_pdp_generation_and_request_id() {
        var query = AuthzDecisionLogQuery.builder()
            .apiIds(Set.of("api-1"))
            .statuses(Set.of("error"))
            .operations(Set.of("search"))
            .targetPdpIds(Set.of("default"))
            .policyGenerations(Set.of("9"))
            .requestIds(Set.of("req-1"))
            .build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        // By content, not by array position: clause order carries no meaning inside a bool filter, so
        // positional assertions would break on a harmless reorder and pass on a wrong field name.
        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"terms\": { \"status\": [ \"error\" ] } }"));
        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"terms\": { \"operation\": [ \"search\" ] } }"));
        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"terms\": { \"target-pdp-id\": [ \"default\" ] } }"));
        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"terms\": { \"policy-generation\": [ \"9\" ] } }"));
        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"terms\": { \"request-id\": [ \"req-1\" ] } }"));
    }

    @Test
    void wraps_the_matched_policy_clause_in_a_nested_query() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).matchedPolicyNames(Set.of("forbid-delete")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        // Nested objects are separate Lucene documents: without the wrapper this matches nothing.
        assertThatJson(result).inPath("$.query.bool.filter[1].nested.path").isEqualTo("\"matched-policies\"");
        assertThatJson(result)
            .inPath("$.query.bool.filter[1].nested.query.terms['matched-policies.name']")
            .isEqualTo(
                """
                [ "forbid-delete" ]
                """
            );
    }

    @Test
    void matches_a_reason_on_a_fragment_rather_than_the_whole_sentence() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).reasonContains("forbid-delete").build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"wildcard\": { \"reasons\": { \"value\": \"*forbid-delete*\", \"case_insensitive\": true } } }"));
    }

    @Test
    void escapes_wildcard_syntax_so_a_reason_needle_cannot_widen_the_search() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).reasonContains("a*b?c").build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        // Unescaped, "*" would match every reason while the caller still sees an active filter.
        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"wildcard\": { \"reasons\": { \"value\": \"*a\\\\*b\\\\?c*\", \"case_insensitive\": true } } }"));
    }
}
