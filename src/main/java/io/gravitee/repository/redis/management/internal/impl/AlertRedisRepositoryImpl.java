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

import io.gravitee.repository.redis.management.internal.AlertRedisRepository;
import io.gravitee.repository.redis.management.model.RedisAlertTrigger;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AlertRedisRepositoryImpl extends AbstractRedisRepository implements AlertRedisRepository {

    private final static String REDIS_KEY = "alert_triggers";

    @Override
    public Set<RedisAlertTrigger> findAll() {
        final Map<Object, Object> alerts = redisTemplate.opsForHash().entries(REDIS_KEY);

        return alerts.values()
                .stream()
                .map(object -> convert(object, RedisAlertTrigger.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisAlertTrigger findById(final String alertId) {
        Object alert = redisTemplate.opsForHash().get(REDIS_KEY, alertId);
        if (alert == null) {
            return null;
        }

        return convert(alert, RedisAlertTrigger.class);
    }

    @Override
    public RedisAlertTrigger saveOrUpdate(final RedisAlertTrigger alert) {
        redisTemplate.executePipelined((RedisConnection connection) ->  {
            final String refKey = getRefKey(alert.getReferenceType(), alert.getReferenceId());
            redisTemplate.opsForHash().put(REDIS_KEY, alert.getId(), alert);
            redisTemplate.opsForSet().add(refKey, alert.getId());
            return null;
        });
        return alert;
    }

    @Override
    public void delete(final String alert) {
        redisTemplate.opsForHash().delete(REDIS_KEY, alert);
    }

    @Override
    public List<RedisAlertTrigger> findByReference(String referenceType, String referenceId) {
        final Set<Object> keys = redisTemplate.opsForSet().members(getRefKey(referenceType, referenceId));
        final List<Object> values = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);
        return values.stream()
                .filter(Objects::nonNull)
                .map(alert -> convert(alert, RedisAlertTrigger.class))
                .collect(toList());
    }

    private String getRefKey(String referenceType, String referenceId) {
        return referenceType + ':' + referenceId;
    }
}
