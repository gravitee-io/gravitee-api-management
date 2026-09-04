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
 * When a target is due. Each target owns the slots {@code k * interval + jitter} of the epoch, where the jitter is a
 * stable hash of its id spread across its declared interval: a target is due once a slot has started since its last
 * evaluation. Targets are so evaluated exactly once per interval, at phases spread over the ticks, and a restart or a
 * bulk import does not make them all due at once for long.
 *
 * <p>An idle target backs off: from {@code backoffAfter} consecutive NOT_EVALUABLE evaluations on, every further
 * miss doubles the interval it is judged on, up to {@code backoffCap}; the first evaluable window brings it back to
 * the declared interval.
 *
 * @param retention evaluations kept per target after each evaluation
 */
public record PerformanceTargetSchedule(int backoffAfter, Duration backoffCap, int retention) {
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
    }

    public Duration effectiveInterval(PerformanceTarget target, int consecutiveNotEvaluable) {
        var interval = target.interval();
        var cap = backoffCap.compareTo(interval) > 0 ? backoffCap : interval;
        for (var misses = consecutiveNotEvaluable; misses >= backoffAfter && interval.compareTo(cap) < 0; misses--) {
            interval = interval.multipliedBy(2);
        }
        return interval.compareTo(cap) > 0 ? cap : interval;
    }

    public Duration jitter(PerformanceTarget target) {
        return Duration.ofSeconds(Math.floorMod(target.id().hashCode(), Math.max(1, target.interval().toSeconds())));
    }

    /**
     * @param lastEvaluatedAt {@code null} when the target was never evaluated, which makes it due at once
     */
    public boolean isDue(PerformanceTarget target, Instant lastEvaluatedAt, int consecutiveNotEvaluable, Instant now) {
        return lastEvaluatedAt == null || lastEvaluatedAt.isBefore(slotStart(target, consecutiveNotEvaluable, now));
    }

    /** The start of the target's slot {@code now} falls in; every node computes the same one for the same tick. */
    public Instant slotStart(PerformanceTarget target, int consecutiveNotEvaluable, Instant now) {
        var interval = Math.max(1, effectiveInterval(target, consecutiveNotEvaluable).toSeconds());
        var jitter = jitter(target).toSeconds();
        return Instant.ofEpochSecond(Math.floorDiv(now.getEpochSecond() - jitter, interval) * interval + jitter);
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
