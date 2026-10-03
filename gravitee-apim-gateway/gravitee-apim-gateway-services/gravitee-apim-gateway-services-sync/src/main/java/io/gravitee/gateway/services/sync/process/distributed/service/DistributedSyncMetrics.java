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
package io.gravitee.gateway.services.sync.process.distributed.service;

import io.gravitee.repository.distributedsync.model.DistributedEventType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counters describing the health of the distributed sync process on this node, exposed through the
 * {@code /sync} endpoint so that a cycle which "succeeded" while skipping items is visible.
 *
 * <ul>
 *   <li><b>dead letters</b>: entities the primary could not turn into a distributed event (deterministic
 *   failure, never retried);</li>
 *   <li><b>decode failures</b>: distributed events a secondary could not turn back into an entity;</li>
 *   <li><b>distribution failures</b>: writes to the distributed sync repository that failed after retries
 *   (the sync window is replayed).</li>
 * </ul>
 */
public class DistributedSyncMetrics {

    private static final int MAX_REPORTED_FAILURES = 10_000;

    private final AtomicLong deadLetters = new AtomicLong();
    private final AtomicLong decodeFailures = new AtomicLong();
    private final AtomicLong distributionFailures = new AtomicLong();
    private final AtomicLong replayedCycles = new AtomicLong();
    private final AtomicLong heldWindows = new AtomicLong();
    private final AtomicLong staleEntriesRemoved = new AtomicLong();
    private final AtomicLong skippedStaleEntriesRemovals = new AtomicLong();
    private final Map<DistributedEventType, AtomicLong> deadLettersByType = Collections.synchronizedMap(
        new EnumMap<>(DistributedEventType.class)
    );
    private final Map<DistributedEventType, AtomicLong> decodeFailuresByType = Collections.synchronizedMap(
        new EnumMap<>(DistributedEventType.class)
    );
    private final Map<String, Boolean> reportedFailures = Collections.synchronizedMap(
        new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<String, Boolean> eldest) {
                return size() > MAX_REPORTED_FAILURES;
            }
        }
    );

    /**
     * @return {@code true} the first time this exact failure is reported, so callers log it at error level
     * once instead of on every overlapping sync window.
     */
    public boolean recordDeadLetter(final DistributedEventType type, final String id) {
        deadLetters.incrementAndGet();
        increment(deadLettersByType, type);
        return firstReport("dead:" + type + ":" + id);
    }

    /**
     * @return {@code true} the first time this exact event version fails to decode.
     */
    public boolean recordDecodeFailure(final DistributedEventType type, final String id, final Object version) {
        decodeFailures.incrementAndGet();
        increment(decodeFailuresByType, type);
        return firstReport("decode:" + type + ":" + id + ":" + version);
    }

    public void recordDistributionFailure() {
        distributionFailures.incrementAndGet();
    }

    public void recordReplayedCycle() {
        replayedCycles.incrementAndGet();
    }

    public void recordHeldWindow() {
        heldWindows.incrementAndGet();
    }

    public void recordStaleEntriesRemoved(final long count) {
        staleEntriesRemoved.addAndGet(count);
    }

    public void recordSkippedStaleEntriesRemoval() {
        skippedStaleEntriesRemovals.incrementAndGet();
    }

    public long staleEntriesRemoved() {
        return staleEntriesRemoved.get();
    }

    public long skippedStaleEntriesRemovals() {
        return skippedStaleEntriesRemovals.get();
    }

    public long deadLetters() {
        return deadLetters.get();
    }

    public long decodeFailures() {
        return decodeFailures.get();
    }

    public long distributionFailures() {
        return distributionFailures.get();
    }

    public long replayedCycles() {
        return replayedCycles.get();
    }

    public long heldWindows() {
        return heldWindows.get();
    }

    public Map<DistributedEventType, Long> deadLettersByType() {
        return snapshot(deadLettersByType);
    }

    public Map<DistributedEventType, Long> decodeFailuresByType() {
        return snapshot(decodeFailuresByType);
    }

    private boolean firstReport(final String key) {
        return reportedFailures.putIfAbsent(key, Boolean.TRUE) == null;
    }

    private static void increment(final Map<DistributedEventType, AtomicLong> counters, final DistributedEventType type) {
        if (type != null) {
            counters.computeIfAbsent(type, t -> new AtomicLong()).incrementAndGet();
        }
    }

    private static Map<DistributedEventType, Long> snapshot(final Map<DistributedEventType, AtomicLong> counters) {
        Map<DistributedEventType, Long> copy = new EnumMap<>(DistributedEventType.class);
        synchronized (counters) {
            counters.forEach((type, count) -> copy.put(type, count.get()));
        }
        return copy;
    }
}
