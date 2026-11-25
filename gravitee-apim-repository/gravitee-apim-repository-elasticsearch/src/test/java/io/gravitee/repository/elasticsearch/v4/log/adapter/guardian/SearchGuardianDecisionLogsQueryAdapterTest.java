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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLogQuery;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchGuardianDecisionLogsQueryAdapterTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("org-1", "env-1");
    private static final int SCOPE_CLAUSES = 3;

    @Test
    void builds_a_paginated_newest_first_query_scoped_to_the_calling_environment_the_guardian_decision_point_and_the_range() {
        var query = GuardianDecisionLogQuery.builder().apiIds(Set.of("api-1")).from(1000L).to(2000L).page(2).size(10).build();

        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result).isEqualTo(
            """
            {
              "query": {
                "bool": {
                  "filter": [
                    { "term": { "org-id": "org-1" } },
                    { "term": { "env-id": "env-1" } },
                    { "term": { "decision-point-type": "guardian" } },
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
    void reads_every_api_of_the_environment_when_no_api_is_given() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, GuardianDecisionLogQuery.builder().build());

        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(SCOPE_CLAUSES);
        assertThat(result).doesNotContain("api-id");
        assertThatJson(result).inPath("$.from").isEqualTo(0);
        assertThatJson(result).inPath("$.size").isEqualTo(20);
    }

    @Test
    void reads_no_api_when_the_given_api_set_is_empty() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(
            QUERY_CONTEXT,
            GuardianDecisionLogQuery.builder().apiIds(Set.of()).build()
        );

        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"terms\": { \"api-id\": [] } }"));
    }

    @Test
    void scopes_every_search_to_the_organization_and_environment_of_the_caller() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(
            new QueryContext("org-2", "env-2"),
            GuardianDecisionLogQuery.builder().build()
        );

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"term\": { \"org-id\": \"org-2\" } }"))
            .contains(json("{ \"term\": { \"env-id\": \"env-2\" } }"))
            .contains(json("{ \"term\": { \"decision-point-type\": \"guardian\" } }"));
    }

    @Test
    void narrows_on_the_guardians_through_the_decision_point_id() {
        var query = GuardianDecisionLogQuery.builder().guardianIds(Set.of("guardian-1")).build();

        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"terms\": { \"decision-point-id\": [\"guardian-1\"] } }"));
    }

    @Test
    void narrows_on_the_outcomes_and_the_exact_request_ids() {
        var query = GuardianDecisionLogQuery.builder().outcomes(Set.of("DENY")).requestIds(Set.of("req-1")).build();

        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"terms\": { \"outcome\": [\"DENY\"] } }"))
            .contains(json("{ \"terms\": { \"request-id\": [\"req-1\"] } }"));
    }

    @Test
    void matches_a_fragment_of_the_request_id_ignoring_case() {
        var query = GuardianDecisionLogQuery.builder().requestIdContains("2212").build();

        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"wildcard\": { \"request-id\": { \"value\": \"*2212*\", \"case_insensitive\": true } } }"));
    }

    @Test
    void escapes_wildcard_syntax_so_a_request_id_fragment_cannot_widen_the_search() {
        var query = GuardianDecisionLogQuery.builder().requestIdContains("*").build();

        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"wildcard\": { \"request-id\": { \"value\": \"*\\\\**\", \"case_insensitive\": true } } }"));
    }

    @Test
    void omits_every_optional_clause_when_none_is_requested() {
        var query = GuardianDecisionLogQuery.builder()
            .guardianIds(Set.of())
            .outcomes(Set.of())
            .requestIds(Set.of())
            .requestIdContains(" ")
            .build();

        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, query);

        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(SCOPE_CLAUSES);
    }

    @Test
    void breaks_timestamp_ties_on_event_id_so_paging_cannot_repeat_or_skip_a_decision() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(QUERY_CONTEXT, GuardianDecisionLogQuery.builder().build());

        assertThatJson(result)
            .inPath("$.sort")
            .isEqualTo(
                """
                [
                  { "@timestamp": { "order": "desc" } },
                  { "event-id": { "order": "asc", "unmapped_type": "keyword" } }
                ]
                """
            );
    }
}
