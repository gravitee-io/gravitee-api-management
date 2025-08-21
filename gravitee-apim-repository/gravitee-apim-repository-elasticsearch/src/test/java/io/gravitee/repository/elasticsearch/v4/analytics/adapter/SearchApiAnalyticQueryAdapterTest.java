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

import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticQuery;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SearchApiAnalyticQueryAdapterTest {

    @ParameterizedTest
    @MethodSource("apiMetricQueries")
    void should_build_query_with_filters_and_time_period(ApiAnalyticQuery query, String expected) {
        var result = SearchApiAnalyticQueryAdapter.adapt(query);
        assertThatJson(result).isEqualTo(expected);
    }

    static Stream<Arguments> apiMetricQueries() {
        return Stream.of(
            Arguments.of(
                new ApiAnalyticQuery("apiId", "requestId"),
                """
                        {
                          "query": {
                            "bool": {
                              "must": [
                                { "term": { "api-id": "apiId" } },
                                { "term": { "request-id": "requestId" } }
                              ]
                            }
                          }
                        }
                        """
            ),
            Arguments.of(
                new ApiAnalyticQuery("apiId", null),
                """
                        {
                          "query": {
                            "bool": {
                              "must": [
                                { "term": { "api-id": "apiId" } }
                              ]
                            }
                          }
                        }
                        """
            ),
            Arguments.of(
                new ApiAnalyticQuery(null, "requestId"),
                """
                        {
                          "query": {
                            "bool": {
                              "must": [
                                { "term": { "request-id": "requestId" } }
                              ]
                            }
                          }
                        }
                        """
            ),
            Arguments.of(new ApiAnalyticQuery(null, null), """
                        {}
                        """)
        );
    }
}
