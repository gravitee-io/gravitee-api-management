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
package io.gravitee.repository;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class EventRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/event-tests/";
    }

    @Test
    public void createEventTest() throws Exception {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        event.setType(EventType.PUBLISH_API);
        event.setPayload("{}");
        event.setParentId(null);
        event.setCreatedAt(new Date());
        event.setUpdatedAt(event.getCreatedAt());

        Event eventCreated = eventRepository.create(event);

        assertEquals("Invalid saved event type.", EventType.PUBLISH_API, eventCreated.getType());
        assertEquals("Invalid saved event payload.", "{}", eventCreated.getPayload());
    }

    @Test
    public void findByIdTest() throws Exception {
        Optional<Event> event = eventRepository.findById("event1");
        assertTrue("Event not found", event.isPresent());
        assertEquals(EventType.PUBLISH_API, event.get().getType());
    }

    @Test
    public void checkModifiabledMap() throws Exception {
        Optional<Event> event = eventRepository.findById("event1");
        assertTrue("Event not found", event.isPresent());
        assertEquals(EventType.PUBLISH_API, event.get().getType());

        event.get().getProperties().put("key", "value");
    }

    @Test
    public void searchNoResults() {
        Page<Event> eventPage = eventRepository.search(
                new EventCriteria.Builder().from(1420070400000L).to(1422748800000L)
                        .types(EventType.START_API).build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build());

        assertEquals(0, eventPage.getTotalElements());
    }

    @Test
    public void searchBySingleEventType() throws Exception {
        Page<Event> eventPage = eventRepository.search(
                new EventCriteria.Builder().from(1451606400000L).to(1470157767000L)
                        .types(EventType.START_API).build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build());
        assertTrue(2L == eventPage.getTotalElements());
        Event event = eventPage.getContent().iterator().next();
        assertTrue("event6".equals(event.getId()));
    }

    @Test
    public void searchByMultipleEventType() throws Exception {
        final EventCriteria eventCriteria = new EventCriteria.Builder().from(1451606400000L).to(1470157767000L)
                .types(EventType.START_API, EventType.STOP_API).build();

        Page<Event> eventPage = eventRepository.search(eventCriteria, new PageableBuilder().pageNumber(0).pageSize(2).build());

        assertEquals(3, eventPage.getTotalElements());
        assertEquals(2, eventPage.getPageElements());
        Event event = eventPage.getContent().iterator().next();
        assertEquals("event6", event.getId());

        eventPage = eventRepository.search(eventCriteria, new PageableBuilder().pageNumber(1).pageSize(2).build());

        assertEquals(3, eventPage.getTotalElements());
        assertEquals(1, eventPage.getPageElements());
        event = eventPage.getContent().iterator().next();
        assertEquals("event4", event.getId());
    }

    @Test
    public void searchByMissingType() throws Exception {
        Page<Event> eventPage = eventRepository.search(
                new EventCriteria.Builder().types(EventType.GATEWAY_STARTED).build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build());

        assertEquals(0, eventPage.getTotalElements());
        assertTrue(eventPage.getContent().isEmpty());
    }

    @Test
    public void searchByAPIId() throws Exception {
        Page<Event> eventPage = eventRepository.search(
                new EventCriteria.Builder()
                        .from(1451606400000L).to(1470157767000L)
                        .property(Event.EventProperties.API_ID.getValue(), "api-1")
                        .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build());

        assertTrue(2L == eventPage.getTotalElements());
        Event event = eventPage.getContent().iterator().next();
        assertTrue("event2".equals(event.getId()));
    }

    @Test
    public void searchByAPI_EmptyPageable() throws Exception {
        Page<Event> eventPage = eventRepository.search(
                new EventCriteria.Builder()
                        .from(1451606400000L).to(1470157767000L)
                        .property(Event.EventProperties.API_ID.getValue(), "api-1")
                        .build(),
                null);

        assertTrue(2L == eventPage.getTotalElements());
        Event event = eventPage.getContent().iterator().next();
        assertTrue("event2".equals(event.getId()));
    }

    @Test
    public void searchByMixProperties() throws Exception {
        Page<Event> eventPage = eventRepository.search(
                new EventCriteria.Builder()
                        .from(1451606400000L).to(1470157767000L)
                        .property(Event.EventProperties.API_ID.getValue(), "api-3")
                        .types(EventType.START_API, EventType.STOP_API)
                        .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build());

        assertTrue(1L == eventPage.getTotalElements());
        Event event = eventPage.getContent().iterator().next();
        assertTrue("event4".equals(event.getId()));
    }

    @Test
    public void searchByCollectionProperty() throws Exception {
        Page<Event> eventPage = eventRepository.search(
                new EventCriteria.Builder()
                        .from(1451606400000L).to(1470157767000L)
                        .property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3"))
                        .build(), null);

        assertTrue(3L == eventPage.getTotalElements());
        Event event = eventPage.getContent().iterator().next();
        assertTrue("event4".equals(event.getId()));
    }

    @Test
    public void searchByCollectionPropertyWithoutPaging() throws Exception {
        List<Event> events = eventRepository.search(
                new EventCriteria.Builder()
                        .from(1451606400000L).to(1470157767000L)
                        .property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3"))
                        .build());

        assertTrue(3L == events.size());
        Event event = events.iterator().next();
        assertTrue("event4".equals(event.getId()));
    }

    @Test
    public void searchByCollectionPropertyWithoutPagingAndBoundary() throws Exception {
        List<Event> events = eventRepository.search(
                new EventCriteria.Builder()
                        .property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3"))
                        .build());

        assertTrue(3L == events.size());
        final Iterator<Event> iterator = events.iterator();
        assertTrue("event4".equals(iterator.next().getId()));
        assertTrue("event2".equals(iterator.next().getId()));
        assertTrue("event1".equals(iterator.next().getId()));
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue(eventRepository.findById("event5").isPresent());

        eventRepository.delete("event5");

        assertFalse(eventRepository.findById("event5").isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownEvent() throws Exception {
        Event unknownEvent = new Event();
        unknownEvent.setId("unknown");
        eventRepository.update(unknownEvent);
        fail("An unknown event should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        eventRepository.update(null);
        fail("A null event should not be updated");
    }
}
