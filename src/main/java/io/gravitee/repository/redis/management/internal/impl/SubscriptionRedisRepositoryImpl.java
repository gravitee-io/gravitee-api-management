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

import io.gravitee.repository.redis.management.internal.SubscriptionRedisRepository;
import io.gravitee.repository.redis.management.model.RedisSubscription;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SubscriptionRedisRepositoryImpl extends AbstractRedisRepository implements SubscriptionRedisRepository {

    private final static String REDIS_KEY = "subscription";

    @Override
    public RedisSubscription find(String subscriptionId) {
        Object subscription = redisTemplate.opsForHash().get(REDIS_KEY, subscriptionId);
        if (subscription == null) {
            return null;
        }

        return convert(subscription, RedisSubscription.class);
    }

    @Override
    public Set<RedisSubscription> findByApplication(String application) {
        Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":application:" + application);
        List<Object> pageObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return pageObjects.stream()
                .map(event -> convert(event, RedisSubscription.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RedisSubscription> findByPlan(String plan) {
        Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":plan:" + plan);
        List<Object> pageObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return pageObjects.stream()
                .map(event -> convert(event, RedisSubscription.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisSubscription saveOrUpdate(RedisSubscription subscription) {
        redisTemplate.opsForHash().put(REDIS_KEY, subscription.getId(), subscription);
        redisTemplate.opsForSet().add(REDIS_KEY + ":plan:" + subscription.getPlan(), subscription.getId());
        redisTemplate.opsForSet().add(REDIS_KEY + ":application:" + subscription.getApplication(), subscription.getId());
        return subscription;
    }

    @Override
    public void delete(String subscription) {
        RedisSubscription redisSubscription = find(subscription);
        redisTemplate.opsForHash().delete(REDIS_KEY, subscription);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":plan:" + redisSubscription.getPlan(), subscription);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":application:" + redisSubscription.getApplication(), subscription);
    }
}
