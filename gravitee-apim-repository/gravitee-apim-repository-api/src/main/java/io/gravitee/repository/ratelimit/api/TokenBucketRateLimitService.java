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
package io.gravitee.repository.ratelimit.api;

import io.gravitee.repository.ratelimit.model.TokenBucket;
import io.reactivex.rxjava3.core.Single;
import java.util.function.Supplier;

/**
 * Gateway-facing facade over the token-bucket store, mirroring {@link RateLimitService}. The
 * {@code async} flag selects the enforcement mode for a single call:
 *
 * <ul>
 *   <li>{@code async = false} (default): strict, exact. The call goes straight to the backing
 *       {@link TokenBucketRateLimitRepository}, which atomically refills and consumes per request.</li>
 *   <li>{@code async = true}: non-strict, eventually consistent. The node keeps a local bucket and
 *       reconciles it to the store periodically, trading exactness for far fewer store round-trips
 *       and higher throughput. The distributed bucket is approximate, so a backend may receive more
 *       than the configured rate.</li>
 * </ul>
 *
 * <p>The {@link TokenBucketConsumeResult} returned is the same regardless of mode, so a policy does
 * not need to know which mode is in effect.
 *
 * @author GraviteeSource Team
 */
public interface TokenBucketRateLimitService {
    /**
     * Refill and try to consume {@code tokensRequested} tokens for {@code key}, in the mode selected
     * by {@code async}. Parameters carry the same meaning as
     * {@link TokenBucketRateLimitRepository#refillAndTryConsume}.
     *
     * @param async {@code true} for the non-strict local-then-reconcile path, {@code false} for the
     *              strict per-request path
     */
    Single<TokenBucketConsumeResult> refillAndTryConsume(
        String key,
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long nowMillis,
        boolean async,
        Supplier<TokenBucket> supplier
    );

    default Single<TokenBucketConsumeResult> refillAndTryConsume(
        String key,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long nowMillis,
        boolean async,
        Supplier<TokenBucket> supplier
    ) {
        return refillAndTryConsume(key, 1, refillRate, refillPeriodMillis, capacity, nowMillis, async, supplier);
    }
}
