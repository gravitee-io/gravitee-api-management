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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramAggregate;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SearchHistogramQueryAdapterTest {

    private static final String API_ID = "f1608475-dd77-4603-a084-75dd775603e9";
    private static final Instant INSTANT_NOW = Instant.parse("2025-07-03T10:15:30Z");
    private static final Instant TO = INSTANT_NOW;
    private static final Instant FROM = INSTANT_NOW.minus(Duration.ofDays(2));
    private static final Duration INTERVAL = Duration.ofMinutes(30);

    private final SearchHistogramQueryAdapter cut = new SearchHistogramQueryAdapter();

    @Nested
    class AdaptQuery {

        @Test
        void should_generate_expected_histogram_query_json() {
            HistogramQuery query = new HistogramQuery(
                API_ID,
                new TimeRange(FROM, TO, INTERVAL),
                List.of(new io.gravitee.repository.log.v4.model.analytics.Aggregation("status", AggregationType.FIELD)),
                Optional.empty()
            );

            String result = cut.adapt(query);

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
                                "minimum_should_match": 1,
                                "should": [
                                  {
                                    "bool": {
                                      "must": [
                                        {
                                          "terms": {
                                            "api-id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                          }
                                        },
                                        {
                                          "terms": {
                                            "entrypoint-id": [ "http-post", "http-get", "http-proxy" ]
                                          }
                                        }
                                      ]
                                    }
                                  }
                                ]
                              }
                            },
                            {
                              "range": {
                                "@timestamp": {
                                  "from": %d,
                                  "to": %d,
                                  "include_lower": true,
                                  "include_upper": true
                                }
                              }
                            }
                          ]
                        }
                      },
                      "aggregations": {
                        "by_date": {
                          "date_histogram": {
                            "field": "@timestamp",
                            "fixed_interval": "1800000ms",
                            "min_doc_count": 0,
                            "extended_bounds": {
                              "min": %d,
                              "max": %d
                            }
                          },
                          "aggregations": {
                            "by_status": {
                              "terms": {
                                "field": "status"
                              }
                            }
                          }
                        }
                      }
                    }
                    """.formatted(
                            FROM.toEpochMilli(),
                            TO.toEpochMilli(),
                            FROM.toEpochMilli(),
                            TO.toEpochMilli()
                        )
                );
        }

        @Test
        void should_generate_expected_avg_aggregation_query_json() {
            HistogramQuery query = new HistogramQuery(
                API_ID,
                new TimeRange(FROM, TO, INTERVAL),
                List.of(new io.gravitee.repository.log.v4.model.analytics.Aggregation("gateway-response-time-ms", AggregationType.AVG)),
                Optional.empty()
            );

            String result = cut.adapt(query);

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
                                "minimum_should_match": 1,
                                "should": [
                                  {
                                    "bool": {
                                      "must": [
                                        {
                                          "terms": {
                                            "api-id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                          }
                                        },
                                        {
                                          "terms": {
                                            "entrypoint-id": [ "http-post", "http-get", "http-proxy" ]
                                          }
                                        }
                                      ]
                                    }
                                  }
                                ]
                              }
                            },
                            {
                              "range": {
                                "@timestamp": {
                                  "from": %d,
                                  "to": %d,
                                  "include_lower": true,
                                  "include_upper": true
                                }
                              }
                            }
                          ]
                        }
                      },
                      "aggregations": {
                        "by_date": {
                          "date_histogram": {
                            "field": "@timestamp",
                            "fixed_interval": "1800000ms",
                            "min_doc_count": 0,
                            "extended_bounds": {
                              "min": %d,
                              "max": %d
                            }
                          },
                          "aggregations": {
                            "avg_gateway-response-time-ms": {
                              "avg": {
                                "field": "gateway-response-time-ms"
                              }
                            }
                          }
                        }
                      }
                    }
                    """.formatted(
                            FROM.toEpochMilli(),
                            TO.toEpochMilli(),
                            FROM.toEpochMilli(),
                            TO.toEpochMilli()
                        )
                );
        }

        @Test
        void should_generate_expected_histogram_query_json_with_query_parameter() {
            HistogramQuery query = new HistogramQuery(
                API_ID,
                new TimeRange(FROM, TO, INTERVAL),
                List.of(new io.gravitee.repository.log.v4.model.analytics.Aggregation("status", AggregationType.FIELD)),
                Optional.of("status:200 AND method:GET")
            );

            String result = cut.adapt(query);

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
                                "minimum_should_match": 1,
                                "should": [
                                  {
                                    "bool": {
                                      "must": [
                                        {
                                          "terms": {
                                            "api-id": [ "f1608475-dd77-4603-a084-75dd775603e9" ]
                                          }
                                        },
                                        {
                                          "terms": {
                                            "entrypoint-id": [ "http-post", "http-get", "http-proxy" ]
                                          }
                                        }
                                      ]
                                    }
                                  }
                                ]
                              }
                            },
                            {
                              "range": {
                                "@timestamp": {
                                  "from": %d,
                                  "to": %d,
                                  "include_lower": true,
                                  "include_upper": true
                                }
                              }
                            },
                            {
                              "query_string": {
                                "query": "status:200 AND method:GET"
                              }
                            }
                          ]
                        }
                      },
                      "aggregations": {
                        "by_date": {
                          "date_histogram": {
                            "field": "@timestamp",
                            "fixed_interval": "1800000ms",
                            "min_doc_count": 0,
                            "extended_bounds": {
                              "min": %d,
                              "max": %d
                            }
                          },
                          "aggregations": {
                            "by_status": {
                              "terms": {
                                "field": "status"
                              }
                            }
                          }
                        }
                      }
                    }
                    """.formatted(
                            FROM.toEpochMilli(),
                            TO.toEpochMilli(),
                            FROM.toEpochMilli(),
                            TO.toEpochMilli()
                        )
                );
        }
    }

    @Nested
    class AdaptResponse {

        @Test
        void should_return_empty_when_no_aggregation() {
            SearchResponse response = new SearchResponse();
            List<HistogramAggregate<?>> result = cut.adaptResponse(response);
            assertThat(result).isEmpty();
        }

        @Test
        void should_parse_histogram_aggregation_response() throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            String json =
                """
                {
                  "aggregations": {
                    "by_date": {
                      "buckets": [
                        {
                          "key": 1751414400000,
                          "by_status": {
                            "buckets": [
                              { "key": "200", "doc_count": 1 }
                            ]
                          }
                        },
                        {
                          "key": 1751587200000,
                          "by_status": {
                            "buckets": [
                              { "key": "404", "doc_count": 2 },
                              { "key": "202", "doc_count": 1 }
                            ]
                          }
                        }
                      ]
                    }
                  }
                }
                """;
            SearchResponse response = new SearchResponse();
            Aggregation byDateAgg = new Aggregation();
            ArrayNode bucketsNode = (ArrayNode) mapper.readTree(json).get("aggregations").get("by_date").get("buckets");
            List<JsonNode> buckets = new java.util.ArrayList<>();
            bucketsNode.forEach(buckets::add);
            byDateAgg.setBuckets(buckets);
            response.setAggregations(Map.of("by_date", byDateAgg));

            cut.adapt(
                new HistogramQuery(
                    API_ID,
                    new TimeRange(FROM, TO, INTERVAL),
                    List.of(new io.gravitee.repository.log.v4.model.analytics.Aggregation("status", AggregationType.FIELD)),
                    Optional.empty()
                )
            );

            List<HistogramAggregate<?>> result = cut.adaptResponse(response);

            assertThat(result).hasSize(1);
            HistogramAggregate<Long> agg = (HistogramAggregate<Long>) result.getFirst();
            assertThat(agg.buckets()).containsOnlyKeys("200", "202", "404");

            assertThat(agg.buckets().get("200")).containsExactly(1L, 0L);
            assertThat(agg.buckets().get("202")).containsExactly(0L, 1L);
            assertThat(agg.buckets().get("404")).containsExactly(0L, 2L);
        }

        @Test
        void should_parse_avg_aggregation_response() throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            String json =
                """
                {
                  "aggregations": {
                    "by_date": {
                      "buckets": [
                        {
                          "key": 1751414400000,
                          "avg_gateway-response-time-ms": {
                            "value": 120.5
                          }
                        },
                        {
                          "key": 1751587200000,
                          "avg_gateway-response-time-ms": {
                            "value": 110.0
                          }
                        }
                      ]
                    }
                  }
                }
                """;
            SearchResponse response = new SearchResponse();
            Aggregation byDateAgg = new Aggregation();
            ArrayNode bucketsNode = (ArrayNode) mapper.readTree(json).get("aggregations").get("by_date").get("buckets");
            List<JsonNode> buckets = new java.util.ArrayList<>();
            bucketsNode.forEach(buckets::add);
            byDateAgg.setBuckets(buckets);
            response.setAggregations(Map.of("by_date", byDateAgg));

            cut.adapt(
                new HistogramQuery(
                    API_ID,
                    new TimeRange(FROM, TO, INTERVAL),
                    List.of(new io.gravitee.repository.log.v4.model.analytics.Aggregation("gateway-response-time-ms", AggregationType.AVG)),
                    Optional.empty()
                )
            );

            List<HistogramAggregate<?>> result = cut.adaptResponse(response);

            assertThat(result).hasSize(1);
            HistogramAggregate<Double> agg = (HistogramAggregate<Double>) result.getFirst();
            assertThat(agg.buckets()).containsOnlyKeys("avg_gateway-response-time-ms");
            assertThat(agg.buckets().get("avg_gateway-response-time-ms")).containsExactly(120.5, 110.0);
        }
    }
}
