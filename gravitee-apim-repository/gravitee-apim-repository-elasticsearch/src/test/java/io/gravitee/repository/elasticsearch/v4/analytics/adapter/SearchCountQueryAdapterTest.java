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

import io.gravitee.repository.log.v4.model.analytics.CountQuery;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchCountQueryAdapterTest {

    @Test
    void should_build_count_only_query_without_filter() {
        var result = SearchCountQueryAdapter.adapt(null);

        assertThatJson(result).isEqualTo("""
            {
                "size": 0
            }
            """);
    }

    @Test
    void should_build_query_with_api_filter() {
        var result = SearchCountQueryAdapter.adapt(new CountQuery("my-api", null, null));

        assertThatJson(result)
            .isEqualTo(
                """
            {
                "size": 0,
                "query": {
                    "bool": {
                        "must": [
                            { "term": { "api-id": "my-api" } }
                        ]
                    }
                }
            }
            """
            );
    }

    @Test
    void should_build_query_with_api_and_time_range_filter() {
        var from = Instant.parse("2021-01-01T00:00:00Z");
        var to = from.plus(Duration.ofDays(1));

        var result = SearchCountQueryAdapter.adapt(new CountQuery("my-api", from, to));

        assertThatJson(result)
            .isEqualTo(
                """
            {
                "size": 0,
                "query": {
                    "bool": {
                        "must": [
                            { "term": { "api-id": "my-api" } },
                            {
                                "range": {
                                    "@timestamp": {
                                        "from": 1609459200000,
                                        "include_lower": true,
                                        "to": 1609545600000,
                                        "include_upper": true
                                    }
                                }
                            }
                        ]
                    }
                }
            }
            """
            );
    }

    @Test
    void should_build_query_with_time_range_only() {
        var from = Instant.parse("2021-01-01T00:00:00Z");
        var to = from.plus(Duration.ofDays(1));

        var result = SearchCountQueryAdapter.adapt(new CountQuery(null, from, to));

        assertThatJson(result)
            .isEqualTo(
                """
            {
                "size": 0,
                "query": {
                    "bool": {
                        "must": [
                            {
                                "range": {
                                    "@timestamp": {
                                        "from": 1609459200000,
                                        "include_lower": true,
                                        "to": 1609545600000,
                                        "include_upper": true
                                    }
                                }
                            }
                        ]
                    }
                }
            }
            """
            );
    }

    @Test
    void should_build_query_with_null_fields() {
        var result = SearchCountQueryAdapter.adapt(new CountQuery(null, null, null));

        assertThatJson(result).isEqualTo("""
            {
                "size": 0
            }
            """);
    }
}
