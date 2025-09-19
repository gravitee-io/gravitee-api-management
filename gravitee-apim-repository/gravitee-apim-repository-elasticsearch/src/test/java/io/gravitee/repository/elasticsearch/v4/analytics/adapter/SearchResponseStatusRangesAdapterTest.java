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

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchResponseStatusRangesAdapter.ALL_APIS_STATUS_RANGES;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchResponseStatusRangesAdapter.BY_ENTRYPOINT_ID_AGG;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchResponseStatusRangesAdapterTest {

    SearchResponseStatusRangesAdapter sut = new SearchResponseStatusRangesAdapter();

    @Nested
    public class Query {

        @Test
        void should_build_query_searching_for_ids() {
            var queryParams = ResponseStatusQueryCriteria.builder().apiIds(List.of("api-id")).build();
            var isEntrypointIdKeyword = true;

            var result = sut.adaptQuery(queryParams, isEntrypointIdKeyword);

            assertThatJson(result).isEqualTo(API_IDS_FILTERED_QUERY);
        }

        @Test
        void should_build_query_searching_for_ids_and_time_ranges() {
            var queryParams = ResponseStatusQueryCriteria.builder()
                .apiIds(List.of("api-id"))
                .from(1728992401566L)
                .to(1729078801566L)
                .build();
            var isEntrypointIdKeyword = true;

            var result = sut.adaptQuery(queryParams, isEntrypointIdKeyword);

            assertThatJson(result).isEqualTo(API_IDS_AND_TIME_FILTERED_QUERY);
        }

        @Test
        void should_build_query_with_empty_id_filter_array_for_null_query_params() {
            ResponseStatusQueryCriteria queryParams = null;
            var isEntrypointIdKeyword = true;

            var result = sut.adaptQuery(queryParams, isEntrypointIdKeyword);

            assertThatJson(result).isEqualTo(API_EMPTY_IDS_ARRAY);
        }

        @Test
        void should_build_query_with_empty_id_filter_array_for_empty_api_ids_query_params() {
            var queryParams = ResponseStatusQueryCriteria.builder().apiIds(List.of()).build();
            var isEntrypointIdKeyword = true;
            var result = sut.adaptQuery(queryParams, isEntrypointIdKeyword);

            assertThatJson(result).isEqualTo(API_EMPTY_IDS_ARRAY);
        }

        private static final String API_IDS_FILTERED_QUERY = """
            {
              "size": 0,
              "query": {
                    "bool": {
                        "filter": [
                            {
                                "bool": {
                                    "should": [
                                        {
                                            "terms": {
                                                "api-id": [
                                                    "api-id"
                                                ]
                                            }
                                        },
                                        {
                                            "terms": {
                                                "api": [
                                                    "api-id"
                                                ]
                                            }
                                        }
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
                },
                "all_apis_status_ranges": {
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
            """;

        private static final String API_IDS_AND_TIME_FILTERED_QUERY = """
            {
                "size": 0,
                "query": {
                    "bool": {
                        "filter": [
                            {
                                "bool": {
                                    "should": [
                                        {
                                            "terms": {
                                                "api-id": [
                                                    "api-id"
                                                ]
                                            }
                                        },
                                        {
                                            "terms": {
                                                "api": [
                                                    "api-id"
                                                ]
                                            }
                                        }
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
                    },
                    "all_apis_status_ranges": {
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
            """;

        private static final String API_EMPTY_IDS_ARRAY = """
            {
                "size": 0,
                "query": {
                    "bool": {
                        "filter": [
                            {
                                "bool": {
                                    "should": [
                                        {
                                            "terms": {
                                                "api-id": []
                                            }
                                        },
                                        {
                                            "terms": {
                                                "api": []
                                            }
                                        }
                                    ]
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
                    },
                    "all_apis_status_ranges": {
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
            """;
    }

    @Nested
    public class Response {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Test
        void should_return_empty_result_if_no_aggregation() {
            final SearchResponse searchResponse = new SearchResponse();

            assertThat(sut.adaptResponse(searchResponse)).isEmpty();
        }

        @Test
        void should_return_empty_result_if_no_entrypoints_aggregation() {
            final SearchResponse searchResponse = new SearchResponse();
            searchResponse.setAggregations(Map.of());

            assertThat(sut.adaptResponse(searchResponse)).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("provideSearchData")
        void should_build_search_requests_count_response(String[] entrypoints) {
            final SearchResponse searchResponse = new SearchResponse();
            final Aggregation aggregation = new Aggregation();
            searchResponse.setAggregations(
                Map.of(BY_ENTRYPOINT_ID_AGG, aggregation, ALL_APIS_STATUS_RANGES, provideAllApiStatusAggregation())
            );

            aggregation.setBuckets(Arrays.stream(entrypoints).map(this::provideBucket).toList());

            assertThat(sut.adaptResponse(searchResponse)).hasValueSatisfying(topHits ->
                assertThat(topHits.getStatusRangesCountByEntrypoint().keySet()).containsExactlyInAnyOrder(entrypoints)
            );
        }

        private JsonNode provideBucket(String entrypoint) {
            var result = objectMapper.createObjectNode();
            result
                .put("key", entrypoint)
                .putObject("status_ranges")
                .putArray("buckets")
                .addObject()
                .put("key", "100.0-200.0")
                .put("doc_count", 1);
            return result;
        }

        private static Stream<Arguments> provideSearchData() {
            return Stream.of(
                Arguments.of((Object) new String[] {}),
                Arguments.of((Object) new String[] { "http-get" }),
                Arguments.of((Object) new String[] { "http-get", "http-post" })
            );
        }

        private Aggregation provideAllApiStatusAggregation() {
            var result = List.<JsonNode>of(
                objectMapper.createObjectNode().put("key", "100.0-200.0").put("doc_count", 1),
                objectMapper.createObjectNode().put("key", "200.0-300.0").put("doc_count", 2),
                objectMapper.createObjectNode().put("key", "300.0-400.0").put("doc_count", 3),
                objectMapper.createObjectNode().put("key", "400.0-500.0").put("doc_count", 4)
            );

            var aggregation = new Aggregation();
            aggregation.setBuckets(result);
            return aggregation;
        }
    }
}
