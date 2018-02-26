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
import io.gravitee.repository.redis.management.internal.PortalNotificationConfigRedisRepository;
import io.gravitee.repository.redis.management.model.RedisPortalNotificationConfig;
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
public class PortalNotificationConfigRedisRepositoryImpl extends AbstractRedisRepository implements PortalNotificationConfigRedisRepository {
    private final static String REDIS_KEY = "portalnotificationconfig";

    @Override
    public RedisPortalNotificationConfig find(String user, String referenceType, String referenceId) {
        final Object obj = redisTemplate.opsForHash().get(REDIS_KEY, getPk(user, referenceType, referenceId));

        if (obj == null) {
            return null;
        }

        return convert(obj, RedisPortalNotificationConfig.class);
    }

    @Override
    public RedisPortalNotificationConfig create(final RedisPortalNotificationConfig cfg) {
        redisTemplate.executePipelined((RedisConnection connection) ->  {
            final String pk = getPk(cfg);
            redisTemplate.opsForHash().putIfAbsent(REDIS_KEY, pk, cfg);
            redisTemplate.opsForSet().add(getReferenceKey(cfg.getReferenceType().name(), cfg.getReferenceId()), pk);
            return null;
        });
        return cfg;
    }

    @Override
    public RedisPortalNotificationConfig update(final RedisPortalNotificationConfig cfg) {
        redisTemplate.opsForHash().put(REDIS_KEY, getPk(cfg), cfg);
        return cfg;
    }

    @Override
    public void delete(String user, String referenceType, String referenceId) {
        String pk = getPk(user, referenceType, referenceId);
        redisTemplate.opsForHash().delete(REDIS_KEY, pk);
        redisTemplate.opsForSet().remove(getReferenceKey(referenceType, referenceId), pk);
    }

    @Override
    public Set<RedisPortalNotificationConfig> findByReference(NotificationReferenceType type, String referenceId) {
        Set<Object> keys = redisTemplate.opsForSet().members(getReferenceKey(type.name(), referenceId));
        List<Object> objects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return objects.stream()
                .map(event -> convert(event, RedisPortalNotificationConfig.class))
                .collect(Collectors.toSet());
    }

    private String getPk(String user, String referenceType, String referenceId) {
        return referenceType + ":" + referenceId + ":" + user;
    }

    private String getPk(final RedisPortalNotificationConfig cfg) {
        return getPk(cfg.getUser(), cfg.getReferenceType().name(), cfg.getReferenceId());
    }

    private String getReferenceKey(String referenceType, String referenceId) {
        return REDIS_KEY + ":refType:" + referenceType + ":" + referenceId;
    }
}
