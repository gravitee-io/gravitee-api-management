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
package io.gravitee.apim.core.performance_target.model;

import static io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status.BREACH;
import static io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status.NOT_EVALUABLE;
import static io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status.PASS;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PerformanceTargetEvaluationTest {

    @Nested
    class StatusOf {

        @Test
        void should_pass_when_every_rule_passes() {
            assertThat(PerformanceTargetEvaluation.Status.of(List.of(aRuleResult(PASS), aRuleResult(PASS)))).isEqualTo(PASS);
        }

        @Test
        void should_breach_when_any_rule_breaches_even_if_another_is_not_evaluable() {
            assertThat(
                PerformanceTargetEvaluation.Status.of(List.of(aRuleResult(PASS), aRuleResult(NOT_EVALUABLE), aRuleResult(BREACH)))
            ).isEqualTo(BREACH);
        }

        @Test
        void should_not_be_evaluable_when_no_rule_breaches_and_one_is_not_evaluable() {
            assertThat(PerformanceTargetEvaluation.Status.of(List.of(aRuleResult(PASS), aRuleResult(NOT_EVALUABLE)))).isEqualTo(
                NOT_EVALUABLE
            );
        }

        @Test
        void should_not_be_evaluable_without_any_rule() {
            assertThat(PerformanceTargetEvaluation.Status.of(List.of())).isEqualTo(NOT_EVALUABLE);
        }
    }

    @Nested
    class DeviationOf {

        @Test
        void should_measure_the_distance_to_the_threshold_in_the_metric_unit_and_relative_to_it() {
            assertThat(PerformanceTargetEvaluation.Deviation.of(4810, 2000)).isEqualTo(
                new PerformanceTargetEvaluation.Deviation(2810, 1.405)
            );
            assertThat(PerformanceTargetEvaluation.Deviation.of(1.5, 6)).isEqualTo(new PerformanceTargetEvaluation.Deviation(-4.5, -0.75));
        }

        @Test
        void should_report_a_full_ratio_when_a_zero_threshold_is_exceeded() {
            assertThat(PerformanceTargetEvaluation.Deviation.of(3, 0)).isEqualTo(new PerformanceTargetEvaluation.Deviation(3, 1));
            assertThat(PerformanceTargetEvaluation.Deviation.of(0, 0)).isEqualTo(new PerformanceTargetEvaluation.Deviation(0, 0));
        }
    }

    private static PerformanceTargetEvaluation.RuleResult aRuleResult(PerformanceTargetEvaluation.Status status) {
        return new PerformanceTargetEvaluation.RuleResult(
            MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME,
            MetricSpec.Measure.P95,
            PerformanceTarget.Operator.LTE,
            2000,
            null,
            null,
            0,
            status
        );
    }
}
