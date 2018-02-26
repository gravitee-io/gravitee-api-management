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

import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.redis.management.internal.GenericNotificationConfigRedisRepository;
import io.gravitee.repository.redis.management.model.RedisGenericNotificationConfig;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class GenericNotificationConfigRedisRepositoryImpl extends AbstractRedisRepository implements GenericNotificationConfigRedisRepository {
    private final static String REDIS_KEY = "genericnotificationconfig";

    @Override
    public RedisGenericNotificationConfig findById(String id) {
        final Object obj = redisTemplate.opsForHash().get(REDIS_KEY, id);

        if (obj == null) {
            return null;
        }

        return convert(obj, RedisGenericNotificationConfig.class);
    }

    @Override
    public RedisGenericNotificationConfig create(final RedisGenericNotificationConfig cfg) {
        redisTemplate.executePipelined((RedisConnection connection) ->  {
            redisTemplate.opsForHash().putIfAbsent(REDIS_KEY, cfg.getId(), cfg);
            redisTemplate.opsForSet().add(getReferenceKey(cfg.getReferenceType().name(), cfg.getReferenceId()), cfg.getId());
            return null;
        });
        return cfg;
    }

    @Override
    public RedisGenericNotificationConfig update(final RedisGenericNotificationConfig cfg) {
        redisTemplate.opsForHash().put(REDIS_KEY, cfg.getId(), cfg);
        return cfg;
    }

    @Override
    public void delete(String id) {
        RedisGenericNotificationConfig cfg = findById(id);
        redisTemplate.opsForHash().delete(REDIS_KEY, id);
        redisTemplate.opsForSet().remove(getReferenceKey(cfg.getReferenceType().name(), cfg.getReferenceId()), id);
    }

    @Override
    public Set<RedisGenericNotificationConfig> findByReference(NotificationReferenceType type, String referenceId) {
        Set<Object> keys = redisTemplate.opsForSet().members(getReferenceKey(type.name(), referenceId));
        List<Object> objects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return objects.stream()
                .map(event -> convert(event, RedisGenericNotificationConfig.class))
                .collect(Collectors.toSet());
    }

    private String getReferenceKey(String referenceType, String referenceId) {
        return REDIS_KEY + ":refType:" + referenceType + ":" + referenceId;
    }
}
