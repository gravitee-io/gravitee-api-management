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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.TopFailedAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AggregateValueCountByFieldAdapterTest {

    private static final String TOP_HITS_COUNT = "top_hits_count";
    private static final String HITS_COUNT = "hits_count";
    private static final long FROM = 1728992401566L;
    private static final long TO = 1729078801566L;
    private static final List<String> API_IDS = List.of("api-id-1", "api-id-2");

    @Nested
    class TopHitsApplications {

        @Nested
        class Query {

            private static final List<String> APP_ID_FIELD_NAMES = List.of("application-id", "application");

            @Test
            void should_build_top_apps_query_searching_for_top_ids_and_time_ranges() {
                var queryParams = new TopHitsQueryCriteria(API_IDS, FROM, TO);

                var result = AggregateValueCountByFieldAdapter.adaptQueryForFields(APP_ID_FIELD_NAMES, queryParams);

                assertThatJson(result).isEqualTo(TOP_APPS_QUERY_WITH_API_IDS_AND_TIME_RANGES);
            }

            @Test
            void should_build_top_apps_query_with_empty_id_filter_array_for_null_query_params() {
                TopHitsQueryCriteria queryParams = null;

                var result = AggregateValueCountByFieldAdapter.adaptQueryForFields(APP_ID_FIELD_NAMES, queryParams);

                assertThatJson(result).isEqualTo(TOP_APPS_QUERY_WITH_NULL_TIME_RANGE_PARAMS);
            }

            @Test
            void should_build_top_apps_query_with_empty_id_filter_array_for_empty_api_ids_query_params() {
                var queryParams = new TopHitsQueryCriteria(List.of(), FROM, TO);
                var result = AggregateValueCountByFieldAdapter.adaptQueryForFields(APP_ID_FIELD_NAMES, queryParams);

                assertThatJson(result).isEqualTo(TOP_APPS_QUERY_WITH_EMPTY_APP_ID_LIST);
            }

            private static final String TOP_APPS_QUERY_WITH_API_IDS_AND_TIME_RANGES = """
                 {
                     "size": 0,
                     "query": {
                         "bool": {
                             "filter": [
                                 {
                                   "bool": {
                                     "should": [
                                       { "terms": { "api-id": ["api-id-1", "api-id-2"] } },
                                       { "terms": { "api": ["api-id-1", "api-id-2"] } }
                                     ]
                                   }
                                 },
                                 {
                                     "range": {
                                         "@timestamp": {
                                             "gte": 1728992401566,
                                             "lte" : 1729078801566
                                         }
                                     }
                                 }
                             ]
                         }
                     },
                     "aggs": {
                          "top_hits_count_application-id": {
                              "terms": {
                                  "field": "application-id"
                              },
                              "aggs": {
                                  "hits_count": {
                                      "value_count": {
                                          "field": "application-id"
                                      }
                                  }
                              }
                          },
                          "top_hits_count_application": {
                              "terms": {
                                  "field": "application"
                              },
                              "aggs": {
                                  "hits_count": {
                                      "value_count": {
                                          "field": "application"
                                      }
                                  }
                              }
                          }
                      }
                 }\s
                \s""";

            private static final String TOP_APPS_QUERY_WITH_NULL_TIME_RANGE_PARAMS = """
                 {
                     "size": 0,
                     "query": {
                         "bool": {
                             "filter": [
                                 {
                                   "bool": {
                                     "should": [
                                       { "terms": { "api-id": [] } },
                                       { "terms": { "api": [] } }
                                     ]
                                   }
                                 }
                             ]
                         }
                     },
                     "aggs": {
                          "top_hits_count_application-id": {
                              "terms": {
                                  "field": "application-id"
                              },
                              "aggs": {
                                  "hits_count": {
                                      "value_count": {
                                          "field": "application-id"
                                      }
                                  }
                              }
                          },
                          "top_hits_count_application": {
                              "terms": {
                                  "field": "application"
                              },
                              "aggs": {
                                  "hits_count": {
                                      "value_count": {
                                          "field": "application"
                                      }
                                  }
                              }
                          }
                      }
                 }\s
                \s""";

            private static final String TOP_APPS_QUERY_WITH_EMPTY_APP_ID_LIST = """
                 {
                     "size": 0,
                     "query": {
                         "bool": {
                             "filter": [
                                 {
                                   "bool": {
                                     "should": [
                                       { "terms": { "api-id": [] } },
                                       { "terms": { "api": [] } }
                                     ]
                                   }
                                 },
                                 {
                                     "range": {
                                         "@timestamp": {
                                             "gte": 1728992401566,
                                             "lte" : 1729078801566
                                         }
                                     }
                                 }
                             ]
                         }
                     },
                     "aggs": {
                          "top_hits_count_application-id": {
                              "terms": {
                                  "field": "application-id"
                              },
                              "aggs": {
                                  "hits_count": {
                                      "value_count": {
                                          "field": "application-id"
                                      }
                                  }
                              }
                          },
                          "top_hits_count_application": {
                              "terms": {
                                  "field": "application"
                              },
                              "aggs": {
                                  "hits_count": {
                                      "value_count": {
                                          "field": "application"
                                      }
                                  }
                              }
                          }
                      }
                 }\s
                \s""";
        }

        @Nested
        class Response {

            private final ObjectMapper objectMapper = new ObjectMapper();
            private final SearchResponse searchResponse = new SearchResponse();

            @Test
            void should_return_empty_result_if_aggregations_is_null() {
                assertThat(AggregateValueCountByFieldAdapter.adaptResponse(searchResponse)).isEmpty();
            }

            @Test
            void should_return_empty_result_if_no_aggregation_exist() {
                searchResponse.setAggregations(Map.of());

                assertThat(AggregateValueCountByFieldAdapter.adaptResponse(searchResponse)).isEmpty();
            }

            @Test
            void should_return_top_apps_aggregate() {
                final var APP_ID_1 = "app-id-1";
                final var APP_ID_1_COUNT = 35L;
                final var APP_ID_2 = "app-id-2";
                final var APP_ID_2_COUNT = 47L;
                final Aggregation topHitsCountAggregation = new Aggregation();

                searchResponse.setAggregations(Map.of(TOP_HITS_COUNT, topHitsCountAggregation));

                topHitsCountAggregation.setBuckets(
                    List.of(createTopHitsBucket(APP_ID_1, APP_ID_1_COUNT), createTopHitsBucket(APP_ID_2, APP_ID_2_COUNT))
                );

                var result = AggregateValueCountByFieldAdapter.adaptResponse(searchResponse);

                assertThat(result).hasValueSatisfying(topHits ->
                    assertThat(topHits.getTopHitsCounts()).containsEntry(APP_ID_1, APP_ID_1_COUNT).containsEntry(APP_ID_2, APP_ID_2_COUNT)
                );
            }

            JsonNode createTopHitsBucket(String apiId, long topHitsCount) {
                var node = objectMapper.createObjectNode();
                node.put("key", apiId);
                node.put("doc_count", topHitsCount);
                node.putObject(HITS_COUNT).put("value", topHitsCount);
                return node;
            }
        }
    }

    @Nested
    class TopHitsApis {

        private static final List<String> API_ID_FIELD_NAMES = List.of("api-id", "api");

        @Nested
        class Query {

            @Test
            void should_build_top_hits_query_searching_for_top_ids_and_time_ranges() {
                var queryParams = new TopHitsQueryCriteria(API_IDS, FROM, TO);

                var result = AggregateValueCountByFieldAdapter.adaptQueryForFields(API_ID_FIELD_NAMES, queryParams);

                assertThatJson(result).isEqualTo(TOP_HIT_QUERY_WITH_API_IDS_AND_TIME_RANGES);
            }

            @Test
            void should_build_top_hits_query_with_empty_id_filter_array_for_null_query_params() {
                TopHitsQueryCriteria queryParams = null;

                var result = AggregateValueCountByFieldAdapter.adaptQueryForFields(API_ID_FIELD_NAMES, queryParams);

                assertThatJson(result).isEqualTo(TOP_HIT_QUERY_WITH_NULL_QUERY_PARAMS);
            }

            @Test
            void should_build_top_hits_query_with_empty_id_filter_array_for_empty_api_ids_query_params() {
                var queryParams = new TopHitsQueryCriteria(List.of(), FROM, TO);
                var result = AggregateValueCountByFieldAdapter.adaptQueryForFields(API_ID_FIELD_NAMES, queryParams);

                assertThatJson(result).isEqualTo(TOP_HIT_QUERY_WITH_EMPTY_ID_LIST);
            }

            private static final String TOP_HIT_QUERY_WITH_API_IDS_AND_TIME_RANGES = """
                 {
                     "size": 0,
                     "query": {
                         "bool": {
                             "filter": [
                                 {
                                   "bool": {
                                     "should": [
                                       { "terms": { "api-id": ["api-id-1", "api-id-2"] } },
                                       { "terms": { "api": ["api-id-1", "api-id-2"] } }
                                     ]
                                   }
                                 },
                                 {
                                     "range": {
                                         "@timestamp": {
                                             "gte": 1728992401566,
                                             "lte" : 1729078801566
                                         }
                                     }
                                 }
                             ]
                         }
                     },
                     "aggs": {
                              "top_hits_count_api-id": {
                                  "terms": {
                                      "field": "api-id"
                                  },
                                  "aggs": {
                                      "hits_count": {
                                          "value_count": {
                                              "field": "api-id"
                                          }
                                      }
                                  }
                              },
                              "top_hits_count_api": {
                                  "terms": {
                                      "field": "api"
                                  },
                                  "aggs": {
                                      "hits_count": {
                                          "value_count": {
                                              "field": "api"
                                          }
                                      }
                                  }
                              }
                          }
                      }
                 }\s
                \s""";

            private static final String TOP_HIT_QUERY_WITH_NULL_QUERY_PARAMS = """
                 {
                     "size": 0,
                     "query": {
                         "bool": {
                             "filter": [
                                 {
                                 "bool": {
                                        "should": [
                                         { "terms": { "api-id": [] } },
                                         { "terms": { "api": [] } }
                                       ]
                                     }
                                 }
                             ]
                         }
                     },
                     "aggs": {
                              "top_hits_count_api-id": {
                                  "terms": {
                                      "field": "api-id"
                                  },
                                  "aggs": {
                                      "hits_count": {
                                          "value_count": {
                                              "field": "api-id"
                                          }
                                      }
                                  }
                              },
                              "top_hits_count_api": {
                                  "terms": {
                                      "field": "api"
                                  },
                                  "aggs": {
                                      "hits_count": {
                                          "value_count": {
                                              "field": "api"
                                          }
                                      }
                                  }
                              }
                          }
                     }
                 }\s
                \s""";

            private static final String TOP_HIT_QUERY_WITH_EMPTY_ID_LIST = """
                 {
                     "size": 0,
                     "query": {
                         "bool": {
                             "filter": [
                                 {
                                    "bool": {
                                        "should": [
                                         { "terms": { "api-id": [] } },
                                         { "terms": { "api": [] } }
                                         ]
                                    }
                                 },
                                 {
                                     "range": {
                                         "@timestamp": {
                                             "gte": 1728992401566,
                                             "lte" : 1729078801566
                                         }
                                     }
                                 }
                             ]
                         }
                     },
                     "aggs": {
                              "top_hits_count_api-id": {
                                  "terms": {
                                      "field": "api-id"
                                  },
                                  "aggs": {
                                      "hits_count": {
                                          "value_count": {
                                              "field": "api-id"
                                          }
                                      }
                                  }
                              },
                              "top_hits_count_api": {
                                  "terms": {
                                      "field": "api"
                                  },
                                  "aggs": {
                                      "hits_count": {
                                          "value_count": {
                                              "field": "api"
                                          }
                                      }
                                  }
                              }
                          }
                     }
                 }\s
                \s""";
        }

        @Nested
        class Response {

            private final ObjectMapper objectMapper = new ObjectMapper();
            private final SearchResponse searchResponse = new SearchResponse();

            @Test
            void should_return_empty_result_if_aggregations_is_null() {
                assertThat(AggregateValueCountByFieldAdapter.adaptResponse(searchResponse)).isEmpty();
            }

            @Test
            void should_return_empty_result_if_no_aggregation_exist() {
                searchResponse.setAggregations(Map.of());

                assertThat(AggregateValueCountByFieldAdapter.adaptResponse(searchResponse)).isEmpty();
            }

            @Test
            void should_return_top_hits_aggregate() {
                final var API_ID_1 = "api-id-1";
                final Aggregation topFailedAggregation = new Aggregation();

                searchResponse.setAggregations(Map.of("failed_apis_agg_api-id", topFailedAggregation));

                topFailedAggregation.setBuckets(List.of(createFailedApiBucket(API_ID_1)));

                var result = SearchTopFailedApisAdapter.adaptResponse(searchResponse);

                assertThat(result).hasValueSatisfying(topHits ->
                    assertThat(topHits.failedApis()).containsEntry(API_ID_1, new TopFailedAggregate.FailedApiInfo(10, 0.5))
                );
            }

            JsonNode createFailedApiBucket(String apiId) {
                var node = objectMapper.createObjectNode();
                node.put("key", apiId);
                node.put("doc_count", 20);
                node.putObject("failed_requests").putObject("failed_requests_count").put("value", 10);
                node.putObject("total_requests").put("value", 20);
                node.putObject("failed_requests_ratio").put("value", 0.5);
                return node;
            }
        }
    }
}
