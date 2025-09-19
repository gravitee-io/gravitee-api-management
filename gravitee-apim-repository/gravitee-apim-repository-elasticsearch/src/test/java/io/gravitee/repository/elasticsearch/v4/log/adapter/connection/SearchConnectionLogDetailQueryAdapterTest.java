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
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetailQuery;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SearchConnectionLogDetailQueryAdapterTest {

    @ParameterizedTest
    @MethodSource("noFilter")
    void should_build_query_without_filter(ConnectionLogDetailQuery.Filter filter) {
        var result = SearchConnectionLogDetailQueryAdapter.adapt(ConnectionLogDetailQuery.builder().filter(filter).build());

        assertThatJson(result).isEqualTo(
            """
            {
            "size":20,"from":0,"sort":{"@timestamp":{"order":"desc"}}
            }
            """
        );
    }

    @Test
    void should_include_projection_fields() {
        var result = SearchConnectionLogDetailQueryAdapter.adapt(
            ConnectionLogDetailQuery.builder()
                .projectionFields(List.of("_id", "api"))
                .filter(ConnectionLogDetailQuery.Filter.builder().build())
                .build()
        );

        assertThatJson(result).isEqualTo(
            """
            {
            "size":20,
            "from":0,
            "_source": [ "_id", "api" ],
            "sort":{"@timestamp":{"order":"desc"}}
            }
            """
        );
    }

    @ParameterizedTest
    @MethodSource("getFilters")
    void should_build_query_with_filters(ConnectionLogDetailQuery.Filter filter, String expected) {
        var result = SearchConnectionLogDetailQueryAdapter.adapt(ConnectionLogDetailQuery.builder().filter(filter).build());

        assertThatJson(result).when(IGNORING_ARRAY_ORDER).isEqualTo(expected);
    }

    private static Stream<Arguments> noFilter() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(ConnectionLogDetailQuery.Filter.builder().build()));
    }

    private static Stream<Arguments> getFilters() {
        return Stream.of(
            Arguments.of(
                ConnectionLogDetailQuery.Filter.builder().apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9")).build(),
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
                                                   "api": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                               }
                                           }, {
                                               "terms": {
                                                   "api-id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                               }
                                           }
                                       ]
                                   }
                               }
                            ]
                        }
                    },"sort":{"@timestamp":{"order":"desc"}}
                 }
                """
            ),
            Arguments.of(
                ConnectionLogDetailQuery.Filter.builder().requestIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9")).build(),
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
                                                   "_id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                               }
                                           }, {
                                               "terms": {
                                                   "request-id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                               }
                                           }
                                       ]
                                   }
                               }
                           ]
                        }
                    },"sort":{"@timestamp":{"order":"desc"}}
                 }
                """
            ),
            Arguments.of(
                ConnectionLogDetailQuery.Filter.builder()
                    .requestIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9"))
                    .apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e5"))
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
                                                     "_id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                                 }
                                             }, {
                                                 "terms": {
                                                     "request-id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
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
                                                     "api": [ "f1608475-dd77-4603-a084-75dd775603e5" ]
                                                 }
                                             }, {
                                                 "terms": {
                                                     "api-id": [ "f1608475-dd77-4603-a084-75dd775603e5" ]
                                                 }
                                             }
                                         ]
                                     }
                                 }
                            ]
                        }
                    },"sort":{"@timestamp":{"order":"desc"}}
                 }
                """
            ),
            Arguments.of(
                ConnectionLogDetailQuery.Filter.builder()
                    .from(0L)
                    .to(1L)
                    .bodyText("curl")
                    .methods(Set.of(HttpMethod.GET, HttpMethod.DELETE))
                    .uri("my-path")
                    .statuses(Set.of(100, 200))
                    .apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e5"))
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
                                                     "api": [ "f1608475-dd77-4603-a084-75dd775603e5" ]
                                                 }
                                             }, {
                                                 "terms": {
                                                     "api-id": [ "f1608475-dd77-4603-a084-75dd775603e5" ]
                                                 }
                                             }
                                         ]
                                     }
                                 },
                                 {
                                     "range": {
                                         "@timestamp": {
                                             "gte":0,
                                             "lte":1
                                         }
                                     }
                                 },
                                 {
                                     "bool": {
                                         "should": [
                                             {
                                                 "match": {
                                                     "entrypoint-request.method": "GET"
                                                 }
                                             }, {
                                                 "match": {
                                                     "entrypoint-request.method": "DELETE"
                                                 }
                                             }, {
                                                 "match": {
                                                     "client-request.method": "GET"
                                                 }
                                             }, {
                                                 "match": {
                                                     "client-request.method": "DELETE"
                                                 }
                                             }
                                         ]
                                     }
                                 }, {
                                     "bool": {
                                         "should": [
                                             {
                                                 "match": {
                                                     "endpoint-response.status": 100
                                                 }
                                             }, {
                                                 "match": {
                                                     "endpoint-response.status": 200
                                                 }
                                             }, {
                                                 "match": {
                                                     "client-response.status": 100
                                                 }
                                             }, {
                                                 "match": {
                                                     "client-response.status": 200
                                                 }
                                             }
                                         ]
                                     }
                                 }, {
                                     "bool": {
                                         "should": [
                                             {
                                                 "match": {
                                                     "client-request.uri": "my-path"
                                                 }
                                             }, {
                                                 "match": {
                                                     "entrypoint-request.uri": "my-path"
                                                 }
                                             }
                                         ]
                                     }
                                 }, {
                                     "query_string": {
                                         "query": "\\\\*.body:curl*"
                                     }
                                 }
                            ]
                        }
                    },"sort":{"@timestamp":{"order":"desc"}}
                 }
                """
            )
        );
    }
}
