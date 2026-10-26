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
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.distributedsync.api.DistributedSyncUnavailableException;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Future;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisDistributedEventRepositoryFencedWriteTest {

    @Test
    void should_acquire_a_lease_when_redis_grants_it() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        Response granted = mock(Response.class);
        when(granted.toString()).thenReturn("OK");
        when(redisAPI.set(any())).thenReturn(Future.succeededFuture(granted));

        new RedisDistributedEventRepository(redisClientReturning(redisAPI))
            .applyFencedWrite("distributed_sync_lease:cluster", "node-1:1", 30_000)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete();

        verify(redisAPI).set(List.of("distributed_sync_lease:cluster", "node-1:1", "NX", "PX", "30000"));
    }

    @Test
    void should_keep_a_lease_held_by_another_node_as_a_lease_error() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        Response denied = mock(Response.class);
        when(denied.toString()).thenReturn(null);
        when(redisAPI.set(any())).thenReturn(Future.succeededFuture(denied));

        new RedisDistributedEventRepository(redisClientReturning(redisAPI))
            .applyFencedWrite("distributed_sync_lease:cluster", "node-1:1", 30_000)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertError(IllegalStateException.class);
    }

    @Test
    void should_report_the_repository_unavailable_when_the_lease_command_fails() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        when(redisAPI.set(any())).thenReturn(Future.failedFuture(new RuntimeException("Connection is closed")));

        new RedisDistributedEventRepository(redisClientReturning(redisAPI))
            .applyFencedWrite("distributed_sync_lease:cluster", "node-1:1", 30_000)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertError(DistributedSyncUnavailableException.class);
    }

    @Test
    void should_write_through_the_lease_script() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        List<List<String>> attempts = new ArrayList<>();
        when(redisAPI.evalsha(any())).thenAnswer(invocation -> {
            attempts.add(invocation.getArgument(0));
            return Future.succeededFuture(mock(Response.class));
        });

        new RedisDistributedEventRepository(redisClientReturning(redisAPI))
            .createOrUpdateFenced(anEvent(), "node-1:1")
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete();

        assertThat(attempts.get(0)).contains(
            RedisDistributedEventRepository.WRITE_SCRIPT_KEY,
            "node-1:1",
            DistributedEvent.Fields.refType,
            DistributedEventType.API.name(),
            DistributedEvent.Fields.refId,
            "api-1",
            DistributedEvent.Fields.schemaVersion,
            "3"
        );
    }

    @Test
    void should_fall_back_to_eval_when_the_script_is_not_cached() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        RedisClient redisClient = redisClientReturning(redisAPI);
        when(redisClient.scriptSource(RedisDistributedEventRepository.WRITE_SCRIPT_KEY)).thenReturn("return 1");
        when(redisAPI.evalsha(any())).thenReturn(Future.failedFuture(new RuntimeException("NOSCRIPT No matching script")));
        List<List<String>> evals = new ArrayList<>();
        when(redisAPI.eval(any())).thenAnswer(invocation -> {
            evals.add(invocation.getArgument(0));
            return Future.succeededFuture(mock(Response.class));
        });

        new RedisDistributedEventRepository(redisClient)
            .createOrUpdateFenced(anEvent(), "node-1:1")
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete();

        assertThat(evals.get(0).get(0)).isEqualTo("return 1");
    }

    @Test
    void should_fail_when_the_script_source_was_not_loaded() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        RedisClient redisClient = redisClientReturning(redisAPI);
        when(redisClient.scriptSource(any())).thenReturn(null);
        when(redisAPI.evalsha(any())).thenReturn(Future.failedFuture(new RuntimeException("NOSCRIPT No matching script")));

        new RedisDistributedEventRepository(redisClient)
            .createOrUpdateFenced(anEvent(), "node-1:1")
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertError(IllegalStateException.class);
    }

    @Test
    void should_surface_an_unreachable_repository_once_fenced_retries_are_exhausted() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        when(redisAPI.evalsha(any())).thenReturn(Future.failedFuture(new RuntimeException("Connection is closed")));

        new RedisDistributedEventRepository(redisClientReturning(redisAPI))
            .createOrUpdateFenced(anEvent(), "node-1:1")
            .test()
            .awaitDone(20, TimeUnit.SECONDS)
            .assertError(DistributedSyncUnavailableException.class);

        verify(redisAPI, atLeast(2)).evalsha(any());
    }

    @Test
    void should_read_the_redis_server_clock() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        Response seconds = mock(Response.class);
        Response micros = mock(Response.class);
        when(seconds.toLong()).thenReturn(1_700_000_000L);
        when(micros.toLong()).thenReturn(5_000L);
        Response time = mock(Response.class);
        when(time.get(0)).thenReturn(seconds);
        when(time.get(1)).thenReturn(micros);
        when(redisAPI.time()).thenReturn(Future.succeededFuture(time));

        Long clock = new RedisDistributedEventRepository(redisClientReturning(redisAPI))
            .serverTime()
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete()
            .values()
            .get(0);

        assertThat(clock).isEqualTo(1_700_000_000_000L + 5L);
    }

    private static RedisClient redisClientReturning(final RedisAPI redisAPI) {
        RedisClient redisClient = mock(RedisClient.class);
        when(redisClient.redisApi()).thenReturn(Future.succeededFuture(redisAPI));
        when(redisAPI.ftCreate(any())).thenReturn(Future.succeededFuture(mock(Response.class)));
        return redisClient;
    }

    private static DistributedEvent anEvent() {
        DistributedEvent event = DistributedEvent.builder()
            .id("event-1")
            .clusterId("cluster-1")
            .refType(DistributedEventType.API)
            .refId("api-1")
            .type(DistributedEventType.API)
            .syncAction(DistributedSyncAction.DEPLOY)
            .payload("payload")
            .updatedAt(new Date(1_000))
            .build();
        event.setSchemaVersion(3);
        return event;
    }
}
