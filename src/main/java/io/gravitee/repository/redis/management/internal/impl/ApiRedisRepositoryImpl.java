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

import io.gravitee.repository.redis.management.internal.ApiRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApi;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiRedisRepositoryImpl extends AbstractRedisRepository implements ApiRedisRepository {

    private final static String REDIS_KEY = "api";

    @Override
    public RedisApi find(String apiId) {
        Object api = redisTemplate.opsForHash().get(REDIS_KEY, apiId);
        if (api == null) {
            return null;
        }

        return convert(api, RedisApi.class);
    }

    @Override
    public Set<RedisApi> find(List<String> apis) {
        return redisTemplate.opsForHash().multiGet(REDIS_KEY, Collections.unmodifiableCollection(apis)).stream()
                .map(o -> this.convert(o, RedisApi.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RedisApi> findAll() {
        Map<Object, Object> applications = redisTemplate.opsForHash().entries(REDIS_KEY);

        return applications.values()
                .stream()
                .map(object -> convert(object, RedisApi.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RedisApi> findByVisibility(String visibility) {
        Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":visibility:" + visibility);
        List<Object> apiObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return apiObjects.stream()
                .map(event -> convert(event, RedisApi.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RedisApi> findByGroups(List<String> groups) {
        Set<Object> keys = new HashSet<>();
        groups.forEach(group ->keys.addAll(redisTemplate.opsForSet().members(REDIS_KEY + ":group:" + group)));
        List<Object> apiObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return apiObjects.stream()
                .map(event -> convert(event, RedisApi.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisApi saveOrUpdate(RedisApi api) {
        RedisApi oldApi = find(api.getId());
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                redisTemplate.opsForHash().put(REDIS_KEY, api.getId(), api);

                if (oldApi != null) {
                    redisTemplate.opsForSet().remove(REDIS_KEY + ":visibility:" + oldApi.getVisibility(), api.getId());
                    if(oldApi.getGroup() != null) {
                        redisTemplate.opsForSet().remove(REDIS_KEY + ":group:" + oldApi.getGroup(), api.getId());
                    }
                }

                redisTemplate.opsForSet().add(REDIS_KEY + ":visibility:" + api.getVisibility(), api.getId());
                if(api.getGroup() != null) {
                    redisTemplate.opsForSet().add(REDIS_KEY + ":group:" + api.getGroup(), api.getId());
                }

                return null;
            }
        });
        return api;
    }

    @Override
    public void delete(String api) {
        redisTemplate.opsForHash().delete(REDIS_KEY, api);
    }

}
