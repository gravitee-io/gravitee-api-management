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

import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import junit.framework.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class RedisEventRepositoryTest extends AbstractRedisTest {

    @Autowired
    private EventRepository eventRepository;

    @Test
    public void shouldCreateEvent() throws Exception {
        Event event = event();
        Event createdEvent = eventRepository.create(event);

        Assert.assertEquals(event.getId(), createdEvent.getId());
    }

    @Test
    public void shouldFindEventsByType() throws Exception {
        eventRepository.create(event());
        eventRepository.create(event2());
        eventRepository.create(event3());

        List<EventType> types = new ArrayList<>();
        types.add(EventType.PUBLISH_API);

        Set<Event> events = eventRepository.findByType(types);
        Assert.assertEquals(2, events.size());
    }

    @Test
    public void shouldFindEventsByMultipleType() throws Exception {
        eventRepository.create(event());
        eventRepository.create(event2());
        eventRepository.create(event3());

        List<EventType> types = new ArrayList<>();
        types.add(EventType.PUBLISH_API);
        types.add(EventType.PUBLISH_API_RESULT);

        Set<Event> events = eventRepository.findByType(types);
        Assert.assertEquals(3, events.size());
    }

    private Event event() {
        Event event = new Event();
        event.setId(UUID.randomUUID().toString());
        event.setCreatedAt(new Date());
        event.setUpdatedAt(new Date());
        event.setType(EventType.PUBLISH_API);

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.name(), "my-api");
        event.setProperties(properties);

        return event;
    }

    private Event event2() {
        Event event = new Event();
        event.setId(UUID.randomUUID().toString());
        event.setCreatedAt(new Date());
        event.setUpdatedAt(new Date());
        event.setType(EventType.PUBLISH_API_RESULT);

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.name(), "my-api2");
        event.setProperties(properties);

        return event;
    }

    private Event event3() {
        Event event = new Event();
        event.setId(UUID.randomUUID().toString());
        event.setCreatedAt(new Date());
        event.setUpdatedAt(new Date());
        event.setType(EventType.PUBLISH_API);

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.name(), "my-api3");
        event.setProperties(properties);

        return event;
    }
}
