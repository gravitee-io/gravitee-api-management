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
package io.gravitee.repository.hazelcast.ratelimit;

import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.ExtendedMapEntry;
import io.gravitee.repository.ratelimit.model.RateLimit;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Atomic rate-limit counter increment running on the Hazelcast partition owner.
 *
 * <p>{@code now} is captured by the caller (not read inside {@link #process})
 * so the processor is deterministic — backup partition members re-run it with
 * the same inputs and reach the same final value as the primary.
 */
public final class IncrementAndGetEntryProcessor implements EntryProcessor<String, RateLimit, RateLimit>, Serializable {

    private static final long serialVersionUID = 1L;

    private final long weight;
    private final RateLimit defaultIfAbsent;
    private final long now;

    public IncrementAndGetEntryProcessor(long weight, RateLimit defaultIfAbsent, long now) {
        this.weight = weight;
        this.defaultIfAbsent = Objects.requireNonNull(defaultIfAbsent, "defaultIfAbsent");
        this.now = now;
    }

    @Override
    public RateLimit process(Map.Entry<String, RateLimit> entry) {
        RateLimit current = entry.getValue();
        if (current == null || current.getResetTime() < now) {
            current = new RateLimit(defaultIfAbsent); // copy ctor; never mutate the seed
        }
        current.setCounter(current.getCounter() + weight);
        // Hazelcast treats a TTL <= 0 as "never expire". Floor at 1ms so a stale or zero
        // resetTime (e.g. healthcheck probes that don't set one) evicts immediately
        // instead of pinning the entry forever.
        long ttlMs = Math.max(1L, current.getResetTime() - now);
        ((ExtendedMapEntry<String, RateLimit>) entry).setValue(current, ttlMs, TimeUnit.MILLISECONDS);
        return current;
    }
}
