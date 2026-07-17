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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.exception.RedisNotConnectedException;
import io.gravitee.repository.exception.RedisOperationTimeoutException;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import io.vertx.rxjava3.RxHelper;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
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
 * Covers APIM-14455: operation.timeout must measure Redis command time on the Vert.x
 * context, not event-loop queue delay observed by an RxJava timeout.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisRateLimitRepositoryTest {

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
    void fails_fast_when_redis_is_not_connected() {
        connected.set(false);
        var repository = new RedisRateLimitRepository(stubRedisClient(Future.succeededFuture(redisApiProxy())), OPERATION_TIMEOUT_MS);

        assertThatThrownBy(() -> repository.incrementAndGet("k", 1, () -> new RateLimit("k")).blockingGet())
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(RedisNotConnectedException.class);
    }

    @Test
    void maps_successful_evalsha_response() {
        evalshaHandler = args -> Future.succeededFuture(rateResponse(7L, 100L, 123L, "sub-1"));
        var repository = new RedisRateLimitRepository(stubRedisClient(Future.succeededFuture(redisApiProxy())), OPERATION_TIMEOUT_MS);

        RateLimit result = repository.incrementAndGet("my-key", 1, () -> new RateLimit("my-key")).blockingGet();

        assertThat(result.getCounter()).isEqualTo(7L);
        assertThat(result.getLimit()).isEqualTo(100L);
        assertThat(result.getResetTime()).isEqualTo(123L);
        assertThat(result.getSubscription()).isEqualTo("sub-1");
    }

    @Test
    @Timeout(5)
    void does_not_timeout_when_redis_answered_while_event_loop_is_saturated() throws Exception {
        // Reproduces the customer race: Redis completes quickly, but the event loop is
        // blocked so the reply cannot be delivered until after operation.timeout.
        // Old RxJava .timeout (computation scheduler) falsely failed; Vert.x Future.timeout
        // shares the event loop and must succeed once the loop unblocks.
        Promise<Response> redisPromise = Promise.promise();
        evalshaHandler = args -> redisPromise.future();

        var repository = new RedisRateLimitRepository(stubRedisClient(Future.succeededFuture(redisApiProxy())), OPERATION_TIMEOUT_MS);
        CountDownLatch subscribed = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<RateLimit> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        vertx.runOnContext(v -> {
            repository
                .incrementAndGet("k", 1, () -> new RateLimit("k"))
                .subscribe(
                    rl -> {
                        result.set(rl);
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
        // Simulate Redis I/O completing while the event loop is still blocked.
        redisPromise.complete(rateResponse(3L, 10L, 99L, "sub"));

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).as("must not raise a false RedisOperationTimeoutException").isNull();
        assertThat(result.get()).isNotNull();
        assertThat(result.get().getCounter()).isEqualTo(3L);
    }

    @Test
    @Timeout(5)
    void times_out_when_redis_command_never_completes() throws Exception {
        Promise<Response> redisPromise = Promise.promise();
        evalshaHandler = args -> redisPromise.future();

        var repository = new RedisRateLimitRepository(stubRedisClient(Future.succeededFuture(redisApiProxy())), OPERATION_TIMEOUT_MS);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        vertx.runOnContext(v ->
            repository
                .incrementAndGet("k", 1, () -> new RateLimit("k"))
                .subscribe(
                    rl -> done.countDown(),
                    t -> {
                        error.set(t);
                        done.countDown();
                    }
                )
        );

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isInstanceOf(RedisOperationTimeoutException.class);
        assertThat(error.get().getMessage()).contains(String.valueOf(OPERATION_TIMEOUT_MS));
        // Timeouts must not look like connection failures (would force reconnect storms).
        assertThat(connectionFailureNotifications.get()).isZero();
    }

    @Test
    @Timeout(5)
    void does_not_apply_command_timeout_to_redis_api_resolution_delay() throws Exception {
        // redisApi() taking longer than operation.timeout must not fail the command itself
        // once evalsha returns promptly — timeout starts at command dispatch.
        Promise<RedisAPI> apiPromise = Promise.promise();
        evalshaHandler = args -> Future.succeededFuture(rateResponse(1L, 5L, 1L, "sub"));

        var repository = new RedisRateLimitRepository(stubRedisClient(apiPromise.future()), OPERATION_TIMEOUT_MS);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<RateLimit> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        repository
            .incrementAndGet("k", 1, () -> new RateLimit("k"))
            .subscribe(
                rl -> {
                    result.set(rl);
                    done.countDown();
                },
                t -> {
                    error.set(t);
                    done.countDown();
                }
            );

        // Resolve API after the old operation.timeout budget would have expired.
        Thread.sleep(OPERATION_TIMEOUT_MS * 3L);
        apiPromise.complete(redisApiProxy());

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isNull();
        assertThat(result.get()).isNotNull();
        assertThat(result.get().getCounter()).isEqualTo(1L);
    }

    @Test
    @Timeout(15)
    void concurrent_bursts_with_intermittent_event_loop_stalls_do_not_false_timeout() throws Exception {
        // Load-style smoke: many overlapping increments, fast Redis RTT, occasional
        // event-loop stalls longer than operation.timeout — none should false-timeout.
        //
        // Redis must complete the Vert.x Promise off the event loop (not via
        // executeBlocking().onComplete). Otherwise, after a stall, Vert.x may run an
        // already-expired Future.timeout timer before the queued completion handler —
        // a test artifact, not the production false-timeout race.
        final int operations = 40;
        final int stallEvery = 8;
        AtomicInteger evalshaCalls = new AtomicInteger();

        evalshaHandler = args -> {
            int n = evalshaCalls.incrementAndGet();
            Promise<Response> promise = Promise.promise();
            Thread io = new Thread(
                () -> {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    promise.complete(rateResponse(n, 1000L, System.currentTimeMillis(), "sub"));
                },
                "rl-redis-io-" + n
            );
            io.setDaemon(true);
            io.start();
            return promise.future();
        };

        var repository = new RedisRateLimitRepository(stubRedisClient(Future.succeededFuture(redisApiProxy())), OPERATION_TIMEOUT_MS);
        CyclicBarrier start = new CyclicBarrier(operations);
        CountDownLatch done = new CountDownLatch(operations);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        List<Throwable> errors = new ArrayList<>();

        for (int i = 0; i < operations; i++) {
            final int idx = i;
            new Thread(
                () -> {
                    try {
                        start.await(5, TimeUnit.SECONDS);
                        Single<RateLimit> call = repository
                            .incrementAndGet("burst-" + idx, 1, () -> new RateLimit("burst-" + idx))
                            .subscribeOn(RxHelper.scheduler(vertx));

                        if (idx % stallEvery == 0) {
                            // Stall the event loop after the subscribe is scheduled, mimicking
                            // gateway pressure that used to trip the RxJava timeout.
                            vertx.runOnContext(v -> {
                                try {
                                    Thread.sleep(OPERATION_TIMEOUT_MS * 3L);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            });
                        }

                        call.blockingSubscribe(
                            rl -> {
                                successes.incrementAndGet();
                                done.countDown();
                            },
                            t -> {
                                synchronized (errors) {
                                    errors.add(t);
                                }
                                failures.incrementAndGet();
                                done.countDown();
                            }
                        );
                    } catch (Exception e) {
                        synchronized (errors) {
                            errors.add(e);
                        }
                        failures.incrementAndGet();
                        done.countDown();
                    }
                },
                "rl-burst-" + i
            )
                .start();
        }

        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        assertThat(failures.get()).as("failures: %s", errors).isZero();
        assertThat(successes.get()).isEqualTo(operations);
        assertThat(errors).isEmpty();
    }

    @Test
    @Timeout(5)
    void propagates_non_timeout_redis_failures() {
        evalshaHandler = args -> Future.failedFuture(new RuntimeException("LOADING Redis is loading the dataset"));
        var repository = new RedisRateLimitRepository(stubRedisClient(Future.succeededFuture(redisApiProxy())), OPERATION_TIMEOUT_MS);

        assertThatThrownBy(() -> repository.incrementAndGet("k", 1, () -> new RateLimit("k")).blockingGet()).hasMessageContaining(
            "LOADING"
        );
        assertThat(connectionFailureNotifications.get()).isEqualTo(1);
    }

    /**
     * Stub that overrides redisApi/isConnected so the per-loop connect machinery is not used.
     * Empty scripts map avoids classpath script loading in the parent constructor path.
     */
    private RedisClient stubRedisClient(Future<RedisAPI> api) {
        RedisOptions options = new RedisOptions().setConnectionString("redis://127.0.0.1:1");
        return new RedisClient(vertx, options, Map.of()) {
            @Override
            public boolean isConnected() {
                return connected.get();
            }

            @Override
            public Future<RedisAPI> redisApi() {
                return api;
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

    private static Response rateResponse(long counter, long limit, long reset, String subscription) {
        List<Response> fields = List.of(longResponse(counter), longResponse(limit), longResponse(reset), stringResponse(subscription));
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

    private static Response stringResponse(String value) {
        return new Response() {
            @Override
            public ResponseType type() {
                return ResponseType.BULK;
            }

            @Override
            public String toString() {
                return value;
            }
        };
    }
}
