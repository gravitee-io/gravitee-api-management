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
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.TopFailedQueryCriteria;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SearchTopFailedApisAdapterTest {

    private static final String TOP_HITS_COUNT = "top_hits_count";
    private static final String HITS_COUNT = "hits_count";
    private static final long FROM = 1728992401566L;
    private static final long TO = 1729078801566L;
    private static final List<String> API_IDS = List.of("api-id-1", "api-id-2");

    @Nested
    class Query {

        @Test
        public void build_top_failed_apis_query() {
            var queryParams = new TopFailedQueryCriteria(API_IDS, FROM, TO);
            var result = SearchTopFailedApisAdapter.adaptQuery(queryParams);
            assertThatJson(result).isEqualTo(TOP_FAILED_QUERY);
        }

        private static final String TOP_FAILED_QUERY = """
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
                                                    "api-id-1",
                                                    "api-id-2"
                                                ]
                                            }
                                        },
                                        {
                                            "terms": {
                                                "api": [
                                                    "api-id-1",
                                                    "api-id-2"
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
                    "failed_apis_agg_api-id": {
                        "terms": {
                            "field": "api-id"
                        },
                        "aggs": {
                            "total_requests": {
                                "value_count": {
                                    "field": "api-id"
                                }
                            },
                            "failed_requests": {
                                "filter": {
                                    "range": {
                                        "status": {
                                            "gte": 500,
                                            "lt": 600
                                        }
                                    }
                                },
                                "aggs": {
                                    "failed_requests_count": {
                                        "value_count": {
                                            "field": "status"
                                        }
                                    }
                                }
                            },
                            "failed_requests_ratio": {
                                "bucket_script": {
                                    "buckets_path": {
                                        "failed_count": "failed_requests>failed_requests_count",
                                        "total_count": "total_requests"
                                    },
                                    "script": "params.failed_count / params.total_count"
                                }
                            }
                        }
                    },
                    "failed_apis_agg_api": {
                        "terms": {
                            "field": "api"
                        },
                        "aggs": {
                            "total_requests": {
                                "value_count": {
                                    "field": "api"
                                }
                            },
                            "failed_requests": {
                                "filter": {
                                    "range": {
                                        "status": {
                                            "gte": 500,
                                            "lt": 600
                                        }
                                    }
                                },
                                "aggs": {
                                    "failed_requests_count": {
                                        "value_count": {
                                            "field": "status"
                                        }
                                    }
                                }
                            },
                            "failed_requests_ratio": {
                                "bucket_script": {
                                    "buckets_path": {
                                        "failed_count": "failed_requests>failed_requests_count",
                                        "total_count": "total_requests"
                                    },
                                    "script": "params.failed_count / params.total_count"
                                }
                            }
                        }
                    }
                }
            }
            """;
    }

    @Nested
    class Response {

        private final ObjectMapper objectMapper = new ObjectMapper();
        private final SearchResponse searchResponse = new SearchResponse();

        @Test
        void should_return_empty_result_if_aggregations_is_null() {
            assertThat(SearchTopFailedApisAdapter.adaptResponse(searchResponse)).isEmpty();
        }

        @Test
        void should_return_empty_result_if_no_aggregation_exist() {
            searchResponse.setAggregations(Map.of());

            assertThat(SearchTopFailedApisAdapter.adaptResponse(searchResponse)).isEmpty();
        }

        JsonNode createTopFailedBuckets(String apiId, long topHitsCount) {
            var node = objectMapper.createObjectNode();
            node.put("key", apiId);
            node.put("doc_count", topHitsCount);
            node.putObject(HITS_COUNT).put("value", topHitsCount);
            return node;
        }
    }

    @Test
    public void test() {
        var result = SearchTopFailedApisAdapter.adaptQuery(
            new TopFailedQueryCriteria(
                List.of(
                    "9f060ca4-326b-473a-860c-a4326be73a28",
                    "c9dfb001-39f2-4e41-9fb0-0139f2de4170",
                    "28113601-5197-4815-9136-015197b81592"
                ),
                1740097471000L,
                1740180271000L
            )
        );
        System.out.println(result);
    }
}
