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

import static org.junit.Assert.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.*;
import org.junit.Test;

public class EventRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/event-tests/";
    }

    @Test
    public void createEventTest() throws Exception {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        event.setEnvironmentId("DEFAULT");
        event.setType(EventType.PUBLISH_API);
        event.setPayload("{}");
        event.setParentId(null);
        event.setCreatedAt(new Date());
        event.setUpdatedAt(event.getCreatedAt());

        Event eventCreated = eventRepository.create(event);

        assertEquals("Invalid saved event type.", EventType.PUBLISH_API, eventCreated.getType());
        assertEquals("Invalid saved event payload.", "{}", eventCreated.getPayload());
        assertEquals("Invalid saved environment id.", "DEFAULT", eventCreated.getEnvironmentId());
    }

    @Test
    public void findByIdTest() throws Exception {
        Optional<Event> event = eventRepository.findById("event01");
        assertTrue("Event not found", event.isPresent());
        assertEquals(EventType.PUBLISH_API, event.get().getType());
    }

    @Test
    public void checkModifiabledMap() throws Exception {
        Optional<Event> event = eventRepository.findById("event01");
        assertTrue("Event not found", event.isPresent());
        assertEquals(EventType.PUBLISH_API, event.get().getType());

        event.get().getProperties().put("key", "value");
    }

    @Test
    public void searchNoResults() {
        Page<Event> eventPage = eventRepository.search(
            new EventCriteria.Builder().from(1420070400000L).to(1422748800000L).types(EventType.START_API).build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertEquals(0, eventPage.getTotalElements());
    }

    @Test
    public void searchBySingleEventType() throws Exception {
        Page<Event> eventPage = eventRepository.search(
            new EventCriteria.Builder().from(1451606400000L).to(1470157767000L).types(EventType.START_API).build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );
        assertEquals(4L, eventPage.getTotalElements());
        Event event = eventPage.getContent().iterator().next();
        assertEquals("event11", event.getId());
    }

    @Test
    public void searchByMultipleEventType() throws Exception {
        final EventCriteria eventCriteria = new EventCriteria.Builder()
            .from(1451606400000L)
            .to(1470157767000L)
            .types(EventType.START_API, EventType.STOP_API)
            .build();

        Page<Event> eventPage = eventRepository.search(eventCriteria, new PageableBuilder().pageNumber(0).pageSize(2).build());

        assertEquals(5, eventPage.getTotalElements());
        assertEquals(2, eventPage.getPageElements());
        Event event = eventPage.getContent().iterator().next();
        assertEquals("event11", event.getId());

        eventPage = eventRepository.search(eventCriteria, new PageableBuilder().pageNumber(2).pageSize(2).build());

        assertEquals(5, eventPage.getTotalElements());
        assertEquals(1, eventPage.getPageElements());
        event = eventPage.getContent().iterator().next();
        assertEquals("event04", event.getId());
    }

    @Test
    public void searchByMissingType() throws Exception {
        Page<Event> eventPage = eventRepository.search(
            new EventCriteria.Builder().types(EventType.GATEWAY_STARTED).build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertEquals(0, eventPage.getTotalElements());
        assertTrue(eventPage.getContent().isEmpty());
    }

    @Test
    public void searchByAPIId() throws Exception {
        Page<Event> eventPage = eventRepository.search(
            new EventCriteria.Builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), "api-1")
                .build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertEquals(2L, eventPage.getTotalElements());
        Event event = eventPage.getContent().iterator().next();
        assertEquals("event02", event.getId());
    }

    @Test
    public void searchByAPI_EmptyPageable() throws Exception {
        Page<Event> eventPage = eventRepository.search(
            new EventCriteria.Builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), "api-1")
                .build(),
            null
        );

        assertEquals(2L, eventPage.getTotalElements());
        Event event = eventPage.getContent().iterator().next();
        assertEquals("event02", event.getId());
    }

    @Test
    public void searchByMixProperties() throws Exception {
        Page<Event> eventPage = eventRepository.search(
            new EventCriteria.Builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), "api-3")
                .types(EventType.START_API, EventType.STOP_API)
                .build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertEquals(1L, eventPage.getTotalElements());
        Event event = eventPage.getContent().iterator().next();
        assertEquals("event04", event.getId());
    }

    @Test
    public void searchByCollectionProperty() throws Exception {
        Page<Event> eventPage = eventRepository.search(
            new EventCriteria.Builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3"))
                .build(),
            null
        );

        assertEquals(3L, eventPage.getTotalElements());
        Event event = eventPage.getContent().iterator().next();
        assertEquals("event04", event.getId());
    }

    @Test
    public void searchByCollectionPropertyWithoutPaging() throws Exception {
        List<Event> events = eventRepository.search(
            new EventCriteria.Builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3"))
                .build()
        );

        assertEquals(3L, events.size());
        Event event = events.iterator().next();
        assertEquals("event04", event.getId());
    }

    @Test
    public void searchByCollectionPropertyWithoutPagingAndBoundary() throws Exception {
        List<Event> events = eventRepository.search(
            new EventCriteria.Builder().property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3")).build()
        );

        assertEquals(3L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("event04", iterator.next().getId());
        assertEquals("event02", iterator.next().getId());
        assertEquals("event01", iterator.next().getId());
    }

    @Test
    public void searchByEnvironment() throws Exception {
        List<Event> events = eventRepository.search(new EventCriteria.Builder().environmentId("DEFAULT").build());

        assertEquals(10L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("event11", iterator.next().getId());
        assertEquals("event10", iterator.next().getId());
        assertEquals("event06", iterator.next().getId());
        assertEquals("event05", iterator.next().getId());
        assertEquals("event04", iterator.next().getId());
        assertEquals("event03", iterator.next().getId());
        assertEquals("event08", iterator.next().getId());
        assertEquals("event07", iterator.next().getId());
        assertEquals("event02", iterator.next().getId());
        assertEquals("event01", iterator.next().getId());
    }

    @Test
    public void searchLatestApiEventsWithoutPagingAndSize() {
        List<Event> events = eventRepository.searchLatest(
            new EventCriteria.Builder().environmentId("DEFAULT").build(),
            Event.EventProperties.API_ID,
            null,
            null
        );

        assertEquals(5L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("event11", iterator.next().getId());
        assertEquals("event10", iterator.next().getId());
        assertEquals("event06", iterator.next().getId());
        assertEquals("event04", iterator.next().getId());
        assertEquals("event02", iterator.next().getId());
    }

    @Test
    public void searchLatestApiEventsPage3Size1() {
        List<Event> events = eventRepository.searchLatest(
            new EventCriteria.Builder().environmentId("DEFAULT").build(),
            Event.EventProperties.API_ID,
            3L,
            1L
        );

        assertEquals(1L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("event04", iterator.next().getId());
    }

    @Test
    public void searchLatestApiEventsWhenSameUpdateTime() {
        //The order of events should be always the same, whatever the page size is

        // Test 1 by 1
        final EventCriteria eventCriteria = new EventCriteria.Builder().environmentId("DEFAULT").build();

        List<Event> events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 0L, 1L);
        Iterator<Event> iterator = events.iterator();
        assertEquals(1L, events.size());
        assertEquals("event11", iterator.next().getId());

        events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 1L, 1L);
        iterator = events.iterator();
        assertEquals(1L, events.size());
        assertEquals("event10", iterator.next().getId());

        events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 2L, 1L);
        iterator = events.iterator();
        assertEquals(1L, events.size());
        assertEquals("event06", iterator.next().getId());

        events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 3L, 1L);
        iterator = events.iterator();
        assertEquals(1L, events.size());
        assertEquals("event04", iterator.next().getId());

        events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 4L, 1L);
        iterator = events.iterator();
        assertEquals(1L, events.size());
        assertEquals("event02", iterator.next().getId());

        // Test 2 by 2
        events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 0L, 2L);
        iterator = events.iterator();
        assertEquals(2L, events.size());
        assertEquals("event11", iterator.next().getId());
        assertEquals("event10", iterator.next().getId());

        events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 1L, 2L);
        assertEquals(2L, events.size());
        iterator = events.iterator();
        assertEquals("event06", iterator.next().getId());
        assertEquals("event04", iterator.next().getId());

        events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 2L, 2L);
        iterator = events.iterator();
        assertEquals(1L, events.size());
        assertEquals("event02", iterator.next().getId());

        // Test 3 by 3
        events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 0L, 3L);
        iterator = events.iterator();
        assertEquals(3L, events.size());
        assertEquals("event11", iterator.next().getId());
        assertEquals("event10", iterator.next().getId());
        assertEquals("event06", iterator.next().getId());

        events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 1L, 3L);
        iterator = events.iterator();
        assertEquals(2L, events.size());
        assertEquals("event04", iterator.next().getId());
        assertEquals("event02", iterator.next().getId());

        // Test 4 by 4
        events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 0L, 4L);
        iterator = events.iterator();
        assertEquals(4L, events.size());
        assertEquals("event11", iterator.next().getId());
        assertEquals("event10", iterator.next().getId());
        assertEquals("event06", iterator.next().getId());
        assertEquals("event04", iterator.next().getId());

        events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 1L, 4L);
        iterator = events.iterator();
        assertEquals(1L, events.size());
        assertEquals("event02", iterator.next().getId());

        // Test 5 by 5
        events = eventRepository.searchLatest(eventCriteria, Event.EventProperties.API_ID, 0L, 5L);
        iterator = events.iterator();
        assertEquals(5L, events.size());
        assertEquals("event11", iterator.next().getId());
        assertEquals("event10", iterator.next().getId());
        assertEquals("event06", iterator.next().getId());
        assertEquals("event04", iterator.next().getId());
        assertEquals("event02", iterator.next().getId());
    }

    @Test
    public void searchLatestApiEventsByMixProperties() throws Exception {
        List<Event> events = eventRepository.searchLatest(
            new EventCriteria.Builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), "api-3")
                .types(EventType.START_API, EventType.STOP_API)
                .build(),
            Event.EventProperties.API_ID,
            0L,
            10L
        );

        assertEquals(1L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("event04", iterator.next().getId());
    }

    @Test
    public void searchLatestApiEventsWithStrictModeEnabled() throws Exception {
        List<Event> events = eventRepository.searchLatest(
            new EventCriteria.Builder()
                .property(Event.EventProperties.API_ID.getValue(), "api-1")
                .types(EventType.START_API, EventType.PUBLISH_API)
                .strictMode(true)
                .build(),
            Event.EventProperties.API_ID,
            0L,
            10L
        );

        assertEquals(0, events.size());
    }

    @Test
    public void searchLatestApiEventsWithStrictModeDisabled() throws Exception {
        List<Event> events = eventRepository.searchLatest(
            new EventCriteria.Builder()
                .property(Event.EventProperties.API_ID.getValue(), "api-1")
                .types(EventType.START_API, EventType.PUBLISH_API)
                .strictMode(false)
                .build(),
            Event.EventProperties.API_ID,
            0L,
            10L
        );

        assertEquals(1L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("event01", iterator.next().getId());
    }

    @Test
    public void searchLatestDictionaryEventsByMixProperties() throws Exception {
        List<Event> events = eventRepository.searchLatest(
            new EventCriteria.Builder().from(1455800000000L).to(1455941000000L).types(EventType.PUBLISH_DICTIONARY).build(),
            Event.EventProperties.DICTIONARY_ID,
            0L,
            10L
        );

        assertEquals(1L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("event09", iterator.next().getId());
    }

    @Test
    public void searchLatestDictionaryEventsWithoutPagingAndSize() {
        List<Event> events = eventRepository.searchLatest(
            new EventCriteria.Builder().build(),
            Event.EventProperties.DICTIONARY_ID,
            null,
            null
        );

        assertEquals(2L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("event12", iterator.next().getId());
        assertEquals("event08", iterator.next().getId());
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue(eventRepository.findById("event05").isPresent());

        eventRepository.delete("event05");

        assertFalse(eventRepository.findById("event05").isPresent());
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
