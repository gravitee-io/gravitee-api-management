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
package io.gravitee.repository.redis.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.redis.management.internal.EventRedisRepository;
import io.gravitee.repository.redis.management.model.RedisEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisEventRepository implements EventRepository {

    @Autowired
    private EventRedisRepository eventRedisRepository;

    @Override
    public Page<Event> search(EventCriteria filter, Pageable pageable) {
        Page<RedisEvent> pagedEvents = eventRedisRepository.search(filter, pageable);

        return new Page<>(
                pagedEvents.getContent().stream().map(this::convert).collect(Collectors.toList()),
                pagedEvents.getPageNumber(), (int) pagedEvents.getTotalElements(),
                pagedEvents.getTotalElements());
     }

    @Override
    public List<Event> search(EventCriteria filter) {
        Page<RedisEvent> pagedEvents = eventRedisRepository.search(filter, null);

        return pagedEvents.getContent()
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Event> findById(String eventId) throws TechnicalException {
        RedisEvent redisEvent = eventRedisRepository.find(eventId);
        return Optional.ofNullable(convert(redisEvent));
    }

    @Override
    public Event create(Event event) throws TechnicalException {
        RedisEvent redisEvent = eventRedisRepository.saveOrUpdate(convert(event));
        return convert(redisEvent);
    }

    @Override
    public Event update(Event event) throws TechnicalException {
        RedisEvent redisEvent = eventRedisRepository.saveOrUpdate(convert(event));
        return convert(redisEvent);
    }

    @Override
    public void delete(String eventId) throws TechnicalException {
        eventRedisRepository.delete(eventId);
    }

    private Event convert(RedisEvent redisEvent) {
        if (redisEvent == null) {
            return null;
        }

        Event event = new Event();
        event.setId(redisEvent.getId());
        event.setParentId(redisEvent.getParentId());
        event.setCreatedAt(new Date(redisEvent.getCreatedAt()));
        event.setUpdatedAt(new Date(redisEvent.getUpdatedAt()));
        event.setPayload(redisEvent.getPayload());
        event.setProperties(redisEvent.getProperties());
        event.setType(EventType.valueOf(redisEvent.getType()));
        return event;
    }

    private RedisEvent convert(Event event) {
        RedisEvent redisEvent = new RedisEvent();
        redisEvent.setId(event.getId());
        redisEvent.setParentId(event.getParentId());
        redisEvent.setCreatedAt(event.getCreatedAt().getTime());
        redisEvent.setUpdatedAt(event.getUpdatedAt().getTime());
        redisEvent.setPayload(event.getPayload());
        redisEvent.setProperties(event.getProperties());
        redisEvent.setType(event.getType().name());
        return redisEvent;
    }
}
