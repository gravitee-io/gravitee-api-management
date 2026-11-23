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

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLogQuery;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchGuardianDecisionLogsQueryAdapterTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("org-1", "env-1");

    @Test
    void reads_the_resolved_guardian_decisions_of_the_apis_newest_first() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(
            QUERY_CONTEXT,
            GuardianDecisionLogQuery.builder().apiIds(Set.of("api-1")).page(3).size(10).build()
        );

        assertThatJson(result).isEqualTo(
            """
            {
              "query": {
                "bool": {
                  "filter": [
                    { "term": { "org-id": "org-1" } },
                    { "term": { "env-id": "env-1" } },
                    { "term": { "decision-point-type": "guardian" } },
                    { "term": { "phase": "RESOLVED" } },
                    { "terms": { "api-id": ["api-1"] } }
                  ]
                }
              },
              "from": 20,
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
    void narrows_on_the_guardian() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(
            QUERY_CONTEXT,
            GuardianDecisionLogQuery.builder().apiIds(Set.of("api-1")).guardianIds(Set.of("guardian-a")).build()
        );

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .hasSize(6)
            .contains(json("{ \"terms\": { \"decision-point-id\": [\"guardian-a\"] } }"));
    }

    @Test
    void narrows_on_the_outcome() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(
            QUERY_CONTEXT,
            GuardianDecisionLogQuery.builder().apiIds(Set.of("api-1")).outcomes(Set.of("DENY")).build()
        );

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .hasSize(6)
            .contains(json("{ \"terms\": { \"outcome\": [\"DENY\"] } }"));
    }

    @Test
    void leaves_out_the_guardian_filter_with_nothing_selected() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(
            QUERY_CONTEXT,
            GuardianDecisionLogQuery.builder().apiIds(Set.of("api-1")).guardianIds(Set.of()).outcomes(Set.of()).build()
        );

        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(5);
    }

    @Test
    void searches_the_start_of_the_request_id_and_a_fragment_of_the_reasons_and_the_rule_names_in_any_case() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(
            QUERY_CONTEXT,
            GuardianDecisionLogQuery.builder().apiIds(Set.of("api-1")).text(" aws ").build()
        );

        assertThatJson(result)
            .inPath("$.query.bool.filter[5]")
            .isEqualTo(
                """
                {
                  "bool": {
                    "should": [
                      { "prefix": { "request-id": { "value": "aws", "case_insensitive": true } } },
                      { "wildcard": { "reasons": { "value": "*aws*", "case_insensitive": true } } },
                      {
                        "nested": {
                          "path": "matched-rules",
                          "ignore_unmapped": true,
                          "query": { "wildcard": { "matched-rules.name": { "value": "*aws*", "case_insensitive": true } } }
                        }
                      }
                    ],
                    "minimum_should_match": 1
                  }
                }
                """
            );
    }

    @Test
    void matches_the_apis_the_text_names_as_well() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(
            QUERY_CONTEXT,
            GuardianDecisionLogQuery.builder().apiIds(Set.of("api-1", "api-2")).text("support").textApiIds(Set.of("api-2")).build()
        );

        assertThatJson(result)
            .inPath("$.query.bool.filter[5].bool.should")
            .isArray()
            .hasSize(4)
            .contains(json("{ \"terms\": { \"api-id\": [\"api-2\"] } }"));
    }

    /** Unescaped, a wildcard character typed in the box would turn the search into no search at all. */
    @Test
    void searches_wildcard_characters_as_written() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(
            QUERY_CONTEXT,
            GuardianDecisionLogQuery.builder().apiIds(Set.of("api-1")).text("a*b?c\\d").build()
        );

        assertThatJson(result)
            .inPath("$.query.bool.filter[5].bool.should[1].wildcard.reasons.value")
            .isString()
            .isEqualTo("*a\\*b\\?c\\\\d*");
    }

    @Test
    void leaves_out_a_blank_text() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(
            QUERY_CONTEXT,
            GuardianDecisionLogQuery.builder().apiIds(Set.of("api-1")).text("   ").textApiIds(Set.of("api-1")).build()
        );

        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(5);
    }

    @Test
    void bounds_the_time_on_both_sides() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(
            QUERY_CONTEXT,
            GuardianDecisionLogQuery.builder().apiIds(Set.of("api-1")).from(100L).to(200L).build()
        );

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"range\": { \"@timestamp\": { \"gte\": 100, \"lte\": 200 } } }"));
    }

    @Test
    void bounds_the_time_on_one_side_only() {
        var result = SearchGuardianDecisionLogsQueryAdapter.adapt(
            QUERY_CONTEXT,
            GuardianDecisionLogQuery.builder().apiIds(Set.of("api-1")).from(100L).build()
        );

        assertThatJson(result)
            .inPath("$.query.bool.filter")
            .isArray()
            .contains(json("{ \"range\": { \"@timestamp\": { \"gte\": 100 } } }"));
    }
}
