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
package io.gravitee.repository.ratelimit;

import io.gravitee.repository.ratelimit.api.TokenBucketCalculator;
import io.gravitee.repository.ratelimit.api.TokenBucketConsumeResult;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * In-memory reference implementation of {@link TokenBucketRateLimitRepository}, used as the
 * executable specification that the persistent backends must match. Atomicity per key is provided
 * by {@link ConcurrentMap#compute}, which runs its remapping function atomically.
 */
public class InMemoryTokenBucketRateLimitRepository implements TokenBucketRateLimitRepository<TokenBucket> {

    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

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
        return Single.fromCallable(() -> {
            TokenBucketConsumeResult[] result = new TokenBucketConsumeResult[1];
            buckets.compute(key, (k, existing) -> {
                TokenBucket bucket = existing != null ? existing : TokenBucketCalculator.newFullBucket(supplier.get(), capacity, nowMillis);
                result[0] = TokenBucketCalculator.refillAndTryConsume(
                    bucket,
                    tokensRequested,
                    refillRate,
                    refillPeriodMillis,
                    capacity,
                    nowMillis
                );
                return bucket;
            });
            return result[0];
        });
    }
}
