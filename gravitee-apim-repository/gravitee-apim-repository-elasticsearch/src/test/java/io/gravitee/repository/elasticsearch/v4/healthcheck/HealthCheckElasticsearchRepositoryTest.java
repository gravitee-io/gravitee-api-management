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
package io.gravitee.repository.elasticsearch.v4.healthcheck;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeQuery;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker")
class HealthCheckElasticsearchRepositoryTest extends AbstractElasticsearchRepositoryTest {

    private static final String API_ID = "bf19088c-f2c7-4fec-9908-8cf2c75fece4";

    @Autowired
    private HealthCheckElasticsearchRepository repository;

    @Nested
    class AverageResponseTime {

        @Test
        void should_return_average_response_time_grouped_by_endpoint() {
            // Given
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = from.plus(Duration.ofDays(1));

            // When
            var result = repository.averageResponseTime(
                new QueryContext("org#1", "env#1"),
                new AverageHealthCheckResponseTimeQuery(API_ID, "endpoint", from, to)
            );

            // Then
            assertThat(result)
                .hasValueSatisfying(responseTime -> {
                    assertThat(responseTime.globalResponseTimeMs()).isEqualTo(7L);
                    assertThat(responseTime.groupedResponseTimeMs()).contains(Map.entry("default", 5L), Map.entry("other", 8L));
                });
        }

        @Test
        void should_return_average_response_time_grouped_by_gateway() {
            // Given
            var now = Instant.now();
            var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
            var to = from.plus(Duration.ofDays(1));

            // When
            var result = repository.averageResponseTime(
                new QueryContext("org#1", "env#1"),
                new AverageHealthCheckResponseTimeQuery(API_ID, "gateway", from, to)
            );

            // Then
            assertThat(result)
                .hasValueSatisfying(responseTime -> {
                    assertThat(responseTime.globalResponseTimeMs()).isEqualTo(6L);
                    assertThat(responseTime.groupedResponseTimeMs()).contains(Map.entry("gw1", 8L), Map.entry("gw2", 3L));
                });
        }
    }
}
