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

import io.gravitee.repository.redis.management.internal.ApiKeyRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApiKey;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiKeyRedisRepositoryImpl extends AbstractRedisRepository implements ApiKeyRedisRepository {

    private final static String REDIS_KEY = "apikey";

    @Override
    public RedisApiKey find(String sApiKey) {
        Object apiKey = redisTemplate.opsForHash().get(REDIS_KEY, sApiKey);
        if (apiKey == null) {
            return null;
        }

        return convert(apiKey, RedisApiKey.class);
    }

    @Override
    public RedisApiKey saveOrUpdate(RedisApiKey apiKey) {
        redisTemplate.opsForHash().put(REDIS_KEY, apiKey.getKey(), apiKey);
        redisTemplate.opsForSet().add(REDIS_KEY + ":api:" + apiKey.getApi(), apiKey.getKey());
        redisTemplate.opsForSet().add(REDIS_KEY + ":application:" + apiKey.getApplication(), apiKey.getKey());
        return apiKey;
    }

    @Override
    public void delete(String apiKey) {
        redisTemplate.opsForHash().delete(REDIS_KEY, apiKey);
    }

    @Override
    public Set<RedisApiKey> findByApplication(String application) {
        Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":application:" + application);
        List<Object> apiKeyObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return apiKeyObjects.stream()
                .map(apiKey -> convert(apiKey, RedisApiKey.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RedisApiKey> findByApi(String api) {
        Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":api:" + api);
        List<Object> apiKeyObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return apiKeyObjects.stream()
                .map(apiKey -> convert(apiKey, RedisApiKey.class))
                .collect(Collectors.toSet());
    }
}
