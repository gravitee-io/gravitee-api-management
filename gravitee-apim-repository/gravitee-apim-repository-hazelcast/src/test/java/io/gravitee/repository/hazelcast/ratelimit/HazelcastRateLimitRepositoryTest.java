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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.ratelimit.model.RateLimit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HazelcastRateLimitRepositoryTest {

    private static final String KEY = "k";
    private static final long WINDOW_MS = 60_000;

    private InMemoryDistributedMap<String, RateLimit> map;
    private HazelcastRateLimitRepository repository;

    @BeforeEach
    void setUp() {
        map = new InMemoryDistributedMap<>();
        repository = new HazelcastRateLimitRepository(map);
    }

    @Test
    void should_seed_from_supplier_on_first_increment() {
        AtomicInteger supplierCalls = new AtomicInteger();
        RateLimit result = repository.incrementAndGet(KEY, 1, freshRateLimit(supplierCalls)).blockingGet();

        assertThat(supplierCalls).hasValue(1);
        assertThat(result.getKey()).isEqualTo(KEY);
        assertThat(result.getCounter()).isEqualTo(1);
    }

    @Test
    void should_not_call_supplier_when_existing_entry_is_in_window() {
        AtomicInteger supplierCalls = new AtomicInteger();
        repository.incrementAndGet(KEY, 1, freshRateLimit(supplierCalls)).blockingGet();
        RateLimit second = repository.incrementAndGet(KEY, 2, freshRateLimit(supplierCalls)).blockingGet();

        assertThat(supplierCalls).hasValue(1);
        assertThat(second.getCounter()).isEqualTo(3);
    }

    @Test
    void should_call_supplier_again_after_window_rolls_over() {
        AtomicInteger supplierCalls = new AtomicInteger();
        Supplier<RateLimit> expiredFirstFreshAfter = () -> {
            RateLimit rl = new RateLimit(KEY);
            rl.setResetTime(supplierCalls.incrementAndGet() == 1 ? System.currentTimeMillis() - 1 : System.currentTimeMillis() + WINDOW_MS);
            return rl;
        };

        repository.incrementAndGet(KEY, 5, expiredFirstFreshAfter).blockingGet();
        RateLimit second = repository.incrementAndGet(KEY, 7, expiredFirstFreshAfter).blockingGet();

        assertThat(supplierCalls).hasValue(2);
        assertThat(second.getCounter()).isEqualTo(7);
    }

    @Test
    void should_write_ttl_as_remaining_window() {
        long resetTime = System.currentTimeMillis() + WINDOW_MS;
        Supplier<RateLimit> supplier = () -> {
            RateLimit rl = new RateLimit(KEY);
            rl.setResetTime(resetTime);
            return rl;
        };

        repository.incrementAndGet(KEY, 1, supplier).blockingGet();

        assertThat(map.lastTtlMillis()).isBetween(WINDOW_MS - 1_000, WINDOW_MS);
    }

    @Test
    void should_write_floor_ttl_when_reset_time_already_passed() {
        Supplier<RateLimit> expiredFirstFreshAfter = new Supplier<>() {
            int call;

            @Override
            public RateLimit get() {
                RateLimit rl = new RateLimit(KEY);
                rl.setResetTime(++call == 1 ? System.currentTimeMillis() - 100 : System.currentTimeMillis() + WINDOW_MS);
                return rl;
            }
        };

        repository.incrementAndGet(KEY, 1, expiredFirstFreshAfter).blockingGet();

        assertThat(map.lastTtlMillis()).isEqualTo(1);
    }

    @Test
    void should_serialize_concurrent_increments_on_same_key() throws InterruptedException {
        final int threads = 16;
        final int incrementsPerThread = 50;
        final long weight = 1;
        Supplier<RateLimit> supplier = () -> {
            RateLimit rl = new RateLimit(KEY);
            rl.setResetTime(System.currentTimeMillis() + WINDOW_MS);
            return rl;
        };

        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    for (int n = 0; n < incrementsPerThread; n++) {
                        repository.incrementAndGet(KEY, weight, supplier).blockingGet();
                    }
                    done.countDown();
                    return null;
                });
            }
            ready.await();
            go.countDown();
            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        RateLimit finalState = map.get(KEY);
        assertThat(finalState).isNotNull();
        assertThat(finalState.getCounter()).isEqualTo((long) threads * incrementsPerThread * weight);
    }

    private static Supplier<RateLimit> freshRateLimit(AtomicInteger callCounter) {
        return () -> {
            callCounter.incrementAndGet();
            RateLimit rl = new RateLimit(KEY);
            rl.setResetTime(System.currentTimeMillis() + WINDOW_MS);
            return rl;
        };
    }
}
