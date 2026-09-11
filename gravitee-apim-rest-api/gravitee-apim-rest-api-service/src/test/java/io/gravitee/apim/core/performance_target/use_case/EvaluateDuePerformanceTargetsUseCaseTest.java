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

import fixtures.core.model.PerformanceTargetFixtures;
import inmemory.InMemoryAlternative;
import inmemory.PerformanceTargetCrudServiceInMemory;
import inmemory.PerformanceTargetEvaluationCrudServiceInMemory;
import inmemory.PerformanceTargetEvaluationQueryServiceInMemory;
import inmemory.PerformanceTargetEvaluatorInMemory;
import inmemory.PerformanceTargetQueryServiceInMemory;
import io.gravitee.apim.core.performance_target.domain_service.PerformanceTargetScheduleStateDomainService;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetSchedule;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EvaluateDuePerformanceTargetsUseCaseTest {

    private static final Instant T0 = Instant.parse("2021-06-01T10:00:00Z");
    private static final Duration INTERVAL = Duration.ofMinutes(5);
    private static final Duration TICK = Duration.ofMinutes(1);
    private static final PerformanceTargetSchedule SCHEDULE = new PerformanceTargetSchedule(3, Duration.ofHours(1), 288);

    PerformanceTargetCrudServiceInMemory targetCrudService = new PerformanceTargetCrudServiceInMemory();
    PerformanceTargetQueryServiceInMemory targetQueryService = new PerformanceTargetQueryServiceInMemory(targetCrudService);
    PerformanceTargetEvaluationCrudServiceInMemory evaluationCrudService = new PerformanceTargetEvaluationCrudServiceInMemory();
    PerformanceTargetEvaluationQueryServiceInMemory evaluationQueryService = new PerformanceTargetEvaluationQueryServiceInMemory(
        evaluationCrudService
    );
    PerformanceTargetEvaluatorInMemory evaluator = new PerformanceTargetEvaluatorInMemory();
    PerformanceTargetScheduleStateDomainService scheduleState = new PerformanceTargetScheduleStateDomainService();

    EvaluateDuePerformanceTargetsUseCase useCase = newUseCase(evaluator, scheduleState);

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
        Stream.of(targetCrudService, evaluationCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_evaluate_every_target_never_evaluated_and_store_the_results_as_latest() {
        targetCrudService.initWith(List.of(aTarget("a"), aTarget("b")));

        var output = tick(T0);

        assertThat(output.targets()).isEqualTo(2);
        assertThat(output.evaluations()).extracting(PerformanceTargetEvaluation::targetId).containsExactly("a", "b");
        assertThat(evaluationCrudService.storage())
            .hasSize(2)
            .allSatisfy(evaluation -> {
                assertThat(evaluation.id()).isNotBlank();
                assertThat(evaluation.latest()).isTrue();
                assertThat(evaluation.evaluatedAt()).isEqualTo(T0);
            });
    }

    @Test
    void should_store_one_evaluation_per_slot_when_several_nodes_tick_at_once() {
        var target = aTarget("a");
        targetCrudService.initWith(List.of(target));
        var boundary = slotBoundaryAfter(target, T0);
        var otherNode = newUseCase(evaluator);

        var first = useCase.execute(input(boundary));
        var second = otherNode.execute(input(boundary.plusSeconds(1)));
        var secondNextTick = otherNode.execute(input(boundary.plus(TICK)));

        assertThat(first.evaluations()).extracting(PerformanceTargetEvaluation::targetId).containsExactly("a");
        assertThat(second.evaluations()).isEmpty();
        assertThat(secondNextTick.evaluations()).isEmpty();
        assertThat(evaluationCrudService.storage()).singleElement().extracting(PerformanceTargetEvaluation::latest).isEqualTo(true);
    }

    @Test
    void should_give_the_same_evaluation_the_same_id_on_every_node() {
        var target = aTarget("a");
        targetCrudService.initWith(List.of(target));
        var boundary = slotBoundaryAfter(target, T0);

        var stored = useCase.execute(input(boundary)).evaluations().getFirst();
        evaluationCrudService.reset();
        var storedAgain = newUseCase(evaluator).execute(input(boundary.plusSeconds(30))).evaluations().getFirst();
        evaluationCrudService.reset();
        var nextSlot = newUseCase(evaluator).execute(input(boundary.plus(INTERVAL))).evaluations().getFirst();

        assertThat(storedAgain.id()).isEqualTo(stored.id());
        assertThat(nextSlot.id()).isNotEqualTo(stored.id());
    }

    @Test
    void should_not_evaluate_a_target_again_inside_the_slot_of_its_last_evaluation() {
        var target = aTarget("a");
        targetCrudService.initWith(List.of(target));
        var boundary = slotBoundaryAfter(target, T0);

        tick(boundary);
        var inside = IntStream.range(1, 5)
            .mapToObj(i -> tick(boundary.plus(TICK.multipliedBy(i))))
            .toList();
        var next = tick(boundary.plus(INTERVAL));

        assertThat(inside).allSatisfy(output -> assertThat(output.evaluations()).isEmpty());
        assertThat(next.evaluations()).extracting(PerformanceTargetEvaluation::targetId).containsExactly("a");
        assertThat(evaluationCrudService.storage()).hasSize(2);
    }

    @Test
    void should_seed_the_schedule_from_the_stored_evaluations_so_that_a_restart_evaluates_only_what_is_due() {
        var inSlot = aTarget("in-slot");
        var pastSlot = aTarget("past-slot");
        targetCrudService.initWith(List.of(inSlot, pastSlot));
        // half a minute past both phases: inside the slot for one, an interval past its last evaluation for the other
        var now = Stream.of(inSlot, pastSlot)
            .map(target -> slotBoundaryAfter(target, T0))
            .max(Instant::compareTo)
            .orElseThrow()
            .plusSeconds(30);
        evaluationCrudService.initWith(
            List.of(
                PerformanceTargetFixtures.anEvaluation("old-1", "in-slot", PerformanceTargetEvaluation.Status.PASS, now.minusSeconds(20)),
                PerformanceTargetFixtures.anEvaluation("old-2", "past-slot", PerformanceTargetEvaluation.Status.PASS, now.minus(INTERVAL))
            )
        );

        var output = newUseCase(evaluator).execute(input(now));

        assertThat(output.evaluations()).extracting(PerformanceTargetEvaluation::targetId).containsExactly("past-slot");
    }

    @Test
    void should_spread_targets_last_evaluated_at_the_same_instant_over_the_following_ticks() {
        // ids whose hashes fall on different minutes of the interval, unlike a numbered series whose hashes are consecutive
        var targets = Stream.of("checkout", "payments", "inventory", "billing", "shipping")
            .map(id -> aTarget(id))
            .toList();
        targetCrudService.initWith(targets);
        evaluationCrudService.initWith(
            targets
                .stream()
                .map(target ->
                    PerformanceTargetFixtures.anEvaluation("old-" + target.id(), target.id(), PerformanceTargetEvaluation.Status.PASS, T0)
                )
                .toList()
        );

        // T0 starts a slot, and an evaluation at a slot's start is that slot's: the targets are due again in the next one
        var evaluatedPerTick = IntStream.range(0, 5)
            .mapToObj(i ->
                tick(T0.plus(INTERVAL).plus(TICK.multipliedBy(i)))
                    .evaluations()
                    .stream()
                    .map(PerformanceTargetEvaluation::targetId)
                    .toList()
            )
            .toList();

        assertThat(evaluatedPerTick.stream().flatMap(List::stream)).containsExactlyInAnyOrderElementsOf(
            targets.stream().map(PerformanceTarget::id).toList()
        );
        assertThat(
            evaluatedPerTick
                .stream()
                .filter(tick -> !tick.isEmpty())
                .count()
        ).isGreaterThan(1);
    }

    @Test
    void should_back_off_an_idle_target_and_return_to_its_interval_when_traffic_reappears() {
        targetCrudService.initWith(List.of(aTarget("idle")));
        evaluator.status(PerformanceTargetEvaluation.Status.NOT_EVALUABLE);

        var idleEvaluations = new ArrayList<Instant>();
        var now = T0;
        while (now.isBefore(T0.plus(Duration.ofHours(5)))) {
            tick(now)
                .evaluations()
                .forEach(evaluation -> idleEvaluations.add(evaluation.evaluatedAt()));
            now = now.plus(TICK);
        }
        var gaps = gaps(idleEvaluations);

        // the first gap reaches the target's phase in the next slot; from then on it is evaluated every interval until the backoff bites
        assertThat(gaps.getFirst()).isBetween(INTERVAL, INTERVAL.multipliedBy(2).minus(TICK));
        assertThat(gaps.get(1)).isEqualTo(INTERVAL);
        assertThat(gaps).allSatisfy(gap -> assertThat(gap).isLessThanOrEqualTo(Duration.ofHours(1)));
        assertThat(gaps.subList(gaps.size() - 2, gaps.size())).allSatisfy(gap -> assertThat(gap).isEqualTo(Duration.ofHours(1)));

        evaluator.status(PerformanceTargetEvaluation.Status.PASS);
        var recovered = new ArrayList<Instant>();
        var end = now.plus(Duration.ofHours(2));
        while (now.isBefore(end)) {
            tick(now)
                .evaluations()
                .forEach(evaluation -> recovered.add(evaluation.evaluatedAt()));
            now = now.plus(TICK);
        }

        assertThat(recovered).hasSizeGreaterThan(3);
        assertThat(recovered.getFirst()).isBeforeOrEqualTo(T0.plus(Duration.ofHours(6)));
        assertThat(gaps(recovered)).allSatisfy(gap -> assertThat(gap).isEqualTo(INTERVAL));
    }

    @Test
    void should_resume_the_backoff_of_an_idle_target_after_a_restart() {
        var target = aTarget("idle");
        targetCrudService.initWith(List.of(target));
        var lastBoundary = slotBoundaryAfter(target, T0);
        evaluationCrudService.initWith(
            IntStream.range(0, 4)
                .mapToObj(i ->
                    PerformanceTargetFixtures.anEvaluation(
                        "old-" + i,
                        "idle",
                        PerformanceTargetEvaluation.Status.NOT_EVALUABLE,
                        lastBoundary.minus(INTERVAL.multipliedBy(i))
                    )
                )
                .toList()
        );

        var afterOneInterval = newUseCase(evaluator).execute(input(lastBoundary.plus(INTERVAL)));

        assertThat(afterOneInterval.evaluations()).isEmpty();
    }

    @Test
    void should_prune_the_history_of_an_evaluated_target_beyond_the_retention() {
        var schedule = new PerformanceTargetSchedule(3, Duration.ofHours(1), 3);
        var target = aTarget("a");
        targetCrudService.initWith(List.of(target));
        evaluationCrudService.initWith(
            IntStream.range(0, 3)
                .mapToObj(i ->
                    PerformanceTargetFixtures.anEvaluation(
                        "old-" + i,
                        "a",
                        PerformanceTargetEvaluation.Status.PASS,
                        T0.minus(Duration.ofHours(1)).minus(INTERVAL.multipliedBy(i))
                    )
                )
                .toList()
        );

        TimeProvider.overrideClock(Clock.fixed(slotBoundaryAfter(target, T0), ZoneId.systemDefault()));
        useCase.execute(new EvaluateDuePerformanceTargetsUseCase.Input(schedule));

        assertThat(evaluationCrudService.storage())
            .extracting(PerformanceTargetEvaluation::id)
            .hasSize(3)
            .contains("old-0", "old-1")
            .doesNotContain("old-2");
    }

    @Test
    void should_treat_a_recreated_target_as_new() {
        var target = aTarget("a");
        targetCrudService.initWith(List.of(target));
        tick(T0);
        var justBeforeNextSlot = slotBoundaryAfter(target, T0.plusSeconds(1)).minusSeconds(1);

        targetCrudService.delete("a");
        evaluationCrudService.deleteByTargetId("a");
        tick(T0.plusSeconds(1));
        targetCrudService.create(target);

        assertThat(tick(justBeforeNextSlot).evaluations()).extracting(PerformanceTargetEvaluation::targetId).containsExactly("a");
    }

    @Test
    void should_not_retry_a_target_the_evaluator_left_out_before_its_next_slot() {
        var target = aTarget("a");
        targetCrudService.initWith(List.of(target));
        var boundary = slotBoundaryAfter(target, T0);
        var calls = new AtomicInteger();
        var leavingOut = new PerformanceTargetEvaluatorInMemory() {
            @Override
            public List<PerformanceTargetEvaluation> evaluateAll(List<PerformanceTarget> targets, Instant now) {
                calls.incrementAndGet();
                return List.of();
            }
        };
        var useCase = newUseCase(leavingOut);

        var first = useCase.execute(input(boundary));
        var retry = useCase.execute(input(boundary.plus(TICK)));

        assertThat(first.targets()).isEqualTo(1);
        assertThat(first.evaluations()).isEmpty();
        assertThat(retry.evaluations()).isEmpty();
        assertThat(calls).hasValue(1);
        assertThat(evaluationCrudService.storage()).isEmpty();
    }

    @Test
    void should_end_the_backoff_of_an_idle_target_as_soon_as_an_on_demand_evaluation_finds_traffic() {
        var target = aTarget("idle");
        targetCrudService.initWith(List.of(target));
        evaluator.status(PerformanceTargetEvaluation.Status.NOT_EVALUABLE);
        var now = T0;
        while (now.isBefore(T0.plus(Duration.ofHours(5)))) {
            tick(now);
            now = now.plus(TICK);
        }
        var lastScheduled = evaluationCrudService
            .storage()
            .stream()
            .map(PerformanceTargetEvaluation::evaluatedAt)
            .max(Instant::compareTo)
            .get();
        // the target is backed off to the cap: nothing is due for the better part of an hour
        assertThat(tick(lastScheduled.plus(INTERVAL.multipliedBy(2))).evaluations()).isEmpty();

        evaluator.status(PerformanceTargetEvaluation.Status.PASS);
        var onDemand = new EvaluatePerformanceTargetUseCase(
            targetCrudService,
            evaluationQueryService,
            evaluationCrudService,
            evaluator,
            scheduleState
        );
        var clicked = lastScheduled.plus(INTERVAL.multipliedBy(2)).plusSeconds(30);
        input(clicked);
        onDemand.execute(new EvaluatePerformanceTargetUseCase.Input(target.environmentId(), target.id()));

        var nextSlot = tick(dueInSlotAfter(target, clicked));

        assertThat(nextSlot.evaluations()).extracting(PerformanceTargetEvaluation::targetId).containsExactly("idle");
    }

    @Test
    void should_evaluate_an_updated_target_at_the_next_tick_whatever_its_backoff_was() {
        var target = aTarget("idle");
        targetCrudService.initWith(List.of(target));
        evaluator.status(PerformanceTargetEvaluation.Status.NOT_EVALUABLE);
        var now = T0;
        while (now.isBefore(T0.plus(Duration.ofHours(5)))) {
            tick(now);
            now = now.plus(TICK);
        }
        assertThat(tick(now).evaluations()).isEmpty();

        // what saving an update does on the node that served it: the definition is stamped and this node's memory is reset
        targetCrudService.update(target.toBuilder().updatedAt(now.atZone(ZoneId.systemDefault())).build());
        scheduleState.reset(target.id());

        assertThat(tick(now.plus(TICK)).evaluations()).extracting(PerformanceTargetEvaluation::targetId).containsExactly("idle");
    }

    /**
     * Only the primary node schedules, but an on-demand evaluation is served by whichever node answers the request:
     * its result reaches the primary through the store, not through the primary's memory.
     */
    @Test
    void should_end_the_backoff_when_another_node_ran_the_on_demand_evaluation() {
        var target = aTarget("idle");
        targetCrudService.initWith(List.of(target));
        evaluator.status(PerformanceTargetEvaluation.Status.NOT_EVALUABLE);
        var now = T0;
        while (now.isBefore(T0.plus(Duration.ofHours(5)))) {
            tick(now);
            now = now.plus(TICK);
        }
        var lastScheduled = evaluationCrudService
            .storage()
            .stream()
            .map(PerformanceTargetEvaluation::evaluatedAt)
            .max(Instant::compareTo)
            .get();
        assertThat(tick(lastScheduled.plus(INTERVAL.multipliedBy(2))).evaluations()).isEmpty();

        evaluator.status(PerformanceTargetEvaluation.Status.PASS);
        var onAnotherNode = new EvaluatePerformanceTargetUseCase(
            targetCrudService,
            evaluationQueryService,
            evaluationCrudService,
            evaluator,
            new PerformanceTargetScheduleStateDomainService()
        );
        var clicked = lastScheduled.plus(INTERVAL.multipliedBy(2)).plusSeconds(30);
        input(clicked);
        onAnotherNode.execute(new EvaluatePerformanceTargetUseCase.Input(target.environmentId(), target.id()));

        var nextSlot = tick(dueInSlotAfter(target, clicked));

        assertThat(nextSlot.evaluations()).extracting(PerformanceTargetEvaluation::targetId).containsExactly("idle");
    }

    @Test
    void should_evaluate_a_target_another_node_redefined_at_the_next_tick() {
        var target = aTarget("idle");
        targetCrudService.initWith(List.of(target));
        evaluator.status(PerformanceTargetEvaluation.Status.NOT_EVALUABLE);
        var now = T0;
        while (now.isBefore(T0.plus(Duration.ofHours(5)))) {
            tick(now);
            now = now.plus(TICK);
        }
        assertThat(tick(now).evaluations()).isEmpty();

        // the store holds the new definition; this node's memory still holds the backoff
        targetCrudService.update(target.toBuilder().updatedAt(now.atZone(ZoneId.systemDefault())).build());

        assertThat(tick(now.plus(TICK)).evaluations()).extracting(PerformanceTargetEvaluation::targetId).containsExactly("idle");
    }

    /** Another node: its own schedule state, the same stores. */
    private EvaluateDuePerformanceTargetsUseCase newUseCase(PerformanceTargetEvaluatorInMemory evaluator) {
        return newUseCase(evaluator, new PerformanceTargetScheduleStateDomainService());
    }

    private EvaluateDuePerformanceTargetsUseCase newUseCase(
        PerformanceTargetEvaluatorInMemory evaluator,
        PerformanceTargetScheduleStateDomainService scheduleState
    ) {
        return new EvaluateDuePerformanceTargetsUseCase(
            targetQueryService,
            evaluationQueryService,
            evaluationCrudService,
            evaluator,
            scheduleState
        );
    }

    private EvaluateDuePerformanceTargetsUseCase.Output tick(Instant now) {
        return useCase.execute(input(now));
    }

    private static EvaluateDuePerformanceTargetsUseCase.Input input(Instant now) {
        TimeProvider.overrideClock(Clock.fixed(now, ZoneId.systemDefault()));
        return new EvaluateDuePerformanceTargetsUseCase.Input(SCHEDULE);
    }

    private static PerformanceTarget aTarget(String id) {
        return PerformanceTargetFixtures.aTarget(id).toBuilder().interval(INTERVAL).build();
    }

    /** The first instant at or after {@code from} at which the target is due: its phase into the slot {@code from} or the next one falls in. */
    private static Instant slotBoundaryAfter(PerformanceTarget target, Instant from) {
        var dueAt = SCHEDULE.dueAt(target, 0, SCHEDULE.slotStart(target, 0, from));
        return dueAt.isBefore(from) ? SCHEDULE.dueAt(target, 0, SCHEDULE.slotStart(target, 0, from.plus(INTERVAL))) : dueAt;
    }

    /** When the target is due in the slot following the one {@code evaluatedAt} falls in, its backoff forgotten. */
    private static Instant dueInSlotAfter(PerformanceTarget target, Instant evaluatedAt) {
        return SCHEDULE.dueAt(target, 0, SCHEDULE.slotStart(target, 0, evaluatedAt).plus(INTERVAL));
    }

    private static List<Duration> gaps(List<Instant> instants) {
        return IntStream.range(1, instants.size())
            .mapToObj(i -> Duration.between(instants.get(i - 1), instants.get(i)))
            .toList();
    }
}
