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
import static org.awaitility.Awaitility.await;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.properties.ClusterProperty;
import io.gravitee.node.api.cluster.DistributedMap;
import io.gravitee.node.api.cluster.DistributedMapProvider;
import io.gravitee.node.plugin.cluster.hazelcast.provider.HazelcastDistributedMapProvider;
import io.gravitee.repository.ratelimit.model.RateLimit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * End-to-end test wiring the repository against a real embedded Hazelcast via the
 * {@link DistributedMapProvider} SPI, proving that the policy-facing contract enforced by unit
 * tests also holds when backed by a real {@link com.hazelcast.map.IMap}: native TTL eviction,
 * {@code IMap.lock}-based per-key locking, and shared state across repeated
 * {@link DistributedMapProvider#get(String)} calls.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HazelcastRateLimitRepositoryIntegrationTest {

    private static final String KEY = "k";
    private static final long WINDOW_MS = 60_000;

    private HazelcastInstance hazelcast;
    private DistributedMapProvider distributedMapProvider;
    private HazelcastRateLimitRepository repository;

    @BeforeEach
    void setUp() {
        Config config = new Config();
        config.setInstanceName("apim-ratelimit-it-" + System.nanoTime());
        config.setProperty(ClusterProperty.HEALTH_MONITORING_LEVEL.getName(), "OFF");
        NetworkConfig network = config.getNetworkConfig();
        network.setPortAutoIncrement(true);
        JoinConfig join = network.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(false);
        join.getAutoDetectionConfig().setEnabled(false);

        hazelcast = Hazelcast.newHazelcastInstance(config);
        distributedMapProvider = new HazelcastDistributedMapProvider(hazelcast);

        DistributedMap<String, RateLimit> counters = distributedMapProvider.get("rate-limits");
        repository = new HazelcastRateLimitRepository(counters);
    }

    @AfterEach
    void tearDown() {
        hazelcast.shutdown();
    }

    @Test
    void should_seed_once_and_accumulate_across_increments() {
        AtomicInteger supplierCalls = new AtomicInteger();
        repository.incrementAndGet(KEY, 1, freshRateLimit(supplierCalls)).blockingGet();
        RateLimit second = repository.incrementAndGet(KEY, 2, freshRateLimit(supplierCalls)).blockingGet();

        assertThat(supplierCalls).hasValue(1);
        assertThat(second.getCounter()).isEqualTo(3);
    }

    @Test
    void should_drop_entry_when_hazelcast_ttl_expires() {
        AtomicInteger supplierCalls = new AtomicInteger();
        Supplier<RateLimit> shortWindow = () -> {
            supplierCalls.incrementAndGet();
            RateLimit rl = new RateLimit(KEY);
            rl.setResetTime(System.currentTimeMillis() + 500);
            return rl;
        };

        repository.incrementAndGet(KEY, 1, shortWindow).blockingGet();
        assertThat(supplierCalls).hasValue(1);

        // Native IMap eviction isn't instantaneous; poll until the next increment sees the entry gone
        // and therefore calls the supplier a second time.
        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                repository.incrementAndGet(KEY, 1, shortWindow).blockingGet();
                assertThat(supplierCalls.get()).isGreaterThanOrEqualTo(2);
            });
    }

    @Test
    void should_serialize_concurrent_increments_on_same_key_via_real_imap_lock() throws InterruptedException {
        final int threads = 16;
        final int incrementsPerThread = 50;
        Supplier<RateLimit> longWindow = () -> {
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
                        repository.incrementAndGet(KEY, 1, longWindow).blockingGet();
                    }
                    done.countDown();
                    return null;
                });
            }
            ready.await();
            go.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        RateLimit finalState = distributedMapProvider.<String, RateLimit>get("rate-limits").get(KEY);
        assertThat(finalState).isNotNull();
        assertThat(finalState.getCounter()).isEqualTo((long) threads * incrementsPerThread);
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
