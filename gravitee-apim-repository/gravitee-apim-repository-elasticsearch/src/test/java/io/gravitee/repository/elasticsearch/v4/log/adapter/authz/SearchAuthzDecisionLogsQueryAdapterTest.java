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
                    { "term": { "doc-type": "authz" } },
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
    void pins_doc_type_so_the_shared_event_metrics_stream_yields_decisions_only() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result).inPath("$.query.bool.filter[0].term.doc-type").isEqualTo("authz");
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

        assertThatJson(result).inPath("$.query.bool.filter").isArray().hasSize(2);
        assertThatJson(result).inPath("$.from").isEqualTo(0);
        assertThatJson(result).inPath("$.size").isEqualTo(20);
    }

    @Test
    void keeps_an_open_ended_range_when_only_one_bound_is_given() {
        var query = AuthzDecisionLogQuery.builder().apiIds(Set.of("api-1")).from(1000L).build();

        var result = SearchAuthzDecisionLogsQueryAdapter.adapt(query);

        assertThatJson(result)
            .inPath("$.query.bool.filter[2].range.@timestamp")
            .isEqualTo(
                """
                { "gte": 1000 }
                """
            );
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
}
