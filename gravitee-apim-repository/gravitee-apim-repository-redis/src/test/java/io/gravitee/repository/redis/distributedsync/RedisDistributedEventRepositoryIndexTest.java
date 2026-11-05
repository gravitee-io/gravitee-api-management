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

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.distributedsync.api.search.DistributedEventCriteria;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.gravitee.repository.redis.common.RedisConnectionFactory;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class RedisDistributedEventRepositoryIndexTest {

    private static GenericContainer<?> redis;
    private static Vertx vertx;
    private static RedisClient redisClient;
    private static RedisDistributedEventRepository repository;

    @BeforeAll
    static void startRedis() {
        redis = new GenericContainer<>(
            DockerImageName.parse(System.getProperty("redisImage", "redis/redis-stack:6.2.6-v9"))
        ).withExposedPorts(6379);
        redis.start();
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(Scope.DISTRIBUTED_SYNC.getName() + ".redis.host", redis.getHost());
        environment.setProperty(Scope.DISTRIBUTED_SYNC.getName() + ".redis.port", redis.getFirstMappedPort().toString());
        vertx = Vertx.vertx();
        redisClient = new RedisConnectionFactory(environment, vertx, Scope.DISTRIBUTED_SYNC.getName(), Map.of()).createRedisClient();
        repository = new RedisDistributedEventRepository(redisClient);
        repository.ensureSearchable().blockingAwait(10, TimeUnit.SECONDS);
    }

    @AfterAll
    static void stopRedis() {
        vertx.close();
        redis.stop();
    }

    @Test
    void should_recreate_a_dropped_search_index_and_find_the_existing_events_again() throws Exception {
        repository
            .createOrUpdate(
                DistributedEvent.builder()
                    .clusterId("cluster")
                    .id("api-1")
                    .type(DistributedEventType.API)
                    .syncAction(DistributedSyncAction.DEPLOY)
                    .payload("payload")
                    .updatedAt(new java.util.Date())
                    .build()
            )
            .blockingAwait(10, TimeUnit.SECONDS);
        redisClient
            .redisApi()
            .flatMap(redisAPI -> redisAPI.ftDropindex(List.of("distributed-event-search-idx-v2")))
            .toCompletionStage()
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        repository.ensureSearchable().test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

        // Recreating an index over existing keys indexes them in the background.
        long deadline = System.currentTimeMillis() + 10_000;
        while (!repository.fullyIndexed().blockingGet() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        var events = repository.searchAll(DistributedEventCriteria.builder().clusterId("cluster").build(), 100).toList().blockingGet();
        assertThat(events).extracting(DistributedEvent::getId).containsExactly("api-1");
    }

    @Test
    void should_keep_an_existing_index_untouched() {
        repository.ensureSearchable().test().awaitDone(10, TimeUnit.SECONDS).assertComplete();
        repository.fullyIndexed().test().awaitDone(10, TimeUnit.SECONDS).assertValue(true);
    }
}
