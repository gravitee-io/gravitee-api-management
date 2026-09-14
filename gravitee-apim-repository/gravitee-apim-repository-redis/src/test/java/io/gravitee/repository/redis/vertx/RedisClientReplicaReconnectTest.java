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

import io.gravitee.repository.redis.common.RedisConnectionFactory;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
 * After READONLY, reconnect must back off. {@code redisApi()} used to start a new connect
 * at retry 0 whenever {@code redisAPIFuture} was null, which skipped that backoff.
 * ROLE is Sentinel-only (it is {@code @dangerous}); ACL users without that category
 * must still connect in standalone mode.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisClientReplicaReconnectTest {

    private final Vertx vertx = Vertx.vertx();

    private GenericContainer<?> redis;

    @BeforeAll
    void start_redis() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
        redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
        redis.start();
    }

    @AfterAll
    void stop_redis() {
        if (redis != null) {
            redis.stop();
        }
        vertx.close();
    }

    @Test
    void should_not_start_a_new_connect_on_every_redisApi_call_while_backoff_pending() throws Exception {
        GenericContainer<?> isolated = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
        isolated.start();
        RedisClient client = createClient(isolated.getHost(), isolated.getFirstMappedPort());
        try {
            await_connected(client);
            isolated.stop();
            TimeUnit.MILLISECONDS.sleep(200);

            AtomicInteger inProgress = new AtomicInteger();
            CountDownLatch done = new CountDownLatch(1);
            long deadline = System.currentTimeMillis() + 400;
            vertx.runOnContext(v -> hammer_redis_api(client, deadline, inProgress, done));
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();

            assertThat(inProgress.get())
                .as("redisApi() must fail with reconnect-pending instead of starting a new connect each call")
                .isGreaterThan(0);
        } finally {
            if (isolated.isRunning()) {
                isolated.stop();
            }
        }
    }

    private void hammer_redis_api(RedisClient redisClient, long deadline, AtomicInteger inProgress, CountDownLatch done) {
        redisClient
            .redisApi()
            .onFailure(t -> {
                if (t.getMessage() != null && t.getMessage().contains("reconnection is in progress")) {
                    inProgress.incrementAndGet();
                }
            });
        if (System.currentTimeMillis() >= deadline) {
            done.countDown();
            return;
        }
        vertx.setTimer(10, id -> hammer_redis_api(redisClient, deadline, inProgress, done));
    }

    @Test
    void should_reconnect_after_replicaof_no_one() throws Exception {
        RedisClient client = createClient();
        await_connected(client);
        await_connected_on_context(client);

        replicaof("127.0.0.1", "1");
        notify_connection_failure_on_context(client, new Exception("READONLY You can't write against a read only replica."));

        replicaof("NO", "ONE");
        await_connected_passive_on_context(client);
        assertThat(ping_on_context(client)).isEqualTo("PONG");
    }

    @Test
    void should_connect_as_acl_user_without_dangerous_category() throws Exception {
        var acl = redis.execInContainer("redis-cli", "ACL", "SETUSER", "limited", "on", "nopass", "~*", "&*", "+@all", "-@dangerous");
        assertThat(acl.getExitCode()).as(acl.getStderr()).isZero();

        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("ratelimit.redis.host", redis.getHost());
        environment.setProperty("ratelimit.redis.port", redis.getFirstMappedPort().toString());
        environment.setProperty("ratelimit.redis.username", "limited");
        RedisClient client = new RedisConnectionFactory(
            environment,
            vertx,
            "ratelimit",
            Map.of(SCRIPT_RATELIMIT_KEY, SCRIPTS_RATELIMIT_LUA)
        ).createRedisClient();

        await_connected(client);
        assertThat(ping_on_context(client)).isEqualTo("PONG");
    }

    private RedisClient createClient() {
        return createClient(redis.getHost(), redis.getFirstMappedPort());
    }

    private RedisClient createClient(String host, Integer port) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("ratelimit.redis.host", host);
        environment.setProperty("ratelimit.redis.port", port.toString());
        return new RedisConnectionFactory(
            environment,
            vertx,
            "ratelimit",
            Map.of(SCRIPT_RATELIMIT_KEY, SCRIPTS_RATELIMIT_LUA)
        ).createRedisClient();
    }

    private void await_connected(RedisClient redisClient) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (!redisClient.isConnected() && System.currentTimeMillis() < deadline) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        assertThat(redisClient.isConnected()).isTrue();
    }

    private void await_connected_on_context(RedisClient redisClient) throws Exception {
        poll_connected_on_context(redisClient, System.currentTimeMillis() + 30_000).get(35, TimeUnit.SECONDS);
    }

    private void await_connected_passive_on_context(RedisClient redisClient) throws Exception {
        poll_connected_on_context(redisClient, System.currentTimeMillis() + 30_000).get(35, TimeUnit.SECONDS);
    }

    private CompletableFuture<Void> poll_connected_on_context(RedisClient redisClient, long deadline) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        poll_connected_on_context(redisClient, deadline, done);
        return done;
    }

    private void poll_connected_on_context(RedisClient redisClient, long deadline, CompletableFuture<Void> done) {
        vertx.runOnContext(v -> {
            if (redisClient.isConnected()) {
                done.complete(null);
                return;
            }
            if (System.currentTimeMillis() >= deadline) {
                done.completeExceptionally(new AssertionError("Redis client did not reconnect in time"));
                return;
            }
            vertx.setTimer(200, id -> poll_connected_on_context(redisClient, deadline, done));
        });
    }

    private void notify_connection_failure_on_context(RedisClient redisClient, Exception failure) throws Exception {
        CompletableFuture<Void> notified = new CompletableFuture<>();
        vertx.runOnContext(v -> {
            redisClient.notifyConnectionFailure(failure);
            notified.complete(null);
        });
        notified.get(10, TimeUnit.SECONDS);
    }

    private void replicaof(String host, String port) throws Exception {
        var result = redis.execInContainer("redis-cli", "REPLICAOF", host, port);
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();
        assertThat(result.getStdout()).containsIgnoringCase("ok");
    }

    private String ping_on_context(RedisClient redisClient) throws Exception {
        CompletableFuture<String> ping = new CompletableFuture<>();
        vertx.runOnContext(v ->
            redisClient
                .redisApi()
                .compose(api -> api.ping(List.of()))
                .onSuccess(response -> ping.complete(response.toString()))
                .onFailure(ping::completeExceptionally)
        );
        return ping.get(10, TimeUnit.SECONDS);
    }
}
