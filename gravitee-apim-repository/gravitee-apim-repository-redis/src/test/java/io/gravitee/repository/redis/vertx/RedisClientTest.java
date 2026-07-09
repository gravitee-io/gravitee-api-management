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

import io.gravitee.repository.exception.RedisOperationTimeoutException;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.gravitee.repository.redis.common.RedisConnectionFactory;
import io.gravitee.repository.redis.ratelimit.RedisRateLimitRepository;
import io.vertx.core.Vertx;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
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
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisClientTest {

    private final Vertx vertx = Vertx.vertx();

    private GenericContainer<?> redis;
    private RedisClient client;
    private RedisRateLimitRepository repository;

    @BeforeAll
    void start_redis() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available");

        redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
        redis.start();

        client = createClient(Map.of(SCRIPT_RATELIMIT_KEY, SCRIPTS_RATELIMIT_LUA));
        repository = new RedisRateLimitRepository(client, 2_000);
        await_connected(client);
    }

    @AfterAll
    void stop_redis() {
        if (redis != null) {
            redis.stop();
        }
        vertx.close();
    }

    @Test
    void script_sha1_is_available_from_non_vertx_thread() throws Exception {
        String sha = CompletableFuture.supplyAsync(() -> client.scriptSha1(SCRIPT_RATELIMIT_KEY)).get(10, TimeUnit.SECONDS);

        assertThat(sha).isNotBlank();
    }

    @Test
    void script_sha1_returns_null_for_unknown_key() {
        assertThat(client.scriptSha1("unknown-key")).isNull();
    }

    @Test
    void script_source_returns_preloaded_lua() {
        assertThat(client.scriptSource(SCRIPT_RATELIMIT_KEY)).isNotBlank();
    }

    @Test
    void is_connected_true_from_non_vertx_thread_when_event_loop_is_connected() {
        assertThat(CompletableFuture.supplyAsync(client::isConnected).join()).isTrue();
    }

    @Test
    void redis_api_is_available_from_non_vertx_thread() throws Exception {
        RedisAPI api = client.redisApi().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);

        assertThat(api).isNotNull();
    }

    @Test
    void should_connect_without_scripts() throws Exception {
        RedisClient clientWithoutScripts = createClient(null);
        await_connected(clientWithoutScripts);

        assertThat(clientWithoutScripts.isConnected()).isTrue();
        assertThat(clientWithoutScripts.scriptSha1(SCRIPT_RATELIMIT_KEY)).isNull();
        assertThat(clientWithoutScripts.scriptSource(SCRIPT_RATELIMIT_KEY)).isNull();
    }

    @Test
    void is_connected_false_when_redis_unreachable() throws Exception {
        RedisOptions options = new RedisOptions().setConnectionString("redis://127.0.0.1:1");
        options.getNetClientOptions().setConnectTimeout(500);
        RedisClient unreachable = new RedisClient(vertx, options, null);

        TimeUnit.MILLISECONDS.sleep(1_500);

        assertThat(unreachable.isConnected()).isFalse();
    }

    @Test
    void should_recover_rate_limit_after_live_connection_is_killed() throws Exception {
        await_connected_on_context(client);

        RateLimit firstIncrement = increment_rate_limit_on_context("reconnect-key");
        assertThat(firstIncrement.getCounter()).isEqualTo(1L);

        kill_client_connections_on_context(client);
        await_connected_passive_on_context(client);

        RateLimit secondIncrement = increment_rate_limit_on_context("reconnect-key");
        assertThat(secondIncrement.getCounter()).isEqualTo(2L);
    }

    @Test
    void should_deduplicate_reconnect_when_many_connection_failures_are_notified() throws Exception {
        await_connected_on_context(client);

        CountDownLatch latch = new CountDownLatch(1);
        vertx.runOnContext(v -> {
            for (int i = 0; i < 100; i++) {
                client.notifyConnectionFailure(new Exception("Connection is closed"));
            }
            vertx.setTimer(500, id -> latch.countDown());
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        await_connected_passive_on_context(client);
        assertThat(ping_on_context(client)).isEqualTo("PONG");
    }

    @Test
    void should_ignore_non_connection_operation_failures() throws Exception {
        await_connected_on_context(client);

        notify_connection_failure_on_context(client, new Exception("NOSCRIPT No matching script"));

        CountDownLatch latch = new CountDownLatch(1);
        vertx.runOnContext(v -> vertx.setTimer(500, id -> latch.countDown()));
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(ping_on_context(client)).isEqualTo("PONG");
    }

    @Test
    void should_not_invalidate_on_operation_timeout() throws Exception {
        await_connected_on_context(client);

        notify_connection_failure_on_context(client, new RedisOperationTimeoutException(30_000));

        CountDownLatch latch = new CountDownLatch(1);
        vertx.runOnContext(v -> vertx.setTimer(500, id -> latch.countDown()));
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(ping_on_context(client)).isEqualTo("PONG");
    }

    private RedisClient createClient(Map<String, String> scripts) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("ratelimit.redis.host", redis.getHost());
        environment.setProperty("ratelimit.redis.port", redis.getFirstMappedPort().toString());

        return new RedisConnectionFactory(environment, vertx, "ratelimit", scripts).createRedisClient();
    }

    private void await_connected(RedisClient redisClient) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (!redisClient.isConnected() && System.currentTimeMillis() < deadline) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        assertThat(redisClient.isConnected()).isTrue();
    }

    private void await_connected_on_context(RedisClient redisClient) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        poll_connected_on_context(redisClient, System.currentTimeMillis() + 30_000, done);
        done.get(35, TimeUnit.SECONDS);
    }

    private void await_connected_passive_on_context(RedisClient redisClient) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        poll_connected_on_context(redisClient, System.currentTimeMillis() + 30_000, done);
        done.get(35, TimeUnit.SECONDS);
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

    private void kill_client_connections_on_context(RedisClient redisClient) throws Exception {
        CompletableFuture<Void> killed = new CompletableFuture<>();
        vertx.runOnContext(v ->
            redisClient
                .redisApi()
                .compose(api -> api.client(List.of("KILL", "TYPE", "normal", "SKIPME", "yes")))
                .onSuccess(response -> killed.complete(null))
                .onFailure(killed::completeExceptionally)
        );
        killed.get(10, TimeUnit.SECONDS);
    }

    private RateLimit increment_rate_limit_on_context(String key) throws Exception {
        CompletableFuture<RateLimit> result = new CompletableFuture<>();
        vertx.runOnContext(v ->
            repository
                .incrementAndGet(key, 1, () -> {
                    RateLimit rate = new RateLimit(key);
                    rate.setLimit(100);
                    rate.setResetTime(System.currentTimeMillis() + 60_000);
                    rate.setSubscription("sub");
                    return rate;
                })
                .subscribe(result::complete, result::completeExceptionally)
        );
        return result.get(10, TimeUnit.SECONDS);
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
