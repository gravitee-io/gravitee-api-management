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

import io.gravitee.repository.log.v4.model.analytics.GroupByQuery;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchGroupByQueryAdapterTest {

    @Test
    void should_build_group_by_query_with_api_and_time_range() {
        var from = Instant.parse("2021-01-01T00:00:00Z");
        var to = from.plus(Duration.ofDays(1));

        var result = SearchGroupByQueryAdapter.adapt(new GroupByQuery("my-api", from, to, "status", 20));

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
                },
                "aggs": {
                    "group_by_field": {
                        "terms": {
                            "field": "status",
                            "size": 20
                        }
                    }
                }
            }
            """
            );
    }

    @Test
    void should_build_group_by_query_with_application_field() {
        var from = Instant.parse("2021-01-01T00:00:00Z");
        var to = from.plus(Duration.ofDays(1));

        var result = SearchGroupByQueryAdapter.adapt(new GroupByQuery("api-1", from, to, "application", 10));

        assertThatJson(result).inPath("aggs.group_by_field.terms.field").isEqualTo("application");
        assertThatJson(result).inPath("aggs.group_by_field.terms.size").isEqualTo(10);
    }

    @Test
    void should_include_aggs_even_with_minimal_query() {
        var result = SearchGroupByQueryAdapter.adapt(new GroupByQuery(null, null, null, "host", 10));

        assertThatJson(result).inPath("aggs.group_by_field").isNotNull();
        assertThatJson(result).inPath("aggs.group_by_field.terms.field").isEqualTo("host");
        assertThatJson(result).inPath("aggs.group_by_field.terms.size").isEqualTo(10);
    }
}
