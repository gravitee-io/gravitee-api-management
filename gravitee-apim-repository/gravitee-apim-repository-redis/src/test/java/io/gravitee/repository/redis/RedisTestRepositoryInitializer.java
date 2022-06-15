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

import io.gravitee.repository.config.TestRepositoryInitializer;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author GraviteeSource Team
 */
public class RedisTestRepositoryInitializer implements TestRepositoryInitializer {

    private final RedisTemplate<?, ?> redisTemplate;

    public RedisTestRepositoryInitializer(RedisTemplate<?, ?> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void setUp() {
        /* noop */
    }

    @Override
    public void tearDown() {
        redisTemplate.execute(
            (RedisCallback<Boolean>) connection -> {
                connection.flushAll();
                return true;
            }
        );
    }
}
