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

import io.gravitee.repository.common.query.QueryContext;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FindDecisionLogQueryAdapterTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("org#1", "env#1");

    @Test
    void builds_a_single_hit_query_pinned_to_the_api_and_the_event_id() {
        var result = FindDecisionLogQueryAdapter.adapt(QUERY_CONTEXT, "api-1", "dec-9");

        assertThatJson(result).isEqualTo(
            """
            {
              "query": {
                "bool": {
                  "filter": [
                    { "term": { "org-id": "org#1" } },
                    { "term": { "env-id": "env#1" } },
                    { "term": { "api-id": "api-1" } },
                    { "term": { "event-id": "dec-9" } }
                  ]
                }
              },
              "size": 1
            }
            """
        );
    }

    @Test
    void scopes_by_organization_and_environment_so_a_foreign_event_id_cannot_be_read() {
        var result = FindDecisionLogQueryAdapter.adapt(QUERY_CONTEXT, "api-1", "dec-9");

        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"term\": { \"org-id\": \"org#1\" } }"));
        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"term\": { \"env-id\": \"env#1\" } }"));
    }

    @Test
    void scopes_by_api_so_an_event_id_from_another_api_cannot_be_read() {
        var result = FindDecisionLogQueryAdapter.adapt(QUERY_CONTEXT, "api-1", "dec-9");

        assertThatJson(result).inPath("$.query.bool.filter").isArray().contains(json("{ \"term\": { \"api-id\": \"api-1\" } }"));
    }

    @Test
    void does_not_pin_the_phase_because_each_record_of_a_decision_has_its_own_event_id() {
        var result = FindDecisionLogQueryAdapter.adapt(QUERY_CONTEXT, "api-1", "dec-9");

        // Pinning RESOLVED here would make the REQUESTED record of an open decision unreadable.
        assertThatJson(result).node("query.bool.filter").isArray().hasSize(4);
    }
}
