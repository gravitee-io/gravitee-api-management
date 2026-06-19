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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.ratelimit.api.TokenBucketConsumeResult;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Behavioural contract every {@link TokenBucketRateLimitRepository} implementation must satisfy.
 *
 * <p>The rate is expressed as {@code refillRate} whole tokens per {@code refillPeriodMillis} (e.g.
 * {@code (2, 1000)} = 2 tokens/s, {@code (1, 2000)} = 1 token every 2s). All timing is driven by an
 * explicit {@code nowMillis} argument rather than a wall clock, so the contract is fully deterministic
 * across the in-memory reference and the persistent backends. Backends provide their implementation by
 * overriding {@link #createRepository()}.
 *
 * <p><strong>Entry TTL is intentionally out of scope for this suite.</strong> {@link
 * io.gravitee.repository.ratelimit.api.TokenBucketCalculator#ttlMillis} defines the shared eviction rule,
 * but expiry is storage-native (Mongo TTL index, Redis {@code PEXPIRE}, a JDBC reaper, Hazelcast entry TTL)
 * and cannot be meaningfully asserted against an in-memory map. TTL conformance is therefore verified
 * per-backend in APIM-14483, not here.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class AbstractTokenBucketRateLimitRepositoryContractTest {

    protected abstract TokenBucketRateLimitRepository<TokenBucket> createRepository();

    private TokenBucketRateLimitRepository<TokenBucket> repository;

    @BeforeEach
    void setUp() {
        repository = createRepository();
    }

    private static Supplier<TokenBucket> seed(String key) {
        return () -> {
            TokenBucket bucket = new TokenBucket(key);
            bucket.setSubscription("sub-1");
            return bucket;
        };
    }

    @Test
    void fresh_bucket_is_full_so_first_request_is_allowed() {
        TokenBucketConsumeResult result = repository.refillAndTryConsume("k1", 1, 3, 1_000L, 300, 1_000L, seed("k1")).blockingGet();

        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(299);
    }

    @Test
    void empty_bucket_rejects_further_requests() {
        Supplier<TokenBucket> seed = seed("k2");
        // capacity 2 at a fixed instant (no refill): drain both tokens, then the third must be rejected
        repository.refillAndTryConsume("k2", 1, 1, 1_000L, 2, 1_000L, seed).blockingGet();
        repository.refillAndTryConsume("k2", 1, 1, 1_000L, 2, 1_000L, seed).blockingGet();

        TokenBucketConsumeResult result = repository.refillAndTryConsume("k2", 1, 1, 1_000L, 2, 1_000L, seed).blockingGet();

        assertThat(result.allowed()).isFalse();
        assertThat(result.remainingTokens()).isZero();
    }

    @Test
    void tokens_refill_over_elapsed_time() {
        Supplier<TokenBucket> seed = seed("k3");
        // capacity 10, 2 tokens per 1000ms; drain to empty at t=1000ms
        for (int i = 0; i < 10; i++) {
            repository.refillAndTryConsume("k3", 1, 2, 1_000L, 10, 1_000L, seed).blockingGet();
        }

        // 1000ms later, 2 tokens have refilled: this request is allowed and leaves 1
        TokenBucketConsumeResult result = repository.refillAndTryConsume("k3", 1, 2, 1_000L, 10, 2_000L, seed).blockingGet();

        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(1);
    }

    @Test
    void refill_never_exceeds_capacity() {
        Supplier<TokenBucket> seed = seed("k4");
        // capacity 5, very high rate (1000 tokens per 1000ms); consume 1 at t=0 (5 -> 4)
        repository.refillAndTryConsume("k4", 1, 1000, 1_000L, 5, 0L, seed).blockingGet();

        // after a huge idle period an uncapped bucket would massively overflow; it must cap at 5,
        // so consuming 1 leaves 4 (not thousands)
        TokenBucketConsumeResult result = repository.refillAndTryConsume("k4", 1, 1000, 1_000L, 5, 3_600_000L, seed).blockingGet();

        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(4);
    }

    @Test
    void each_token_takes_a_full_refill_period() {
        Supplier<TokenBucket> seed = seed("k8");
        // capacity 1, 1 token per 2000ms: after draining, a token is due exactly one period later.
        repository.refillAndTryConsume("k8", 1, 1, 2_000L, 1, 0L, seed).blockingGet(); // drain the single token

        // 1999ms is one millisecond short of a whole token — still rejected; the elapsed time is kept,
        // not discarded, so the very next millisecond tips it over.
        boolean allowedAt1999 = repository.refillAndTryConsume("k8", 1, 1, 2_000L, 1, 1_999L, seed).blockingGet().allowed();
        boolean allowedAt2000 = repository.refillAndTryConsume("k8", 1, 1, 2_000L, 1, 2_000L, seed).blockingGet().allowed();

        assertThat(allowedAt1999).isFalse();
        assertThat(allowedAt2000).isTrue();
    }

    @Test
    void rate_above_one_refills_multiple_tokens_per_period() {
        Supplier<TokenBucket> seed = seed("k9");
        // capacity 10, 5 tokens per 1000ms; drain to empty at t=0
        for (int i = 0; i < 10; i++) {
            repository.refillAndTryConsume("k9", 1, 5, 1_000L, 10, 0L, seed).blockingGet();
        }

        // one full period later exactly 5 tokens have refilled: consume 1 and 4 remain (never more)
        TokenBucketConsumeResult result = repository.refillAndTryConsume("k9", 1, 5, 1_000L, 10, 1_000L, seed).blockingGet();

        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(4);
    }

    @Test
    void reports_when_next_token_becomes_available_once_empty() {
        Supplier<TokenBucket> seed = seed("k5");
        // capacity 1, 2 tokens per 1000ms (one token every 500ms); consume the only token at t=1000
        repository.refillAndTryConsume("k5", 1, 2, 1_000L, 1, 1_000L, seed).blockingGet();

        // a second request at the same instant is rejected; the next token is due 500ms later
        TokenBucketConsumeResult result = repository.refillAndTryConsume("k5", 1, 2, 1_000L, 1, 1_000L, seed).blockingGet();

        assertThat(result.allowed()).isFalse();
        assertThat(result.nextAvailableAtMillis()).isEqualTo(1_500L);
    }

    @Test
    void out_of_order_timestamps_do_not_inflate_refill() {
        Supplier<TokenBucket> seed = seed("k7");
        long base = 1_000_000L; // capacity 10, 1 token per 1000ms
        // consume at base, then 500ms later (half a token, floored to 0), then replay an *earlier*
        // timestamp, then the 500ms timestamp again. A correct bucket must not let the out-of-order
        // replay drag the refill anchor backward and re-accrue a refill.
        repository.refillAndTryConsume("k7", 1, 1, 1_000L, 10, base, seed).blockingGet();
        repository.refillAndTryConsume("k7", 1, 1, 1_000L, 10, base + 500, seed).blockingGet();
        repository.refillAndTryConsume("k7", 1, 1, 1_000L, 10, base, seed).blockingGet(); // out-of-order (earlier)
        TokenBucketConsumeResult result = repository.refillAndTryConsume("k7", 1, 1, 1_000L, 10, base + 500, seed).blockingGet();

        // 4 tokens consumed; no whole token ever legitimately refilled within 500ms, so 6 remain.
        // A backend that moves the anchor backward inflates this to 7.
        assertThat(result.remainingTokens()).isEqualTo(6);
    }

    @Test
    void multi_token_request_consumes_rejects_and_never_overdraws() {
        Supplier<TokenBucket> seed = seed("kmt");
        // capacity 10, refill disabled (0 tokens per period): consume 3 in one request -> 7 remain
        TokenBucketConsumeResult three = repository.refillAndTryConsume("kmt", 3, 0, 1_000L, 10, 1_000L, seed).blockingGet();
        assertThat(three.allowed()).isTrue();
        assertThat(three.remainingTokens()).isEqualTo(7);

        // a request larger than the remaining balance (8 > 7) is rejected and must NOT draw the balance down
        TokenBucketConsumeResult tooMany = repository.refillAndTryConsume("kmt", 8, 0, 1_000L, 10, 1_000L, seed).blockingGet();
        assertThat(tooMany.allowed()).isFalse();
        assertThat(tooMany.remainingTokens()).isEqualTo(7);

        // a request for exactly the remaining balance (7) is allowed and empties the bucket
        TokenBucketConsumeResult exact = repository.refillAndTryConsume("kmt", 7, 0, 1_000L, 10, 1_000L, seed).blockingGet();
        assertThat(exact.allowed()).isTrue();
        assertThat(exact.remainingTokens()).isZero();
    }

    @Test
    void next_available_is_now_when_tokens_remain() {
        Supplier<TokenBucket> seed = seed("kna");
        // capacity 5, 1 token per 1000ms; one consume leaves 4, so the next token is available immediately
        TokenBucketConsumeResult result = repository.refillAndTryConsume("kna", 1, 1, 1_000L, 5, 1_000L, seed).blockingGet();

        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(4);
        assertThat(result.nextAvailableAtMillis()).isEqualTo(1_000L);
    }

    @Test
    void next_available_is_max_value_when_refill_is_disabled() {
        Supplier<TokenBucket> seed = seed("knr");
        // capacity 1, refill disabled (0 tokens per period); once empty, the next token never comes
        repository.refillAndTryConsume("knr", 1, 0, 1_000L, 1, 1_000L, seed).blockingGet();
        TokenBucketConsumeResult result = repository.refillAndTryConsume("knr", 1, 0, 1_000L, 1, 1_000L, seed).blockingGet();

        assertThat(result.allowed()).isFalse();
        assertThat(result.nextAvailableAtMillis()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void rejects_invalid_arguments() {
        Supplier<TokenBucket> seed = seed("kerr");
        // every backend must reject the same contract violations identically (no divide-by-zero or over-draw)
        assertThatThrownBy(() -> repository.refillAndTryConsume("kerr", 1, 1, 0, 10, 1_000L, seed).blockingGet()).isInstanceOf(
            IllegalArgumentException.class
        ); // refillPeriodMillis <= 0
        assertThatThrownBy(() -> repository.refillAndTryConsume("kerr", -1, 1, 1_000L, 10, 1_000L, seed).blockingGet()).isInstanceOf(
            IllegalArgumentException.class
        ); // tokensRequested < 0
        assertThatThrownBy(() -> repository.refillAndTryConsume("kerr", 1, 1, 1_000L, -1, 1_000L, seed).blockingGet()).isInstanceOf(
            IllegalArgumentException.class
        ); // capacity < 0
    }

    @Test
    void concurrent_consumes_never_exceed_available_tokens() throws Exception {
        Supplier<TokenBucket> seed = seed("k6");
        int capacity = 100;
        int attempts = 200; // twice the capacity, all at the same instant so no refill can occur

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            futures.add(
                pool.submit(() -> {
                    start.await();
                    // refillRate 0: refill disabled, so exactly `capacity` requests can ever succeed
                    if (repository.refillAndTryConsume("k6", 1, 0, 1_000L, capacity, 1_000L, seed).blockingGet().allowed()) {
                        allowed.incrementAndGet();
                    }
                    return null;
                })
            );
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(allowed).hasValue(capacity);
    }
}
