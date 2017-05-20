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
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.redis.management.internal.EventRedisRepository;
import io.gravitee.repository.redis.management.model.RedisEvent;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class EventRedisRepositoryImpl extends AbstractRedisRepository implements EventRedisRepository {

    private final static String REDIS_KEY = "event";

    @Override
    public RedisEvent find(String eventId) {
        final Object event = redisTemplate.opsForHash().get(REDIS_KEY, eventId);

        if (event == null) {
            return null;
        }

        return convert(event, RedisEvent.class);
    }

    @Override
    public Page<RedisEvent> search(EventCriteria filter, Pageable pageable) {
        Set<String> filterKeys = new HashSet<>();
        String tempDestination = "tmp-" + Math.abs(filter.hashCode());

        // Implement OR clause for event type
        if (! filter.getTypes().isEmpty()) {
            filter.getTypes().forEach(type -> filterKeys.add(REDIS_KEY + ":type:" + type));
            redisTemplate.opsForZSet().unionAndStore(null, filterKeys, tempDestination);
            filterKeys.clear();
            filterKeys.add(tempDestination);
        }

        // Add clause based on event properties
        Set<String> internalUnionFilter = new HashSet<>();
        filter.getProperties().forEach((propertyKey, propertyValue) -> {
            if (propertyValue instanceof Collection) {
                Set<String> collectionFilter = new HashSet<>(((Collection) propertyValue).size());
                String collectionTempDestination = "tmp-" + propertyKey + ":" + propertyValue.hashCode();
                        ((Collection) propertyValue).forEach(value ->
                        collectionFilter.add(REDIS_KEY + ":" + propertyKey + ":" + value));
                redisTemplate.opsForZSet().unionAndStore(null, collectionFilter, collectionTempDestination);
                internalUnionFilter.add(collectionTempDestination);
                filterKeys.add(collectionTempDestination);
            } else {
                filterKeys.add(REDIS_KEY + ":" + propertyKey + ":" + propertyValue);
            }
        });

        // And finally add clause based on event update date
        filterKeys.add(REDIS_KEY + ":updated_at");

        redisTemplate.opsForZSet().intersectAndStore(null, filterKeys, tempDestination);

        Set<Object> keys;

        if (filter.getFrom() != 0 && filter.getTo() != 0) {
            if (pageable != null) {
                keys = redisTemplate.opsForZSet().reverseRangeByScore(
                        tempDestination,
                        filter.getFrom(), filter.getTo(),
                        pageable.from(), pageable.pageSize());
            } else {
                keys = redisTemplate.opsForZSet().reverseRangeByScore(
                        tempDestination,
                        filter.getFrom(), filter.getTo());
            }
        } else {
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
        internalUnionFilter.forEach(dest -> redisTemplate.opsForZSet().removeRange(dest, 0, -1));
        List<Object> eventObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return new Page<>(
                eventObjects.stream()
                        .map(event -> convert(event, RedisEvent.class))
                        .collect(Collectors.toList()),
                (pageable != null) ? pageable.pageNumber() : 0,
                (pageable != null) ? pageable.pageSize() : 0,
                keys.size());
    }

    @Override
    public RedisEvent saveOrUpdate(RedisEvent event) {
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                redisTemplate.opsForHash().put(REDIS_KEY, event.getId(), event);
                redisTemplate.opsForSet().add(REDIS_KEY + ":type:" + event.getType(), event.getId());
                redisTemplate.opsForZSet().add(REDIS_KEY + ":updated_at", event.getId(), event.getCreatedAt());
                if (event.getProperties() != null) {
                    event.getProperties().forEach((key, value) ->
                            redisTemplate.opsForSet().add(REDIS_KEY + ":" + key + ":" + value, event.getId()));
                }

                return null;
            }
        });

        return event;
    }

    @Override
    public void delete(String event) {
        final RedisEvent redisEvent = find(event);

        redisTemplate.executePipelined((RedisConnection connection) ->  {
            redisTemplate.opsForHash().delete(REDIS_KEY, event);
            redisTemplate.opsForSet().remove(REDIS_KEY + ":type:" + redisEvent.getType(), redisEvent.getId());
            redisTemplate.opsForZSet().remove(REDIS_KEY + ":updated_at", redisEvent.getId());
            if (redisEvent.getProperties() != null) {
                redisEvent.getProperties().forEach((key, value) ->
                        redisTemplate.opsForSet().remove(REDIS_KEY + ":" + key + ":" + value, redisEvent.getId()));
            }
            return null;
        });
    }
}
