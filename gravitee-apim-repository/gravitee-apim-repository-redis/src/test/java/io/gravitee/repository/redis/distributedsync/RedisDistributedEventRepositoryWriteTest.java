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
import static org.mockito.Mockito.when;

import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Future;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RedisDistributedEventRepositoryWriteTest {

    @Test
    void should_stamp_every_write_attempt_with_the_time_it_is_sent() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        List<List<String>> attempts = Collections.synchronizedList(new ArrayList<>());
        when(redisAPI.hset(any())).thenAnswer(invocation -> {
            attempts.add(invocation.getArgument(0));
            return attempts.size() == 1
                ? Future.failedFuture(new RuntimeException("Connection is closed"))
                : Future.succeededFuture(mock(Response.class));
        });
        long before = System.currentTimeMillis();

        new RedisDistributedEventRepository(redisClientReturning(redisAPI))
            .createOrUpdateAtWriteTime(anEvent(new Date(1_000)))
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete();

        assertThat(attempts).hasSize(2);
        long firstStamp = updatedAt(attempts.get(0));
        long secondStamp = updatedAt(attempts.get(1));
        assertThat(firstStamp).isGreaterThanOrEqualTo(before);
        assertThat(secondStamp).isGreaterThan(firstStamp);
    }

    @Test
    void should_keep_the_provided_update_date_on_a_plain_write() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        List<List<String>> attempts = Collections.synchronizedList(new ArrayList<>());
        when(redisAPI.hset(any())).thenAnswer(invocation -> {
            attempts.add(invocation.getArgument(0));
            return Future.succeededFuture(mock(Response.class));
        });

        new RedisDistributedEventRepository(redisClientReturning(redisAPI))
            .createOrUpdate(anEvent(new Date(1_000)))
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete();

        assertThat(updatedAt(attempts.get(0))).isEqualTo(1_000L);
    }

    @Test
    void should_only_update_the_reference_and_its_children_and_not_a_reference_sharing_its_id_prefix() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        List<String> scanArgs = new ArrayList<>();
        when(redisAPI.scan(any())).thenAnswer(invocation -> {
            scanArgs.addAll(invocation.getArgument(0));
            return Future.succeededFuture(
                scanResponse(
                    "distributed_event:cluster:API:api-1",
                    "distributed_event:cluster:API:api-1:SUBSCRIPTION:sub-1",
                    "distributed_event:cluster:API:api-10",
                    "distributed_event:cluster:API:api-10:SUBSCRIPTION:sub-2"
                )
            );
        });
        List<String> updatedKeys = Collections.synchronizedList(new ArrayList<>());
        when(redisAPI.hset(any())).thenAnswer(invocation -> {
            List<String> args = invocation.getArgument(0);
            updatedKeys.add(args.get(0));
            return Future.succeededFuture(mock(Response.class));
        });

        new RedisDistributedEventRepository(redisClientReturning(redisAPI))
            .updateAllAtWriteTime("cluster", DistributedEventType.API, "api-1", DistributedSyncAction.UNDEPLOY)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete();

        assertThat(updatedKeys).containsExactlyInAnyOrder(
            "distributed_event:cluster:API:api-1",
            "distributed_event:cluster:API:api-1:SUBSCRIPTION:sub-1"
        );
        assertThat(scanArgs).contains("distributed_event:cluster:API:api-1*");
    }

    @Test
    void should_escape_glob_characters_in_the_scan_pattern() {
        assertThat(RedisDistributedEventRepository.escapeGlob("a*b?c[d]e\\f")).isEqualTo("a\\*b\\?c\\[d\\]e\\\\f");
        assertThat(RedisDistributedEventRepository.escapeGlob("plain-id")).isEqualTo("plain-id");
    }

    private static long updatedAt(final List<String> hsetArgs) {
        int index = hsetArgs.indexOf(DistributedEvent.Fields.updatedAt);
        return Long.parseLong(hsetArgs.get(index + 1));
    }

    private static Response scanResponse(final String... keys) {
        Response response = mock(Response.class);
        Response cursor = mock(Response.class);
        Response keyList = mock(Response.class);
        when(response.type()).thenReturn(ResponseType.MULTI);
        when(response.get(0)).thenReturn(cursor);
        when(response.get(1)).thenReturn(keyList);
        when(cursor.toLong()).thenReturn(0L);
        when(keyList.stream()).thenAnswer(invocation ->
            Stream.of(keys).map(key -> {
                Response keyResponse = mock(Response.class);
                when(keyResponse.toString()).thenReturn(key);
                return keyResponse;
            })
        );
        return response;
    }

    private static RedisClient redisClientReturning(final RedisAPI redisAPI) {
        RedisClient redisClient = mock(RedisClient.class);
        when(redisClient.redisApi()).thenReturn(Future.succeededFuture(redisAPI));
        when(redisAPI.ftCreate(any())).thenReturn(Future.succeededFuture(mock(Response.class)));
        return redisClient;
    }

    private static DistributedEvent anEvent(final Date updatedAt) {
        return DistributedEvent.builder()
            .id("event-1")
            .clusterId("cluster-1")
            .type(DistributedEventType.API)
            .syncAction(DistributedSyncAction.DEPLOY)
            .updatedAt(updatedAt)
            .build();
    }
}
