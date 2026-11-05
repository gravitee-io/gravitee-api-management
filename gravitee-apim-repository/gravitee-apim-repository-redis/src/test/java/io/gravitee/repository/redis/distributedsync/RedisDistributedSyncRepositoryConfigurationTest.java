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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Future;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisDistributedSyncRepositoryConfigurationTest {

    private static final String HOST = Scope.DISTRIBUTED_SYNC.getName() + ".redis.cluster.nodes[0].host";
    private static final String ALLOW = Scope.DISTRIBUTED_SYNC.getName() + ".redis.cluster.allowDistributedSync";

    @Test
    void should_reject_redis_cluster_unless_a_cluster_wide_search_is_explicitly_allowed() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty(HOST)).thenReturn("10.0.0.1");
        when(environment.getProperty(ALLOW, Boolean.class, false)).thenReturn(false);

        assertThatThrownBy(() -> RedisDistributedSyncRepositoryConfiguration.assertSupportedTopology(environment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("does not support Redis Cluster");

        when(environment.getProperty(ALLOW, Boolean.class, false)).thenReturn(true);
        assertThatCode(() -> RedisDistributedSyncRepositoryConfiguration.assertSupportedTopology(environment)).doesNotThrowAnyException();
    }

    @Test
    void should_accept_a_standalone_redis() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty(HOST)).thenReturn(null);
        when(environment.getProperty(ALLOW, Boolean.class, false)).thenReturn(false);

        assertThatCode(() -> RedisDistributedSyncRepositoryConfiguration.assertSupportedTopology(environment)).doesNotThrowAnyException();
    }

    @Test
    void should_warn_when_the_maxmemory_policy_can_evict_any_key() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        Response policy = mock(Response.class);
        when(policy.toString()).thenReturn("allkeys-lru");
        when(redisAPI.config(List.of("GET", "maxmemory-policy"))).thenReturn(Future.succeededFuture(policy));
        RedisClient redisClient = mock(RedisClient.class);
        when(redisClient.redisApi()).thenReturn(Future.succeededFuture(redisAPI));

        RedisDistributedSyncRepositoryConfiguration.warnOnEvictingPolicy(redisClient);

        verify(redisAPI).config(List.of("GET", "maxmemory-policy"));
    }

    @Test
    void should_stay_quiet_for_a_non_evicting_policy_and_when_the_policy_cannot_be_read() {
        RedisAPI redisAPI = mock(RedisAPI.class);
        Response policy = mock(Response.class);
        when(policy.toString()).thenReturn("noeviction");
        when(redisAPI.config(List.of("GET", "maxmemory-policy"))).thenReturn(Future.succeededFuture(policy));
        RedisClient quiet = mock(RedisClient.class);
        when(quiet.redisApi()).thenReturn(Future.succeededFuture(redisAPI));
        RedisDistributedSyncRepositoryConfiguration.warnOnEvictingPolicy(quiet);

        RedisClient unreachable = mock(RedisClient.class);
        when(unreachable.redisApi()).thenReturn(Future.failedFuture("connection refused"));
        RedisDistributedSyncRepositoryConfiguration.warnOnEvictingPolicy(unreachable);

        verify(redisAPI).config(List.of("GET", "maxmemory-policy"));
    }
}
