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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.vertx.client.redis.VertxRedisClientFactory;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.vertx.core.Vertx;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * On Vert.x 5.0.x a closed Redis client stays registered on the close future of the context that created it.
 * Reconnecting must therefore reuse the client instead of creating a new one per attempt.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisClientCloseFutureRetentionTest {

    private static final int CONNECT_ATTEMPTS = 8;

    private Vertx vertx;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        vertx.close().await();
    }

    @Test
    void should_not_retain_a_client_per_failed_connect_attempt() throws Exception {
        CountingConnectFactory factory = new CountingConnectFactory(vertx);
        RedisClientOptions options = RedisClientOptions.builder().host("127.0.0.1").port(1).connectTimeout(500).build();
        int before = RetainedClients.count(vertx);

        RedisClient client = new RedisClient(vertx, factory, options, null);
        try {
            Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> factory.connects.get() >= CONNECT_ATTEMPTS);
            assertThat(factory.connects.get()).isGreaterThanOrEqualTo(CONNECT_ATTEMPTS);

            assertThat(RetainedClients.count(vertx) - before).as("Redis clients retained by the close future").isLessThanOrEqualTo(1);
        } finally {
            client.close();
        }
    }

    @Test
    void should_stop_reconnecting_and_close_its_client_once_closed() throws Exception {
        CountingConnectFactory factory = new CountingConnectFactory(vertx);
        RedisClientOptions options = RedisClientOptions.builder().host("127.0.0.1").port(1).connectTimeout(500).build();
        RedisClient client = new RedisClient(vertx, factory, options, null);
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .until(() -> factory.connects.get() >= 2);

        client.close();
        int connectsAtClose = factory.connects.get();
        // No reconnect is scheduled any more: the count stays put while backoffs would have elapsed.
        Awaitility.await()
            .during(2, TimeUnit.SECONDS)
            .atMost(3, TimeUnit.SECONDS)
            .until(() -> factory.connects.get() == connectsAtClose);

        assertThat(factory.closes.get()).as("clients closed").isEqualTo(factory.creates.get());
        assertThat(factory.connects.get()).as("connect attempts after close").isEqualTo(connectsAtClose);
        assertThat(client.redisApi().failed()).isTrue();
    }

    @Test
    void should_still_leak_when_the_client_comes_straight_from_vertx() {
        // This watches the Vert.x version the repository is built against (vertx.version in the root pom).
        RedisClientOptions options = RedisClientOptions.builder().host("127.0.0.1").port(1).build();
        int before = RetainedClients.count(vertx);

        for (int i = 0; i < CONNECT_ATTEMPTS; i++) {
            new VertxRedisClientFactory(vertx).createClient(options).close().await();
        }

        assertThat(RetainedClients.count(vertx) - before)
            .as(
                "[vertx-5.0-close-future-leak] Vert.x no longer leaks closed clients: RedisClient.createClient can create the client directly, drop this test"
            )
            .isEqualTo(CONNECT_ATTEMPTS);
    }
}
