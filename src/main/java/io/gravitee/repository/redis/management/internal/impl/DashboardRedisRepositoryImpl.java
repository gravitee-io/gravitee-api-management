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

import io.gravitee.repository.redis.management.internal.DashboardRedisRepository;
import io.gravitee.repository.redis.management.model.RedisDashboard;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DashboardRedisRepositoryImpl extends AbstractRedisRepository implements DashboardRedisRepository {

    private final static String REDIS_KEY = "dashboard";

    @Override
    public Set<RedisDashboard> findAll() {
        final Map<Object, Object> dictionaries = redisTemplate.opsForHash().entries(REDIS_KEY);
        return dictionaries.values()
                .stream()
                .map(object -> convert(object, RedisDashboard.class))
                .collect(toSet());
    }
    
    @Override
    public RedisDashboard create(final RedisDashboard dashboard) {
        redisTemplate.executePipelined((RedisConnection connection) ->  {
            redisTemplate.opsForHash().putIfAbsent(REDIS_KEY, dashboard.getId(), dashboard);
            redisTemplate.opsForSet().add(dashboard.getReferenceType(), dashboard.getId());
            return null;
        });
        return dashboard;
    }

    @Override
    public RedisDashboard update(final RedisDashboard dashboard) {
        redisTemplate.opsForHash().put(REDIS_KEY, dashboard.getId(), dashboard);
        return dashboard;
    }

    @Override
    public void delete(final String id) {
        redisTemplate.opsForHash().delete(REDIS_KEY, id);
    }

    @Override
    public RedisDashboard findById(final String id) {
        return convert(redisTemplate.opsForHash().get(REDIS_KEY, id), RedisDashboard.class);
    }

    @Override
    public List<RedisDashboard> findByReferenceType(final String referenceType) {
        final Set<Object> keys = redisTemplate.opsForSet().members(referenceType);
        final List<Object> values = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);
        return values.stream()
                .filter(Objects::nonNull)
                .map(dashboard -> convert(dashboard, RedisDashboard.class))
                .sorted(comparing(RedisDashboard::getOrder))
                .collect(toList());
    }
}
