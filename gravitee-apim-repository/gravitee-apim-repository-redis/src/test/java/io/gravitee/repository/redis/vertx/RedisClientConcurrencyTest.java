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
package io.gravitee.repository.redis.vertx;

import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPTS_RATELIMIT_LUA;
import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPT_RATELIMIT_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.gravitee.repository.redis.common.RedisConnectionFactory;
import io.gravitee.repository.redis.ratelimit.RedisRateLimitRepository;
import io.vertx.core.Vertx;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Stress-tests {@link RedisClient} and {@link RedisRateLimitRepository} under concurrent load.
 * Complements unit tests; per-event-loop connection scaling is validated via k6 benchmarks.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisClientConcurrencyTest {

    private final Vertx vertx = Vertx.vertx();

    private GenericContainer<?> redis;
    private RedisRateLimitRepository repository;

    @BeforeAll
    void start_redis() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available");

        redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
        redis.start();

        String propertyPrefix = Scope.RATE_LIMIT.getName() + ".redis.";
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(propertyPrefix + "host", redis.getHost());
        environment.setProperty(propertyPrefix + "port", redis.getFirstMappedPort().toString());

        RedisClient client = new RedisConnectionFactory(
            environment,
            vertx,
            Scope.RATE_LIMIT.getName(),
            Map.of(SCRIPT_RATELIMIT_KEY, SCRIPTS_RATELIMIT_LUA)
        ).createRedisClient();

        await_connected(client);
        repository = new RedisRateLimitRepository(client, 2000);
    }

    @AfterAll
    void stop_redis() {
        if (redis != null) {
            redis.stop();
        }
        vertx.close();
    }

    @Test
    void should_increment_rate_limit_concurrently_without_errors() throws Exception {
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    RateLimit rateLimit = repository
                        .incrementAndGet("concurrent-key", 1, () -> {
                            RateLimit rate = new RateLimit("concurrent-key");
                            rate.setLimit(100_000);
                            rate.setResetTime(System.currentTimeMillis() + 60_000);
                            rate.setSubscription("sub");
                            return rate;
                        })
                        .blockingGet();
                    assertThat(rateLimit.getCounter()).isPositive();
                    successes.incrementAndGet();
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(error.get()).isNull();
        assertThat(successes.get()).isEqualTo(threads);
    }

    private void await_connected(RedisClient client) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (!client.isConnected() && System.currentTimeMillis() < deadline) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        assertThat(client.isConnected()).isTrue();
    }
}
