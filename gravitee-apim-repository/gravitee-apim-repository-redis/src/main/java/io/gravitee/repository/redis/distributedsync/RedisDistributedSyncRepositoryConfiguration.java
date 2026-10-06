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

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.redis.common.RedisConnectionFactory;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class RedisDistributedSyncRepositoryConfiguration {

    protected static final String REDIS_KEY_SEPARATOR = ":";

    @Bean("redisDistributedSyncClient")
    public RedisClient redisRedisClient(Environment environment, Vertx vertx) {
        assertSupportedTopology(environment);
        RedisClient redisClient = new RedisConnectionFactory(
            environment,
            vertx,
            Scope.DISTRIBUTED_SYNC.getName(),
            Map.of()
        ).createRedisClient();
        warnOnEvictingPolicy(redisClient);
        return redisClient;
    }

    /**
     * Distributed sync relies on SCAN and on a RediSearch index, which in an OSS Redis Cluster only cover the shard a
     * command lands on: secondaries would silently get a partial catalog. Only deployments providing a cluster-wide
     * search (e.g. Redis Enterprise) can opt in.
     */
    static void assertSupportedTopology(final Environment environment) {
        String prefix = Scope.DISTRIBUTED_SYNC.getName() + ".redis.";
        boolean clusterMode = StringUtils.hasText(environment.getProperty(prefix + "cluster.nodes[0].host"));
        boolean allowed = environment.getProperty(prefix + "cluster.allowDistributedSync", Boolean.class, false);
        if (clusterMode && !allowed) {
            throw new IllegalStateException(
                "Distributed sync does not support Redis Cluster: searches and scans only cover one shard. Use a standalone or " +
                    "Sentinel Redis, or set '" +
                    prefix +
                    "cluster.allowDistributedSync=true' if your Redis provides a cluster-wide search."
            );
        }
    }

    /**
     * Distributed events have no expiry: a policy evicting any key may silently drop part of the catalog.
     */
    private static void warnOnEvictingPolicy(final RedisClient redisClient) {
        redisClient
            .redisApi()
            .flatMap(redisAPI -> redisAPI.config(List.of("GET", "maxmemory-policy")))
            .onSuccess(response -> {
                String policy = String.valueOf(response);
                if (policy.contains("allkeys")) {
                    log.warn(
                        "Redis used for distributed sync has an evicting maxmemory-policy ({}): distributed events may be evicted and " +
                            "secondary gateways would miss them. Use 'noeviction' or a 'volatile-*' policy.",
                        policy
                    );
                }
            })
            .onFailure(throwable -> log.debug("Unable to read the Redis maxmemory-policy", throwable));
    }

    @Bean
    public RedisDistributedEventRepository redisDistributedEventRepository(
        @Qualifier("redisDistributedSyncClient") RedisClient redisClient
    ) {
        return new RedisDistributedEventRepository(redisClient);
    }

    @Bean
    public RedisDistributedSyncStateRepository redisDistributedSyncStateRepository(
        @Qualifier("redisDistributedSyncClient") RedisClient redisClient
    ) {
        return new RedisDistributedSyncStateRepository(redisClient);
    }
}
