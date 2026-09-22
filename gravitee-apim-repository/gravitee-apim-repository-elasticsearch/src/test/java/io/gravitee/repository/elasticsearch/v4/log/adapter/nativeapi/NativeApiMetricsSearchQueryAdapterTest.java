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
package io.gravitee.repository.elasticsearch.v4.log.adapter.nativeapi;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.log.v4.model.connection.NativeApiMetricsQuery;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeApiMetricsSearchQueryAdapterTest {

    /** Elasticsearch's own default for {@code index.max_result_window}. */
    private static final int MAX_RESULT_WINDOW = 10_000;

    @Test
    void builds_paginated_query_filtered_by_api_id_and_timestamp_range() {
        var query = NativeApiMetricsQuery.builder().apiId("api-1").from(1000L).to(2000L).page(2).size(10).build();

        var result = NativeApiMetricsSearchQueryAdapter.adapt(query, MAX_RESULT_WINDOW);

        assertThatJson(result).isEqualTo(
            """
            {
              "from": 10,
              "size": 10,
              "track_total_hits": true,
              "query": {
                "bool": {
                  "must": [
                    { "term": { "api-id": "api-1" } },
                    { "range": { "@timestamp": { "gte": 1000, "lte": 2000 } } }
                  ]
                }
              },
              "sort": [
                { "@timestamp": { "order": "desc" } },
                { "request-id": { "order": "asc", "unmapped_type": "keyword" } }
              ]
            }
            """
        );
    }

    @Test
    void omits_timestamp_range_when_no_bounds_provided() {
        var query = NativeApiMetricsQuery.builder().apiId("api-1").build();

        var result = NativeApiMetricsSearchQueryAdapter.adapt(query, MAX_RESULT_WINDOW);

        assertThatJson(result).isEqualTo(
            """
            {
              "from": 0,
              "size": 20,
              "track_total_hits": true,
              "query": {
                "bool": {
                  "must": [
                    { "term": { "api-id": "api-1" } }
                  ]
                }
              },
              "sort": [
                { "@timestamp": { "order": "desc" } },
                { "request-id": { "order": "asc", "unmapped_type": "keyword" } }
              ]
            }
            """
        );
    }

    @Test
    void adds_application_plan_and_connection_status_terms_filters() {
        var query = NativeApiMetricsQuery.builder()
            .apiId("api-1")
            .applicationIds(Set.of("app-1"))
            .planIds(Set.of("plan-1"))
            .connectionStatuses(Set.of("CONNECTED"))
            .build();

        var result = NativeApiMetricsSearchQueryAdapter.adapt(query, MAX_RESULT_WINDOW);

        assertThatJson(result).isEqualTo(
            """
            {
              "from": 0,
              "size": 20,
              "track_total_hits": true,
              "query": {
                "bool": {
                  "must": [
                    { "term": { "api-id": "api-1" } },
                    { "terms": { "application-id": ["app-1"] } },
                    { "terms": { "plan-id": ["plan-1"] } },
                    { "terms": { "additional-metrics.keyword_native-kafka_connection-status": ["CONNECTED"] } }
                  ]
                }
              },
              "sort": [
                { "@timestamp": { "order": "desc" } },
                { "request-id": { "order": "asc", "unmapped_type": "keyword" } }
              ]
            }
            """
        );
    }

    @Test
    void should_accept_the_last_page_that_fits_inside_the_window() {
        var query = NativeApiMetricsQuery.builder().apiId("api-1").page(1_000).size(10).build();

        var result = NativeApiMetricsSearchQueryAdapter.adapt(query, MAX_RESULT_WINDOW);

        // from + size lands exactly on the window: the last page Elasticsearch will serve, and it must not
        // be refused by an off-by-one in the guard.
        assertThatJson(result).node("from").isEqualTo(9_990);
        assertThatJson(result).node("size").isEqualTo(10);
    }

    @Test
    void should_refuse_a_page_that_reaches_past_the_window() {
        var query = NativeApiMetricsQuery.builder().apiId("api-1").page(1_001).size(10).build();

        assertThatThrownBy(() -> NativeApiMetricsSearchQueryAdapter.adapt(query, MAX_RESULT_WINDOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("page 1001")
            .hasMessageContaining("10000");
    }

    @Test
    void should_honour_a_window_the_operator_raised() {
        // index.max_result_window is an index setting, routinely raised on log indices. The page refused
        // above is served when the cluster says it can be.
        var query = NativeApiMetricsQuery.builder().apiId("api-1").page(1_001).size(10).build();

        var result = NativeApiMetricsSearchQueryAdapter.adapt(query, 50_000);

        assertThatJson(result).node("from").isEqualTo(10_000);
    }

    @Test
    void should_refuse_a_page_whose_offset_overflows_an_int() {
        // page and size are only bounded below, so (page - 1) * size overflows for a value a client is free
        // to send. Computed as an int it wraps negative, slips past the guard, and reaches Elasticsearch as
        // "from": -4 — a 400 from the cluster instead of one from here.
        var query = NativeApiMetricsQuery.builder().apiId("api-1").page(1_073_741_824).size(4).build();

        assertThatThrownBy(() -> NativeApiMetricsSearchQueryAdapter.adapt(query, MAX_RESULT_WINDOW)).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    void should_tell_a_caller_with_an_oversized_page_size_to_shrink_it() {
        // Page 1 and no filter problem: telling this caller to narrow the time range is advice that cannot
        // work, because the events they asked for are the first ones. perPage has no upper bound of its own.
        var query = NativeApiMetricsQuery.builder().apiId("api-1").page(1).size(20_000).build();

        assertThatThrownBy(() -> NativeApiMetricsSearchQueryAdapter.adapt(query, MAX_RESULT_WINDOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("smaller page size")
            .hasMessageNotContaining("narrow the time range");
    }
}
