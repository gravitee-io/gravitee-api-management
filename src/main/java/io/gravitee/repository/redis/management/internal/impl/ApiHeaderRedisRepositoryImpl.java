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

import io.gravitee.repository.redis.management.internal.ApiHeaderRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApiHeader;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiHeaderRedisRepositoryImpl extends AbstractRedisRepository implements ApiHeaderRedisRepository {

    private final static String REDIS_KEY = "apiheader";

    @Override
    public Set<RedisApiHeader> findAll() {
        final Map<Object, Object> views = redisTemplate.opsForHash().entries(REDIS_KEY);

        return views.values()
                .stream()
                .map(object -> convert(object, RedisApiHeader.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisApiHeader findById(final String id) {
        Object redis = redisTemplate.opsForHash().get(REDIS_KEY, id);
        if (redis == null) {
            return null;
        }

        return convert(redis, RedisApiHeader.class);
    }

    @Override
    public RedisApiHeader saveOrUpdate(final RedisApiHeader redis) {
        redisTemplate.opsForHash().put(REDIS_KEY, redis.getId(), redis);
        return redis;
    }

    @Override
    public void delete(final String view) {
        redisTemplate.opsForHash().delete(REDIS_KEY, view);
    }
}
