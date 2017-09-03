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

import io.gravitee.repository.redis.management.internal.GroupRedisRepository;
import io.gravitee.repository.redis.management.internal.PageRedisRepository;
import io.gravitee.repository.redis.management.model.RedisGroup;
import io.gravitee.repository.redis.management.model.RedisUser;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GroupRedisRepositoryImpl extends AbstractRedisRepository implements GroupRedisRepository {

    private final static String REDIS_KEY = "group";

    @Override
    public RedisGroup find(String groupId) {
        Object group = redisTemplate.opsForHash().get(REDIS_KEY, groupId);
        if (group == null) {
            return null;
        }

        return convert(group, RedisGroup.class);
    }

    @Override
    public Set<RedisGroup> findByIds(Set<String> ids) {
        return redisTemplate.opsForHash().multiGet(REDIS_KEY, Collections.unmodifiableCollection(ids)).
                stream().
                filter(Objects::nonNull).
                map(o -> this.convert(o, RedisGroup.class)).
                collect(Collectors.toSet());
    }

    @Override
    public RedisGroup saveOrUpdate(RedisGroup group) {
        redisTemplate.opsForHash().put(REDIS_KEY, group.getId(), group);
        redisTemplate.opsForSet().add(REDIS_KEY + ":type:" + group.getType(), group.getId());
        return group;
    }

    @Override
    public void delete(String groupId) {
        RedisGroup group = find(groupId);
        redisTemplate.opsForHash().delete(REDIS_KEY, groupId);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":type:" + group.getType(), groupId);
    }

    @Override
    public Set<RedisGroup> findAll() {
        return redisTemplate.opsForHash().entries(REDIS_KEY)
                .values()
                .stream()
                .map(object -> convert(object, RedisGroup.class))
                .collect(Collectors.toSet());
    }
}
