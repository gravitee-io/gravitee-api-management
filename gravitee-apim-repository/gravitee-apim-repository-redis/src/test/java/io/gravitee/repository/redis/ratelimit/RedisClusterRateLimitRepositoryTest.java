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
package io.gravitee.repository.redis.ratelimit;

import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPTS_RATELIMIT_LUA;
import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPT_RATELIMIT_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.gravitee.repository.redis.common.RedisConnectionFactory;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Vertx;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Acceptance test for cluster-safe rate limiting (APIM-14146).
 *
 * <p>Runs the rate-limit script against a single-node Redis Cluster. A single node still
 * runs in cluster mode and therefore enforces the CROSSSLOT rule, so this reproduces the
 * original failure (weight passed as a second KEY hashed to a different slot) while avoiding
 * the multi-node announce-IP networking issues of a full cluster on Docker Desktop.
 *
 * <p><b>Scope:</b> a single node owns all slots, so EVALSHA always hits after the local
 * SCRIPT LOAD — this does <i>not</i> exercise the cross-master NOSCRIPT path. The
 * NOSCRIPT&rarr;EVAL fallback is covered by the mock unit test in
 * {@link RedisRateLimitRepositoryTest} and the manual 3-master smoke test, not here.
 *
 * <p>Requires a Docker daemon; skipped (not failed) when none is available.
 *
 * @author GraviteeSource Team
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisClusterRateLimitRepositoryTest {

    // A free host port chosen at runtime, used as both the container port and the cluster-announced
    // port so the announced address (127.0.0.1:PORT) is reachable from the host without collisions.
    private final int CLUSTER_PORT = findFreePort();

    private final Vertx vertx = Vertx.vertx();
    private GenericContainer<?> redis;
    private RedisClient redisClient;
    private RedisRateLimitRepository repository;

    @BeforeAll
    void startCluster() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available — skipping Redis cluster integration test");
        redis = new GenericContainer<>(DockerImageName.parse("redis:7.4"))
            .withExposedPorts(CLUSTER_PORT)
            .withCreateContainerCmdModifier(cmd ->
                cmd.getHostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(CLUSTER_PORT), new ExposedPort(CLUSTER_PORT)))
            )
            .withCommand(
                "redis-server --port " +
                    CLUSTER_PORT +
                    " --cluster-enabled yes --cluster-node-timeout 5000" +
                    " --cluster-announce-ip 127.0.0.1 --cluster-announce-port " +
                    CLUSTER_PORT
            )
            .waitingFor(Wait.forListeningPort());
        redis.start();

        // A single node owning all 16384 slots forms a healthy single-node cluster.
        redis.execInContainer("redis-cli", "-p", String.valueOf(CLUSTER_PORT), "cluster", "addslotsrange", "0", "16383");
        awaitClusterStateOk();

        String prefix = Scope.RATE_LIMIT.getName() + ".redis.";
        MockEnvironment env = new MockEnvironment();
        env.setProperty(prefix + "cluster.nodes[0].host", "127.0.0.1");
        env.setProperty(prefix + "cluster.nodes[0].port", String.valueOf(CLUSTER_PORT));

        redisClient = new RedisConnectionFactory(
            env,
            vertx,
            Scope.RATE_LIMIT.getName(),
            Map.of(SCRIPT_RATELIMIT_KEY, SCRIPTS_RATELIMIT_LUA)
        ).createRedisClient();
        awaitConnected(redisClient);

        repository = new RedisRateLimitRepository(redisClient, 5000);
    }

    private void awaitClusterStateOk() throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            var result = redis.execInContainer("redis-cli", "-p", String.valueOf(CLUSTER_PORT), "cluster", "info");
            if (result.getStdout().contains("cluster_state:ok")) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(200);
        }
        throw new IllegalStateException("Redis cluster did not reach state ok");
    }

    @AfterAll
    void stopCluster() {
        if (redis != null) {
            redis.stop();
        }
        vertx.close();
    }

    @Test
    void increments_rate_limit_counters_across_many_slots_without_crossslot() {
        // Distinct keys hash to many different slots; on the unfixed script each call passed the
        // weight as a second KEY and failed with CROSSSLOT. Each key must count independently.
        long reset = 1_900_000_000_000L; // fixed far-future reset so keys don't expire mid-test
        for (int i = 0; i < 50; i++) {
            String key = "api-" + i + "-plan-" + i;
            String subscription = "sub-" + i;

            RateLimit first = increment(key, 1, reset, subscription);
            assertThat(first.getCounter()).isEqualTo(1);

            RateLimit second = increment(key, 4, reset, subscription);
            assertThat(second.getCounter()).isEqualTo(5); // 1 + weight(4)
            assertThat(second.getLimit()).isEqualTo(1000);
            // Assert reset (ARGV[4]) and subscription (ARGV[5]) too, so a swapped Lua index is caught.
            assertThat(second.getResetTime()).isEqualTo(reset);
            assertThat(second.getSubscription()).isEqualTo(subscription);
        }
    }

    private static void awaitConnected(RedisClient client) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (!client.isConnected() && System.currentTimeMillis() < deadline) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        assertThat(client.isConnected()).as("Redis cluster client connected").isTrue();
    }

    private RateLimit increment(String key, long weight, long reset, String subscription) {
        return repository
            .incrementAndGet(key, weight, () -> {
                RateLimit rate = new RateLimit(key);
                rate.setLimit(1000);
                rate.setResetTime(reset);
                rate.setSubscription(subscription);
                return rate;
            })
            .blockingGet();
    }

    // Scan an explicit low range rather than ServerSocket(0): Redis cluster's bus port is
    // dataPort+10000 and must be <= 65535, so the data port must be < 55535 — but the OS ephemeral
    // range can sit entirely above that. Close-then-bind is a TOCTOU race (the port could be grabbed
    // before the container binds), acceptable for this single local test and far better than a fixed port.
    private static int findFreePort() {
        for (int port = 20000; port < 30000; port++) {
            try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
                return socket.getLocalPort();
            } catch (java.io.IOException ignored) {
                // in use — try the next one
            }
        }
        throw new IllegalStateException("Could not find a free port in 20000-30000 for the Redis cluster test");
    }
}
