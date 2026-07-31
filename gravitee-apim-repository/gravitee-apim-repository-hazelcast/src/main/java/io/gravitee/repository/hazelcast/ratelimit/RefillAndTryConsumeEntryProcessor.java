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
import io.gravitee.repository.ratelimit.api.TokenBucketCalculator;
import io.gravitee.repository.ratelimit.api.TokenBucketConsumeResult;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Atomic token-bucket refill-and-consume running on the Hazelcast partition owner.
 *
 * <p>All time-dependent inputs ({@code nowMillis}) are captured by the caller (not read inside
 * {@link #process}) so the processor is deterministic — backup partition members re-run it with the
 * same inputs and reach the same state as the primary.
 */
public final class RefillAndTryConsumeEntryProcessor
    implements EntryProcessor<String, TokenBucket, TokenBucketConsumeResult>, Serializable {

    private static final long serialVersionUID = 1L;

    private final long tokensRequested;
    private final long refillRate;
    private final long refillPeriodMillis;
    private final long capacity;
    private final long nowMillis;
    private final TokenBucket seed;

    public RefillAndTryConsumeEntryProcessor(
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long nowMillis,
        TokenBucket seed
    ) {
        this.tokensRequested = tokensRequested;
        this.refillRate = refillRate;
        this.refillPeriodMillis = refillPeriodMillis;
        this.capacity = capacity;
        this.nowMillis = nowMillis;
        this.seed = Objects.requireNonNull(seed, "seed");
    }

    @Override
    public TokenBucketConsumeResult process(Map.Entry<String, TokenBucket> entry) {
        TokenBucket current = entry.getValue();
        if (current == null) {
            // copy ctor; never mutate the seed (the processor may be re-run on backup members)
            current = TokenBucketCalculator.newFullBucket(new TokenBucket(seed), capacity, nowMillis);
        }

        TokenBucketConsumeResult result = TokenBucketCalculator.refillAndTryConsume(
            current,
            tokensRequested,
            refillRate,
            refillPeriodMillis,
            capacity,
            nowMillis
        );

        // Evict once the bucket would have refilled to full anyway: a long-idle entry is then recreated
        // full on next access, equivalent to keeping it. Shared TTL with the other backends; a zero rate
        // yields an effectively-forever TTL so a never-refilling bucket persists.
        long ttlMs = TokenBucketCalculator.ttlMillis(refillRate, refillPeriodMillis, capacity);
        ((ExtendedMapEntry<String, TokenBucket>) entry).setValue(current, ttlMs, TimeUnit.MILLISECONDS);
        return result;
    }
}
