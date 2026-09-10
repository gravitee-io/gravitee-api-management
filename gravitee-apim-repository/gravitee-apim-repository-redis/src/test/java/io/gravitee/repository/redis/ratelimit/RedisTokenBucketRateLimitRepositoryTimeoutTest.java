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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.vertx.client.redis.VertxRedisClientFactory;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.gravitee.repository.exception.RedisOperationTimeoutException;
import io.gravitee.repository.ratelimit.api.TokenBucketConsumeResult;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Token-bucket must time the Redis command on the Vert.x context, same as
 * {@link RedisRateLimitRepository}: an RxJava timeout around the whole Single includes
 * event-loop queue time and falsely fires {@link RedisOperationTimeoutException}.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisTokenBucketRateLimitRepositoryTimeoutTest {

    private static final int OPERATION_TIMEOUT_MS = 50;

    private Vertx vertx;
    private AtomicBoolean connected;
    private AtomicInteger connectionFailureNotifications;
    private Function<List<String>, Future<Response>> evalshaHandler;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        connected = new AtomicBoolean(true);
        connectionFailureNotifications = new AtomicInteger();
        evalshaHandler = args -> Future.failedFuture("evalsha handler not configured");
    }

    @AfterEach
    void tearDown() throws Exception {
        CountDownLatch closed = new CountDownLatch(1);
        vertx.close().onComplete(ar -> closed.countDown());
        assertThat(closed.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @Timeout(5)
    void does_not_timeout_when_redis_answered_while_event_loop_is_saturated() throws Exception {
        Promise<Response> redisPromise = Promise.promise();
        evalshaHandler = args -> redisPromise.future();

        var repository = new RedisTokenBucketRateLimitRepository(stubRedisClient(), OPERATION_TIMEOUT_MS);
        CountDownLatch subscribed = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<TokenBucketConsumeResult> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        vertx.runOnContext(v -> {
            repository
                .refillAndTryConsume("k", 1, 10, 1_000L, 20, 1_000L, () -> new TokenBucket("k"))
                .subscribe(
                    r -> {
                        result.set(r);
                        done.countDown();
                    },
                    t -> {
                        error.set(t);
                        done.countDown();
                    }
                );
            subscribed.countDown();
            try {
                Thread.sleep(OPERATION_TIMEOUT_MS * 4L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertThat(subscribed.await(2, TimeUnit.SECONDS)).isTrue();
        redisPromise.complete(consumeResponse(1L, 5L));

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).as("must not raise a false RedisOperationTimeoutException").isNull();
        assertThat(result.get()).isNotNull();
        assertThat(result.get().allowed()).isTrue();
        assertThat(result.get().remainingTokens()).isEqualTo(5L);
    }

    @Test
    @Timeout(5)
    void times_out_when_redis_command_never_completes() throws Exception {
        Promise<Response> redisPromise = Promise.promise();
        evalshaHandler = args -> redisPromise.future();

        var repository = new RedisTokenBucketRateLimitRepository(stubRedisClient(), OPERATION_TIMEOUT_MS);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        vertx.runOnContext(v ->
            repository
                .refillAndTryConsume("k", 1, 10, 1_000L, 20, 1_000L, () -> new TokenBucket("k"))
                .subscribe(
                    r -> done.countDown(),
                    t -> {
                        error.set(t);
                        done.countDown();
                    }
                )
        );

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isInstanceOf(RedisOperationTimeoutException.class);
        assertThat(connectionFailureNotifications.get()).isEqualTo(1);
    }

    private RedisClient stubRedisClient() {
        RedisClientOptions options = RedisClientOptions.builder().host("127.0.0.1").port(1).connectTimeout(500).build();
        return new RedisClient(vertx, new VertxRedisClientFactory(vertx), options, Map.of()) {
            @Override
            public boolean isConnected() {
                return connected.get();
            }

            @Override
            public Future<RedisAPI> redisApi() {
                return Future.succeededFuture(redisApiProxy());
            }

            @Override
            public String scriptSha1(final String key) {
                return "the-sha";
            }

            @Override
            public void notifyConnectionFailure(final Throwable failure) {
                connectionFailureNotifications.incrementAndGet();
            }
        };
    }

    private RedisAPI redisApiProxy() {
        return (RedisAPI) Proxy.newProxyInstance(
            RedisAPI.class.getClassLoader(),
            new Class<?>[] { RedisAPI.class },
            (proxy, method, args) -> {
                if ("evalsha".equals(method.getName()) && args != null && args.length == 1) {
                    @SuppressWarnings("unchecked")
                    List<String> command = (List<String>) args[0];
                    return evalshaHandler.apply(command);
                }
                if ("toString".equals(method.getName())) {
                    return "RedisAPI-stub";
                }
                throw new UnsupportedOperationException("Unexpected RedisAPI call: " + method.getName());
            }
        );
    }

    private static Response consumeResponse(long allowed, long tokens) {
        List<Response> fields = List.of(longResponse(allowed), longResponse(tokens));
        return new Response() {
            @Override
            public ResponseType type() {
                return ResponseType.MULTI;
            }

            @Override
            public String toString() {
                return fields.toString();
            }

            @Override
            public Response get(int index) {
                return fields.get(index);
            }

            @Override
            public int size() {
                return fields.size();
            }
        };
    }

    private static Response longResponse(long value) {
        return new Response() {
            @Override
            public ResponseType type() {
                return ResponseType.NUMBER;
            }

            @Override
            public String toString() {
                return Long.toString(value);
            }

            @Override
            public Long toLong() {
                return value;
            }
        };
    }
}
