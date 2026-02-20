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
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisDistributedSyncRepositoryConfiguration {

    protected static final String REDIS_KEY_SEPARATOR = ":";

    @Bean("redisDistributedSyncClient")
    public RedisClient redisRedisClient(Environment environment, Vertx vertx) {
        return new RedisConnectionFactory(
            environment,
            vertx,
            "repositories." + Scope.DISTRIBUTED_SYNC.getName(),
            Map.of()
        ).createRedisClient();
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
