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
package io.gravitee.apim.core.performance_target.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PerformanceTargetFixtures;
import inmemory.InMemoryAlternative;
import inmemory.PerformanceTargetCrudServiceInMemory;
import inmemory.PerformanceTargetEvaluationCrudServiceInMemory;
import inmemory.PerformanceTargetEvaluationQueryServiceInMemory;
import inmemory.PerformanceTargetEvaluatorInMemory;
import inmemory.PerformanceTargetTransitionPublisherInMemory;
import io.gravitee.apim.core.performance_target.domain_service.PerformanceTargetScheduleStateDomainService;
import io.gravitee.apim.core.performance_target.domain_service.PerformanceTargetTransitionDomainService;
import io.gravitee.apim.core.performance_target.exception.PerformanceTargetEvaluatedTooRecentlyException;
import io.gravitee.apim.core.performance_target.exception.PerformanceTargetNotFoundException;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetNotificationPolicy;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EvaluatePerformanceTargetUseCaseTest {

    private static final String ENVIRONMENT_ID = PerformanceTargetFixtures.ENVIRONMENT_ID;
    private static final String TARGET_ID = "target-id";
    private static final Instant NOW = Instant.parse("2021-06-01T10:00:00Z");

    PerformanceTargetCrudServiceInMemory targetCrudService = new PerformanceTargetCrudServiceInMemory();
    PerformanceTargetEvaluationCrudServiceInMemory evaluationCrudService = new PerformanceTargetEvaluationCrudServiceInMemory();
    PerformanceTargetEvaluationQueryServiceInMemory evaluationQueryService = new PerformanceTargetEvaluationQueryServiceInMemory(
        evaluationCrudService
    );

    PerformanceTargetScheduleStateDomainService scheduleState = new PerformanceTargetScheduleStateDomainService();
    PerformanceTargetEvaluatorInMemory evaluator = new PerformanceTargetEvaluatorInMemory();
    PerformanceTargetTransitionPublisherInMemory transitionPublisher = new PerformanceTargetTransitionPublisherInMemory();

    EvaluatePerformanceTargetUseCase useCase = new EvaluatePerformanceTargetUseCase(
        targetCrudService,
        evaluationQueryService,
        evaluationCrudService,
        evaluator,
        scheduleState,
        new PerformanceTargetTransitionDomainService(),
        transitionPublisher,
        new PerformanceTargetNotificationPolicy(3)
    );

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneId.systemDefault()));
        UuidString.overrideGenerator(() -> "evaluation-id");
        targetCrudService.initWith(List.of(PerformanceTargetFixtures.aTarget(TARGET_ID)));
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
        UuidString.reset();
        Stream.of(targetCrudService, evaluationCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_store_the_evaluation_as_the_latest_of_the_target() {
        var output = useCase.execute(new EvaluatePerformanceTargetUseCase.Input(ENVIRONMENT_ID, TARGET_ID));

        assertThat(output.evaluation().id()).isEqualTo("evaluation-id");
        assertThat(output.evaluation().latest()).isTrue();
        assertThat(output.evaluation().evaluatedAt()).isEqualTo(NOW);
        assertThat(evaluationCrudService.storage()).containsExactly(output.evaluation());
    }

    @Test
    void should_tell_the_scheduler_about_the_evaluation_so_a_backoff_ends_with_it() {
        scheduleState.stateOf(TARGET_ID, () -> new PerformanceTargetScheduleStateDomainService.State(NOW.minusSeconds(3600), 6));

        useCase.execute(new EvaluatePerformanceTargetUseCase.Input(ENVIRONMENT_ID, TARGET_ID));

        assertThat(scheduleState.current(TARGET_ID)).contains(new PerformanceTargetScheduleStateDomainService.State(NOW, 0));
    }

    @Test
    void should_refuse_when_the_target_was_evaluated_less_than_30_seconds_ago() {
        givenLatestEvaluationAt(NOW.minusSeconds(12));

        assertThatThrownBy(() ->
            useCase.execute(new EvaluatePerformanceTargetUseCase.Input(ENVIRONMENT_ID, TARGET_ID))
        ).isInstanceOfSatisfying(PerformanceTargetEvaluatedTooRecentlyException.class, e ->
            assertThat(e.getRetryAfter()).isEqualTo(Duration.ofSeconds(18))
        );
        assertThat(evaluationCrudService.storage()).hasSize(1);
    }

    @Test
    void should_evaluate_when_exactly_30_seconds_have_passed() {
        givenLatestEvaluationAt(NOW.minusSeconds(30));

        useCase.execute(new EvaluatePerformanceTargetUseCase.Input(ENVIRONMENT_ID, TARGET_ID));

        assertThat(evaluationCrudService.storage())
            .filteredOn(PerformanceTargetEvaluation::latest)
            .extracting(e -> e.id())
            .containsExactly("evaluation-id");
    }

    @Test
    void should_prune_the_history_of_the_target_beyond_the_retention() {
        var history = IntStream.range(0, PerformanceTargetEvaluation.HISTORY_RETENTION)
            .mapToObj(i ->
                PerformanceTargetFixtures.anEvaluation(
                    "old-" + i,
                    TARGET_ID,
                    PerformanceTargetEvaluation.Status.PASS,
                    NOW.minus(Duration.ofHours(1)).minus(Duration.ofMinutes(5L * i))
                )
            )
            .toList();
        evaluationCrudService.initWith(history);

        useCase.execute(new EvaluatePerformanceTargetUseCase.Input(ENVIRONMENT_ID, TARGET_ID));

        assertThat(evaluationCrudService.storage())
            .hasSize(PerformanceTargetEvaluation.HISTORY_RETENTION)
            .extracting(PerformanceTargetEvaluation::id)
            .contains("evaluation-id", "old-0")
            .doesNotContain("old-" + (PerformanceTargetEvaluation.HISTORY_RETENTION - 1));
    }

    @Test
    void should_not_evaluate_a_target_of_another_environment() {
        assertThatThrownBy(() -> useCase.execute(new EvaluatePerformanceTargetUseCase.Input("other-environment", TARGET_ID))).isInstanceOf(
            PerformanceTargetNotFoundException.class
        );
    }

    @Nested
    class Transitions {

        @Test
        void should_publish_nothing_on_the_first_evaluation_of_a_target() {
            evaluator.status(PerformanceTargetEvaluation.Status.BREACH);

            var output = useCase.execute(new EvaluatePerformanceTargetUseCase.Input(ENVIRONMENT_ID, TARGET_ID));

            assertThat(output.transitions()).isEmpty();
            assertThat(transitionPublisher.runs()).isEmpty();
        }

        @Test
        void should_publish_a_rule_going_from_met_to_missed_once() {
            givenHistory(PerformanceTargetEvaluation.Status.PASS);
            evaluator.status(PerformanceTargetEvaluation.Status.BREACH);

            var output = useCase.execute(new EvaluatePerformanceTargetUseCase.Input(ENVIRONMENT_ID, TARGET_ID));

            assertThat(output.transitions())
                .singleElement()
                .satisfies(transition -> {
                    assertThat(transition.kind()).isEqualTo(PerformanceTargetRuleTransition.Kind.RULE_MISSED);
                    assertThat(transition.rule().id()).isEqualTo(PerformanceTargetFixtures.LATENCY_RULE_ID);
                    assertThat(transition.evaluation()).isEqualTo(output.evaluation());
                });
            assertThat(transitionPublisher.runs()).containsExactly(output.transitions());
        }

        @Test
        void should_publish_nothing_while_the_verdict_stands() {
            givenHistory(PerformanceTargetEvaluation.Status.BREACH);
            evaluator.status(PerformanceTargetEvaluation.Status.BREACH);

            var output = useCase.execute(new EvaluatePerformanceTargetUseCase.Input(ENVIRONMENT_ID, TARGET_ID));

            assertThat(output.transitions()).isEmpty();
            assertThat(transitionPublisher.runs()).isEmpty();
        }

        @Test
        void should_publish_a_recovery() {
            givenHistory(PerformanceTargetEvaluation.Status.BREACH);
            evaluator.status(PerformanceTargetEvaluation.Status.PASS);

            var output = useCase.execute(new EvaluatePerformanceTargetUseCase.Input(ENVIRONMENT_ID, TARGET_ID));

            assertThat(output.transitions())
                .extracting(PerformanceTargetRuleTransition::kind)
                .containsExactly(PerformanceTargetRuleTransition.Kind.RULE_RECOVERED);
        }

        @Test
        void should_publish_not_evaluable_only_at_the_configured_number_of_consecutive_windows() {
            givenHistory(
                PerformanceTargetEvaluation.Status.NOT_EVALUABLE,
                PerformanceTargetEvaluation.Status.NOT_EVALUABLE,
                PerformanceTargetEvaluation.Status.PASS
            );
            evaluator.status(PerformanceTargetEvaluation.Status.NOT_EVALUABLE);

            var output = useCase.execute(new EvaluatePerformanceTargetUseCase.Input(ENVIRONMENT_ID, TARGET_ID));

            assertThat(output.transitions())
                .extracting(PerformanceTargetRuleTransition::kind)
                .containsExactly(PerformanceTargetRuleTransition.Kind.RULE_NOT_EVALUABLE);
        }

        @Test
        void should_publish_nothing_for_an_empty_window_before_the_configured_number() {
            givenHistory(PerformanceTargetEvaluation.Status.NOT_EVALUABLE, PerformanceTargetEvaluation.Status.PASS);
            evaluator.status(PerformanceTargetEvaluation.Status.NOT_EVALUABLE);

            var output = useCase.execute(new EvaluatePerformanceTargetUseCase.Input(ENVIRONMENT_ID, TARGET_ID));

            assertThat(output.transitions()).isEmpty();
        }

        @Test
        void should_publish_evaluable_again_when_traffic_is_back_on_a_reported_not_evaluable_rule() {
            givenHistory(
                PerformanceTargetEvaluation.Status.NOT_EVALUABLE,
                PerformanceTargetEvaluation.Status.NOT_EVALUABLE,
                PerformanceTargetEvaluation.Status.NOT_EVALUABLE
            );
            evaluator.status(PerformanceTargetEvaluation.Status.PASS);

            var output = useCase.execute(new EvaluatePerformanceTargetUseCase.Input(ENVIRONMENT_ID, TARGET_ID));

            assertThat(output.transitions())
                .extracting(PerformanceTargetRuleTransition::kind)
                .containsExactly(PerformanceTargetRuleTransition.Kind.RULE_EVALUABLE_AGAIN);
        }

        /** Stored evaluations, most recent first, each an interval apart and old enough not to rate-limit this run. */
        private void givenHistory(PerformanceTargetEvaluation.Status... statuses) {
            var history = IntStream.range(0, statuses.length)
                .mapToObj(i ->
                    PerformanceTargetFixtures.anEvaluation(
                        "history-" + i,
                        TARGET_ID,
                        statuses[i],
                        NOW.minus(Duration.ofMinutes(5L * (i + 1)))
                    )
                )
                .toList();
            evaluationCrudService.initWith(history);
        }
    }

    private void givenLatestEvaluationAt(Instant evaluatedAt) {
        evaluationCrudService.initWith(
            List.of(PerformanceTargetFixtures.anEvaluation("previous", TARGET_ID, PerformanceTargetEvaluation.Status.PASS, evaluatedAt))
        );
    }
}
