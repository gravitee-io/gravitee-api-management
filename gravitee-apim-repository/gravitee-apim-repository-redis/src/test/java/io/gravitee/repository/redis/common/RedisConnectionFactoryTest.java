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
package io.gravitee.repository.redis.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.vertx.redis.client.RedisOptions;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisConnectionFactoryTest {

    private static final String PROPERTY_PREFIX = "ratelimit";

    private RedisConnectionFactory redisConnectionFactory;
    private MockEnvironment environment;

    @Before
    public void setUp() {
        environment = new MockEnvironment();
        redisConnectionFactory = new RedisConnectionFactory(environment, null, PROPERTY_PREFIX);
    }

    @Test
    public void shouldReturnRedisOptionsWithSecuredEndpoint() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        assertThat(options).isNotNull();
        assertThat(options.getEndpoint()).isEqualTo("rediss://redis:6379");
    }

    @Test
    public void shouldReturnRedisOptionsWithoutSecuredEndpoint() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        assertThat(options).isNotNull();
        assertThat(options.getEndpoint()).isEqualTo("redis://redis:6379");
    }

    @Test
    public void shouldReturnRedisOptionsWithSentinelsEndpoints() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.master", "redis-master");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].host", "sent1");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].port", "26379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].host", "sent2");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].port", "26379");

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        List<String> sentinelEndpoints = new ArrayList<>();
        sentinelEndpoints.add("redis://sent1:26379");
        sentinelEndpoints.add("redis://sent2:26379");

        assertThat(options).isNotNull();
        assertThat(options.getEndpoints()).containsAll(sentinelEndpoints);
    }

    @Test
    public void shouldReturnRedisOptionsWithSentinelsSecuredEndpoints() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.master", "redis-master");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].host", "sent1");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].port", "26379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].host", "sent2");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].port", "26379");

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        List<String> sentinelEndpoints = new ArrayList<>();
        sentinelEndpoints.add("rediss://sent1:26379");
        sentinelEndpoints.add("rediss://sent2:26379");

        assertThat(options).isNotNull();
        assertThat(options.getEndpoints()).containsAll(sentinelEndpoints);
    }

    @Test
    public void shouldThrowAnExceptionWhenMissingSentinelMaster() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].host", "sent1");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].port", "26379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].host", "sent2");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].port", "26379");

        assertThatIllegalStateException()
            .isThrownBy(() -> redisConnectionFactory.buildRedisOptions())
            .withMessageContaining("Incorrect Sentinel configuration");
    }
}
