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

import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesQuery;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchResponseStatusRangesQueryAdapterTest {

    @Test
    void should_build_query() {
        var result = SearchResponseStatusRangesQueryAdapter.adapt(ResponseStatusRangesQuery.builder().apiId("api-id").build(), true);

        assertThatJson(result).isEqualTo(
            """
                    {
                      "size": 0,
                      "query": {
                        "term": {
                          "api-id": "api-id"
                        }
                      },
                      "aggs": {
                        "entrypoint_id_agg": {
                          "terms": {
                            "field": "entrypoint-id"
                          },
                          "aggs": {
                            "status_ranges": {
                              "range": {
                                "field": "status",
                                "ranges": [
                                  {
                                    "from": 100.0,
                                    "to": 200.0
                                  },
                                  {
                                    "from": 200.0,
                                    "to": 300.0
                                  },
                                  {
                                    "from": 300.0,
                                    "to": 400.0
                                  },
                                  {
                                    "from": 400.0,
                                    "to": 500.0
                                  },
                                  {
                                    "from": 500.0,
                                    "to": 600.0
                                  }
                                ]
                              }
                            }
                          }
                        },
                         "all_apis_status_ranges": {
                            "range": {
                                "field": "status",
                                "ranges": [
                                {
                                    "from": 100.0,
                                    "to": 200.0
                                },
                                {
                                    "from": 200.0,
                                    "to": 300.0
                                },
                                {
                                    "from": 300.0,
                                    "to": 400.0
                                },
                                {
                                    "from": 400.0,
                                    "to": 500.0
                                },
                                {
                                    "from": 500.0,
                                    "to": 600.0
                                }
                            ]
                        }
                    }
                   }
                 }
            """
        );
    }
}
