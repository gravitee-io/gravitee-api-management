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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.GroupByQueryCriteria;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchGroupByAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_build_query_without_filter() {
        var result = SearchGroupByAdapter.adaptQuery(null);

        assertThatJson(result)
            .isEqualTo(
                """
                {
                  "size": 0,
                  "query": {
                    "match_all": {}
                  },
                  "aggs": {
                    "group_by_values": {
                      "terms": {
                        "field": "status",
                        "size": 10,
                        "order": [
                          {"_count":"desc"},
                          {"_key":"asc"}
                        ]
                      }
                    }
                  }
                }
                """
            );
    }

    @Test
    void should_build_query_with_api_time_field_size_and_asc_order() {
        var result = SearchGroupByAdapter.adaptQuery(
            new GroupByQueryCriteria(
                "api-id",
                Instant.parse("2023-10-21T10:15:30Z"),
                Instant.parse("2023-10-22T10:15:30Z"),
                "status",
                5,
                GroupByQueryCriteria.Order.ASC
            )
        );

        assertThatJson(result)
            .isEqualTo(
                """
                {
                  "size": 0,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "api-id": "api-id" } },
                        {
                          "range": {
                            "@timestamp": {
                              "from": 1697883330000,
                              "include_lower": true,
                              "to": 1697969730000,
                              "include_upper": true
                            }
                          }
                        }
                      ]
                    }
                  },
                  "aggs": {
                    "group_by_values": {
                      "terms": {
                        "field": "status",
                        "size": 5,
                        "order": [
                          {"_count":"asc"},
                          {"_key":"asc"}
                        ]
                      }
                    }
                  }
                }
                """
            );
    }

    @Test
    void should_return_empty_result_if_no_aggregation() {
        assertThat(SearchGroupByAdapter.adaptResponse(new SearchResponse())).isEmpty();
    }

    @Test
    void should_adapt_group_by_response_keeping_bucket_order() {
        var response = new SearchResponse();
        var aggregation = new Aggregation();
        response.setAggregations(Map.of("group_by_values", aggregation));
        aggregation.setBuckets(
            java.util.List.of(
                objectMapper.createObjectNode().put("key", "404").put("doc_count", 7L),
                objectMapper.createObjectNode().put("key", "200").put("doc_count", 3L)
            )
        );

        assertThat(SearchGroupByAdapter.adaptResponse(response))
            .hasValueSatisfying(groupByAggregate ->
                assertThat(groupByAggregate.getValues()).containsExactly(entry("404", 7L), entry("200", 3L))
            );
    }

    private static Map.Entry<String, Long> entry(String key, Long value) {
        return Map.entry(key, value);
    }
}
