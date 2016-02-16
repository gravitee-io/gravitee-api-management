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

import io.gravitee.repository.redis.management.internal.EventRedisRepository;
import io.gravitee.repository.redis.management.model.RedisEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Component
public class EventRedisRepositoryImpl extends AbstractRedisRepository implements EventRedisRepository {

    private final static String REDIS_KEY = "event";

    @Override
    public RedisEvent find(String eventId) {
        Object event = redisTemplate.opsForHash().get(REDIS_KEY, eventId);
        if (event == null) {
            return null;
        }

        return convert(event, RedisEvent.class);
    }

    @Override
    public Set<RedisEvent> findByType(String eventType) {
        Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":type:" + eventType);
        List<Object> eventObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return eventObjects.stream()
                .map(event -> convert(event, RedisEvent.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisEvent saveOrUpdate(RedisEvent event) {
        redisTemplate.opsForHash().put(REDIS_KEY, event.getId(), event);
        redisTemplate.opsForSet().add(REDIS_KEY + ":type:"+event.getType(), event.getId());
        return event;
    }

    @Override
    public void delete(String event) {
        this.redisTemplate.opsForHash().delete(REDIS_KEY, event);
    }
}
