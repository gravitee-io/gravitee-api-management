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
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.redis.management.internal.SubscriptionRedisRepository;
import io.gravitee.repository.redis.management.model.RedisSubscription;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
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
    public Page<RedisSubscription> search(SubscriptionCriteria criteria, Pageable pageable) {
        Set<String> filterKeys = new HashSet<>();

        if (criteria.getClientId() != null) {
            filterKeys.add(REDIS_KEY + ":client_id:" + criteria.getClientId());
        }

        // Implement criteria for API
        if (criteria.getApis() != null && ! criteria.getApis().isEmpty()) {
            String tmpDst = "tmp-sub-api-" + Math.abs(criteria.hashCode());
            Set<String> apiFilterKeys = new HashSet<>();
            criteria.getApis().forEach(api -> apiFilterKeys.add(REDIS_KEY + ":api:" + api));
            redisTemplate.opsForZSet().unionAndStore(null, apiFilterKeys, tmpDst);
            filterKeys.add(tmpDst);
        }

        // Implement criteria for Plan
        if (criteria.getPlans() != null && ! criteria.getPlans().isEmpty()) {
            String tmpDst = "tmp-sub-plan-" + Math.abs(criteria.hashCode());
            Set<String> planFilterKeys = new HashSet<>();
            criteria.getPlans().forEach(plan -> planFilterKeys.add(REDIS_KEY + ":plan:" + plan));
            redisTemplate.opsForZSet().unionAndStore(null, planFilterKeys, tmpDst);
            filterKeys.add(tmpDst);
        }

        // Implement criteria for application
        if (criteria.getApplications() != null && ! criteria.getApplications().isEmpty()) {
            String tmpDst = "tmp-sub-app-" + Math.abs(criteria.hashCode());
            Set<String> appFilterKeys = new HashSet<>();
            criteria.getApplications().forEach(app -> appFilterKeys.add(REDIS_KEY + ":application:" + app));
            redisTemplate.opsForZSet().unionAndStore(null, appFilterKeys, tmpDst);
            filterKeys.add(tmpDst);
        }

        // Implement criteria for status
        if (criteria.getStatuses() != null && ! criteria.getStatuses().isEmpty()) {
            String tmpDst = "tmp-sub-status-" + Math.abs(criteria.hashCode());
            Set<String> statusFilterKeys = new HashSet<>();
            criteria.getStatuses().forEach(status -> statusFilterKeys.add(REDIS_KEY + ":status:" + status));
            redisTemplate.opsForZSet().unionAndStore(null, statusFilterKeys, tmpDst);
            filterKeys.add(tmpDst);
        }

        // And finally add clause based on event update date
        filterKeys.add(REDIS_KEY + ":updated_at");

        String tempDestination = "tmp-sub-search-" + Math.abs(criteria.hashCode());

        redisTemplate.opsForZSet().intersectAndStore(null, filterKeys, tempDestination);

        Set<Object> keys;
        long total;

        if (criteria.getFrom() != 0 && criteria.getTo() != 0) {
            keys = redisTemplate.opsForZSet().rangeByScore(tempDestination,
                    criteria.getFrom(), criteria.getTo(), 0, Long.MAX_VALUE);
            total = keys.size();
            if (pageable != null) {
                keys = redisTemplate.opsForZSet().reverseRangeByScore(
                        tempDestination,
                        criteria.getFrom(), criteria.getTo(),
                        pageable.from(), pageable.pageSize());
            } else {
                keys = redisTemplate.opsForZSet().reverseRangeByScore(
                        tempDestination,
                        criteria.getFrom(), criteria.getTo());
            }
        } else {
            keys = redisTemplate.opsForZSet().rangeByScore(tempDestination,
                    0, Long.MAX_VALUE);
            total = keys.size();
            if (pageable != null) {
                keys = redisTemplate.opsForZSet().reverseRangeByScore(
                        tempDestination,
                        0, Long.MAX_VALUE,
                        pageable.from(), pageable.pageSize());
            } else {
                keys = redisTemplate.opsForZSet().reverseRangeByScore(
                        tempDestination,
                        0, Long.MAX_VALUE);
            }
        }

        redisTemplate.opsForZSet().removeRange(tempDestination, 0, -1);
        List<Object> subscriptionObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return new Page<>(
                subscriptionObjects.stream()
                        .map(event -> convert(event, RedisSubscription.class))
                        .sorted(Comparator.comparing(RedisSubscription::getCreatedAt).reversed())
                        .collect(Collectors.toList()),
                (pageable != null) ? pageable.pageNumber() : 0,
                keys.size(),
                total);
    }

    @Override
    public RedisSubscription saveOrUpdate(RedisSubscription subscription) {
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                redisTemplate.opsForHash().put(REDIS_KEY, subscription.getId(), subscription);
                redisTemplate.opsForSet().add(REDIS_KEY + ":status:" + subscription.getStatus(), subscription.getId());
                redisTemplate.opsForSet().add(REDIS_KEY + ":plan:" + subscription.getPlan(), subscription.getId());
                redisTemplate.opsForSet().add(REDIS_KEY + ":application:" + subscription.getApplication(), subscription.getId());
                redisTemplate.opsForSet().add(REDIS_KEY + ":api:" + subscription.getApi(), subscription.getId());
                redisTemplate.opsForZSet().add(REDIS_KEY + ":updated_at", subscription.getId(), subscription.getUpdatedAt());

                if (subscription.getClientId() != null) {
                    redisTemplate.opsForSet().add(REDIS_KEY + ":client_id:" + subscription.getClientId(), subscription.getId());
                }
                return null;
            }
        });

        return subscription;
    }

    @Override
    public void delete(String subscription) {
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                RedisSubscription redisSubscription = find(subscription);
                redisTemplate.opsForHash().delete(REDIS_KEY, subscription);
                redisTemplate.opsForSet().remove(REDIS_KEY + ":status:" + redisSubscription.getStatus(), subscription);
                redisTemplate.opsForSet().remove(REDIS_KEY + ":plan:" + redisSubscription.getPlan(), subscription);
                redisTemplate.opsForSet().remove(REDIS_KEY + ":application:" + redisSubscription.getApplication(), subscription);
                redisTemplate.opsForSet().remove(REDIS_KEY + ":api:" + redisSubscription.getApi(), subscription);
                redisTemplate.opsForZSet().remove(REDIS_KEY + ":updated_at", redisSubscription.getId());
                if (redisSubscription.getClientId() != null) {
                    redisTemplate.opsForSet().remove(REDIS_KEY + ":client_id:" + redisSubscription.getClientId(), subscription);
                }
                return null;
            }
        });
    }
}
