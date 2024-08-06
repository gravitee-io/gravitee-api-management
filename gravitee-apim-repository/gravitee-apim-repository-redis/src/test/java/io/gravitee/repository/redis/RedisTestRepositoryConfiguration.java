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
package io.gravitee.repository.redis;

import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPTS_RATELIMIT_LUA;
import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPT_RATELIMIT_KEY;

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.redis.common.RedisConnectionFactory;
import io.gravitee.repository.redis.distributedsync.RedisDistributedEventRepository;
import io.gravitee.repository.redis.distributedsync.RedisDistributedSyncStateRepository;
import io.gravitee.repository.redis.ratelimit.RedisRateLimitRepository;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Vertx;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author GraviteeSource Team
 */
@ComponentScan("io.gravitee.repository.redis")
public class RedisTestRepositoryConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(RedisTestRepositoryConfiguration.class);

    @Value("${redisStackVersion:6.2.6-v9}")
    private String redisStackVersion;

    @Bean(destroyMethod = "stop")
    public GenericContainer<?> redisContainer() {
        var redis = new GenericContainer<>(DockerImageName.parse("redis/redis-stack:" + redisStackVersion)).withExposedPorts(6379);

        redis.start();

        LOG.info("Running tests with redis version: {}", redisStackVersion);

        return redis;
    }

    @Bean
    public RedisClient redisRateLimitClient(GenericContainer<?> redisContainer) {
        String propertyPrefix = Scope.RATE_LIMIT.getName() + ".redis.";

        MockEnvironment mockEnvironment = new MockEnvironment();
        mockEnvironment.setProperty(propertyPrefix + "host", redisContainer.getHost());
        mockEnvironment.setProperty(propertyPrefix + "port", redisContainer.getFirstMappedPort().toString());

        RedisConnectionFactory redisConnectionFactory = new RedisConnectionFactory(
            mockEnvironment,
            Vertx.vertx(),
            Scope.RATE_LIMIT.getName(),
            Map.of(SCRIPT_RATELIMIT_KEY, SCRIPTS_RATELIMIT_LUA)
        );

        return redisConnectionFactory.createRedisClient();
    }

    @Bean
    public RedisRateLimitRepository redisRateLimitRepository(@Qualifier("redisRateLimitClient") RedisClient redisRateLimitClient) {
        return new RedisRateLimitRepository(redisRateLimitClient, 500);
    }

    @Bean
    public RedisClient redisDistributedSyncClient(GenericContainer<?> redisContainer) {
        String propertyPrefix = Scope.DISTRIBUTED_SYNC.getName() + ".redis.";

        MockEnvironment mockEnvironment = new MockEnvironment();
        mockEnvironment.setProperty(propertyPrefix + "host", redisContainer.getHost());
        mockEnvironment.setProperty(propertyPrefix + "port", redisContainer.getFirstMappedPort().toString());

        RedisConnectionFactory redisConnectionFactory = new RedisConnectionFactory(
            mockEnvironment,
            Vertx.vertx(),
            Scope.DISTRIBUTED_SYNC.getName(),
            Map.of()
        );

        return redisConnectionFactory.createRedisClient();
    }

    @Bean
    public RedisDistributedEventRepository redisDistributedEventRepository(
        @Qualifier("redisDistributedSyncClient") RedisClient redisDistributedSyncClient
    ) {
        return new RedisDistributedEventRepository(redisDistributedSyncClient);
    }

    @Bean
    public RedisDistributedSyncStateRepository redisDistributedSyncStateRepository(
        @Qualifier("redisDistributedSyncClient") RedisClient redisDistributedSyncClient
    ) {
        return new RedisDistributedSyncStateRepository(redisDistributedSyncClient);
    }
}
