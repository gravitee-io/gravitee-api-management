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

import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetailQuery;
import java.util.stream.Stream;
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
                ConnectionLogDetailQuery.Filter.builder().apiId("f1608475-dd77-4603-a084-75dd775603e9").build(),
                """
                {
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
                    }
                 }
                """
            ),
            Arguments.of(
                ConnectionLogDetailQuery.Filter.builder().requestId("f1608475-dd77-4603-a084-75dd775603e9").build(),
                """
                {
                    "query": {
                        "bool": {
                            "must": [
                                {
                                    "term": {
                                        "request-id": "f1608475-dd77-4603-a084-75dd775603e9"
                                    }
                                }
                            ]
                        }
                    }
                 }
                """
            ),
            Arguments.of(
                ConnectionLogDetailQuery.Filter.builder()
                    .requestId("f1608475-dd77-4603-a084-75dd775603e9")
                    .apiId("f1608475-dd77-4603-a084-75dd775603e5")
                    .build(),
                """
                {
                    "query": {
                        "bool": {
                            "must": [
                                {
                                    "term": {
                                        "request-id": "f1608475-dd77-4603-a084-75dd775603e9"
                                    }
                                },
                                {
                                    "term": {
                                        "api-id": "f1608475-dd77-4603-a084-75dd775603e5"
                                    }
                                }
                            ]
                        }
                    }
                 }
                """
            )
        );
    }
}
