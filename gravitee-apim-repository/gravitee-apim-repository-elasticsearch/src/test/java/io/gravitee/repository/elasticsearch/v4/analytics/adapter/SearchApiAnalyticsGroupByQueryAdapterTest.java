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

import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsField;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByOrder;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByQuery;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchApiAnalyticsGroupByQueryAdapterTest {

    @Test
    void should_build_group_by_query_with_filters_and_defaults() {
        var now = Instant.parse("2021-01-01T00:00:00Z");
        var from = now.truncatedTo(ChronoUnit.DAYS);
        var to = from.plus(Duration.ofDays(1));

        var query = new ApiAnalyticsGroupByQuery("api-id", from, to, ApiAnalyticsField.STATUS, null, null);

        var result = SearchApiAnalyticsGroupByQueryAdapter.adapt(query);

        assertThatJson(result)
            .isEqualTo(
                """
                {
                  "size": 0,
                  "query": {
                    "bool": {
                      "filter": [
                        {
                          "bool": {
                            "should": [
                              {"term":{"api-id":"api-id"}},
                              {"term":{"api":"api-id"}}
                            ]
                          }
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
                    "group_by": {
                      "terms": {
                        "field": "status",
                        "size": 10
                      }
                    }
                  }
                }
                """
            );
    }

    @Test
    void should_build_group_by_query_with_size_and_order() {
        var query = new ApiAnalyticsGroupByQuery(
            "api-id",
            Instant.parse("2021-01-01T00:00:00Z"),
            Instant.parse("2021-01-02T00:00:00Z"),
            ApiAnalyticsField.STATUS,
            5,
            ApiAnalyticsGroupByOrder.COUNT_ASC
        );

        var result = SearchApiAnalyticsGroupByQueryAdapter.adapt(query);

        assertThatJson(result).node("aggs.group_by.terms.size").isEqualTo(5);
        assertThatJson(result).node("aggs.group_by.terms.order._count").isEqualTo("asc");
    }
}
