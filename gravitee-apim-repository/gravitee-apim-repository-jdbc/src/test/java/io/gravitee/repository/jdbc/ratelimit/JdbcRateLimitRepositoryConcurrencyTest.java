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
package io.gravitee.repository.jdbc.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.config.TestRepositoryInitializer;
import io.gravitee.repository.jdbc.JdbcTestRepositoryConfiguration;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Concurrent {@code incrementAndGet} calls on the JDBC {@link RateLimitRepository}: every increment
 * must be counted exactly once, whether the row already exists, does not exist yet or holds an expired
 * window. The database is selected by the {@code jdbcType} property (PostgreSQL by default).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JdbcTestRepositoryConfiguration.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class JdbcRateLimitRepositoryConcurrencyTest {

    private static final int THREADS = 16;
    private static final int INCREMENTS = 200;

    @Autowired
    private RateLimitRepository<RateLimit> rateLimitRepository;

    @Autowired
    private TestRepositoryInitializer testRepositoryInitializer;

    @BeforeEach
    void setUp() {
        testRepositoryInitializer.setUp();
    }

    @AfterEach
    void tearDown() {
        testRepositoryInitializer.tearDown();
    }

    @Test
    void concurrent_increments_on_an_existing_key_are_all_counted() throws Exception {
        Supplier<RateLimit> window = window("existing", 60_000, 1_000);
        rateLimitRepository.incrementAndGet("existing", 0, window).blockingGet();

        List<Long> counters = incrementConcurrently("existing", window);

        assertThat(counters).containsExactlyInAnyOrderElementsOf(LongStream.rangeClosed(1, INCREMENTS).boxed().toList());
        assertThat(rateLimitRepository.incrementAndGet("existing", 0, window).blockingGet().getCounter()).isEqualTo(INCREMENTS);
    }

    @Test
    void concurrent_first_increments_on_an_unknown_key_all_succeed_and_are_all_counted() throws Exception {
        Supplier<RateLimit> window = window("unknown", 60_000, 1_000);

        List<Long> counters = incrementConcurrently("unknown", window);

        assertThat(counters).containsExactlyInAnyOrderElementsOf(LongStream.rangeClosed(1, INCREMENTS).boxed().toList());
    }

    @Test
    void concurrent_increments_on_an_expired_window_restart_it_once() throws Exception {
        rateLimitRepository.incrementAndGet("expired", 500, window("expired", -1_000, 1_000)).blockingGet();
        Supplier<RateLimit> window = window("expired", 60_000, 2_000);

        List<Long> counters = incrementConcurrently("expired", window);

        assertThat(counters).containsExactlyInAnyOrderElementsOf(LongStream.rangeClosed(1, INCREMENTS).boxed().toList());
        RateLimit rateLimit = rateLimitRepository.incrementAndGet("expired", 0, window).blockingGet();
        assertThat(rateLimit.getCounter()).isEqualTo(INCREMENTS);
        assertThat(rateLimit.getLimit()).isEqualTo(2_000);
    }

    @Test
    void an_expired_window_restarts_from_the_supplied_rate_limit() {
        rateLimitRepository.incrementAndGet("restart", 500, window("restart", -1_000, 1_000)).blockingGet();
        Supplier<RateLimit> window = window("restart", 60_000, 2_000);
        long resetTime = window.get().getResetTime();

        RateLimit rateLimit = rateLimitRepository.incrementAndGet("restart", 3, window).blockingGet();

        assertThat(rateLimit.getCounter()).isEqualTo(3);
        assertThat(rateLimit.getLimit()).isEqualTo(2_000);
        assertThat(rateLimit.getResetTime()).isGreaterThanOrEqualTo(resetTime);
        assertThat(rateLimit.getSubscription()).isEqualTo("restart-subscription");
    }

    private List<Long> incrementConcurrently(String key, Supplier<RateLimit> window) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < INCREMENTS; i++) {
                futures.add(
                    pool.submit(() -> {
                        start.await();
                        return rateLimitRepository.incrementAndGet(key, 1, window).blockingGet().getCounter();
                    })
                );
            }
            start.countDown();
            List<Long> counters = new ArrayList<>();
            for (Future<Long> future : futures) {
                // Bounded, so a lock regression fails the build instead of hanging it.
                counters.add(future.get(30, TimeUnit.SECONDS));
            }
            return counters;
        } finally {
            pool.shutdownNow();
        }
    }

    /** A fresh window for {@code key} at each call, as the rate-limit policy supplies one. */
    private static Supplier<RateLimit> window(String key, long expireInMillis, long limit) {
        return () -> {
            RateLimit rateLimit = new RateLimit(key);
            rateLimit.setSubscription(key + "-subscription");
            rateLimit.setResetTime(System.currentTimeMillis() + expireInMillis);
            rateLimit.setLimit(limit);
            return rateLimit;
        };
    }
}
