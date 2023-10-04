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
package io.gravitee.repository.elasticsearch.v4.log.adapter;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchConnectionLogQueryAdapter;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogQuery;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SearchConnectionLogQueryAdapterTest {

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

        assertThatJson(result).isEqualTo(expected);
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

    @Test
    void should_build_query_asking_filtering_by_application_ids_and_api() {
        var result = SearchConnectionLogQueryAdapter.adapt(
            ConnectionLogQuery
                .builder()
                .page(1)
                .size(10)
                .filter(ConnectionLogQuery.Filter.builder().apiId("1").applicationIds(Set.of("2", "3")).build())
                .build()
        );

        assertThatJson(result)
            .when(IGNORING_ARRAY_ORDER)
            .isEqualTo(
                """
                        {
                            "from": 0,
                            "size": 10,
                            "query": {
                                "bool": {
                                    "must": [
                                        {
                                            "term": {
                                                "api-id": "1"
                                            }
                                        },
                                        {
                                            "terms": {
                                                "application-id": ["2", "3"]
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
            );
    }

    private static Stream<Arguments> noFilter() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(ConnectionLogQuery.Filter.builder().build()));
    }

    private static Stream<Arguments> getFilters() {
        return Stream.of(
            Arguments.of(
                ConnectionLogQuery.Filter.builder().apiId("f1608475-dd77-4603-a084-75dd775603e9").build(),
                """
                {
                    "from": 0,
                    "size": 20,
                    "query": {
                        "bool": {
                            "must": [
                                {
                                    "term": {
                                        "api-id": "f1608475-dd77-4603-a084-75dd775603e9"
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
                    .apiId("f1608475-dd77-4603-a084-75dd775603e9")
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
                                    "term": {
                                        "api-id": "f1608475-dd77-4603-a084-75dd775603e9"
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
            )
        );
    }
}
