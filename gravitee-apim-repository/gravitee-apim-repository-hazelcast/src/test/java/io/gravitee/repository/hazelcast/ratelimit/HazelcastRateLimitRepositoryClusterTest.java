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
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.properties.ClusterProperty;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Multi-member tests covering the partition routing + cross-member replication path that the
 * single-member suite cannot exercise. Two real HZ members are joined via TCP-IP on localhost.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HazelcastRateLimitRepositoryClusterTest {

    private static final String CLUSTER_NAME = "graviteeio-apim-ratelimit-cluster-test";
    private static final int PORT_M1 = 5921;
    private static final int PORT_M2 = 5922;

    private HazelcastInstance m1;
    private HazelcastInstance m2;
    private RateLimitRepository<RateLimit> repo1;
    private RateLimitRepository<RateLimit> repo2;

    @BeforeAll
    void startCluster() {
        m1 = newMember(PORT_M1);
        m2 = newMember(PORT_M2);
        await()
            .atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(m1.getCluster().getMembers()).hasSize(2);
                assertThat(m2.getCluster().getMembers()).hasSize(2);
            });
        repo1 = new HazelcastRateLimitRepository(m1.getMap(RATE_LIMIT_MAP));
        repo2 = new HazelcastRateLimitRepository(m2.getMap(RATE_LIMIT_MAP));
    }

    @AfterAll
    void stopCluster() {
        if (m1 != null) m1.shutdown();
        if (m2 != null) m2.shutdown();
    }

    @Test
    void counter_is_shared_across_members_via_imap_partition_routing() {
        RateLimit r1 = repo1.incrementAndGet("shared", 1, () -> initial("shared", 60_000)).blockingGet();
        RateLimit r2 = repo2.incrementAndGet("shared", 1, () -> initial("shared", 60_000)).blockingGet();
        RateLimit r3 = repo1.incrementAndGet("shared", 1, () -> initial("shared", 60_000)).blockingGet();

        assertThat(r1.getCounter()).isEqualTo(1);
        assertThat(r2.getCounter()).isEqualTo(2);
        assertThat(r3.getCounter()).isEqualTo(3);

        // Either member's view of the IMap returns the same counter — proves replication.
        long viaM1 = m1.<String, RateLimit>getMap(RATE_LIMIT_MAP).get("shared").getCounter();
        long viaM2 = m2.<String, RateLimit>getMap(RATE_LIMIT_MAP).get("shared").getCounter();
        assertThat(viaM1).isEqualTo(3);
        assertThat(viaM2).isEqualTo(3);
    }

    @Test
    void concurrent_increments_across_members_settle_to_exact_total() throws InterruptedException {
        final int threads = 10;
        final int iterations = 50;
        final long expected = (long) threads * iterations;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            for (int t = 0; t < threads; t++) {
                final int threadIndex = t;
                pool.submit(() -> {
                    try {
                        start.await();
                        RateLimitRepository<RateLimit> repo = (threadIndex % 2 == 0) ? repo1 : repo2;
                        for (int i = 0; i < iterations; i++) {
                            repo.incrementAndGet("multi-contended", 1, () -> initial("multi-contended", 60_000)).blockingGet();
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

            long viaM1 = m1.<String, RateLimit>getMap(RATE_LIMIT_MAP).get("multi-contended").getCounter();
            long viaM2 = m2.<String, RateLimit>getMap(RATE_LIMIT_MAP).get("multi-contended").getCounter();
            assertThat(viaM1).isEqualTo(expected);
            assertThat(viaM2).isEqualTo(expected);
        } finally {
            pool.shutdownNow();
        }
    }

    private static HazelcastInstance newMember(int port) {
        Config config = new Config();
        config.setClusterName(CLUSTER_NAME);
        config.setInstanceName("multi-member-" + port + "-" + System.nanoTime());
        config.setProperty(ClusterProperty.HEALTH_MONITORING_LEVEL.getName(), "OFF");
        config.setProperty(ClusterProperty.LOGGING_TYPE.getName(), "slf4j");

        NetworkConfig network = config.getNetworkConfig();
        network.setPort(port).setPortAutoIncrement(false);

        JoinConfig join = network.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getAutoDetectionConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(true).setMembers(List.of("127.0.0.1:" + PORT_M1, "127.0.0.1:" + PORT_M2));

        return Hazelcast.newHazelcastInstance(config);
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
