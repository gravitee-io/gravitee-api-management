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
import java.time.Duration;

/**
 * The token-bucket algorithm, expressed as pure functions over {@link TokenBucket} state and a
 * caller-supplied clock. This is the canonical reference the persistent backends must match: the
 * Java backends (in-memory, JDBC, Hazelcast) reuse it directly, while Mongo/Redis re-express the
 * same integer math in their own server-side language.
 *
 * <p>The rate is expressed as {@code refillRate} whole tokens per {@code refillPeriodMillis}
 * milliseconds (e.g. 100 tokens / 60000 ms). Balances are whole tokens and every operation is integer
 * arithmetic, so there is no floating-point drift. Tokens accrue in whole units: the refill anchor is
 * advanced to {@code now} only once at least one whole token has been credited; until then the elapsed
 * time keeps accumulating against the unchanged anchor, so no accrual is silently discarded. This can
 * never over-admit; under traffic that is sparse and not aligned to the refill period it may be at
 * most one in-flight token stricter than a continuous-accrual model.
 *
 * @author GraviteeSource Team
 */
public final class TokenBucketCalculator {

    /** Lower bound on a bucket's TTL so a high-rate/low-capacity bucket is not evicted within seconds of use. */
    private static final long MIN_TTL_MS = 10_000L;

    /** A zero-rate (never-refilling) bucket must persist to keep enforcing; this is "effectively forever". */
    private static final long ZERO_RATE_TTL_MS = Duration.ofDays(3650).toMillis();

    private TokenBucketCalculator() {}

    /**
     * Create a bucket that starts <em>full</em> (at {@code capacity}) as of {@code nowMillis}.
     *
     * @param seed     the seed bucket carrying identity (key, subscription)
     * @param capacity burst capacity, in whole tokens
     * @param nowMillis current time, in epoch millis
     * @return the same instance, with tokens set to capacity and refill anchored at {@code nowMillis}
     */
    public static TokenBucket newFullBucket(TokenBucket seed, long capacity, long nowMillis) {
        seed.setTokens(capacity);
        seed.setLastRefillTime(nowMillis);
        return seed;
    }

    /**
     * Refill {@code bucket} based on time elapsed since its last refill (capped at {@code capacity}),
     * then try to consume {@code tokensRequested} tokens. Mutates {@code bucket} in place.
     *
     * @param refillRate         whole tokens added per {@code refillPeriodMillis} ({@code <= 0} disables refill)
     * @param refillPeriodMillis the refill period, in milliseconds (must be {@code > 0})
     * @return the outcome: whether the request was allowed, remaining whole tokens, and when the next token is due
     * @throws IllegalArgumentException if {@code refillPeriodMillis <= 0}, {@code tokensRequested < 0} or {@code capacity < 0}
     */
    public static TokenBucketConsumeResult refillAndTryConsume(
        TokenBucket bucket,
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long nowMillis
    ) {
        requireValidArgs(tokensRequested, refillPeriodMillis, capacity);

        long elapsed = nowMillis - bucket.getLastRefillTime();
        if (elapsed > 0 && refillRate > 0) {
            // Whole tokens accrued = floor(elapsed * refillRate / refillPeriodMillis). Cap the elapsed
            // span to the time that would refill the whole bucket, so elapsed * refillRate cannot overflow
            // on extreme idle gaps. This assumes capacity * refillPeriodMillis itself fits in a long (true
            // for any sane capacity/period); beyond that the cap wraps and refill degrades toward 0 — which
            // is fail-closed.
            long maxUsefulElapsed = (capacity * refillPeriodMillis) / refillRate + refillPeriodMillis;
            long effectiveElapsed = Math.min(elapsed, maxUsefulElapsed);
            long refill = (effectiveElapsed * refillRate) / refillPeriodMillis;
            if (refill > 0) {
                bucket.setTokens(Math.min(capacity, bucket.getTokens() + refill));
                // Anchor forward to now only once a whole token is credited; until then the elapsed time
                // keeps accumulating against the unchanged anchor, so no accrual is lost. Forward-only:
                // out-of-order (elapsed <= 0) leaves the anchor put, and a later request cannot re-accrue
                // this window (which would over-admit).
                bucket.setLastRefillTime(nowMillis);
            }
        }

        boolean allowed = false;
        if (bucket.getTokens() >= tokensRequested) {
            bucket.setTokens(bucket.getTokens() - tokensRequested);
            allowed = true;
        }

        long tokensAfter = bucket.getTokens();
        return new TokenBucketConsumeResult(
            allowed,
            tokensAfter,
            nextAvailableAtMillis(tokensAfter, refillRate, refillPeriodMillis, nowMillis)
        );
    }

    /**
     * When at least one whole token is available, the next token is available "now"; otherwise it is
     * one refill interval — {@code ceil(refillPeriodMillis / refillRate)} — from now.
     *
     * <p>Public so backends that compute the new balance server-side (e.g. MongoDB's update pipeline)
     * can derive this result field consistently with the Java path.
     */
    public static long nextAvailableAtMillis(long tokensAfter, long refillRate, long refillPeriodMillis, long nowMillis) {
        if (tokensAfter >= 1) {
            return nowMillis;
        }
        if (refillRate <= 0) {
            return Long.MAX_VALUE;
        }
        // ceil(refillPeriodMillis / refillRate): the time to accrue one whole token.
        long waitMillis = (refillPeriodMillis + refillRate - 1) / refillRate;
        return nowMillis + waitMillis;
    }

    /**
     * Validate the per-request arguments shared by every backend, so all of them fail the same way on a
     * contract violation instead of dividing by zero (Mongo/Redis) or letting a negative request inflate
     * the balance above capacity (which would corrupt the bucket and over-admit afterwards).
     *
     * @throws IllegalArgumentException if {@code refillPeriodMillis <= 0}, {@code tokensRequested < 0} or {@code capacity < 0}
     */
    public static void requireValidArgs(long tokensRequested, long refillPeriodMillis, long capacity) {
        if (refillPeriodMillis <= 0) {
            throw new IllegalArgumentException("refillPeriodMillis must be > 0, was " + refillPeriodMillis);
        }
        if (tokensRequested < 0 || capacity < 0) {
            throw new IllegalArgumentException("tokensRequested and capacity must be >= 0, were " + tokensRequested + "/" + capacity);
        }
    }

    /**
     * TTL for a stored bucket: the time to refill the whole capacity, floored at {@value #MIN_TTL_MS} ms.
     * An idle bucket that would have refilled to full is equivalent to a fresh (full) one, so evicting it
     * is safe and bounds storage to roughly the active keys. A zero rate never refills, so it must persist.
     * Shared by every backend so eviction is uniform.
     */
    public static long ttlMillis(long refillRate, long refillPeriodMillis, long capacity) {
        if (refillRate <= 0) {
            return ZERO_RATE_TTL_MS;
        }
        return Math.max(MIN_TTL_MS, (capacity * refillPeriodMillis) / refillRate);
    }
}
