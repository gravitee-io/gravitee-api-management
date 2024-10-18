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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.elasticsearch.version.Version;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.Test;

public class ResponseTimeRangeQueryAdapterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SneakyThrows
    @Test
    public void buildQuery() {
        // Given
        ResponseTimeRangeQueryAdapter adapter = new ResponseTimeRangeQueryAdapter();
        var end = Instant.now();
        var start = end.minus(Duration.ofDays(1));
        Duration interval = Duration.ofMinutes(30);
        ResponseTimeRangeQuery query = new ResponseTimeRangeQuery("MyAPI", start, end, interval);
        ElasticsearchInfo info = new ElasticsearchInfo();
        info.setVersion(mock(Version.class));
        when(info.getVersion().canUseDateHistogramFixedInterval()).thenReturn(true);

        // When
        String result = adapter.queryAdapt(info, query);

        // Then
        JsonNode jsonQuery = MAPPER.readTree(result);
        // query part
        assertThat(jsonQuery.at("/query/bool/filter/0/term/api-id").asText()).isEqualTo("MyAPI");
        assertThat(Instant.ofEpochMilli(jsonQuery.at("/query/bool/filter/1/range/@timestamp/from").asLong()))
            .isBeforeOrEqualTo(start)
            .isCloseTo(start, within(interval.toMillis(), ChronoUnit.MILLIS));
        assertThat(Instant.ofEpochMilli(jsonQuery.at("/query/bool/filter/1/range/@timestamp/to").asLong()))
            .isAfterOrEqualTo(end)
            .isCloseTo(end, within(interval.toMillis(), ChronoUnit.MILLIS));
        // aggs
        assertThat(jsonQuery.at("/aggregations/by_date/date_histogram/field").asText()).isEqualTo("@timestamp");
        assertThat(jsonQuery.at("/aggregations/by_date/date_histogram/fixed_interval").asText()).isEqualTo("1800000ms");
        // timestamp is in ms so we need to truncate
        assertThat(Instant.ofEpochMilli(jsonQuery.at("/aggregations/by_date/date_histogram/extended_bounds/min").asLong()))
            .isEqualTo(start.truncatedTo(ChronoUnit.MILLIS));
        assertThat(Instant.ofEpochMilli(jsonQuery.at("/aggregations/by_date/date_histogram/extended_bounds/max").asLong()))
            .isEqualTo(end.truncatedTo(ChronoUnit.MILLIS));
    }

    @SneakyThrows
    @Test
    public void buildQueryOldEs() {
        // Given
        ResponseTimeRangeQueryAdapter adapter = new ResponseTimeRangeQueryAdapter();
        var end = Instant.now();
        var start = end.minus(Duration.ofDays(1));
        Duration interval = Duration.ofMinutes(30);
        ResponseTimeRangeQuery query = new ResponseTimeRangeQuery("MyAPI", start, end, interval);
        ElasticsearchInfo info = new ElasticsearchInfo();
        info.setVersion(mock(Version.class));
        when(info.getVersion().canUseDateHistogramFixedInterval()).thenReturn(false);

        // When
        String result = adapter.queryAdapt(info, query);

        // Then
        JsonNode jsonQuery = MAPPER.readTree(result);
        // query part
        assertThat(jsonQuery.at("/query/bool/filter/0/term/api-id").asText()).isEqualTo("MyAPI");
        assertThat(Instant.ofEpochMilli(jsonQuery.at("/query/bool/filter/1/range/@timestamp/from").asLong()))
            .isBeforeOrEqualTo(start)
            .isCloseTo(start, within(interval.toMillis(), ChronoUnit.MILLIS));
        assertThat(Instant.ofEpochMilli(jsonQuery.at("/query/bool/filter/1/range/@timestamp/to").asLong()))
            .isAfterOrEqualTo(end)
            .isCloseTo(end, within(interval.toMillis(), ChronoUnit.MILLIS));
        // aggs
        assertThat(jsonQuery.at("/aggregations/by_date/date_histogram/field").asText()).isEqualTo("@timestamp");
        assertThat(jsonQuery.at("/aggregations/by_date/date_histogram/interval").asText()).isEqualTo("1800000ms");
        // timestamp is in ms so we need to truncate
        assertThat(Instant.ofEpochMilli(jsonQuery.at("/aggregations/by_date/date_histogram/extended_bounds/min").asLong()))
            .isEqualTo(start.truncatedTo(ChronoUnit.MILLIS));
        assertThat(Instant.ofEpochMilli(jsonQuery.at("/aggregations/by_date/date_histogram/extended_bounds/max").asLong()))
            .isEqualTo(end.truncatedTo(ChronoUnit.MILLIS));
    }

    @SneakyThrows
    @Test
    public void readResponse() {
        // Given
        ResponseTimeRangeQueryAdapter adapter = new ResponseTimeRangeQueryAdapter();
        SearchResponse response = new SearchResponse();
        JsonNode jsonNode = MAPPER.readTree(RESPONSE);
        Aggregation aggregation = new Aggregation();
        var buckets = new ArrayList<JsonNode>();
        jsonNode.at("/aggregations/by_date/buckets").forEach(buckets::add);
        aggregation.setBuckets(buckets);
        response.setAggregations(Map.of("by_date", aggregation));

        // When
        var result = adapter.responseAdapt(response).blockingGet();

        // Then
        assertThat(result.getAverageBy()).hasSize(25);
        assertThat(result.getAverageBy().entrySet().stream()).map(Map.Entry::getKey).isSorted();
        assertThat(result.getAverageBy().get("2024-10-17T09:00:00.000Z")).isEqualTo(262.961164, within(0.001));
    }

    private static final String RESPONSE =
        """
            {
              "took": 27,
              "timed_out": false,
              "_shards": {
                "total": 2,
                "successful": 2,
                "skipped": 0,
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
                "by_date": {
                  "buckets": [
                    {
                      "key_as_string": "2024-10-17T08:30:00.000Z",
                      "key": 1729153800000,
                      "doc_count": 1800,
                      "avg_gateway-response-time-ms": {
                        "value": 262.46222222222224
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T09:00:00.000Z",
                      "key": 1729155600000,
                      "doc_count": 20651,
                      "avg_gateway-response-time-ms": {
                        "value": 262.9611641082756
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T09:30:00.000Z",
                      "key": 1729157400000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T10:00:00.000Z",
                      "key": 1729159200000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T10:30:00.000Z",
                      "key": 1729161000000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T11:00:00.000Z",
                      "key": 1729162800000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T11:30:00.000Z",
                      "key": 1729164600000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T12:00:00.000Z",
                      "key": 1729166400000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T12:30:00.000Z",
                      "key": 1729168200000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T13:00:00.000Z",
                      "key": 1729170000000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T13:30:00.000Z",
                      "key": 1729171800000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T14:00:00.000Z",
                      "key": 1729173600000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T14:30:00.000Z",
                      "key": 1729175400000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T15:00:00.000Z",
                      "key": 1729177200000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T15:30:00.000Z",
                      "key": 1729179000000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T16:00:00.000Z",
                      "key": 1729180800000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T16:30:00.000Z",
                      "key": 1729182600000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T17:00:00.000Z",
                      "key": 1729184400000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T17:30:00.000Z",
                      "key": 1729186200000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T18:00:00.000Z",
                      "key": 1729188000000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T18:30:00.000Z",
                      "key": 1729189800000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T19:00:00.000Z",
                      "key": 1729191600000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T19:30:00.000Z",
                      "key": 1729193400000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T20:00:00.000Z",
                      "key": 1729195200000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    },
                    {
                      "key_as_string": "2024-10-17T20:30:00.000Z",
                      "key": 1729197000000,
                      "doc_count": 0,
                      "avg_gateway-response-time-ms": {
                        "value": null
                      }
                    }
                  ]
                }
              }
            }
            """;
}
