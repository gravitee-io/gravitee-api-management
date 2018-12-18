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
package io.gravitee.repository.redis.management.internal.impl;

import io.gravitee.repository.redis.management.internal.EntrypointRedisRepository;
import io.gravitee.repository.redis.management.model.RedisEntrypoint;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class EntrypointRedisRepositoryImpl extends AbstractRedisRepository implements EntrypointRedisRepository {

    private final static String REDIS_KEY = "entrypoint";

    @Override
    public Set<RedisEntrypoint> findAll() {
        final Map<Object, Object> entrypoints = redisTemplate.opsForHash().entries(REDIS_KEY);

        return entrypoints.values()
                .stream()
                .map(object -> convert(object, RedisEntrypoint.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisEntrypoint findById(final String entrypointId) {
        Object entrypoint = redisTemplate.opsForHash().get(REDIS_KEY, entrypointId);
        return convert(entrypoint, RedisEntrypoint.class);
    }

    @Override
    public RedisEntrypoint saveOrUpdate(final RedisEntrypoint entrypoint) {
        redisTemplate.opsForHash().put(REDIS_KEY, entrypoint.getId(), entrypoint);
        return entrypoint;
    }

    @Override
    public void delete(final String entrypoint) {
        redisTemplate.opsForHash().delete(REDIS_KEY, entrypoint);
    }
}
