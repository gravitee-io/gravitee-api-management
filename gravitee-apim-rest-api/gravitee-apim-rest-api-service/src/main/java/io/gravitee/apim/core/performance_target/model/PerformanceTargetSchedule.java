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

import java.time.Duration;
import java.time.Instant;

/**
 * When a target is due. Time is cut into slots of the target's interval, aligned on the epoch: the slot an evaluation
 * belongs to is the same for the scheduler, for an evaluation run on demand, and for a timeline drawn against the
 * clock, so one evaluation per slot reads as an unbroken strip. A target is due once in every slot: at the first tick
 * on or after its phase in the slot, unless something already evaluated it in that slot.
 *
 * <p>The phase is a stable hash of the target's id, spread across the declared interval and capped so a tick still
 * falls in the slot: targets sharing an interval longer than the tick are evaluated on different ticks, and a restart
 * or a bulk import does not make them all due at once for long. For an interval no longer than the tick the phase is
 * the slot's start, which is the only tick the slot has.
 *
 * <p>An idle target backs off: from {@code backoffAfter} consecutive NOT_EVALUABLE evaluations on, every further
 * miss doubles the interval it is judged on, up to {@code backoffCap}; the first evaluable window brings it back to
 * the declared interval.
 *
 * @param tick      how often the scheduler runs
 * @param retention evaluations kept per target after each evaluation
 */
public record PerformanceTargetSchedule(int backoffAfter, Duration backoffCap, int retention, Duration tick) {
    public static final Duration DEFAULT_TICK = Duration.ofMinutes(1);

    public PerformanceTargetSchedule {
        if (backoffAfter < 1) {
            throw new IllegalArgumentException("The backoff threshold must be at least 1 evaluation");
        }
        if (backoffCap == null || !backoffCap.isPositive()) {
            throw new IllegalArgumentException("The backoff cap must be a positive duration");
        }
        if (retention < 1) {
            throw new IllegalArgumentException("The retention must keep at least 1 evaluation");
        }
        if (tick == null || !tick.isPositive()) {
            throw new IllegalArgumentException("The tick must be a positive duration");
        }
    }

    /** A schedule ticking every minute, which is what the service does unless configured otherwise. */
    public PerformanceTargetSchedule(int backoffAfter, Duration backoffCap, int retention) {
        this(backoffAfter, backoffCap, retention, DEFAULT_TICK);
    }

    public Duration effectiveInterval(PerformanceTarget target, int consecutiveNotEvaluable) {
        var interval = target.interval();
        var cap = backoffCap.compareTo(interval) > 0 ? backoffCap : interval;
        for (var misses = consecutiveNotEvaluable; misses >= backoffAfter && interval.compareTo(cap) < 0; misses--) {
            interval = interval.multipliedBy(2);
        }
        return interval.compareTo(cap) > 0 ? cap : interval;
    }

    /** Where in its declared interval the target is evaluated: stable, and spread across targets. */
    public Duration jitter(PerformanceTarget target) {
        return Duration.ofSeconds(Math.floorMod(target.id().hashCode(), Math.max(1, target.interval().toSeconds())));
    }

    /**
     * @param lastEvaluatedAt {@code null} when the target was never evaluated, which makes it due at once
     */
    public boolean isDue(PerformanceTarget target, Instant lastEvaluatedAt, int consecutiveNotEvaluable, Instant now) {
        if (lastEvaluatedAt == null) {
            return true;
        }
        var slotStart = slotStart(target, consecutiveNotEvaluable, now);
        return lastEvaluatedAt.isBefore(slotStart) && !now.isBefore(dueAt(target, consecutiveNotEvaluable, slotStart));
    }

    /** The start of the slot {@code now} falls in: aligned on the epoch, so every node and every reader cuts time alike. */
    public Instant slotStart(PerformanceTarget target, int consecutiveNotEvaluable, Instant now) {
        var interval = Math.max(1, effectiveInterval(target, consecutiveNotEvaluable).toSeconds());
        return Instant.ofEpochSecond(Math.floorDiv(now.getEpochSecond(), interval) * interval);
    }

    /**
     * When in a slot the target becomes due: its phase into the slot, held back so that at least one tick of the
     * scheduler falls on or after it before the slot ends.
     */
    public Instant dueAt(PerformanceTarget target, int consecutiveNotEvaluable, Instant slotStart) {
        var room = effectiveInterval(target, consecutiveNotEvaluable).minus(tick);
        var phase = room.isNegative() ? Duration.ZERO : min(jitter(target), room);
        return slotStart.plus(phase);
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    /**
     * How many of the latest evaluations tell the effective interval of a target: past this many consecutive misses
     * the interval is capped, so older history changes nothing.
     */
    public int historyDepth(PerformanceTarget target) {
        var depth = backoffAfter;
        while (effectiveInterval(target, depth).compareTo(effectiveInterval(target, depth + 1)) < 0) {
            depth++;
        }
        return depth;
    }
}
