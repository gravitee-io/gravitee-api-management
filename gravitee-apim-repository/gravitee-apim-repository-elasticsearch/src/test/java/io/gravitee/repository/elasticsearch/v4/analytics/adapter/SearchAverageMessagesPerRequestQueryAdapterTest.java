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

import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchAverageMessagesPerRequestQueryAdapterTest {

    public static final String QUERY_WITHOUT_FILTER = """
           {
                 "size": 0,
                 "aggs": {
                   "entrypoints_agg": {
                     "aggs": {
                       "avg_messages_per_request": {
                         "bucket_script": {
                           "buckets_path": {
                             "msg_count": "messages_count",
                             "req_count": "distinct_requests_count"
                           },
                           "script": "params.msg_count / params.req_count"
                         }
                       },
                       "distinct_requests_count": {
                         "cardinality": {
                           "field": "request-id"
                         }
                       },
                       "messages_count": {
                         "sum": {
                           "field": "count-increment"
                         }
                       }
                     },
                     "terms": {
                       "field": "connector-id"
                     }
                   }
                 },
                 "query": {
                   "bool": {
                     "must": [
                       {
                         "term": {
                           "connector-type": "entrypoint"
                         }
                       }
                     ]
                   }
                 }
               }
        """;

    @Test
    void should_build_query_without_filter() {
        var result = SearchAverageMessagesPerRequestQueryAdapter.adapt(null);

        assertThatJson(result).isEqualTo(QUERY_WITHOUT_FILTER);
    }

    @Test
    void should_build_query_with_empty_filter() {
        var result = SearchAverageMessagesPerRequestQueryAdapter.adapt(AverageMessagesPerRequestQuery.builder().build());

        assertThatJson(result).isEqualTo(QUERY_WITHOUT_FILTER);
    }

    @Test
    void should_build_query_with_api_filter() {
        var result = SearchAverageMessagesPerRequestQueryAdapter.adapt(AverageMessagesPerRequestQuery.builder().apiId("api-id").build());

        assertThatJson(result).isEqualTo(
            """
            {
              "size": 0,
              "query": {
                "bool": {
                  "must": [
                    {
                      "term": {
                        "connector-type": "entrypoint"
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
                    "field": "connector-id"
                  },
                  "aggs": {
                    "messages_count": {
                      "sum": {
                        "field": "count-increment"
                      }
                    },
                    "distinct_requests_count": {
                      "cardinality": {
                        "field": "request-id"
                      }
                    },
                    "avg_messages_per_request": {
                      "bucket_script": {
                        "buckets_path": {
                          "req_count": "distinct_requests_count",
                          "msg_count": "messages_count"
                        },
                        "script": "params.msg_count / params.req_count"
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
