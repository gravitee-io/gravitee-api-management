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

import io.gravitee.repository.redis.management.internal.PortalNotificationRedisRepository;
import io.gravitee.repository.redis.management.model.RedisPortalNotification;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class PortalNotificationRedisRepositoryImpl extends AbstractRedisRepository implements PortalNotificationRedisRepository {
    private final static String REDIS_KEY = "portalnotification";

    public RedisPortalNotification find(String id) {
        final Object obj = redisTemplate.opsForHash().get(REDIS_KEY, id);

        if (obj == null) {
            return null;
        }

        return convert(obj, RedisPortalNotification.class);
    }

    @Override
    public void saveOrUpdate(List<RedisPortalNotification> notifications) {
        for (RedisPortalNotification notification : notifications) {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                redisTemplate.opsForHash().put(REDIS_KEY, notification.getId(), notification);
                redisTemplate.opsForSet().add(REDIS_KEY + ":user:" + notification.getUser(), notification.getId());
                return null;
            });
        }
    }

    @Override
    public void delete(String id) {
        final RedisPortalNotification redisPortalNotification = find(id);

        redisTemplate.executePipelined((RedisConnection connection) ->  {
            redisTemplate.opsForHash().delete(REDIS_KEY, id);
            redisTemplate.opsForSet().remove(REDIS_KEY + ":user:" + redisPortalNotification.getUser(), redisPortalNotification.getId());
            return null;
        });
    }

    @Override
    public Set<RedisPortalNotification> findByUser(String user) {
        Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":user:" + user);
        List<Object> pageObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return pageObjects.stream()
                .map(event -> convert(event, RedisPortalNotification.class))
                .collect(Collectors.toSet());
    }
}
