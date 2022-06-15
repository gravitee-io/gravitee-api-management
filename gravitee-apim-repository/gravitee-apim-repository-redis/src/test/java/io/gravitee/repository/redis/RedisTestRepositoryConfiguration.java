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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author GraviteeSource Team
 */
@ComponentScan("io.gravitee.repository.redis")
public class RedisTestRepositoryConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(RedisTestRepositoryConfiguration.class);

    @Value("${redisVersion:7.0.2}")
    private String redisVersion;

    @Bean(destroyMethod = "stop")
    public GenericContainer<?> redisContainer() {
        var redis = new GenericContainer<>(DockerImageName.parse("redis:" + redisVersion)).withExposedPorts(6379);

        redis.start();

        LOG.info("Running tests with redis version: {}", redisVersion);

        return redis;
    }

    @Bean(name = "rateLimitRedisTemplate")
    public StringRedisTemplate redisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(GenericContainer<?> container) {
        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
        standaloneConfiguration.setHostName(container.getHost());
        standaloneConfiguration.setPort(container.getFirstMappedPort());
        LettucePoolingClientConfiguration options = LettucePoolingClientConfiguration.builder().build();
        return new LettuceConnectionFactory(standaloneConfiguration, options);
    }
}
