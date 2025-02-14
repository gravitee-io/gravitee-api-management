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
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogQuery;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SearchConnectionLogQueryAdapterTest {

    @ParameterizedTest
    @MethodSource("noFilter")
    void should_build_query_without_filter(ConnectionLogQuery.Filter filter) {
        var result = SearchConnectionLogQueryAdapter.adapt(ConnectionLogQuery.builder().page(1).size(20).filter(filter).build());

        assertThatJson(result)
            .isEqualTo(
                """
                             {
                               "from": 0,
                               "size": 20,
                               "sort": {
                                 "@timestamp": { "order": "desc" }
                               }
                             }
                             """
            );
    }

    @ParameterizedTest
    @MethodSource("getFilters")
    void should_build_query_with_filters(ConnectionLogQuery.Filter filter, String expected) {
        var result = SearchConnectionLogQueryAdapter.adapt(ConnectionLogQuery.builder().page(1).size(20).filter(filter).build());

        assertThatJson(result).when(IGNORING_ARRAY_ORDER).isEqualTo(expected);
    }

    @Test
    void should_build_query_asking_another_page() {
        var result = SearchConnectionLogQueryAdapter.adapt(ConnectionLogQuery.builder().page(3).size(10).build());

        assertThatJson(result)
            .isEqualTo(
                """
                             {
                               "from": 20,
                               "size": 10,
                               "sort": {
                                 "@timestamp": { "order": "desc" }
                               }
                             }
                             """
            );
    }

    private static Stream<Arguments> noFilter() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(ConnectionLogQuery.Filter.builder().build()));
    }

    private static Stream<Arguments> getFilters() {
        return Stream.of(
            Arguments.of(
                ConnectionLogQuery.Filter.builder().apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9")).build(),
                """
                             {
                                 "from": 0,
                                 "size": 20,
                                 "query": {
                                     "bool": {
                                         "must": [
                                         {
                                             "bool": {
                                                "should": [
                                                    {
                                                         "terms": {
                                                             "api-id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                                         }
                                                    }, {
                                                         "terms": {
                                                             "api": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                                         }
                                                    }
                                                ]
                                             }
                                         }]
                                     }
                                 },
                                 "sort": {
                                     "@timestamp": {
                                         "order": "desc"
                                     }
                                 }
                              }
                             """
            ),
            Arguments.of(
                ConnectionLogQuery.Filter
                    .builder()
                    .apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9"))
                    .from(1695081660000L)
                    .to(1695167999000L)
                    .build(),
                """
                             {
                                 "from": 0,
                                 "size": 20,
                                 "query": {
                                     "bool": {
                                         "must": [
                                             {
                                                 "bool": {
                                                    "should": [
                                                        {
                                                             "terms": {
                                                                 "api-id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                                             }
                                                        }, {
                                                             "terms": {
                                                                 "api": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                                             }
                                                        }
                                                    ]
                                                 }
                                             },
                                             {
                                                 "range": {
                                                     "@timestamp": {
                                                         "gte": 1695081660000,
                                                         "lte": 1695167999000
                                                     }
                                                 }
                                             }
                                         ]
                                     }
                                 },
                                 "sort": {
                                     "@timestamp": {
                                         "order": "desc"
                                     }
                                 }
                              }
                             """
            ),
            Arguments.of(
                ConnectionLogQuery.Filter.builder().from(1695081660000L).to(1695167999000L).build(),
                """
                             {
                                 "from": 0,
                                 "size": 20,
                                 "query": {
                                     "bool": {
                                         "must": [
                                             {
                                                 "range": {
                                                     "@timestamp": {
                                                         "gte": 1695081660000,
                                                         "lte": 1695167999000
                                                     }
                                                 }
                                             }
                                         ]
                                     }
                                 },
                                 "sort": {
                                     "@timestamp": {
                                         "order": "desc"
                                     }
                                 }
                              }
                             """
            ),
            Arguments.of(
                ConnectionLogQuery.Filter.builder().to(1695167999000L).build(),
                """
                             {
                                 "from": 0,
                                 "size": 20,
                                 "query": {
                                     "bool": {
                                         "must": [
                                             {
                                                 "range": {
                                                     "@timestamp": {
                                                         "lte": 1695167999000
                                                     }
                                                 }
                                             }
                                         ]
                                     }
                                 },
                                 "sort": {
                                     "@timestamp": {
                                         "order": "desc"
                                     }
                                 }
                              }
                             """
            ),
            Arguments.of(
                ConnectionLogQuery.Filter.builder().from(1695081660000L).build(),
                """
                             {
                                 "from": 0,
                                 "size": 20,
                                 "query": {
                                     "bool": {
                                         "must": [
                                             {
                                                 "range": {
                                                     "@timestamp": {
                                                         "gte": 1695081660000
                                                     }
                                                 }
                                             }
                                         ]
                                     }
                                 },
                                 "sort": {
                                     "@timestamp": {
                                         "order": "desc"
                                     }
                                 }
                              }
                             """
            ),
            Arguments.of(
                ConnectionLogQuery.Filter.builder().apiIds(Set.of("1")).applicationIds(Set.of("2", "3")).build(),
                """
                             {
                                 "from": 0,
                                 "size": 20,
                                 "query": {
                                     "bool": {
                                         "must": [
                                             {
                                                 "bool": {
                                                    "should": [
                                                        {
                                                             "terms": {
                                                                 "api-id": [ "1" ]
                                                             }
                                                        }, {
                                                             "terms": {
                                                                 "api": [ "1" ]
                                                             }
                                                        }
                                                    ]
                                                 }
                                             },{
                                                 "bool": {
                                                    "should": [
                                                        {
                                                             "terms": {
                                                                 "application-id": ["2", "3"]
                                                             }
                                                        }, {
                                                             "terms": {
                                                                 "application": ["2", "3"]
                                                             }
                                                        }
                                                    ]
                                                 }
                                             }
                                         ]
                                     }
                                 },
                                 "sort": {
                                     "@timestamp": {
                                         "order": "desc"
                                     }
                                 }
                             }
                             """
            ),
            Arguments.of(
                ConnectionLogQuery.Filter
                    .builder()
                    .apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9"))
                    .planIds(Set.of("plan-1", "plan-2"))
                    .build(),
                """
                             {
                                 "from": 0,
                                 "size": 20,
                                 "query": {
                                     "bool": {
                                         "must": [
                                         {
                                                 "bool": {
                                                    "should": [
                                                        {
                                                             "terms": {
                                                                 "api-id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                                             }
                                                        }, {
                                                             "terms": {
                                                                 "api": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                                             }
                                                        }
                                                    ]
                                                 }
                                             },{
                                                 "bool": {
                                                    "should": [
                                                        {
                                                             "terms": {
                                                                 "plan-id": ["plan-1", "plan-2"]
                                                             }
                                                        }, {
                                                             "terms": {
                                                                 "plan": ["plan-1", "plan-2"]
                                                             }
                                                        }
                                                    ]
                                                 }
                                             }
                                         ]
                                     }
                                 },
                                 "sort": {
                                     "@timestamp": {
                                         "order": "desc"
                                     }
                                 }
                              }
                             """
            ),
            Arguments.of(
                ConnectionLogQuery.Filter
                    .builder()
                    .apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9"))
                    .planIds(Set.of("plan-1", "plan-2"))
                    .applicationIds(Set.of("app-1", "app-2", "app-3"))
                    .build(),
                """
                             {
                                 "from": 0,
                                 "size": 20,
                                 "query": {
                                     "bool": {
                                         "must": [
                                            {
                                                 "bool": {
                                                    "should": [
                                                        {
                                                             "terms": {
                                                                 "api-id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                                             }
                                                        }, {
                                                             "terms": {
                                                                 "api": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                                             }
                                                        }
                                                    ]
                                                 }
                                             },{
                                                 "bool": {
                                                    "should": [
                                                        {
                                                             "terms": {
                                                                 "plan-id": ["plan-1", "plan-2"]
                                                             }
                                                        }, {
                                                             "terms": {
                                                                 "plan": ["plan-1", "plan-2"]
                                                             }
                                                        }
                                                    ]
                                                 }
                                             },{
                                                 "bool": {
                                                    "should": [
                                                        {
                                                             "terms": {
                                                                 "application-id": ["app-1", "app-2", "app-3"]
                                                             }
                                                        }, {
                                                             "terms": {
                                                                 "application": ["app-1", "app-2", "app-3"]
                                                             }
                                                        }
                                                    ]
                                                 }
                                             }
                                         ]
                                     }
                                 },
                                 "sort": {
                                     "@timestamp": {
                                         "order": "desc"
                                     }
                                 }
                              }
                             """
            ),
            Arguments.of(
                ConnectionLogQuery.Filter.builder().apiIds(Set.of("1")).methods(Set.of(HttpMethod.GET, HttpMethod.CONNECT)).build(),
                """
                                     {
                                         "from": 0,
                                         "size": 20,
                                         "query": {
                                             "bool": {
                                                 "must": [
                                                 {
                                                     "bool": {
                                                        "should": [
                                                            {
                                                                 "terms": {
                                                                     "api-id": [ "1" ]
                                                                 }
                                                            }, {
                                                                 "terms": {
                                                                     "api": [ "1" ]
                                                                 }
                                                            }
                                                        ]
                                                     }
                                                 },{
                                                     "bool": {
                                                        "should": [
                                                            {
                                                                 "terms": {
                                                                     "http-method": [3, 1]
                                                                 }
                                                            }, {
                                                                 "terms": {
                                                                     "method": [3, 1]
                                                                 }
                                                            }
                                                        ]
                                                     }
                                                 }
                                                 ]
                                             }
                                         },
                                         "sort": {
                                             "@timestamp": {
                                                 "order": "desc"
                                             }
                                         }
                                     }
                                     """
            ),
            Arguments.of(
                ConnectionLogQuery.Filter
                    .builder()
                    .apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9"))
                    .statuses(Set.of(200, 202))
                    .build(),
                """
                                     {
                                         "from": 0,
                                         "size": 20,
                                         "query": {
                                             "bool": {
                                                 "must": [
                                                     {
                                                     "bool": {
                                                        "should": [
                                                            {
                                                                 "terms": {
                                                                     "api-id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                                                 }
                                                            }, {
                                                                 "terms": {
                                                                     "api": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                                                 }
                                                            }
                                                        ]
                                                     }
                                                 },
                                                     {
                                                         "terms": {
                                                             "status": [200, 202]
                                                         }
                                                    }
                                                 ]
                                             }
                                         },
                                         "sort": {
                                             "@timestamp": {
                                                 "order": "desc"
                                             }
                                         }
                                      }
                                     """
            ),
            Arguments.of(
                ConnectionLogQuery.Filter.builder().entrypointIds(Set.of("http-post", "http-get")).build(),
                """
                                     {
                                         "from": 0,
                                         "size": 20,
                                         "query": {
                                             "bool": {
                                                 "must": [
                                                     {
                                                         "terms": {
                                                             "entrypoint-id": ["http-post", "http-get"]
                                                         }
                                                     }
                                                 ]
                                             }
                                         },
                                         "sort": {
                                             "@timestamp": {
                                                 "order": "desc"
                                             }
                                         }
                                      }
            """
            ),
            Arguments.of(
                ConnectionLogQuery.Filter
                    .builder()
                    .requestIds(Set.of("req-1", "req-2"))
                    .transactionIds(Set.of("t-1"))
                    .uri("my-path")
                    .build(),
                """
                                     {
                                         "from": 0,
                                         "size": 20,
                                         "query": {
                                             "bool": {
                                                 "must": [
                                                     {
                                                         "bool": {
                                                            "should": [
                                                                {
                                                                     "terms": {
                                                                         "_id": [ "req-1", "req-2" ]
                                                                     }
                                                                }, {
                                                                     "terms": {
                                                                         "request-id": [ "req-1", "req-2" ]
                                                                     }
                                                                }
                                                            ]
                                                         }
                                                     },
                                                     {
                                                         "bool": {
                                                            "should": [
                                                                {
                                                                     "terms": {
                                                                         "transaction": [ "t-1" ]
                                                                     }
                                                                }, {
                                                                     "terms": {
                                                                         "transaction-id": [ "t-1" ]
                                                                     }
                                                                }
                                                            ]
                                                         }
                                                     },
                                                     {
                                                         "term": {
                                                             "uri": "my-path"
                                                         }
                                                     }
                                                 ]
                                             }
                                         },
                                         "sort": {
                                             "@timestamp": {
                                                 "order": "desc"
                                             }
                                         }
                                      }
            """
            )
        );
    }
}
