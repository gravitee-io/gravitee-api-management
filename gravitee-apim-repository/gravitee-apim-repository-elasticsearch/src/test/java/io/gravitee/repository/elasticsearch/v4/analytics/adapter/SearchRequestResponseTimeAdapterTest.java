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

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SearchRequestResponseTimeAdapterTest {

    private static final long FROM = 1728992401566L;
    private static final long TO = 1729078801566L;
    private static final List<String> API_IDS = List.of("api-id-1", "api-id-2");

    @Nested
    class Query {

        @Test
        void should_build_request_response_time_query_searching_for_ids_and_time_ranges() {
            var queryParams = new RequestResponseTimeQueryCriteria(API_IDS, FROM, TO);

            var result = SearchRequestResponseTimeAdapter.adaptQuery(queryParams);

            assertThatJson(result).isEqualTo(REQUEST_RESPONSE_TIME_QUERY_WITH_API_IDS_AND_TIME_RANGES);
        }

        @Test
        void should_build_top_hits_query_with_empty_id_filter_array_for_null_query_params() {
            RequestResponseTimeQueryCriteria queryParams = null;

            var result = SearchRequestResponseTimeAdapter.adaptQuery(queryParams);

            assertThatJson(result).isEqualTo(REQUEST_RESPONSE_TIME_QUERY_WITH_NULL_QUERY_PARAMS);
        }

        @Test
        void should_build_top_hits_query_with_empty_id_filter_array_for_empty_api_ids_query_params() {
            var queryParams = new RequestResponseTimeQueryCriteria(List.of(), FROM, TO);
            var result = SearchRequestResponseTimeAdapter.adaptQuery(queryParams);

            assertThatJson(result).isEqualTo(REQUEST_RESPONSE_TIME_QUERY_WITH_EMPTY_ID_LIST);
        }

        private static final String REQUEST_RESPONSE_TIME_QUERY_WITH_API_IDS_AND_TIME_RANGES =
            """
                {
                    "size": 0,
                    "query": {
                        "bool": {
                            "filter": [
                                {
                                    "terms": {
                                        "api-id": [
                                            "api-id-1",
                                            "api-id-2"
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
                                },
                                {
                                    "terms": {
                                        "entrypoint-id": [
                                            "http-post",
                                            "http-get",
                                            "http-proxy"
                                        ]
                                    }
                                }
                            ]
                        }
                    },
                    "aggs": {
                        "max_response_time": {
                            "max": {
                                "field": "gateway-response-time-ms"
                            }
                        },
                        "min_response_time": {
                            "min": {
                                "field": "gateway-response-time-ms"
                            }
                        },
                        "avg_response_time": {
                            "avg": {
                                "field": "gateway-response-time-ms"
                            }
                        }
                    }
                }
                """;

        private static final String REQUEST_RESPONSE_TIME_QUERY_WITH_NULL_QUERY_PARAMS =
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
                                },
                                {
                                    "terms": {
                                        "entrypoint-id": [
                                            "http-post",
                                            "http-get",
                                            "http-proxy"
                                        ]
                                    }
                                }
                            ]
                        }
                    },
                    "aggs": {
                        "max_response_time": {
                            "max": {
                                "field": "gateway-response-time-ms"
                            }
                        },
                        "min_response_time": {
                            "min": {
                                "field": "gateway-response-time-ms"
                            }
                        },
                        "avg_response_time": {
                            "avg": {
                                "field": "gateway-response-time-ms"
                            }
                        }
                    }
                }
                """;

        private static final String REQUEST_RESPONSE_TIME_QUERY_WITH_EMPTY_ID_LIST =
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
                                },
                                {
                                    "range": {
                                        "@timestamp": {
                                            "gte": 1728992401566,
                                            "lte": 1729078801566
                                        }
                                    }
                                },
                                {
                                    "terms": {
                                        "entrypoint-id": [
                                            "http-post",
                                            "http-get",
                                            "http-proxy"
                                        ]
                                    }
                                }
                            ]
                        }
                    },
                    "aggs": {
                        "max_response_time": {
                            "max": {
                                "field": "gateway-response-time-ms"
                            }
                        },
                        "min_response_time": {
                            "min": {
                                "field": "gateway-response-time-ms"
                            }
                        },
                        "avg_response_time": {
                            "avg": {
                                "field": "gateway-response-time-ms"
                            }
                        }
                    }
                }
                """;
    }

    @Nested
    class Response {

        private final SearchResponse searchResponse = new SearchResponse();
        private final RequestResponseTimeQueryCriteria queryParams = new RequestResponseTimeQueryCriteria(API_IDS, FROM, TO);

        @Test
        void should_return_empty_result_if_aggregations_is_null() {
            assertThat(SearchRequestResponseTimeAdapter.adaptResponse(searchResponse, queryParams))
                .isEqualTo(RequestResponseTimeAggregate.builder().build());
        }

        @Test
        void should_return_just_zeros_result_if_no_aggregation_exist() {
            searchResponse.setAggregations(Map.of());

            assertThat(SearchRequestResponseTimeAdapter.adaptResponse(searchResponse, queryParams))
                .isEqualTo(RequestResponseTimeAggregate.builder().build());
        }

        @Test
        void should_return_just_zeros_result_if_no_top_hits_count_aggregation_exist() {
            searchResponse.setAggregations(Map.of("fake_aggregation", new Aggregation()));

            assertThat(SearchRequestResponseTimeAdapter.adaptResponse(searchResponse, queryParams))
                .isEqualTo(RequestResponseTimeAggregate.builder().build());
        }

        @Test
        void should_return_just_zeros_result_if_query_criteria_is_null() {
            assertThat(SearchRequestResponseTimeAdapter.adaptResponse(searchResponse, null))
                .isEqualTo(RequestResponseTimeAggregate.builder().build());
        }

        @Test
        void should_return_request_response_time_aggregate() {
            final Aggregation minResponseTimeAggregation = new Aggregation();
            minResponseTimeAggregation.setValue(20.0f);
            final Aggregation maxResponseTimeAggregation = new Aggregation();
            maxResponseTimeAggregation.setValue(1200.0f);
            final Aggregation avgResponseTimeAggregation = new Aggregation();
            avgResponseTimeAggregation.setValue(335.23f);

            searchResponse.setAggregations(
                Map.of(
                    "max_response_time",
                    maxResponseTimeAggregation,
                    "min_response_time",
                    minResponseTimeAggregation,
                    "avg_response_time",
                    avgResponseTimeAggregation
                )
            );

            final SearchHits searchHits = new SearchHits();
            searchHits.setTotal(new TotalHits(4L));
            searchResponse.setSearchHits(searchHits);

            var result = SearchRequestResponseTimeAdapter.adaptResponse(searchResponse, queryParams);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.getRequestsPerSecond()).isEqualTo(4.6296296296296294E-5);
                soft.assertThat(result.getRequestsTotal()).isEqualTo(4);
                soft.assertThat(result.getResponseMinTime()).isEqualTo(20.0);
                soft.assertThat(result.getResponseMaxTime()).isEqualTo(1200.0);
                soft.assertThat(result.getResponseAvgTime()).isEqualTo(335.2300109863281);
            });
        }
    }
}
