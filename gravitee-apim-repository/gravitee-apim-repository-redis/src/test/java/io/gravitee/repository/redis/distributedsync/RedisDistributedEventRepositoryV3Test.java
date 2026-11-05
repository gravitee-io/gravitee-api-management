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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Future;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisDistributedEventRepositoryV3Test {

    @Test
    void should_keep_starting_when_the_search_indexes_cannot_be_created() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        RedisClient redisClient = redisClientReturning(redisAPI);
        when(redisAPI.ftCreate(any())).thenReturn(Future.failedFuture(new RuntimeException("boom")));
        assertThatCode(() -> new RedisDistributedEventRepository(redisClient)).doesNotThrowAnyException();

        when(redisAPI.ftCreate(any())).thenReturn(Future.failedFuture(new RuntimeException("Index already exists")));
        assertThatCode(() -> new RedisDistributedEventRepository(redisClient)).doesNotThrowAnyException();
    }

    @Test
    void should_propagate_a_v3_search_failure_that_is_not_a_missing_index() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        when(redisAPI.ftAggregate(any())).thenReturn(Future.failedFuture(new RuntimeException("OOM command not allowed")));

        new RedisDistributedEventRepository(redisClientReturning(redisAPI))
            .updateAll("cluster", DistributedEventType.API, "api-1", DistributedSyncAction.UNDEPLOY, new Date())
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertError(error -> error.getMessage().contains("OOM"));
    }

    @Test
    void should_close_a_v3_cursor_when_the_next_page_fails() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        when(redisAPI.hset(any())).thenReturn(Future.succeededFuture(mock(Response.class)));
        Response key = mock(Response.class);
        when(key.toString()).thenReturn("distributed_event:cluster:API:api-1");
        Response row = mock(Response.class);
        when(row.type()).thenReturn(ResponseType.MULTI);
        when(row.get(any())).thenAnswer(invocation -> "__key".equals(invocation.getArgument(0)) ? key : null);
        Response results = mock(Response.class);
        when(results.size()).thenReturn(1);
        when(results.stream()).thenAnswer(invocation -> Stream.of(row));
        Response cursor = mock(Response.class);
        when(cursor.toLong()).thenReturn(7L);
        Response page = mock(Response.class);
        when(page.get(0)).thenReturn(results);
        when(page.get(1)).thenReturn(cursor);
        when(redisAPI.ftAggregate(any())).thenReturn(Future.succeededFuture(page));
        when(redisAPI.ftCursor(any())).thenAnswer(invocation -> {
            List<String> args = invocation.getArgument(0);
            return Future.failedFuture(new RuntimeException("DEL".equals(args.get(0)) ? "cannot close" : "cursor read failed"));
        });

        new RedisDistributedEventRepository(redisClientReturning(redisAPI))
            .updateAll("cluster", DistributedEventType.API, "api-1", DistributedSyncAction.UNDEPLOY, new Date())
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertError(error -> error.getMessage().contains("cursor read failed"));

        verify(redisAPI).ftCursor(List.of("DEL", "distributed-event-search-idx-v3", "7"));
    }

    @Test
    void should_advance_the_change_sequence() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        when(redisAPI.incr(any())).thenReturn(Future.succeededFuture(mock(Response.class)));

        new RedisDistributedEventRepository(redisClientReturning(redisAPI))
            .advanceChangeSequence("cluster")
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete();

        verify(redisAPI).incr("distributed_sync_seq:cluster");
    }

    private static RedisClient redisClientReturning(final RedisAPI redisAPI) {
        RedisClient redisClient = mock(RedisClient.class);
        when(redisClient.redisApi()).thenReturn(Future.succeededFuture(redisAPI));
        when(redisAPI.ftCreate(any())).thenReturn(Future.succeededFuture(mock(Response.class)));
        return redisClient;
    }
}
