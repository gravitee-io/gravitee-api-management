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
package io.gravitee.repository.redis.distributedsync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Future;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import java.nio.channels.ClosedChannelException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Guards the distributed-event write retry: both the classification predicate (which transient failures are
 * worth retrying) and the wiring (that the write is actually retried up to the bound and the final error is
 * surfaced). Runs without a real Redis by mocking {@link RedisClient}.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisDistributedEventRepositoryRetryTest {

    @Test
    void should_classify_dropped_connections_as_retryable() {
        assertThat(RedisDistributedEventRepository.isRetryableWriteFailure(new RuntimeException("Connection is closed"))).isTrue();
        assertThat(RedisDistributedEventRepository.isRetryableWriteFailure(new RuntimeException("Connection reset by peer"))).isTrue();
        assertThat(RedisDistributedEventRepository.isRetryableWriteFailure(new ClosedChannelException())).isTrue();
    }

    @Test
    void should_classify_timeout_reconnect_window_and_queue_saturation_as_retryable() {
        assertThat(RedisDistributedEventRepository.isRetryableWriteFailure(new TimeoutException())).isTrue();
        assertThat(
            RedisDistributedEventRepository.isRetryableWriteFailure(new RuntimeException("Redis connection is not available"))
        ).isTrue();
        assertThat(RedisDistributedEventRepository.isRetryableWriteFailure(new RuntimeException("Timeout on HSET command"))).isTrue();
        assertThat(RedisDistributedEventRepository.isRetryableWriteFailure(new RuntimeException("Connection was closed"))).isTrue();
        assertThat(RedisDistributedEventRepository.isRetryableWriteFailure(new RuntimeException("Redis waiting queue is full"))).isTrue();
    }

    @Test
    void should_classify_a_wrapped_recoverable_cause_as_retryable() {
        Throwable wrapped = new RuntimeException("distribution failed", new TimeoutException("Timeout on command"));
        assertThat(RedisDistributedEventRepository.isRetryableWriteFailure(wrapped)).isTrue();
    }

    @Test
    void should_not_classify_permanent_failures_as_retryable() {
        assertThat(RedisDistributedEventRepository.isRetryableWriteFailure(new RuntimeException("malformed payload"))).isFalse();
        assertThat(RedisDistributedEventRepository.isRetryableWriteFailure(new IllegalStateException())).isFalse();
    }

    @Test
    void should_retry_a_recoverable_write_until_it_succeeds() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        RedisClient redisClient = redisClientReturning(redisAPI);
        AtomicInteger hsetCalls = new AtomicInteger();
        when(redisAPI.hset(any())).thenAnswer(invocation ->
            hsetCalls.incrementAndGet() <= 2
                ? Future.failedFuture(new RuntimeException("Connection is closed"))
                : Future.succeededFuture(mock(Response.class))
        );

        new RedisDistributedEventRepository(redisClient).createOrUpdate(anEvent()).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

        verify(redisAPI, times(3)).hset(any()); // 2 failures + 1 success
    }

    @Test
    void should_not_retry_a_permanent_write_failure() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        RedisClient redisClient = redisClientReturning(redisAPI);
        when(redisAPI.hset(any())).thenReturn(Future.failedFuture(new RuntimeException("malformed payload")));

        new RedisDistributedEventRepository(redisClient)
            .createOrUpdate(anEvent())
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertError(error -> "malformed payload".equals(error.getMessage()));

        verify(redisAPI, times(1)).hset(any());
    }

    private static RedisClient redisClientReturning(final RedisAPI redisAPI) {
        RedisClient redisClient = mock(RedisClient.class);
        when(redisClient.redisApi()).thenReturn(Future.succeededFuture(redisAPI));
        // Constructor eagerly creates the search index.
        when(redisAPI.ftCreate(any())).thenReturn(Future.succeededFuture(mock(Response.class)));
        return redisClient;
    }

    private static DistributedEvent anEvent() {
        return DistributedEvent.builder()
            .id("event-1")
            .clusterId("cluster-1")
            .refId("api-1")
            .refType(DistributedEventType.API)
            .type(DistributedEventType.API)
            .syncAction(DistributedSyncAction.DEPLOY)
            .updatedAt(new Date())
            .build();
    }
}
