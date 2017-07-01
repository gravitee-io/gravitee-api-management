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

import io.gravitee.repository.redis.management.internal.RoleRedisRepository;
import io.gravitee.repository.redis.management.model.RedisRole;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RoleRedisRepositoryImpl extends AbstractRedisRepository implements RoleRedisRepository {

    private final static String REDIS_KEY = "role";

    @Override
    public RedisRole find(String roleId) {
        Object role = redisTemplate.opsForHash().get(REDIS_KEY, roleId);
        if (role == null) {
            return null;
        }

        return convert(role, RedisRole.class);
    }

    @Override
    public RedisRole saveOrUpdate(RedisRole role) {
        redisTemplate.opsForHash().put(REDIS_KEY, role.getId(), role);
        redisTemplate.opsForSet().add(REDIS_KEY + ":scope:" + role.getScope(), role.getId());
        return role;
    }

    @Override
    public void delete(String role) {
        RedisRole redisRole = find(role);
        redisTemplate.opsForHash().delete(REDIS_KEY, role);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":scope:" + redisRole.getScope(), role);
    }

    @Override
    public Set<RedisRole> findByScope(int scope) {
        Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":scope:" + scope);
        List<Object> pageObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return pageObjects.stream()
                .map(event -> convert(event, RedisRole.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RedisRole> findAll() {
        Map<Object, Object> applications = redisTemplate.opsForHash().entries(REDIS_KEY);

        return applications.values()
                .stream()
                .map(object -> convert(object, RedisRole.class))
                .collect(Collectors.toSet());
    }
}
