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

import javax.swing.plaf.synth.SynthEditorPaneUI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserRedisRepositoryImpl extends AbstractRedisRepository implements UserRedisRepository {

    private final static String REDIS_KEY = "user";
    private final static String REDIS_KEY_USERNAME = "username";

    @Override
    public RedisUser find(String userId) {
        Object user = redisTemplate.opsForHash().get(REDIS_KEY, userId);
        if (user == null) {
            return null;
        }

        return convert(user, RedisUser.class);
    }

    @Override
    public RedisUser findByUsername(String username) {
        Object user = redisTemplate.opsForHash().get(REDIS_KEY_USERNAME, username);
        if (user == null) {
            return null;
        }

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
        Set<Object> keys = redisTemplate
                .opsForHash()
                .keys(REDIS_KEY);

        Set<Object> subKeys = keys.stream()
                .skip(pageable.from())
                .limit(pageable.to())
                .collect(Collectors.toSet());

        List<Object> usersObject = redisTemplate
                .opsForHash()
                .multiGet(REDIS_KEY, subKeys);

        Page<RedisUser> page = new Page<>(
                usersObject.stream()
                        .map(u -> convert(u, RedisUser.class))
                        .collect(Collectors.toList()),
                pageable.pageNumber(),
                pageable.pageSize(),
                keys.size());
        return page;
    }

    @Override
    public RedisUser saveOrUpdate(RedisUser user) {
        redisTemplate.opsForHash().put(REDIS_KEY, user.getId(), user);
        redisTemplate.opsForHash().put(REDIS_KEY_USERNAME, user.getUsername(), user);
        return user;
    }

    @Override
    public void delete(String userId) {
        RedisUser user = find(userId);

        redisTemplate.opsForHash().delete(REDIS_KEY, user.getId());
        redisTemplate.opsForHash().delete(REDIS_KEY_USERNAME, user.getUsername());
    }
}
