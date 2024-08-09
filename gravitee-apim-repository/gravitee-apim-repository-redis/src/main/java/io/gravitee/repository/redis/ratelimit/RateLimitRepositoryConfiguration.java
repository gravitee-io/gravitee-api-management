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

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.redis.common.RedisConnectionFactory;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Vertx;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RateLimitRepositoryConfiguration {

    public static final String SCRIPT_RATELIMIT_KEY = "ratelimit";
    public static final String SCRIPTS_RATELIMIT_LUA = "scripts/ratelimit/ratelimit.lua";

    @Bean("redisRateLimitClient")
    public RedisClient redisRedisClient(Environment environment, Vertx vertx) {
        return new RedisConnectionFactory(
            environment,
            vertx,
            Scope.RATE_LIMIT.getName(),
            Map.of(SCRIPT_RATELIMIT_KEY, SCRIPTS_RATELIMIT_LUA)
        )
            .createRedisClient();
    }

    @Bean
    public RedisRateLimitRepository redisRateLimitRepository(
        @Qualifier("redisRateLimitClient") RedisClient redisClient,
        @Value("${ratelimit.redis.operation.timeout:10}") int operationTimeout
    ) {
        return new RedisRateLimitRepository(redisClient, operationTimeout);
    }
}
