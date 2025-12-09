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
package io.gravitee.repository.elasticsearch.v4.log.adapter.message;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

import io.gravitee.repository.log.v4.model.message.MessageMetricsQuery;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SearchMessageMetricsQueryAdapterTest {

    @ParameterizedTest
    @MethodSource("noFilter")
    void should_build_query_without_filter(MessageMetricsQuery.Filter filter) {
        var result = SearchMessageMetricsQueryAdapter.adapt(MessageMetricsQuery.builder().page(1).size(20).filter(filter).build());

        assertThatJson(result).isEqualTo(
            """
            {
              "from": 0,
              "size": 20,
              "sort": {
                "@timestamp": { "order": "desc" }
              }
            }
            """
        );
    }

    @ParameterizedTest
    @MethodSource("getFilters")
    void should_build_query_with_filters(MessageMetricsQuery.Filter filter, String expected) {
        var result = SearchMessageMetricsQueryAdapter.adapt(MessageMetricsQuery.builder().page(1).size(20).filter(filter).build());

        assertThatJson(result).when(IGNORING_ARRAY_ORDER).isEqualTo(expected);
    }

    private static Stream<Arguments> noFilter() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(MessageMetricsQuery.Filter.builder().build()));
    }

    private static Stream<Arguments> getFilters() {
        return Stream.of(
            Arguments.of(
                MessageMetricsQuery.Filter.builder().apiId("f1608475-dd77-4603-a084-75dd775603e9").build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "api-id":"f1608475-dd77-4603-a084-75dd775603e9" } }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageMetricsQuery.Filter.builder()
                    .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                    .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "api-id":"f1608475-dd77-4603-a084-75dd775603e9" } },
                        { "term": { "request-id":"8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48" } }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageMetricsQuery.Filter.builder()
                    .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                    .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                    .connectorType("entrypoint")
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "api-id":"f1608475-dd77-4603-a084-75dd775603e9" } },
                        { "term": { "request-id":"8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48" } },
                        { "term": { "connector-type":"entrypoint" } }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageMetricsQuery.Filter.builder()
                    .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                    .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                    .connectorType("entrypoint")
                    .operation("subscribe")
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "api-id":"f1608475-dd77-4603-a084-75dd775603e9" } },
                        { "term": { "request-id":"8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48" } },
                        { "term": { "connector-type":"entrypoint" } },
                        { "term": { "operation":"subscribe" } }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageMetricsQuery.Filter.builder()
                    .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                    .requestId("8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48")
                    .connectorType("entrypoint")
                    .operation("subscribe")
                    .connectorId("webhook")
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "api-id":"f1608475-dd77-4603-a084-75dd775603e9" } },
                        { "term": { "request-id":"8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48" } },
                        { "term": { "connector-type":"entrypoint" } },
                        { "term": { "operation":"subscribe" } },
                        { "term": { "connector-id":"webhook" } }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageMetricsQuery.Filter.builder()
                    .connectorId("webhook")
                    .connectorType("entrypoint")
                    .operation("subscribe")
                    .additional(java.util.Map.of("string_webhook_url", java.util.List.of("https://webhook.site/d0aa84f5-975d-4d5e-b0")))
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "connector-type":"entrypoint" } },
                        { "term": { "operation":"subscribe" } },
                        { "term": { "connector-id":"webhook" } },
                        {
                          "match_phrase": {
                            "additional-metrics.string_webhook_url": "https://webhook.site/d0aa84f5-975d-4d5e-b0"
                          }
                        }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageMetricsQuery.Filter.builder()
                    .connectorId("webhook")
                    .connectorType("entrypoint")
                    .operation("subscribe")
                    .additional(
                        java.util.Map.of(
                            "string_webhook_url",
                            java.util.List.of("https://webhook.site/test"),
                            "int_webhook_resp-status",
                            java.util.List.of("200")
                        )
                    )
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "connector-type":"entrypoint" } },
                        { "term": { "operation":"subscribe" } },
                        { "term": { "connector-id":"webhook" } },
                        {
                          "match_phrase": {
                            "additional-metrics.string_webhook_url": "https://webhook.site/test"
                          }
                        },
                        {
                          "term": {
                            "additional-metrics.int_webhook_resp-status": "200"
                          }
                        }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageMetricsQuery.Filter.builder()
                    .connectorId("webhook")
                    .connectorType("entrypoint")
                    .operation("subscribe")
                    .additional(
                        java.util.Map.of("string_webhook_url", java.util.List.of("https://webhook.site/url1", "https://webhook.site/url2"))
                    )
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "connector-type":"entrypoint" } },
                        { "term": { "operation":"subscribe" } },
                        { "term": { "connector-id":"webhook" } },
                        {
                          "bool": {
                            "should": [
                              {
                                "match_phrase": {
                                  "additional-metrics.string_webhook_url": "https://webhook.site/url1"
                                }
                              },
                              {
                                "match_phrase": {
                                  "additional-metrics.string_webhook_url": "https://webhook.site/url2"
                                }
                              }
                            ],
                            "minimum_should_match": 1
                          }
                        }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageMetricsQuery.Filter.builder()
                    .connectorId("webhook")
                    .connectorType("entrypoint")
                    .operation("subscribe")
                    .additional(java.util.Map.of("keyword_webhook_app-id", java.util.List.of("app-123")))
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "connector-type":"entrypoint" } },
                        { "term": { "operation":"subscribe" } },
                        { "term": { "connector-id":"webhook" } },
                        {
                          "term": {
                            "additional-metrics.keyword_webhook_app-id": "app-123"
                          }
                        }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageMetricsQuery.Filter.builder()
                    .connectorId("webhook")
                    .connectorType("entrypoint")
                    .operation("subscribe")
                    .requiresAdditional(true)
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "connector-type":"entrypoint" } },
                        { "term": { "operation":"subscribe" } },
                        { "term": { "connector-id":"webhook" } },
                        {
                          "exists": {
                            "field": "additional-metrics"
                          }
                        }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageMetricsQuery.Filter.builder()
                    .connectorId("webhook")
                    .connectorType("entrypoint")
                    .operation("subscribe")
                    .requiresAdditional(false)
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "connector-type":"entrypoint" } },
                        { "term": { "operation":"subscribe" } },
                        { "term": { "connector-id":"webhook" } }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            ),
            Arguments.of(
                MessageMetricsQuery.Filter.builder()
                    .connectorId("webhook")
                    .connectorType("entrypoint")
                    .operation("subscribe")
                    .requiresAdditional(true)
                    .additional(java.util.Map.of("int_webhook_resp-status", java.util.List.of("200")))
                    .build(),
                """
                {
                  "from": 0,
                  "size": 20,
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "connector-type":"entrypoint" } },
                        { "term": { "operation":"subscribe" } },
                        { "term": { "connector-id":"webhook" } },
                        {
                          "exists": {
                            "field": "additional-metrics"
                          }
                        },
                        {
                          "term": {
                            "additional-metrics.int_webhook_resp-status": "200"
                          }
                        }
                      ]
                    }
                  },
                  "sort": {
                    "@timestamp": { "order": "desc" }
                  }
                }
                """
            )
        );
    }
}
