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

import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchResponseStatusQueryCriteriaAdapterTest {

    @Test
    void should_build_query_searching_for_ids() {
        var queryParams = ResponseStatusQueryCriteria.builder().apiIds(List.of("api-id")).build();
        var isEntrypointIdKeyword = true;

        var result = SearchResponseStatusRangesQueryAdapter.adapt(queryParams, isEntrypointIdKeyword);

        assertThatJson(result).isEqualTo(API_IDS_FILTERED_QUERY);
    }

    @Test
    void should_build_query_searching_for_ids_and_time_ranges() {
        var queryParams = ResponseStatusQueryCriteria.builder().apiIds(List.of("api-id")).from(1728992401566L).to(1729078801566L).build();
        var isEntrypointIdKeyword = true;

        var result = SearchResponseStatusRangesQueryAdapter.adapt(queryParams, isEntrypointIdKeyword);

        assertThatJson(result).isEqualTo(API_IDS_AND_TIME_FILTERED_QUERY);
    }

    @Test
    void should_build_query_with_empty_id_filter_array_for_null_query_params() {
        ResponseStatusQueryCriteria queryParams = null;
        var isEntrypointIdKeyword = true;

        var result = SearchResponseStatusRangesQueryAdapter.adapt(queryParams, isEntrypointIdKeyword);

        assertThatJson(result).isEqualTo(API_EMPTY_IDS_ARRAY);
    }

    @Test
    void should_build_query_with_empty_id_filter_array_for_empty_api_ids_query_params() {
        var queryParams = ResponseStatusQueryCriteria.builder().apiIds(List.of()).build();
        var isEntrypointIdKeyword = true;
        var result = SearchResponseStatusRangesQueryAdapter.adapt(queryParams, isEntrypointIdKeyword);

        assertThatJson(result).isEqualTo(API_EMPTY_IDS_ARRAY);
    }

    private static final String API_IDS_FILTERED_QUERY =
        """
            {
              "size": 0,
              "query": {
                "bool": {
                  "filter": [
                      {
                        "terms": {
                          "api-id": [
                              "api-id"
                          ]
                        }
                      }
                    ]
                }
              },
              "aggs": {
                "entrypoint_id_agg": {
                  "terms": { "field": "entrypoint-id" },
                  "aggs": {
                    "status_ranges": {
                      "range": {
                        "field": "status",
                        "ranges": [
                          { "from": 100.0, "to": 200.0 },
                          { "from": 200.0, "to": 300.0 },
                          { "from": 300.0, "to": 400.0 },
                          { "from": 400.0, "to": 500.0 },
                          { "from": 500.0, "to": 600.0 }
                        ]
                      }
                    }
                  }
                }
              }
            }
            """;

    private static final String API_IDS_AND_TIME_FILTERED_QUERY =
        """
            {
                "size": 0,
                "query": {
                    "bool": {
                        "filter": [
                            {
                                "terms": {
                                    "api-id": [
                                        "api-id"
                                    ]
                                }
                            },
                            {
                                "range": {
                                    "@timestamp": {
                                        "gte": 1728992401566,
                                        "lte": 1729078801566
                                    }
                                }
                            }
                        ]
                    }
                },
                "aggs": {
                    "entrypoint_id_agg": {
                        "terms": {
                            "field": "entrypoint-id"
                        },
                        "aggs": {
                            "status_ranges": {
                                "range": {
                                    "field": "status",
                                    "ranges": [
                                        {
                                            "from": 100.0,
                                            "to": 200.0
                                        },
                                        {
                                            "from": 200.0,
                                            "to": 300.0
                                        },
                                        {
                                            "from": 300.0,
                                            "to": 400.0
                                        },
                                        {
                                            "from": 400.0,
                                            "to": 500.0
                                        },
                                        {
                                            "from": 500.0,
                                            "to": 600.0
                                        }
                                    ]
                                }
                            }
                        }
                    }
                }
            }
            """;

    private static final String API_EMPTY_IDS_ARRAY =
        """
            {
              "size": 0,
              "query": {
                "bool": {
                  "filter": [
                      {
                        "terms": {
                          "api-id": []
                        }
                      }
                    ]
                }
              },
              "aggs": {
                "entrypoint_id_agg": {
                  "terms": { "field": "entrypoint-id" },
                  "aggs": {
                    "status_ranges": {
                      "range": {
                        "field": "status",
                        "ranges": [
                          { "from": 100.0, "to": 200.0 },
                          { "from": 200.0, "to": 300.0 },
                          { "from": 300.0, "to": 400.0 },
                          { "from": 400.0, "to": 500.0 },
                          { "from": 500.0, "to": 600.0 }
                        ]
                      }
                    }
                  }
                }
              }
            }
            """;
}
