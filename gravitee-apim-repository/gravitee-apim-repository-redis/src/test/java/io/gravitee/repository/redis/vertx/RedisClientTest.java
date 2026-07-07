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

import io.gravitee.node.vertx.client.redis.VertxRedisClientFactory;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.gravitee.repository.redis.common.RedisConnectionFactory;
import io.vertx.core.Vertx;
import io.vertx.redis.client.RedisAPI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

    @BeforeAll
    void start_redis() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available");

        redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
        redis.start();

        client = createClient(Map.of(SCRIPT_RATELIMIT_KEY, SCRIPTS_RATELIMIT_LUA));
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
        RedisClientOptions options = RedisClientOptions.builder().host("127.0.0.1").port(1).connectTimeout(500).build();
        RedisClient unreachable = new RedisClient(vertx, new VertxRedisClientFactory(vertx), options, null);

        TimeUnit.MILLISECONDS.sleep(1_500);

        assertThat(unreachable.isConnected()).isFalse();
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
}
