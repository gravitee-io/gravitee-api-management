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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.definition.model.v4.ApiType;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PerformanceTargetTest {

    private static final Map<String, ApiType> SUBJECT_API_TYPES = Map.of(
        "a2a-api",
        ApiType.A2A_PROXY,
        "llm-api",
        ApiType.LLM_PROXY,
        "llm-api-2",
        ApiType.LLM_PROXY
    );

    private final PerformanceTarget target = PerformanceTarget.builder()
        .id("target-id")
        .environmentId("environment-id")
        .subject(new PerformanceTarget.Subject(List.of("a2a-api", "llm-api", "llm-api-2"), "agent-42"))
        .window(Duration.ofHours(1))
        .interval(Duration.ofMinutes(5))
        .minSampleSize(20)
        .build();

    @Test
    void should_resolve_a_rule_restricted_by_api_type_to_the_subject_ids_of_that_type() {
        var llmRule = aRule(Set.of(ApiType.LLM_PROXY));

        assertThat(target.apiIdsFor(llmRule, SUBJECT_API_TYPES)).containsExactly("llm-api", "llm-api-2");
    }

    @Test
    void should_resolve_an_unrestricted_rule_to_the_whole_subject() {
        var rule = aRule(Set.of());

        assertThat(target.apiIdsFor(rule, SUBJECT_API_TYPES)).containsExactly("a2a-api", "llm-api", "llm-api-2");
    }

    @Test
    void should_leave_out_subject_ids_whose_type_is_unknown_when_the_rule_is_restricted() {
        var rule = aRule(Set.of(ApiType.A2A_PROXY));

        assertThat(target.apiIdsFor(rule, Map.of("llm-api", ApiType.LLM_PROXY))).isEmpty();
    }

    @Nested
    class Evaluate {

        private final PerformanceTarget.Rule rule = aRule(Set.of());

        @Test
        void should_pass_when_the_observed_value_meets_the_threshold() {
            var result = rule.evaluate(1500.0, 63, 20);

            assertThat(result).isEqualTo(
                new PerformanceTargetEvaluation.RuleResult(
                    MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME,
                    MetricSpec.Measure.P95,
                    PerformanceTarget.Operator.LTE,
                    2000,
                    1500.0,
                    new PerformanceTargetEvaluation.Deviation(-500, -0.25),
                    63,
                    PerformanceTargetEvaluation.Status.PASS
                )
            );
        }

        @Test
        void should_breach_when_the_observed_value_misses_the_threshold() {
            var result = rule.evaluate(4810.0, 63, 20);

            assertThat(result.status()).isEqualTo(PerformanceTargetEvaluation.Status.BREACH);
            assertThat(result.observed()).isEqualTo(4810.0);
            assertThat(result.deviation()).isEqualTo(new PerformanceTargetEvaluation.Deviation(2810, 1.405));
            assertThat(result.sampleCount()).isEqualTo(63);
        }

        @Test
        void should_pass_when_the_observed_value_equals_an_inclusive_threshold() {
            assertThat(rule.evaluate(2000.0, 63, 20).status()).isEqualTo(PerformanceTargetEvaluation.Status.PASS);
        }

        @Test
        void should_not_be_evaluable_below_the_minimum_sample_size_even_with_an_observed_value() {
            var result = rule.evaluate(4810.0, 19, 20);

            assertThat(result.status()).isEqualTo(PerformanceTargetEvaluation.Status.NOT_EVALUABLE);
            assertThat(result.observed()).isNull();
            assertThat(result.deviation()).isNull();
            assertThat(result.sampleCount()).isEqualTo(19);
        }

        @Test
        void should_not_be_evaluable_without_an_observed_value() {
            var result = rule.evaluate(null, 63, 20);

            assertThat(result.status()).isEqualTo(PerformanceTargetEvaluation.Status.NOT_EVALUABLE);
            assertThat(result.observed()).isNull();
            assertThat(result.deviation()).isNull();
            assertThat(result.sampleCount()).isEqualTo(63);
        }

        @Test
        void should_evaluate_exactly_the_minimum_sample_size() {
            assertThat(rule.evaluate(1500.0, 20, 20).status()).isEqualTo(PerformanceTargetEvaluation.Status.PASS);
        }
    }

    @Nested
    class OperatorHolds {

        @ParameterizedTest
        @CsvSource(
            {
                "LT, 1, 2, true",
                "LT, 2, 2, false",
                "LTE, 2, 2, true",
                "LTE, 3, 2, false",
                "GT, 3, 2, true",
                "GT, 2, 2, false",
                "GTE, 2, 2, true",
                "GTE, 1, 2, false",
            }
        )
        void should_compare_the_observed_value_to_the_threshold(
            PerformanceTarget.Operator operator,
            double observed,
            double threshold,
            boolean expected
        ) {
            assertThat(operator.holds(observed, threshold)).isEqualTo(expected);
        }
    }

    private static PerformanceTarget.Rule aRule(Set<ApiType> apiTypes) {
        return PerformanceTarget.Rule.builder()
            .metric(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME)
            .measure(MetricSpec.Measure.P95)
            .operator(PerformanceTarget.Operator.LTE)
            .threshold(2000)
            .apiTypes(apiTypes)
            .build();
    }
}
