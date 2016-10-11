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

import io.gravitee.repository.redis.management.internal.PlanRedisRepository;
import io.gravitee.repository.redis.management.model.RedisPlan;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PlanRedisRepositoryImpl extends AbstractRedisRepository implements PlanRedisRepository {

    private final static String REDIS_KEY = "plan";

    @Override
    public RedisPlan find(String planId) {
        Object plan = redisTemplate.opsForHash().get(REDIS_KEY, planId);
        if (plan == null) {
            return null;
        }

        return convert(plan, RedisPlan.class);
    }

    @Override
    public Set<RedisPlan> findByApi(String api) {
        Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":api:" + api);
        List<Object> pageObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return pageObjects.stream()
                .map(event -> convert(event, RedisPlan.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisPlan saveOrUpdate(RedisPlan plan) {
        redisTemplate.opsForHash().put(REDIS_KEY, plan.getId(), plan);
        redisTemplate.opsForSet().add(REDIS_KEY + ":api:" + plan.getApis().iterator().next(), plan.getId());
        return plan;
    }

    @Override
    public void delete(String plan) {
        RedisPlan redisPlan = find(plan);
        redisTemplate.opsForHash().delete(REDIS_KEY, plan);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":api:" + redisPlan.getApis().iterator().next(), plan);
    }
}
