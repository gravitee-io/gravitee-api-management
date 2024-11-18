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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.healthcheck.v4.model.ApiFieldPeriod;
import io.gravitee.repository.healthcheck.v4.model.AvailabilityResponse;
import io.reactivex.rxjava3.observers.TestObserver;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AvailabilityQueryMapperTest {

    private static final String API_ID = "my-api-id";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant TO = INSTANT_NOW;
    private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);
    private static final String FIELD = "gateway";

    AvailabilityQueryMapper availabilityQueryMapper;

    @BeforeEach
    void setUp() {
        availabilityQueryMapper = new AvailabilityQueryMapper();
    }

    @Nested
    class Query {

        @Test
        void adapt_query() {
            //Given
            var apiFieldPeriod = new ApiFieldPeriod(API_ID, FIELD, TO, FROM);

            //When
            var result = availabilityQueryMapper.adaptQuery(apiFieldPeriod);

            //Then
            assertThatJson(result)
                .isEqualTo(
                    """
                    {
                      "size": 0,
                      "query": {
                        "bool": {
                          "filter": [
                            { "term": { "api": "my-api-id" } },
                            {
                              "range": {
                                "@timestamp": {
                                  "from": 1697969730000,
                                  "to": 1697883330000,
                                  "include_lower": true,
                                  "include_upper": true
                                }
                              }
                            }
                          ]
                        }
                      },
                      "aggregations": {
                        "by_field": {
                          "terms": { "field": "gateway" },
                          "aggregations": {
                            "results": { "terms": { "field": "available", "include": [true] } }
                          }
                        }
                      }
                    }
                    """
                );
        }
    }

    @Nested
    class Response {

        private final ObjectMapper objectMapper = new ObjectMapper();
        private SearchResponse searchResponse;

        @BeforeEach
        void setUp() {
            searchResponse = new SearchResponse();
        }

        @Test
        void adapt__correct_response() throws JsonProcessingException {
            //Given
            var searchResponse = objectMapper.readValue(response(), SearchResponse.class);
            AvailabilityResponse expectedResponse = new AvailabilityResponse(
                0.4476F,
                Map.of("api-1", 0.0F, "api-2", 1.0F, "api-3", 0.0F, "api-4", 1.0F)
            );

            //When
            TestObserver<AvailabilityResponse> result = availabilityQueryMapper.adaptResponse(searchResponse).test();

            //Then
            result.assertNoErrors();
            result.assertValue(expectedResponse);
        }

        private static String response() {
            return """
                {
                  "took": 192,
                  "timed_out": false,
                  "_shards": {
                    "total": 1118,
                    "successful": 1118,
                    "skipped": 1070,
                    "failed": 0
                  },
                  "hits": {
                    "total": {
                      "value": 10000,
                      "relation": "gte"
                    },
                    "max_score": null,
                    "hits": []
                  },
                  "aggregations": {
                    "by_field": {
                      "doc_count_error_upper_bound": 0,
                      "sum_other_doc_count": 0,
                      "buckets": [
                        {
                          "key": "api-1",
                          "doc_count": 1565333,
                          "results": {
                            "doc_count_error_upper_bound": 0,
                            "sum_other_doc_count": 0,
                            "buckets": [
                              {
                                "key": 1,
                                "key_as_string": "true",
                                "doc_count": 66
                              }
                            ]
                          }
                        },
                        {
                          "key": "api-2",
                          "doc_count": 1299657,
                          "results": {
                            "doc_count_error_upper_bound": 0,
                            "sum_other_doc_count": 0,
                            "buckets": [
                              {
                                "key": 1,
                                "key_as_string": "true",
                                "doc_count": 1299657
                              }
                            ]
                          }
                        },
                        {
                          "key": "api-3",
                          "doc_count": 820874,
                          "results": {
                            "doc_count_error_upper_bound": 0,
                            "sum_other_doc_count": 0,
                            "buckets": [
                              {
                                "key": 1,
                                "key_as_string": "true",
                                "doc_count": 18
                              }
                            ]
                          }
                        },
                        {
                          "key": "api-4",
                          "doc_count": 633953,
                          "results": {
                            "doc_count_error_upper_bound": 0,
                            "sum_other_doc_count": 0,
                            "buckets": [
                              {
                                "key": 1,
                                "key_as_string": "true",
                                "doc_count": 633953
                              }
                            ]
                          }
                        }
                      ]
                    }
                  }
                }
                """;
        }

        @Test
        void should_return_empty_result_if_aggregations_is_null() {
            //When
            TestObserver<AvailabilityResponse> result = availabilityQueryMapper.adaptResponse(searchResponse).test();

            //Then
            result.assertNoErrors();
            result.assertNoValues();
        }

        @Test
        void should_return_empty_result_if_no_aggregation_exist() {
            searchResponse.setAggregations(Map.of());

            //When
            TestObserver<AvailabilityResponse> result = availabilityQueryMapper.adaptResponse(searchResponse).test();

            //Then
            result.assertNoErrors();
            result.assertNoValues();
        }

        @Test
        void should_return_empty_result_if_no_by_field_aggregation_exist() {
            searchResponse.setAggregations(Map.of("fake_aggregation", new Aggregation()));

            //When
            TestObserver<AvailabilityResponse> result = availabilityQueryMapper.adaptResponse(searchResponse).test();

            //Then
            result.assertNoErrors();
            result.assertNoValues();
        }
    }
}
