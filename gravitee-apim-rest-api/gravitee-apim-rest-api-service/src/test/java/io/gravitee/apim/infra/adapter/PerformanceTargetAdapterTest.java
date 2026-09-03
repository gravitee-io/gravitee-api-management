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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.definition.model.v4.ApiType;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PerformanceTargetAdapterTest {

    private final PerformanceTarget target = PerformanceTarget.builder()
        .id("target-id")
        .environmentId("environment-id")
        .subject(new PerformanceTarget.Subject(List.of("a2a-api", "llm-api"), "agent-42"))
        .window(Duration.ofHours(1))
        .interval(Duration.ofMinutes(5))
        .minSampleSize(20)
        .rules(
            List.of(
                PerformanceTarget.Rule.builder()
                    .metric(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME)
                    .measure(MetricSpec.Measure.P95)
                    .operator(PerformanceTarget.Operator.LTE)
                    .threshold(2000)
                    .apiTypes(Set.of(ApiType.A2A_PROXY))
                    .filters(List.of(new Filter(FilterSpec.Name.MCP_PROXY_TOOL, FilterOperator.IN, List.of("search", "fetch"))))
                    .build(),
                PerformanceTarget.Rule.builder()
                    .metric(MetricSpec.Name.HTTP_ERROR_RATE)
                    .measure(MetricSpec.Measure.PERCENTAGE)
                    .operator(PerformanceTarget.Operator.LT)
                    .threshold(5)
                    .build()
            )
        )
        .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
        .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
        .build();

    @Test
    void should_flatten_subject_and_durations_for_the_repository() {
        var repository = PerformanceTargetAdapter.INSTANCE.toRepository(target);

        assertThat(repository.getApiIds()).containsExactly("a2a-api", "llm-api");
        assertThat(repository.getReference()).isEqualTo("agent-42");
        assertThat(repository.getWindowSeconds()).isEqualTo(3600);
        assertThat(repository.getIntervalSeconds()).isEqualTo(300);
        assertThat(repository.getRules())
            .first()
            .isEqualTo(
                new io.gravitee.repository.management.model.PerformanceTarget.Rule(
                    "HTTP_GATEWAY_RESPONSE_TIME",
                    "P95",
                    "LTE",
                    2000,
                    List.of("A2A_PROXY"),
                    List.of(
                        new io.gravitee.repository.management.model.PerformanceTarget.Filter(
                            "MCP_PROXY_TOOL",
                            "IN",
                            List.of("search", "fetch")
                        )
                    )
                )
            );
    }

    @Test
    void should_round_trip_through_the_repository_model() {
        var roundTrip = PerformanceTargetAdapter.INSTANCE.toEntity(PerformanceTargetAdapter.INSTANCE.toRepository(target));

        assertThat(roundTrip).isEqualTo(target);
    }
}
