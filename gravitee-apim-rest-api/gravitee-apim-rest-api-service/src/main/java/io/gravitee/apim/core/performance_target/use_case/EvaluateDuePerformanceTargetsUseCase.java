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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.performance_target.crud_service.PerformanceTargetEvaluationCrudService;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetSchedule;
import io.gravitee.apim.core.performance_target.query_service.PerformanceTargetEvaluationQueryService;
import io.gravitee.apim.core.performance_target.query_service.PerformanceTargetQueryService;
import io.gravitee.apim.core.performance_target.service_provider.PerformanceTargetEvaluator;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

/**
 * One tick of the scheduler: evaluates every target whose slot has started since its last evaluation (see
 * {@link PerformanceTargetSchedule}), stores each result as the target's latest evaluation and prunes its history.
 *
 * <p>The schedule state of a target, when it was last evaluated and how many times in a row it was not evaluable,
 * is kept in memory and seeded from its stored evaluations the first time the target is seen, so a restart resumes
 * where the previous node left off instead of evaluating everything at once. A target the evaluator leaves out is
 * still counted as attempted: it is retried at its next slot, not at every tick.
 */
@RequiredArgsConstructor
@UseCase
public class EvaluateDuePerformanceTargetsUseCase {

    private final PerformanceTargetQueryService performanceTargetQueryService;
    private final PerformanceTargetEvaluationQueryService performanceTargetEvaluationQueryService;
    private final PerformanceTargetEvaluationCrudService performanceTargetEvaluationCrudService;
    private final PerformanceTargetEvaluator performanceTargetEvaluator;

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public Output execute(Input input) {
        var schedule = input.schedule();
        var now = TimeProvider.instantNow();
        var targets = performanceTargetQueryService.findAll();
        states.keySet().retainAll(targets.stream().map(PerformanceTarget::id).collect(toSet()));

        var due = targets
            .stream()
            .filter(target -> {
                var state = states.computeIfAbsent(target.id(), id -> seed(target, schedule));
                return schedule.isDue(target, state.lastEvaluatedAt(), state.consecutiveNotEvaluable(), now);
            })
            .toList();
        if (due.isEmpty()) {
            return new Output(targets.size(), List.of());
        }

        var evaluationsByTarget = performanceTargetEvaluator
            .evaluateAll(due, now)
            .stream()
            .collect(toMap(PerformanceTargetEvaluation::targetId, identity()));
        var stored = new ArrayList<PerformanceTargetEvaluation>();
        for (var target : due) {
            var previous = states.get(target.id());
            var evaluation = evaluationsByTarget.get(target.id());
            if (evaluation == null) {
                states.put(target.id(), new State(now, previous.consecutiveNotEvaluable()));
                continue;
            }
            var slotStart = schedule.slotStart(target, previous.consecutiveNotEvaluable(), now);
            var latest = evaluation.toBuilder().id(evaluationId(target, slotStart)).latest(true).build();
            performanceTargetEvaluationCrudService
                .createIfAbsent(latest)
                .ifPresent(created -> {
                    performanceTargetEvaluationCrudService.pruneHistory(target.id(), schedule.retention());
                    stored.add(created);
                });
            states.put(target.id(), previous.after(latest));
        }
        return new Output(targets.size(), stored);
    }

    /**
     * One id per target and slot, the same on every node: without a cluster manager every node believes it is the
     * primary and evaluates the same slot, and the store keeps only the first record.
     */
    private static String evaluationId(PerformanceTarget target, Instant slotStart) {
        return UuidString.generateForEnvironment(target.environmentId(), target.id(), String.valueOf(slotStart.getEpochSecond()));
    }

    private State seed(PerformanceTarget target, PerformanceTargetSchedule schedule) {
        var history = performanceTargetEvaluationQueryService
            .findByTargetId(target.id(), new PageableImpl(1, schedule.historyDepth(target)))
            .getContent();
        var state = new State(null, 0);
        for (var evaluation : history.reversed()) {
            state = state.after(evaluation);
        }
        return state;
    }

    /**
     * @param lastEvaluatedAt {@code null} when the target was never evaluated
     */
    private record State(Instant lastEvaluatedAt, int consecutiveNotEvaluable) {
        State after(PerformanceTargetEvaluation evaluation) {
            var notEvaluable = evaluation.status() == PerformanceTargetEvaluation.Status.NOT_EVALUABLE;
            return new State(evaluation.evaluatedAt(), notEvaluable ? consecutiveNotEvaluable + 1 : 0);
        }
    }

    public record Input(PerformanceTargetSchedule schedule) {}

    /**
     * @param targets     how many targets exist, due or not
     * @param evaluations the evaluations stored by this tick
     */
    public record Output(int targets, List<PerformanceTargetEvaluation> evaluations) {}
}
