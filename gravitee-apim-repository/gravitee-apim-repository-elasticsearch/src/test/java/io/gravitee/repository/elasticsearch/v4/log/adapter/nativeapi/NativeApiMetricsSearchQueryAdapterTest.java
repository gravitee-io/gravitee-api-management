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
package io.gravitee.repository.elasticsearch.v4.log.adapter.connection;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import io.gravitee.repository.log.v4.model.connection.NativeApiMetricsQuery;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeApiMetricsSearchQueryAdapterTest {

    @Test
    void builds_paginated_query_filtered_by_api_id_and_timestamp_range() {
        var query = NativeApiMetricsQuery.builder().apiId("api-1").from(1000L).to(2000L).page(2).size(10).build();

        var result = NativeApiMetricsSearchQueryAdapter.adapt(query);

        assertThatJson(result).isEqualTo(
            """
            {
              "from": 10,
              "size": 10,
              "query": {
                "bool": {
                  "must": [
                    { "term": { "api-id": "api-1" } },
                    { "range": { "@timestamp": { "gte": 1000, "lte": 2000 } } }
                  ]
                }
              },
              "sort": [
                { "@timestamp": { "order": "desc" } }
              ]
            }
            """
        );
    }

    @Test
    void omits_timestamp_range_when_no_bounds_provided() {
        var query = NativeApiMetricsQuery.builder().apiId("api-1").build();

        var result = NativeApiMetricsSearchQueryAdapter.adapt(query);

        assertThatJson(result).isEqualTo(
            """
            {
              "from": 0,
              "size": 20,
              "query": {
                "bool": {
                  "must": [
                    { "term": { "api-id": "api-1" } }
                  ]
                }
              },
              "sort": [
                { "@timestamp": { "order": "desc" } }
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

        var result = NativeApiMetricsSearchQueryAdapter.adapt(query);

        assertThatJson(result).isEqualTo(
            """
            {
              "from": 0,
              "size": 20,
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
                { "@timestamp": { "order": "desc" } }
              ]
            }
            """
        );
    }
}
