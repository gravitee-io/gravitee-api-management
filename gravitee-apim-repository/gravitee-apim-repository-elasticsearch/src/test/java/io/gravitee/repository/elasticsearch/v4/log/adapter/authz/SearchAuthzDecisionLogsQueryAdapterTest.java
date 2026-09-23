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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLogQuery;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchAuthzDecisionLogsQueryAdapterTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("org-1", "env-1");
    private static final int SCOPE_CLAUSES = 5;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void builds_a_paginated_query_scoped_to_the_calling_environment_the_authz_decision_point_and_the_timestamp_range() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).from(1000L).to(2000L).page(2).size(10).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result).isEqualTo(
            """
            {
              "query": {
                "bool": {
                  "filter": [
                    { "term": { "org-id": "org-1" } },
                    { "term": { "env-id": "env-1" } },
                    { "term": { "decision-point-type": "authz" } },
                    { "term": { "phase": "RESOLVED" } },
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
    void scopes_every_search_to_the_organization_and_environment_of_the_caller() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(new QueryContext("org-2", "env-2"), query);

        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"term\": { \"org-id\": \"org-2\" } }"));
        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"term\": { \"env-id\": \"env-2\" } }"));
    }

    @Test
    void reads_only_the_resolved_decisions_of_the_authz_decision_point() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"term\": { \"decision-point-type\": \"authz\" } }"))
            .contains(json("{ \"term\": { \"phase\": \"RESOLVED\" } }"));
    }

    @Test
    void breaks_timestamp_ties_on_event_id_so_paging_cannot_repeat_or_skip_a_decision() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

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

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(SCOPE_CLAUSES);
        assertThatJson(result).inPath("$.from").isEqualTo(0);
        assertThatJson(result).inPath("$.size").isEqualTo(20);
    }

    @Test
    void keeps_an_open_ended_range_when_only_one_bound_is_given() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).from(1000L).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"range\": { \"@timestamp\": { \"gte\": 1000 } } }"));
    }

    @Test
    void narrows_on_the_requested_decisions_through_the_verdict() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).decisions(Set.of("FORBID")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"terms\": { \"verdict\": [ \"FORBID\" ] } }"));
    }

    @Test
    void narrows_on_subject_action_resource_and_caller() {
        var query = AuthzDecisionLogQuery.builder()
            .apiIds(Set.of("api-1"))
            .subjectIds(Set.of("alice"))
            .actions(Set.of("read"))
            .resourceIds(Set.of("doc-1"))
            .callers(Set.of("pep"))
            .build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"terms\": { \"subject-id\": [ \"alice\" ] } }"))
            .contains(json("{ \"terms\": { \"action\": [ \"read\" ] } }"))
            .contains(json("{ \"terms\": { \"resource-id\": [ \"doc-1\" ] } }"))
            .contains(json("{ \"terms\": { \"caller\": [ \"pep\" ] } }"));
    }

    @Test
    @SneakyThrows
    void shares_one_entity_reference_budget_between_the_subject_and_resource_clauses() {
        var subjects = IntStream.range(0, 100)
            .mapToObj(i -> "T" + i + "::a::b::c")
            .collect(Collectors.toSet());
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).subjectIds(subjects).resourceIds(Set.of("Doc::\"d1\"")).build();

        JsonNode filters = MAPPER.readTree(SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query)).at("/query/bool/filter");

        assertThat(filters.valueStream().filter(clause -> clause.has("bool")))
            .singleElement()
            .satisfies(clause -> assertThat(clause.at("/bool/should").size()).isEqualTo(129));
        assertThat(filters.valueStream().filter(clause -> clause.at("/terms").has("resource-id")))
            .singleElement()
            .satisfies(clause -> assertThat(clause.at("/terms/resource-id")).isEqualTo(MAPPER.readTree("[\"Doc::\\\"d1\\\"\"]")));
    }

    @Test
    void omits_every_optional_clause_when_none_is_requested() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(SCOPE_CLAUSES);
    }

    @Test
    void omits_the_decision_clause_when_none_is_requested() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(SCOPE_CLAUSES);
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
    void narrows_on_status_indeterminate_cause_pdp_engine_generation_and_request_id() {
        var query = AuthzDecisionLogQuery.builder()
            .apiIds(Set.of("api-1"))
            .statuses(Set.of("error"))
            .indeterminateCauses(Set.of("NOT_READY"))
            .targetPdpIds(Set.of("default"))
            .policyGenerations(Set.of("9"))
            .requestIds(Set.of("req-1"))
            .build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"terms\": { \"status\": [ \"error\" ] } }"))
            .contains(json("{ \"terms\": { \"indeterminate-cause\": [ \"NOT_READY\" ] } }"))
            .contains(json("{ \"terms\": { \"decision-point-id\": [ \"default\" ] } }"))
            .contains(json("{ \"terms\": { \"decision-point-version\": [ \"9\" ] } }"))
            .contains(json("{ \"terms\": { \"request-id\": [ \"req-1\" ] } }"));
        assertThat(result).doesNotContain("\"operation\"");
    }

    @Test
    void wraps_the_matched_policy_clause_in_a_nested_query_on_the_matched_rules() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).matchedPolicyNames(Set.of("forbid-delete")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        // Nested objects are separate Lucene documents: without the wrapper this matches nothing.
        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(
                json(
                    """
                    {
                      "nested": {
                        "path": "matched-rules",
                        "ignore_unmapped": true,
                        "query": {
                          "bool": {
                            "should": [
                              { "terms": { "matched-rules.name": [ "forbid-delete" ] } },
                              { "prefix": { "matched-rules.name": "forbid-delete#" } }
                            ],
                            "minimum_should_match": 1
                          }
                        }
                      }
                    }
                    """
                )
            );
    }

    @Test
    void matches_a_policy_name_on_every_rule_of_that_policy() {
        var names = new LinkedHashSet<>(List.of("QA default", "other#1"));
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).matchedPolicyNames(names).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result)
            .inPath("$.query.bool.filter[5].nested.query.bool.should")
            .isEqualTo(
                json(
                    """
                    [
                      { "terms": { "matched-rules.name": [ "QA default", "other#1" ] } },
                      { "prefix": { "matched-rules.name": "QA default#" } },
                      { "prefix": { "matched-rules.name": "other#1#" } }
                    ]
                    """
                )
            );
    }

    @Test
    void narrows_on_the_error_type() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).errorTypes(Set.of("pdp_unavailable")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"terms\": { \"error-type\": [ \"pdp_unavailable\" ] } }"));
    }

    @Test
    void matches_a_reason_on_a_fragment_rather_than_the_whole_sentence() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).reasonContains("forbid-delete").build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"wildcard\": { \"reasons\": { \"value\": \"*forbid-delete*\", \"case_insensitive\": true } } }"));
    }

    @Test
    void escapes_wildcard_syntax_so_a_reason_needle_cannot_widen_the_search() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).reasonContains("a*b?c").build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        // Unescaped, "*" would match every reason while the caller still sees an active filter.
        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"wildcard\": { \"reasons\": { \"value\": \"*a\\\\*b\\\\?c*\", \"case_insensitive\": true } } }"));
    }
}
