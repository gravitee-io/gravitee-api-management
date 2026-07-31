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
 * Storage contract for token-bucket (burst) rate limiting.
 *
 * <p>Separate from {@link RateLimitRepository}, whose fixed-window {@code incrementAndGet} cannot
 * express token-bucket semantics (it always increments, even for rejected requests, and has no
 * notion of time-based refill capped at a burst capacity).
 *
 * @author GraviteeSource Team
 */
public interface TokenBucketRateLimitRepository<T extends TokenBucket> {
    /**
     * Atomically refill the bucket based on elapsed time, then try to consume {@code tokensRequested}
     * tokens. The bucket is created <em>full</em> (at {@code capacity}) on first use.
     *
     * @param key                the bucket key
     * @param tokensRequested    whole tokens to consume (typically 1)
     * @param refillRate         whole tokens added per refill period ({@code <= 0} disables refill)
     * @param refillPeriodMillis the refill period, in milliseconds (must be {@code > 0})
     * @param capacity           burst capacity — the maximum number of accumulated tokens
     * @param nowMillis          caller-supplied current time, in epoch millis (the authoritative clock)
     * @param supplier           supplies the seed bucket (key, subscription) when none exists yet
     * @return the outcome: whether the request was allowed, remaining tokens, and when the next token is due;
     *         or a {@code null} reference (not an empty or error {@code Single}) when rate limiting is
     *         <em>disabled</em> (the no-op backend), which callers must treat as pass-through — mirrors
     *         {@link RateLimitRepository#incrementAndGet}
     * @throws IllegalArgumentException if {@code refillPeriodMillis <= 0}, {@code tokensRequested < 0} or {@code capacity < 0}
     */
    Single<TokenBucketConsumeResult> refillAndTryConsume(
        String key,
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long nowMillis,
        Supplier<T> supplier
    );
}
