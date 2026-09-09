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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * What the scheduler knows about each target on this node: when it was last evaluated and how many times in a row it
 * was not evaluable, which is what its backoff is judged on (see {@code PerformanceTargetSchedule}).
 *
 * <p>Kept here rather than inside the scheduled use case so that every path that produces or invalidates an
 * evaluation can update it: an on-demand evaluation that finds traffic ends a backoff on the spot instead of at the
 * next backed-off slot, and an updated target starts a fresh schedule instead of inheriting the backoff of the
 * definition it replaced.
 *
 * <p>Each node holds its own map, and only the primary schedules, while an on-demand evaluation or an update is served
 * by whichever node answers the request. What those nodes did reaches the primary through the store: before judging a
 * target the scheduler {@link #reconcile reconciles} its memory with the stored latest evaluation and the target's
 * update time.
 */
@DomainService
public class PerformanceTargetScheduleStateDomainService {

    private final Map<String, State> states = new ConcurrentHashMap<>();

    /** The state of a target, seeding it when this node has not seen the target yet. */
    public State stateOf(String targetId, Supplier<State> seed) {
        return states.computeIfAbsent(targetId, id -> seed.get());
    }

    public Optional<State> current(String targetId) {
        return Optional.ofNullable(states.get(targetId));
    }

    /** An evaluation was stored for the target, by the scheduler or on demand. */
    public void record(String targetId, PerformanceTargetEvaluation evaluation) {
        states.compute(targetId, (id, state) -> (state == null ? State.FRESH : state).after(evaluation));
    }

    /** The scheduler tried but the evaluator left the target out: it is retried at its next slot, not at every tick. */
    public void attempted(String targetId, Instant now) {
        states.computeIfPresent(targetId, (id, state) -> new State(now, state.consecutiveNotEvaluable()));
    }

    /** The target was redefined: its next slot is due at once and its backoff is forgotten. */
    public void reset(String targetId) {
        states.put(targetId, State.FRESH);
    }

    /**
     * Folds in what this node did not see happen. A target redefined since this node's last evaluation of it starts
     * a fresh schedule, as {@link #reset} does on the node that saved it. An evaluation stored since this node's last
     * one, by another node's on-demand run or by a previous primary, moves the state as if recorded here, so a backoff
     * ends where the traffic was found and not at the next backed-off slot. Evaluations that predate a redefinition
     * belong to the definition it replaced and are ignored.
     *
     * @param redefinedAt  when the target was last updated, {@code null} when unknown
     * @param storedLatest the target's latest stored evaluation, {@code null} when it has none
     * @return the reconciled state, which is now this node's state for the target
     */
    public State reconcile(String targetId, Instant redefinedAt, PerformanceTargetEvaluation storedLatest) {
        return states.compute(targetId, (id, known) -> {
            var state = known == null ? State.FRESH : known;
            if (redefinedAt != null && state.lastEvaluatedAt() != null && redefinedAt.isAfter(state.lastEvaluatedAt())) {
                state = State.FRESH;
            }
            if (storedLatest != null && isNewerThan(storedLatest, state) && !predates(storedLatest, redefinedAt)) {
                state = state.after(storedLatest);
            }
            return state;
        });
    }

    private static boolean isNewerThan(PerformanceTargetEvaluation evaluation, State state) {
        return state.lastEvaluatedAt() == null || evaluation.evaluatedAt().isAfter(state.lastEvaluatedAt());
    }

    private static boolean predates(PerformanceTargetEvaluation evaluation, Instant redefinedAt) {
        return redefinedAt != null && evaluation.evaluatedAt().isBefore(redefinedAt);
    }

    /** Forgets every target not in the set, so a deleted target does not linger. */
    public void retain(Set<String> targetIds) {
        states.keySet().retainAll(targetIds);
    }

    /**
     * @param lastEvaluatedAt {@code null} when the target was never evaluated
     */
    public record State(Instant lastEvaluatedAt, int consecutiveNotEvaluable) {
        public static final State FRESH = new State(null, 0);

        public State after(PerformanceTargetEvaluation evaluation) {
            var notEvaluable = evaluation.status() == PerformanceTargetEvaluation.Status.NOT_EVALUABLE;
            return new State(evaluation.evaluatedAt(), notEvaluable ? consecutiveNotEvaluable + 1 : 0);
        }
    }
}
