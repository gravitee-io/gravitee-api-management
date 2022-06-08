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

import io.gravitee.platform.repository.api.RepositoryProvider;
import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisRepositoryProvider implements RepositoryProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRepositoryProvider.class);

    public RedisRepositoryProvider() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            try {
                Class.forName(LettuceConnection.class.getName(), true, getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                LOGGER.error("Unable to initialize class LettuceConnection", e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    @Override
    public String type() {
        return "redis";
    }

    @Override
    public Scope[] scopes() {
        return new Scope[] { Scope.RATE_LIMIT };
    }

    @Override
    public Class<?> configuration(Scope scope) {
        if (scope == Scope.RATE_LIMIT) {
            return RateLimitRepositoryConfiguration.class;
        }
        LOGGER.debug("Skipping unhandled repository scope {}", scope);
        return null;
    }
}
