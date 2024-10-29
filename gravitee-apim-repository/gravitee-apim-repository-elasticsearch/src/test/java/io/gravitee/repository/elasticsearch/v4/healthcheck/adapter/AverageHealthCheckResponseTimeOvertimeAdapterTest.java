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

import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.elasticsearch.version.Version;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeOvertimeQuery;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AverageHealthCheckResponseTimeOvertimeAdapterTest {

    private final AverageHealthCheckResponseTimeOvertimeAdapter adapter = new AverageHealthCheckResponseTimeOvertimeAdapter();

    @Nested
    class AdaptQuery {

        private static final String API_ID = "my-api-id";
        private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
        private static final Instant TO = INSTANT_NOW;
        private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);
        private static final Duration INTERVAL = Duration.ofMinutes(10);

        @Test
        void adapt_query_for_ElasticSearch() {
            // Given
            var query = new AverageHealthCheckResponseTimeOvertimeQuery(API_ID, FROM, TO, INTERVAL);
            var esInfo = anElasticsearchInfo();

            // When
            var result = adapter.adaptQuery(query, esInfo);

            // Then
            assertThatJson(result.toString())
                .isEqualTo(
                    """
                                {
                                   "aggregations": {
                                     "by_date": {
                                       "aggregations": { "avg_response-time": { "avg": { "field": "response-time" } } },
                                       "date_histogram": {
                                         "extended_bounds": { "max": 1697969730000, "min": 1697883330000 },
                                         "field": "@timestamp",
                                         "fixed_interval": "600000ms",
                                         "min_doc_count": 0
                                       }
                                     }
                                   },
                                   "query": {
                                     "bool": {
                                       "filter": [
                                         { "term": { "api": "my-api-id" } },
                                         {
                                           "range": {
                                             "@timestamp": {
                                               "from": 1697882730000,
                                               "to": 1697970330000,
                                               "include_lower": true,
                                               "include_upper": true
                                             }
                                           }
                                         }
                                       ]
                                     }
                                   },
                                   "size": 0
                                 }"""
                );
        }

        @Test
        void adapt_query_for_OpenSearch() {
            // Given
            var query = new AverageHealthCheckResponseTimeOvertimeQuery(API_ID, FROM, TO, INTERVAL);
            var esInfo = anOpenSearchInfo();

            // When
            var result = adapter.adaptQuery(query, esInfo);

            // Then
            assertThatJson(result.toString())
                .isEqualTo(
                    """
                            {
                               "aggregations": {
                                 "by_date": {
                                   "aggregations": { "avg_response-time": { "avg": { "field": "response-time" } } },
                                   "date_histogram": {
                                     "extended_bounds": { "max": 1697969730000, "min": 1697883330000 },
                                     "field": "@timestamp",
                                     "interval": "600000ms",
                                     "min_doc_count": 0
                                   }
                                 }
                               },
                               "query": {
                                 "bool": {
                                   "filter": [
                                     { "term": { "api": "my-api-id" } },
                                     {
                                       "range": {
                                         "@timestamp": {
                                           "from": 1697882730000,
                                           "to": 1697970330000,
                                           "include_lower": true,
                                           "include_upper": true
                                         }
                                       }
                                     }
                                   ]
                                 }
                               },
                               "size": 0
                             }"""
                );
        }
    }

    private ElasticsearchInfo anElasticsearchInfo() {
        var version = new Version();
        version.setNumber("8.0.0");

        var esInfo = new ElasticsearchInfo();
        esInfo.setVersion(version);

        return esInfo;
    }

    private ElasticsearchInfo anOpenSearchInfo() {
        var version = new Version();
        version.setNumber("2.0.0");
        version.setDistribution("opensearch");

        var esInfo = new ElasticsearchInfo();
        esInfo.setVersion(version);

        return esInfo;
    }
}
