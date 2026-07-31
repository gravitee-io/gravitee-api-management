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
import io.gravitee.repository.ratelimit.api.TokenBucketConsumeResult;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import io.reactivex.rxjava3.core.Single;
import java.util.function.Supplier;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Hazelcast-backed {@link TokenBucketRateLimitRepository}. Each operation is one
 * {@link IMap#submitToKey} call: Hazelcast routes the entry processor to the partition owner of the
 * key, runs it under the partition lock, and replicates the result to backup members.
 */
@CustomLog
@RequiredArgsConstructor
public final class HazelcastTokenBucketRateLimitRepository implements TokenBucketRateLimitRepository<TokenBucket> {

    private final IMap<String, TokenBucket> buckets;

    @Override
    public Single<TokenBucketConsumeResult> refillAndTryConsume(
        String key,
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long nowMillis,
        Supplier<TokenBucket> supplier
    ) {
        TokenBucket seed = supplier.get();
        return Single.fromCompletionStage(
            buckets.submitToKey(
                key,
                new RefillAndTryConsumeEntryProcessor(tokensRequested, refillRate, refillPeriodMillis, capacity, nowMillis, seed)
            )
        ).doOnError(t -> log.warn("Hazelcast token-bucket consume failed for key={}", key, t));
    }
}
