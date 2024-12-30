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

import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchAverageConnectionDurationQueryAdapterTest {

    public static final String QUERY_WITHOUT_FILTER =
        """
                 {
                       "size": 0,
                       "query": {
                         "bool": {
                           "must": [
                             {
                               "term": {
                                 "request-ended": "true"
                               }
                             }
                           ]
                         }
                       },
                       "aggs": {
                         "entrypoints_agg": {
                           "terms": {
                             "field": "entrypoint-id"
                           },
                           "aggs": {
                             "avg_ended_request_duration_ms": {
                               "avg": {
                                 "field": "gateway-response-time-ms"
                               }
                             }
                           }
                         }
                       }
                     }
              """;

    @Test
    void should_build_query_without_filter() {
        var result = SearchAverageConnectionDurationQueryAdapter.adapt(null, true);

        assertThatJson(result).isEqualTo(QUERY_WITHOUT_FILTER);
    }

    @Test
    void should_build_query_with_empty_filter() {
        var result = SearchAverageConnectionDurationQueryAdapter.adapt(new AverageConnectionDurationQuery(), true);

        assertThatJson(result).isEqualTo(QUERY_WITHOUT_FILTER);
    }

    @Test
    void should_build_query_with_api_filter() {
        var result = SearchAverageConnectionDurationQueryAdapter.adapt(new AverageConnectionDurationQuery("api-id"), true);

        assertThatJson(result)
            .isEqualTo(
                """
                             {
                               "size": 0,
                               "query": {
                                 "bool": {
                                   "must": [
                                     {
                                       "term": {
                                         "request-ended": "true"
                                       }
                                     },
                                     {
                                       "term": {
                                         "api-id": "api-id"
                                       }
                                     }
                                   ]
                                 }
                               },
                               "aggs": {
                                 "entrypoints_agg": {
                                   "terms": {
                                     "field": "entrypoint-id"
                                   },
                                   "aggs": {
                                     "avg_ended_request_duration_ms": {
                                       "avg": {
                                         "field": "gateway-response-time-ms"
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
    void should_adapt_the_query_when_entrypoint_id_is_not_a_keyword() {
        var result = SearchAverageConnectionDurationQueryAdapter.adapt(new AverageConnectionDurationQuery(), false);

        assertThatJson(result)
            .isEqualTo(
                """
                             {
                               "size": 0,
                               "query": {
                                 "bool": {
                                   "must": [
                                     {
                                       "term": {
                                         "request-ended": "true"
                                       }
                                     }
                                   ]
                                 }
                               },
                               "aggs": {
                                 "entrypoints_agg": {
                                   "terms": {
                                     "field": "entrypoint-id.keyword"
                                   },
                                   "aggs": {
                                     "avg_ended_request_duration_ms": {
                                       "avg": {
                                         "field": "gateway-response-time-ms"
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
    void should_build_query_with_time_period_filter() {
        var now = Instant.parse("2021-01-01T00:00:00Z");
        var from = now.truncatedTo(ChronoUnit.DAYS);
        var to = from.plus(Duration.ofDays(1));

        var result = SearchAverageConnectionDurationQueryAdapter.adapt(
            new AverageConnectionDurationQuery(Optional.of("api-id"), Optional.of(from), Optional.of(to)),
            true
        );

        assertThatJson(result)
            .isEqualTo(
                """
                                     {
                                       "size": 0,
                                       "query": {
                                         "bool": {
                                           "must": [
                                             {
                                               "term": { "request-ended": "true" }
                                             },
                                             {
                                               "term": { "api-id": "api-id" }
                                             },
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
                                         "entrypoints_agg": {
                                           "terms": {
                                             "field": "entrypoint-id"
                                           },
                                           "aggs": {
                                             "avg_ended_request_duration_ms": {
                                               "avg": {
                                                 "field": "gateway-response-time-ms"
                                               }
                                             }
                                           }
                                         }
                                       }
                                     }
                                     """
            );
    }
}
