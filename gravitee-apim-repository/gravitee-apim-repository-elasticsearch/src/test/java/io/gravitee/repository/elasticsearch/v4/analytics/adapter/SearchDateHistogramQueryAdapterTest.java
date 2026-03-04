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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.elasticsearch.version.Version;
import io.gravitee.repository.log.v4.model.analytics.DateHistogramQuery;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchDateHistogramQueryAdapterTest {

    private static ElasticsearchInfo esInfoWithFixedInterval() {
        var version = mock(Version.class);
        when(version.canUseDateHistogramFixedInterval()).thenReturn(true);
        var info = new ElasticsearchInfo();
        info.setVersion(version);
        return info;
    }

    private static ElasticsearchInfo esInfoWithLegacyInterval() {
        var version = mock(Version.class);
        when(version.canUseDateHistogramFixedInterval()).thenReturn(false);
        var info = new ElasticsearchInfo();
        info.setVersion(version);
        return info;
    }

    @Nested
    class With_fixed_interval {

        @Test
        void should_build_date_histogram_query_with_api_and_time_range() {
            var from = Instant.parse("2021-01-01T00:00:00Z");
            var to = from.plus(Duration.ofDays(1));
            var interval = Duration.ofHours(1);

            var query = new DateHistogramQuery("my-api", from, to, "status", interval, 20);
            var result = SearchDateHistogramQueryAdapter.adapt(query, esInfoWithFixedInterval());

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
                        "by_date": {
                            "date_histogram": {
                                "field": "@timestamp",
                                "fixed_interval": "3600000ms",
                                "min_doc_count": 0,
                                "extended_bounds": {
                                    "min": 1609459200000,
                                    "max": 1609545600000
                                }
                            },
                            "aggregations": {
                                "by_field": {
                                    "terms": {
                                        "field": "status",
                                        "size": 20
                                    }
                                }
                            }
                        }
                    }
                }
                """
                );
        }

        @Test
        void should_use_fixed_interval_for_elasticsearch_8() {
            var from = Instant.parse("2021-01-01T00:00:00Z");
            var to = from.plus(Duration.ofHours(6));
            var query = new DateHistogramQuery("api-1", from, to, "status", Duration.ofMinutes(10), 10);

            var result = SearchDateHistogramQueryAdapter.adapt(query, esInfoWithFixedInterval());

            assertThatJson(result).inPath("aggs.by_date.date_histogram.fixed_interval").isEqualTo("600000ms");
            assertThatJson(result).inPath("aggs.by_date.date_histogram.interval").isAbsent();
        }
    }

    @Nested
    class With_legacy_interval {

        @Test
        void should_use_interval_for_older_elasticsearch() {
            var from = Instant.parse("2021-01-01T00:00:00Z");
            var to = from.plus(Duration.ofHours(6));
            var query = new DateHistogramQuery("api-1", from, to, "host", Duration.ofMinutes(30), 15);

            var result = SearchDateHistogramQueryAdapter.adapt(query, esInfoWithLegacyInterval());

            assertThatJson(result).inPath("aggs.by_date.date_histogram.interval").isEqualTo("1800000ms");
            assertThatJson(result).inPath("aggs.by_date.date_histogram.fixed_interval").isAbsent();
        }
    }

    @Test
    void should_include_aggs_even_with_minimal_query() {
        var result = SearchDateHistogramQueryAdapter.adapt(
            new DateHistogramQuery(null, null, null, "uri", Duration.ofMillis(5000), 10),
            esInfoWithFixedInterval()
        );

        assertThatJson(result).inPath("aggs.by_date").isNotNull();
        assertThatJson(result).inPath("aggs.by_date.aggregations.by_field.terms.field").isEqualTo("uri");
        assertThatJson(result).inPath("aggs.by_date.aggregations.by_field.terms.size").isEqualTo(10);
    }
}
