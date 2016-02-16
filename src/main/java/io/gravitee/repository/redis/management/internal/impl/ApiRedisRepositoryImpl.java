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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiRedisRepositoryImpl extends AbstractRedisRepository implements ApiRedisRepository {

    private final static String REDIS_KEY = "api";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public RedisApi find(String apiId) {
        Object api = redisTemplate.opsForHash().get(REDIS_KEY, apiId);
        if (api == null) {
            return null;
        }

        return convert(api, RedisApi.class);
    }

    @Override
    public Set<RedisApi> findAll() {
        Map<Object, Object> apis = redisTemplate.opsForHash().entries(REDIS_KEY);

        return apis.values()
                .stream()
                .map(object -> convert(object, RedisApi.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisApi saveOrUpdate(RedisApi api) {
        redisTemplate.opsForHash().put(REDIS_KEY, api.getId(), api);
        return api;
    }

    @Override
    public void delete(String api) {
        redisTemplate.opsForHash().delete(REDIS_KEY, api);
    }
}
