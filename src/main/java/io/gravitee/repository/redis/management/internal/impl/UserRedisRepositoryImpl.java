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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.redis.management.internal.UserRedisRepository;
import io.gravitee.repository.redis.management.model.RedisUser;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserRedisRepositoryImpl extends AbstractRedisRepository implements UserRedisRepository {

    private final static String REDIS_KEY = "user";
    private static Comparator<String> nullSafeStringComparator = Comparator.nullsFirst(String::compareToIgnoreCase);

    @Override
    public RedisUser findBySource(String source, String sourceId) {
        Set<Object> keys = redisTemplate.opsForSet().members(generateSourceKey(source, sourceId));

        if (! keys.isEmpty()) {
            Object user = redisTemplate.opsForHash().get(REDIS_KEY, keys.iterator().next());
            return convert(user, RedisUser.class);
        }

        return null;
    }

    @Override
    public RedisUser find(String userId) {
        Object user = redisTemplate.opsForHash().get(REDIS_KEY, userId);
        return convert(user, RedisUser.class);
    }

    @Override
    public Set<RedisUser> find(List<String> ids) {
        return redisTemplate.opsForHash().multiGet(REDIS_KEY, Collections.unmodifiableCollection(ids)).stream()
                .filter(Objects::nonNull)
                .map(o -> this.convert(o, RedisUser.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RedisUser> findAll() {
        Map<Object, Object> users = redisTemplate.opsForHash().entries(REDIS_KEY);

        return users.values()
                .stream()
                .map(object -> convert(object, RedisUser.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Page<RedisUser> search(Pageable pageable) {
        final List<RedisUser> users = redisTemplate.opsForHash().entries(REDIS_KEY).values().stream()
                .filter(Objects::nonNull)
                .map(u -> convert(u, RedisUser.class))
                .sorted(comparing(RedisUser::getLastname, nullSafeStringComparator).thenComparing(RedisUser::getFirstname, nullSafeStringComparator))
                .skip(pageable.from())
                .limit(pageable.pageSize())
                .collect(toList());
        return new Page<>(
                users,
                pageable.pageNumber(),
                users.size() > pageable.pageSize() ? pageable.pageSize() : users.size(),
                users.size());
    }

    @Override
    public RedisUser saveOrUpdate(RedisUser user) {
        redisTemplate.opsForHash().put(REDIS_KEY, user.getId(), user);
        redisTemplate.opsForSet().add(generateSourceKey(user.getSource(), user.getSourceId()), user.getId());
        return user;
    }

    @Override
    public void delete(String userId) {
        RedisUser user = find(userId);

        redisTemplate.opsForHash().delete(REDIS_KEY, user.getId());
        redisTemplate.opsForSet().remove(generateSourceKey(user.getSource(), user.getSourceId()), userId);
    }

    private String generateSourceKey(String source, String sourceId) {
        return REDIS_KEY + ":source:" + source + ':' + (sourceId==null ? "" : sourceId.toUpperCase());
    }
}
