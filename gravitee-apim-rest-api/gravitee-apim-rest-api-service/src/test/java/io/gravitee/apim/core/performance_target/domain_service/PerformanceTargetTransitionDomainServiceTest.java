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
package io.gravitee.apim.core.performance_target.domain_service;

import static io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status.BREACH;
import static io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status.NOT_EVALUABLE;
import static io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status.PASS;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PerformanceTargetFixtures;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetNotificationPolicy;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition.Kind;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PerformanceTargetTransitionDomainServiceTest {

    private static final int NOT_EVALUABLE_AFTER = 3;
    private static final PerformanceTargetNotificationPolicy POLICY = new PerformanceTargetNotificationPolicy(NOT_EVALUABLE_AFTER);
    private static final Instant T0 = Instant.parse("2021-06-01T10:00:00Z");

    private final PerformanceTargetTransitionDomainService service = new PerformanceTargetTransitionDomainService();

    @Nested
    class Transition {

        @ParameterizedTest(name = "{0} after {1} (newest first) -> {2}")
        @CsvSource(
            delimiter = '|',
            value = {
                // a rule never evaluated so far has nothing to be compared against
                "BREACH        |                                       | ",
                "PASS          |                                       | ",
                "NOT_EVALUABLE |                                       | ",
                "BREACH        | NOT_EVALUABLE                         | ",
                "BREACH        | NOT_EVALUABLE,NOT_EVALUABLE           | ",
                // met to missed, and back
                "BREACH        | PASS                                  | RULE_MISSED",
                "BREACH        | PASS,BREACH                           | RULE_MISSED",
                "PASS          | BREACH                                | RULE_RECOVERED",
                "PASS          | PASS                                  | ",
                "BREACH        | BREACH                                | ",
                // pending empty windows keep the last evaluable verdict as the baseline
                "BREACH        | NOT_EVALUABLE,PASS                    | RULE_MISSED",
                "PASS          | NOT_EVALUABLE,NOT_EVALUABLE,BREACH    | RULE_RECOVERED",
                "PASS          | NOT_EVALUABLE,NOT_EVALUABLE,PASS      | ",
                // not evaluable is reported once, at the configured number of consecutive windows
                "NOT_EVALUABLE | PASS                                  | ",
                "NOT_EVALUABLE | NOT_EVALUABLE,PASS                    | ",
                "NOT_EVALUABLE | NOT_EVALUABLE,NOT_EVALUABLE,PASS      | RULE_NOT_EVALUABLE",
                "NOT_EVALUABLE | NOT_EVALUABLE,NOT_EVALUABLE,BREACH    | RULE_NOT_EVALUABLE",
                "NOT_EVALUABLE | NOT_EVALUABLE,NOT_EVALUABLE,NOT_EVALUABLE | ",
                // out of a reported not-evaluable state
                "PASS          | NOT_EVALUABLE,NOT_EVALUABLE,NOT_EVALUABLE | RULE_EVALUABLE_AGAIN",
                "BREACH        | NOT_EVALUABLE,NOT_EVALUABLE,NOT_EVALUABLE | RULE_MISSED",
                "PASS          | NOT_EVALUABLE,NOT_EVALUABLE,NOT_EVALUABLE,PASS | RULE_EVALUABLE_AGAIN",
            }
        )
        void should_tell_the_transition_from_the_previous_verdicts(String now, String previous, String expected) {
            var history = previous == null ? List.<Status>of() : List.of(previous.split(",")).stream().map(Status::valueOf).toList();

            var transition = PerformanceTargetTransitionDomainService.transition(Status.valueOf(now), history, NOT_EVALUABLE_AFTER);

            assertThat(transition).isEqualTo(Optional.ofNullable(expected).map(Kind::valueOf));
        }

        @Test
        void should_report_not_evaluable_at_the_first_empty_window_when_configured_so() {
            assertThat(PerformanceTargetTransitionDomainService.transition(NOT_EVALUABLE, List.of(PASS), 1)).contains(
                Kind.RULE_NOT_EVALUABLE
            );
            assertThat(PerformanceTargetTransitionDomainService.transition(PASS, List.of(NOT_EVALUABLE, PASS), 1)).contains(
                Kind.RULE_EVALUABLE_AGAIN
            );
        }
    }

    @Nested
    class Detect {

        private final PerformanceTarget.Rule latency = PerformanceTargetFixtures.aLatencyRule();
        private final PerformanceTarget.Rule errors = PerformanceTarget.Rule.builder()
            .id("errors-rule")
            .metric(MetricSpec.Name.HTTP_ERROR_RATE)
            .measure(MetricSpec.Measure.PERCENTAGE)
            .operator(PerformanceTarget.Operator.LTE)
            .threshold(5)
            .build();
        private final PerformanceTarget target = PerformanceTargetFixtures.aTarget().toBuilder().rules(List.of(latency, errors)).build();

        @Test
        void should_report_each_rule_on_its_own_history_matched_by_id() {
            var history = List.of(evaluation("h1", Map.of(latency.id(), PASS, errors.id(), BREACH), T0.minusSeconds(300)));
            var current = evaluation("now", Map.of(latency.id(), BREACH, errors.id(), PASS), T0);

            var transitions = service.detect(target, current, history, POLICY);

            assertThat(transitions)
                .extracting(PerformanceTargetRuleTransition::kind, t -> t.rule().id(), t -> t.result().status())
                .containsExactly(
                    org.assertj.core.groups.Tuple.tuple(Kind.RULE_MISSED, latency.id(), BREACH),
                    org.assertj.core.groups.Tuple.tuple(Kind.RULE_RECOVERED, errors.id(), PASS)
                );
            assertThat(transitions).allSatisfy(transition -> {
                assertThat(transition.target()).isEqualTo(target);
                assertThat(transition.evaluation()).isEqualTo(current);
            });
        }

        @Test
        void should_ignore_a_rule_the_history_does_not_know_and_a_rule_without_id() {
            var anonymous = latency.toBuilder().id(null).build();
            var redefined = target.toBuilder().rules(List.of(anonymous, errors)).build();
            var history = List.of(evaluation("h1", Map.of(latency.id(), PASS), T0.minusSeconds(300)));
            var current = PerformanceTargetEvaluation.builder()
                .id("now")
                .targetId(target.id())
                .status(BREACH)
                .rules(List.of(result(null, BREACH), result(errors.id(), BREACH)))
                .evaluatedAt(T0)
                .build();

            assertThat(service.detect(redefined, current, history, POLICY)).isEmpty();
        }

        @Test
        void should_look_past_windows_that_did_not_hold_the_rule() {
            // the errors rule was added after h2: only h1 knows it, and its verdict there is what counts
            var history = List.of(
                evaluation("h2", Map.of(latency.id(), PASS), T0.minusSeconds(300)),
                evaluation("h1", Map.of(latency.id(), PASS, errors.id(), BREACH), T0.minusSeconds(600))
            );
            var current = evaluation("now", Map.of(latency.id(), PASS, errors.id(), PASS), T0);

            assertThat(service.detect(target, current, history, POLICY))
                .extracting(PerformanceTargetRuleTransition::kind, t -> t.rule().id())
                .containsExactly(org.assertj.core.groups.Tuple.tuple(Kind.RULE_RECOVERED, errors.id()));
        }

        @Test
        void should_report_not_evaluable_once_the_configured_windows_are_reached() {
            var history = IntStream.range(1, NOT_EVALUABLE_AFTER)
                .mapToObj(i -> evaluation("ne-" + i, Map.of(latency.id(), NOT_EVALUABLE), T0.minusSeconds(300L * i)))
                .toList();
            var withBaseline = new java.util.ArrayList<>(history);
            withBaseline.add(evaluation("pass", Map.of(latency.id(), PASS), T0.minusSeconds(300L * NOT_EVALUABLE_AFTER)));
            var current = evaluation("now", Map.of(latency.id(), NOT_EVALUABLE), T0);

            assertThat(service.detect(target.toBuilder().rules(List.of(latency)).build(), current, withBaseline, POLICY))
                .extracting(PerformanceTargetRuleTransition::kind)
                .containsExactly(Kind.RULE_NOT_EVALUABLE);
            assertThat(service.detect(target.toBuilder().rules(List.of(latency)).build(), current, history, POLICY)).isEmpty();
        }

        private PerformanceTargetEvaluation evaluation(String id, Map<String, Status> statuses, Instant evaluatedAt) {
            var results = statuses
                .entrySet()
                .stream()
                .map(entry -> result(entry.getKey(), entry.getValue()))
                .toList();
            return PerformanceTargetEvaluation.builder()
                .id(id)
                .targetId(target.id())
                .environmentId(target.environmentId())
                .reference(target.subject().reference())
                .status(Status.of(results))
                .rules(results)
                .evaluatedAt(evaluatedAt)
                .build();
        }

        private static PerformanceTargetEvaluation.RuleResult result(String ruleId, Status status) {
            var evaluable = status != NOT_EVALUABLE;
            return new PerformanceTargetEvaluation.RuleResult(
                ruleId,
                MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME,
                MetricSpec.Measure.P95,
                PerformanceTarget.Operator.LTE,
                2000,
                evaluable ? 1000.0 : null,
                evaluable ? new PerformanceTargetEvaluation.Deviation(-1000, -0.5) : null,
                evaluable ? 42 : 0,
                status
            );
        }
    }
}
