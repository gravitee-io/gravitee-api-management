/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.redis;

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.redis.common.RedisConnectionFactory;
import io.gravitee.repository.redis.ratelimit.RedisRateLimitRepository;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Value("${redisVersion:7.0.10}")
    private String redisVersion;

    @Bean(destroyMethod = "stop")
    public GenericContainer<?> redisContainer() {
        var redis = new GenericContainer<>(DockerImageName.parse("redis:" + redisVersion)).withExposedPorts(6379);

        redis.start();

        LOG.info("Running tests with redis version: {}", redisVersion);

        return redis;
    }

    @Bean
    public RedisClient redisConnectionFactory(GenericContainer<?> redisContainer) {
        String propertyPrefix = Scope.RATE_LIMIT.getName() + ".redis.";

        MockEnvironment mockEnvironment = new MockEnvironment();
        mockEnvironment.setProperty(propertyPrefix + "host", redisContainer.getHost());
        mockEnvironment.setProperty(propertyPrefix + "port", redisContainer.getFirstMappedPort().toString());

        RedisConnectionFactory redisConnectionFactory = new RedisConnectionFactory(
            mockEnvironment,
            Vertx.vertx(),
            Scope.RATE_LIMIT.getName()
        );

        return redisConnectionFactory.getObject();
    }

    @Bean
    public RedisRateLimitRepository redisRateLimitRepository(RedisClient redisClient) {
        return new RedisRateLimitRepository(redisClient);
    }
}
