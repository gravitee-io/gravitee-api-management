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

import com.hazelcast.map.IMap;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.rxjava3.core.Single;
import java.util.function.Supplier;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Hazelcast-backed implementation of {@link RateLimitRepository}. Each increment is one
 * {@link IMap#submitToKey} call: Hazelcast routes the {@link IncrementAndGetEntryProcessor} to
 * the partition owner of the key, runs it under the partition lock, replicates the result to
 * backup members, and returns asynchronously.
 *
 * <p>The repository propagates HZ failures via {@code Single.onError}; the calling rate-limit
 * policy decides whether to fail-open (allow) or fail-closed (reject) via its own configuration.
 */
@CustomLog
@RequiredArgsConstructor
public final class HazelcastRateLimitRepository implements RateLimitRepository<RateLimit> {

    private final IMap<String, RateLimit> counters;

    /**
     * Atomically increment the rate-limit counter for {@code key} by {@code weight} and return
     * the resulting {@link RateLimit}.
     *
     * <p>The {@code supplier} is invoked exactly once per call (before submission) to materialise
     * the seed entry that will be used if no live entry exists for the key.
     */
    @Override
    public Single<RateLimit> incrementAndGet(String key, long weight, Supplier<RateLimit> supplier) {
        RateLimit defaultIfAbsent = supplier.get();
        long now = System.currentTimeMillis();
        return Single.fromCompletionStage(
            counters.submitToKey(key, new IncrementAndGetEntryProcessor(weight, defaultIfAbsent, now))
        ).doOnError(t -> log.warn("Hazelcast rate-limit increment failed for key={}", key, t));
    }
}
