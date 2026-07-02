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

import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPTS_TOKEN_BUCKET_LUA;
import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPT_TOKEN_BUCKET_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.ratelimit.AbstractTokenBucketRateLimitRepositoryContractTest;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import io.gravitee.repository.redis.common.RedisConnectionFactory;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Vertx;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs the shared {@link AbstractTokenBucketRateLimitRepositoryContractTest} against a standalone
 * Redis. Each test flushes the database first so it sees an empty store. Requires a Docker daemon;
 * skipped (not failed) when none is available.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisTokenBucketRateLimitRepositoryTest extends AbstractTokenBucketRateLimitRepositoryContractTest {

    private final Vertx vertx = Vertx.vertx();
    private GenericContainer<?> redis;
    private RedisClient redisClient;
    private RedisTokenBucketRateLimitRepository repo;

    @BeforeAll
    void startRedis() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available — skipping Redis token-bucket test");
        redis = new GenericContainer<>(DockerImageName.parse("redis:7.4")).withExposedPorts(6379).waitingFor(Wait.forListeningPort());
        redis.start();

        String prefix = Scope.RATE_LIMIT.getName() + ".redis.";
        MockEnvironment env = new MockEnvironment();
        env.setProperty(prefix + "host", redis.getHost());
        env.setProperty(prefix + "port", String.valueOf(redis.getMappedPort(6379)));

        redisClient = new RedisConnectionFactory(
            env,
            vertx,
            Scope.RATE_LIMIT.getName(),
            Map.of(SCRIPT_TOKEN_BUCKET_KEY, SCRIPTS_TOKEN_BUCKET_LUA)
        ).createRedisClient();
        awaitConnected(redisClient);

        repo = new RedisTokenBucketRateLimitRepository(redisClient, 5000);
    }

    @AfterAll
    void stopRedis() {
        if (redis != null) {
            redis.stop();
        }
        vertx.close();
    }

    @Override
    protected TokenBucketRateLimitRepository<TokenBucket> createRepository() {
        flushAll();
        return repo;
    }

    @Test
    void sets_key_ttl_to_the_full_refill_window() throws Exception {
        // capacity 100 at 2 tokens per 1000ms => the bucket would refill fully in 50s, so the key gets a ~50s TTL.
        repo.refillAndTryConsume("ttl", 1, 2, 1_000L, 100, System.currentTimeMillis(), () -> new TokenBucket("ttl")).blockingGet();

        long pttl = Long.parseLong(redis.execInContainer("redis-cli", "pttl", "tokenbucket:ttl").getStdout().trim());
        assertThat(pttl).isBetween(45_000L, 51_000L);
    }

    private void flushAll() {
        try {
            redis.execInContainer("redis-cli", "flushall");
        } catch (Exception e) {
            throw new RuntimeException("Failed to flush Redis between tests", e);
        }
    }

    private static void awaitConnected(RedisClient client) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (!client.isConnected() && System.currentTimeMillis() < deadline) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        assertThat(client.isConnected()).as("Redis client connected").isTrue();
    }
}
