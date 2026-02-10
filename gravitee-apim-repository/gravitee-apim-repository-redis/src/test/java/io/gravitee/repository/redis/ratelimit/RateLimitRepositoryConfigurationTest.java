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
package io.gravitee.repository.redis.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.repository.redis.common.RedisConnectionFactory;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class RateLimitRepositoryConfigurationTest {

    private RateLimitRepositoryConfiguration configuration;

    @Mock
    private Environment environment;

    @Mock
    private Vertx vertx;

    @Mock
    private RedisClient mockRedisClient;

    @BeforeEach
    void setUp() {
        configuration = new RateLimitRepositoryConfiguration();
    }

    @Test
    @DisplayName("Should use NEW prefix 'repositories.ratelimit' when defined via redis.host")
    void shouldCreateClientWithNewPrefixHost() {
        when(environment.containsProperty("repositories.ratelimit.redis.host")).thenReturn(true);

        testRedisClientPrefix("repositories.ratelimit");
    }

    @Test
    @DisplayName("Should use NEW prefix 'repositories.ratelimit' when defined via redis.endpoints")
    void shouldCreateClientWithNewPrefixEndpoints() {
        when(environment.containsProperty("repositories.ratelimit.redis.host")).thenReturn(false);
        when(environment.containsProperty("repositories.ratelimit.redis.endpoints[0]")).thenReturn(true);

        testRedisClientPrefix("repositories.ratelimit");
    }

    @Test
    @DisplayName("Should fallback to OLD prefix 'ratelimit' when new config is missing")
    void shouldFallbackToOldPrefix() {
        when(environment.containsProperty("repositories.ratelimit.redis.host")).thenReturn(false);
        when(environment.containsProperty("repositories.ratelimit.redis.endpoints[0]")).thenReturn(false);
        when(environment.containsProperty("repositories.ratelimit.redis.sentinel.nodes[0].host")).thenReturn(false);

        testRedisClientPrefix("ratelimit");
    }

    private void testRedisClientPrefix(String expectedPrefix) {
        try (
            MockedConstruction<RedisConnectionFactory> mockedFactory = mockConstruction(RedisConnectionFactory.class, (mock, context) -> {
                assertThat(context.arguments().get(2)).isEqualTo(expectedPrefix);
                when(mock.createRedisClient()).thenReturn(mockRedisClient);
            })
        ) {
            RedisClient client = configuration.redisRedisClient(environment, vertx);
            assertThat(client).isEqualTo(mockRedisClient);
            assertThat(mockedFactory.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("Should use NEW timeout property if defined")
    void shouldUseNewTimeoutProperty() {
        String newKey = "repositories.ratelimit.redis.operation.timeout";
        when(environment.getProperty(newKey, Integer.class)).thenReturn(500);

        RedisRateLimitRepository repository = configuration.redisRateLimitRepository(mockRedisClient, environment);

        assertThat(repository).isNotNull();
        verify(environment, never()).getProperty(eq("ratelimit.redis.operation.timeout"), eq(Integer.class), anyInt());
    }

    @Test
    @DisplayName("Should use OLD timeout property if new is missing")
    void shouldUseOldTimeoutProperty() {
        String newKey = "repositories.ratelimit.redis.operation.timeout";
        String oldKey = "ratelimit.redis.operation.timeout";

        when(environment.getProperty(newKey, Integer.class)).thenReturn(null);
        when(environment.getProperty(oldKey, Integer.class, 10)).thenReturn(200);

        RedisRateLimitRepository repository = configuration.redisRateLimitRepository(mockRedisClient, environment);

        assertThat(repository).isNotNull();
        verify(environment).getProperty(oldKey, Integer.class, 10);
    }
}
