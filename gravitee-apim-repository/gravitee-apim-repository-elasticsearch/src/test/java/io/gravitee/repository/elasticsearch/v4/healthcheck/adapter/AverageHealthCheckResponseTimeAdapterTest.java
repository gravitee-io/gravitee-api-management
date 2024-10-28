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
package io.gravitee.repository.elasticsearch.v4.healthcheck.adapter;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeQuery;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AverageHealthCheckResponseTimeAdapterTest {

    private final AverageHealthCheckResponseTimeAdapter adapter = new AverageHealthCheckResponseTimeAdapter();

    @Nested
    class AdaptQuery {

        private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
        private static final Instant TO = INSTANT_NOW;
        private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);

        @Test
        void should_adapt_query() {
            // Given
            var query = new AverageHealthCheckResponseTimeQuery("api-id", "endpoint", FROM, TO);

            // When
            var result = adapter.adaptQuery(query);

            // Then
            assertThatJson(result)
                .isEqualTo(
                    """
                                {
                                  "size": 0,
                                  "query": {
                                    "bool": {
                                      "filter": [
                                        {
                                          "term": { "api": "api-id" }
                                        }
                                      ]
                                    }
                                  },
                                  "aggregations": {
                                    "terms": {
                                      "aggregations": {
                                        "ranges": {
                                          "aggregations": {
                                            "results": {
                                              "avg": { "field": "response-time" }
                                            }
                                          },
                                          "date_range": {
                                            "field": "@timestamp",
                                            "keyed": false,
                                            "ranges": [
                                              {
                                                "from": 1697883330000,
                                                "to": 1697969730000,
                                                "key": "period"
                                              }
                                            ]
                                          }
                                        }
                                      },
                                      "terms": {
                                        "field" : "endpoint",
                                        "order": [
                                          { "_count": "desc" },
                                          { "_key": "asc" }
                                        ],
                                        "size": 100
                                      }
                                   }
                                }
                              }"""
                );
        }
    }
}
