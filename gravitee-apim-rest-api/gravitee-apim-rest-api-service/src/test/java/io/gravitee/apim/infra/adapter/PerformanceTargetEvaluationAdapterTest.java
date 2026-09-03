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

import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEnvironmentSummary;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PerformanceTargetEvaluationAdapterTest {

    private final PerformanceTargetEvaluation evaluation = PerformanceTargetEvaluation.builder()
        .id("evaluation-id")
        .targetId("target-id")
        .environmentId("environment-id")
        .reference("agent-42")
        .status(Status.BREACH)
        .rules(
            List.of(
                PerformanceTargetEvaluation.RuleResult.builder()
                    .metric(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME)
                    .measure(MetricSpec.Measure.P95)
                    .operator(PerformanceTarget.Operator.LTE)
                    .threshold(2000)
                    .observed(4810.0)
                    .deviation(new PerformanceTargetEvaluation.Deviation(2810, 1.405))
                    .sampleCount(63)
                    .status(Status.BREACH)
                    .build(),
                PerformanceTargetEvaluation.RuleResult.builder()
                    .metric(MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST)
                    .measure(MetricSpec.Measure.AVG)
                    .operator(PerformanceTarget.Operator.LTE)
                    .threshold(0.01)
                    .sampleCount(0)
                    .status(Status.NOT_EVALUABLE)
                    .build()
            )
        )
        .windowFrom(Instant.parse("2020-02-01T20:00:00Z"))
        .windowTo(Instant.parse("2020-02-01T21:00:00Z"))
        .coveredApiIds(List.of("a2a-api"))
        .evaluatedAt(Instant.parse("2020-02-01T21:00:00Z"))
        .latest(true)
        .build();

    @Test
    void should_split_deviation_into_repository_columns_and_keep_not_evaluable_rules_without_values() {
        var repository = PerformanceTargetEvaluationAdapter.INSTANCE.toRepository(evaluation);

        assertThat(repository.getStatus()).isEqualTo(io.gravitee.repository.management.model.PerformanceTargetEvaluation.Status.BREACH);
        assertThat(repository.getRules()).containsExactly(
            new io.gravitee.repository.management.model.PerformanceTargetEvaluation.RuleResult(
                "HTTP_GATEWAY_RESPONSE_TIME",
                "P95",
                "LTE",
                2000,
                4810.0,
                2810.0,
                1.405,
                63,
                io.gravitee.repository.management.model.PerformanceTargetEvaluation.Status.BREACH
            ),
            new io.gravitee.repository.management.model.PerformanceTargetEvaluation.RuleResult(
                "LLM_PROMPT_TOKEN_TOTAL_COST",
                "AVG",
                "LTE",
                0.01,
                null,
                null,
                null,
                0,
                io.gravitee.repository.management.model.PerformanceTargetEvaluation.Status.NOT_EVALUABLE
            )
        );
    }

    @Test
    void should_round_trip_through_the_repository_model() {
        var roundTrip = PerformanceTargetEvaluationAdapter.INSTANCE.toEntity(
            PerformanceTargetEvaluationAdapter.INSTANCE.toRepository(evaluation)
        );

        assertThat(roundTrip).isEqualTo(evaluation);
    }

    @Test
    void should_map_environment_summary() {
        var summary = PerformanceTargetEvaluationAdapter.INSTANCE.toEntity(
            new io.gravitee.repository.management.model.PerformanceTargetEnvironmentSummary("environment-id", 3, 1, 2)
        );

        assertThat(summary).isEqualTo(new PerformanceTargetEnvironmentSummary("environment-id", 3, 1, 2));
    }
}
