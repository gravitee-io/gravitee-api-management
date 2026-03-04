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

import io.gravitee.repository.log.v4.model.analytics.StatsQuery;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchStatsQueryAdapterTest {

    @Test
    void should_build_stats_query_with_api_and_time_range() {
        var from = Instant.parse("2021-01-01T00:00:00Z");
        var to = from.plus(Duration.ofDays(1));

        var result = SearchStatsQueryAdapter.adapt(new StatsQuery("my-api", from, to, "gateway-response-time-ms"));

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
                    "stats_field": {
                        "stats": { "field": "gateway-response-time-ms" }
                    }
                }
            }
            """
            );
    }

    @Test
    void should_build_stats_query_with_request_content_length_field() {
        var from = Instant.parse("2021-01-01T00:00:00Z");
        var to = from.plus(Duration.ofDays(1));

        var result = SearchStatsQueryAdapter.adapt(new StatsQuery("api-1", from, to, "request-content-length"));

        assertThatJson(result).inPath("aggs.stats_field.stats.field").isEqualTo("request-content-length");
    }

    @Test
    void should_include_aggs_even_with_minimal_query() {
        var result = SearchStatsQueryAdapter.adapt(new StatsQuery(null, null, null, "status"));

        assertThatJson(result).inPath("aggs.stats_field").isNotNull();
        assertThatJson(result).inPath("aggs.stats_field.stats.field").isEqualTo("status");
    }
}
