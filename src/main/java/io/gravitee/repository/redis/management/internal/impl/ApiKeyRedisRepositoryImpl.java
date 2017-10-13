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
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.redis.management.internal.ApiKeyRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApiKey;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
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
        redisTemplate.opsForSet().add(REDIS_KEY + ":plan:" + apiKey.getPlan(), apiKey.getKey());
        redisTemplate.opsForSet().add(REDIS_KEY + ":subscription:" + apiKey.getSubscription(), apiKey.getKey());
        return apiKey;
    }

    @Override
    public void delete(String apiKey) {
        RedisApiKey redisApiKey = find(apiKey);
        redisTemplate.opsForHash().delete(REDIS_KEY, apiKey);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":plan:" + redisApiKey.getPlan(), apiKey);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":subscription:" + redisApiKey.getSubscription(), apiKey);
    }

    @Override
    public Set<RedisApiKey> findBySubscription(String application) {
        Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":subscription:" + application);
        List<Object> apiKeyObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return apiKeyObjects.stream()
                .map(apiKey -> convert(apiKey, RedisApiKey.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RedisApiKey> findByPlan(String plan) {
        Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":plan:" + plan);
        List<Object> apiKeyObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return apiKeyObjects.stream()
                .map(apiKey -> convert(apiKey, RedisApiKey.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Page<RedisApiKey> search(ApiKeyCriteria criteria) {
        Set<String> filterKeys = new HashSet<>();
        String tempDestination = "tmp-" + Math.abs(criteria.hashCode());

        // Implement OR clause for event type
        if (! criteria.getPlans().isEmpty()) {
            criteria.getPlans().forEach(type -> filterKeys.add(REDIS_KEY + ":plan:" + type));
            redisTemplate.opsForZSet().unionAndStore(null, filterKeys, tempDestination);
            filterKeys.clear();
            filterKeys.add(tempDestination);
        }

        // And finally add clause based on event update date
        filterKeys.add(REDIS_KEY + ":updated_at");

        redisTemplate.opsForZSet().intersectAndStore(null, filterKeys, tempDestination);

        Set<Object> keys;

        if (criteria.getFrom() != 0 && criteria.getTo() != 0) {
                keys = redisTemplate.opsForZSet().reverseRangeByScore(
                        tempDestination,
                        criteria.getFrom(), criteria.getTo());
        } else {
                keys = redisTemplate.opsForZSet().reverseRangeByScore(
                        tempDestination,
                        0, Long.MAX_VALUE);
        }

        redisTemplate.opsForZSet().removeRange(tempDestination, 0, -1);
        List<Object> apiKeysObject = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return new Page<>(
                apiKeysObject.stream()
                        .map(apiKey -> convert(apiKey, RedisApiKey.class))
                        .collect(Collectors.toList()), 0, 0, keys.size());
    }
}
