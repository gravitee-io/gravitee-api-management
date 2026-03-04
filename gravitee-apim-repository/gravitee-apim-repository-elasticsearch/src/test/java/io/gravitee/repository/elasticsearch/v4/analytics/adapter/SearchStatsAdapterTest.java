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
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.StatsQueryCriteria;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchStatsAdapterTest {

    @Test
    void should_build_query_without_filter() {
        var result = SearchStatsAdapter.adaptQuery(null);

        assertThatJson(result)
            .isEqualTo(
                """
                {
                  "size": 0,
                  "query": {
                    "match_all": {}
                  },
                  "aggs": {
                    "stats_count": {
                      "value_count": {
                        "field": "status"
                      }
                    },
                    "stats_min": {
                      "min": {
                        "field": "status"
                      }
                    },
                    "stats_max": {
                      "max": {
                        "field": "status"
                      }
                    },
                    "stats_avg": {
                      "avg": {
                        "field": "status"
                      }
                    },
                    "stats_sum": {
                      "sum": {
                        "field": "status"
                      }
                    }
                  }
                }
                """
            );
    }

    @Test
    void should_build_query_with_api_time_and_field_filters() {
        var result = SearchStatsAdapter.adaptQuery(
            new StatsQueryCriteria(
                "api-id",
                Instant.parse("2023-10-21T10:15:30Z"),
                Instant.parse("2023-10-22T10:15:30Z"),
                "gateway-response-time-ms"
            )
        );

        assertThatJson(result)
            .isEqualTo(
                """
                {
                  "size": 0,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "api-id": "api-id" } },
                        {
                          "range": {
                            "@timestamp": {
                              "from": 1697883330000,
                              "include_lower": true,
                              "to": 1697969730000,
                              "include_upper": true
                            }
                          }
                        }
                      ]
                    }
                  },
                  "aggs": {
                    "stats_count": {
                      "value_count": {
                        "field": "gateway-response-time-ms"
                      }
                    },
                    "stats_min": {
                      "min": {
                        "field": "gateway-response-time-ms"
                      }
                    },
                    "stats_max": {
                      "max": {
                        "field": "gateway-response-time-ms"
                      }
                    },
                    "stats_avg": {
                      "avg": {
                        "field": "gateway-response-time-ms"
                      }
                    },
                    "stats_sum": {
                      "sum": {
                        "field": "gateway-response-time-ms"
                      }
                    }
                  }
                }
                """
            );
    }

    @Test
    void should_return_empty_result_if_no_aggregation() {
        assertThat(SearchStatsAdapter.adaptResponse(new SearchResponse())).isEmpty();
    }

    @Test
    void should_adapt_stats_response() {
        var searchResponse = new SearchResponse();
        searchResponse.setAggregations(
            Map.of(
                "stats_count",
                aggregation(12345D),
                "stats_min",
                aggregation(2D),
                "stats_max",
                aggregation(1500D),
                "stats_avg",
                aggregation(125.5D),
                "stats_sum",
                aggregation(1549567D)
            )
        );

        assertThat(SearchStatsAdapter.adaptResponse(searchResponse))
            .hasValueSatisfying(stats -> {
                assertThat(stats.getCount()).isEqualTo(12345L);
                assertThat(stats.getMin()).isEqualTo(2D);
                assertThat(stats.getMax()).isEqualTo(1500D);
                assertThat(stats.getAvg()).isEqualTo(125.5D);
                assertThat(stats.getSum()).isEqualTo(1549567D);
            });
    }

    private static Aggregation aggregation(double value) {
        var aggregation = new Aggregation();
        aggregation.setValue((float) value);
        return aggregation;
    }
}
