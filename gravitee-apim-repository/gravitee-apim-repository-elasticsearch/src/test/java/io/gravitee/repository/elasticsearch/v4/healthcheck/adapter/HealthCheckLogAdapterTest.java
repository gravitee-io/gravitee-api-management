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

import io.gravitee.repository.healthcheck.v4.model.HealthCheckLogQuery;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HealthCheckLogAdapterTest {

    @Nested
    class AdaptQuery {

        private static final String API_ID = "my-api-id";
        private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
        private static final Instant TO = INSTANT_NOW;
        private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);
        private static final Duration INTERVAL = Duration.ofMinutes(10);

        @Test
        void adapt_query_without_optional_query_param() {
            // Given
            var pageable = new PageableBuilder().pageSize(5).pageNumber(1).build();
            var adapter = new HealthCheckLogAdapter(pageable);
            var query = new HealthCheckLogQuery(API_ID, FROM, TO, Optional.empty(), pageable);

            // When
            var result = adapter.adaptQuery(query);

            // Then
            assertThatJson(result)
                .isEqualTo(
                    """
                                {
                                   "query": {
                                     "bool": {
                                       "filter": [
                                         { "term": { "api": "my-api-id" } },
                                         {
                                           "range": {
                                             "@timestamp": {
                                               "from": 1697883330000,
                                               "to": 1697969730000,
                                               "include_lower": true,
                                               "include_upper": true
                                             }
                                           }
                                         }
                                       ]
                                     }
                                   },
                                   "sort": [{ "@timestamp": { "order": "asc" } }],
                                   "size": 5,
                                   "from": 0
                                 }"""
                );
        }

        @Test
        void adapt_query_with_optional_query_param() {
            // Given
            var pageable = new PageableBuilder().pageSize(5).pageNumber(1).build();
            var adapter = new HealthCheckLogAdapter(pageable);
            var query = new HealthCheckLogQuery(API_ID, FROM, TO, Optional.of(false), pageable);

            // When
            var result = adapter.adaptQuery(query);

            // Then
            assertThatJson(result)
                .isEqualTo(
                    """
                                {
                                   "query": {
                                     "bool": {
                                       "must": { "term": { "success": false } },
                                       "filter": [
                                         { "term": { "api": "my-api-id" } },
                                         {
                                           "range": {
                                             "@timestamp": {
                                               "from": 1697883330000,
                                               "to": 1697969730000,
                                               "include_lower": true,
                                               "include_upper": true
                                             }
                                           }
                                         }
                                       ]
                                     }
                                   },
                                   "sort": [{ "@timestamp": { "order": "asc" } }],
                                   "size": 5,
                                   "from": 0
                                 }"""
                );
        }

        @Test
        void adapt_query_for_a_specific_page() {
            // Given
            var pageable = new PageableBuilder().pageSize(5).pageNumber(3).build();
            var adapter = new HealthCheckLogAdapter(pageable);
            var query = new HealthCheckLogQuery(API_ID, FROM, TO, Optional.empty(), pageable);

            // When
            var result = adapter.adaptQuery(query);

            // Then
            assertThatJson(result)
                .isEqualTo(
                    """
                                {
                                   "query": {
                                     "bool": {
                                       "filter": [
                                         { "term": { "api": "my-api-id" } },
                                         {
                                           "range": {
                                             "@timestamp": {
                                               "from": 1697883330000,
                                               "to": 1697969730000,
                                               "include_lower": true,
                                               "include_upper": true
                                             }
                                           }
                                         }
                                       ]
                                     }
                                   },
                                   "sort": [{ "@timestamp": { "order": "asc" } }],
                                   "size": 5,
                                   "from": 10
                                 }"""
                );
        }
    }
}
