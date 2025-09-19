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
package io.gravitee.repository.elasticsearch.v4.log.adapter.message;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

import io.gravitee.repository.log.v4.model.message.MessageLogQuery;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SearchMessageLogQueryAdapterTest {

    @ParameterizedTest
    @MethodSource("noFilter")
    void should_build_query_without_filter(MessageLogQuery.Filter filter) {
        var result = SearchMessageLogQueryAdapter.adapt(MessageLogQuery.builder().page(1).size(20).filter(filter).build());

        assertThatJson(result).isEqualTo(
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
    void should_build_query_with_filters(MessageLogQuery.Filter filter, String expected) {
        var result = SearchMessageLogQueryAdapter.adapt(MessageLogQuery.builder().page(1).size(20).filter(filter).build());

        assertThatJson(result).when(IGNORING_ARRAY_ORDER).isEqualTo(expected);
    }

    private static Stream<Arguments> noFilter() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(MessageLogQuery.Filter.builder().build()));
    }

    private static Stream<Arguments> getFilters() {
        return Stream.of(
            Arguments.of(
                MessageLogQuery.Filter.builder().apiId("f1608475-dd77-4603-a084-75dd775603e9").build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "api-id":"f1608475-dd77-4603-a084-75dd775603e9" } }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageLogQuery.Filter.builder()
                    .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                    .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "api-id":"f1608475-dd77-4603-a084-75dd775603e9" } },
                        { "term": { "request-id":"8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48" } }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageLogQuery.Filter.builder()
                    .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                    .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                    .connectorType("entrypoint")
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "api-id":"f1608475-dd77-4603-a084-75dd775603e9" } },
                        { "term": { "request-id":"8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48" } },
                        { "term": { "connector-type":"entrypoint" } }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            )
        );
    }
}
