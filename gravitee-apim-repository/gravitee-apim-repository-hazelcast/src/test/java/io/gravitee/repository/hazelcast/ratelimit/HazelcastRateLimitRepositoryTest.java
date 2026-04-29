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

import static io.gravitee.repository.hazelcast.ratelimit.RateLimitRepositoryConfiguration.RATE_LIMIT_MAP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.properties.ClusterProperty;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HazelcastRateLimitRepositoryTest {

    private HazelcastInstance hazelcast;
    private RateLimitRepository<RateLimit> repository;

    @BeforeEach
    void setUp() throws Exception {
        Config config = new FileSystemXmlConfig("src/test/resources/cluster.xml");
        config.setProperty(ClusterProperty.HEALTH_MONITORING_LEVEL.getName(), "OFF");
        config.setInstanceName("test-ratelimit-hz-" + System.nanoTime());
        hazelcast = Hazelcast.newHazelcastInstance(config);
        repository = new HazelcastRateLimitRepository(hazelcast.getMap(RATE_LIMIT_MAP));
    }

    @AfterEach
    void tearDown() {
        hazelcast.shutdown();
    }

    @Test
    void increments_counter_on_repeat_calls_within_window() {
        RateLimit first = repository.incrementAndGet("key", 1, () -> initial("key", 60_000)).blockingGet();
        RateLimit second = repository.incrementAndGet("key", 1, () -> initial("key", 60_000)).blockingGet();
        RateLimit third = repository.incrementAndGet("key", 1, () -> initial("key", 60_000)).blockingGet();

        assertThat(first.getCounter()).isEqualTo(1);
        assertThat(second.getCounter()).isEqualTo(2);
        assertThat(third.getCounter()).isEqualTo(3);
    }

    @Test
    void resets_counter_when_window_expired() {
        RateLimit beforeWindow = repository.incrementAndGet("key", 1, () -> initial("key", 100)).blockingGet();
        await()
            .atMost(2, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(hazelcast.<String, RateLimit>getMap(RATE_LIMIT_MAP).get("key")).isNull());

        RateLimit afterWindow = repository.incrementAndGet("key", 1, () -> initial("key", 60_000)).blockingGet();

        assertThat(afterWindow.getCounter()).isEqualTo(1);
        // Reset time must come from the new supplier — not the prior expired entry — so a regression
        // that drops the defensive copy in the EntryProcessor would fail this assertion.
        assertThat(afterWindow.getResetTime()).isGreaterThan(beforeWindow.getResetTime());
    }

    @Test
    void increments_by_weight() {
        RateLimit r = repository.incrementAndGet("key", 5, () -> initial("key", 60_000)).blockingGet();
        assertThat(r.getCounter()).isEqualTo(5);
    }

    @Test
    void isolates_keys() {
        repository.incrementAndGet("a", 3, () -> initial("a", 60_000)).blockingGet();
        RateLimit b = repository.incrementAndGet("b", 1, () -> initial("b", 60_000)).blockingGet();
        assertThat(b.getCounter()).isEqualTo(1);
    }

    @Test
    void preserves_subscription_on_fresh_window() {
        RateLimit r = repository.incrementAndGet("sub-key", 1, () -> initial("sub-key", 60_000)).blockingGet();
        assertThat(r.getSubscription()).isEqualTo("test-sub");
        RateLimit stored = hazelcast.<String, RateLimit>getMap(RATE_LIMIT_MAP).get("sub-key");
        assertThat(stored.getSubscription()).isEqualTo("test-sub");
    }

    @Test
    void evicts_entry_after_ttl() {
        repository.incrementAndGet("ttl", 1, () -> initial("ttl", 100)).blockingGet();
        await()
            .atMost(2, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(hazelcast.<String, RateLimit>getMap(RATE_LIMIT_MAP).get("ttl")).isNull());
    }

    @Test
    void serializes_concurrent_increments_via_entry_processor() throws InterruptedException {
        final int threads = 16;
        final int iterations = 50;
        final long expected = (long) threads * iterations;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < iterations; i++) {
                            repository.incrementAndGet("contended", 1, () -> initial("contended", 60_000)).blockingGet();
                        }
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();

            RateLimit finalState = hazelcast.<String, RateLimit>getMap(RATE_LIMIT_MAP).get("contended");
            assertThat(finalState.getCounter()).isEqualTo(expected);
        } finally {
            pool.shutdownNow();
        }
    }

    private static RateLimit initial(String key, long windowMs) {
        RateLimit rl = new RateLimit(key);
        rl.setCounter(0);
        rl.setLimit(Long.MAX_VALUE);
        rl.setResetTime(System.currentTimeMillis() + windowMs);
        rl.setSubscription("test-sub");
        return rl;
    }
}
