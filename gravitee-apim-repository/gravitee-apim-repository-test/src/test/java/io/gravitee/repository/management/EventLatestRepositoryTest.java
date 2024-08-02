/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.management;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class EventLatestRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/event-latest-tests/";
    }

    protected void createModel(Object object) throws TechnicalException {
        if (object instanceof Event) {
            eventLatestRepository.createOrUpdate((Event) object);
        }
    }

    @Test
    public void shouldCreateEventLatest() throws Exception {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        event.setEnvironments(singleton("DEFAULT"));
        event.setOrganizations(singleton("MY_ORG"));
        event.setType(EventType.PUBLISH_API);
        event.setPayload("{}");
        event.setParentId(null);
        event.setCreatedAt(new Date());
        event.setUpdatedAt(event.getCreatedAt());

        Event eventCreated = eventLatestRepository.createOrUpdate(event);

        assertEquals("Invalid saved event type.", EventType.PUBLISH_API, eventCreated.getType());
        assertEquals("Invalid saved event payload.", "{}", eventCreated.getPayload());
        assertTrue("Invalid saved environment id.", eventCreated.getEnvironments().contains("DEFAULT"));
        assertTrue("Invalid saved organization id.", eventCreated.getOrganizations().contains("MY_ORG"));
    }

    @Test
    public void shouldPatchCreatedEventLatest() throws Exception {
        String id = UUID.toString(UUID.random());
        Event event = new Event();
        event.setId(id);
        event.setEnvironments(singleton("DEFAULT"));
        event.setOrganizations(singleton("MY_ORG"));
        event.setType(EventType.PUBLISH_API);
        event.setPayload("{}");
        event.setParentId(null);
        event.setCreatedAt(new Date());
        event.setUpdatedAt(event.getCreatedAt());
        Map<String, String> properties = Map.of("key", "value");
        event.setProperties(properties);

        Event eventCreated = eventLatestRepository.createOrUpdate(event);

        assertEquals("Invalid saved event type.", EventType.PUBLISH_API, eventCreated.getType());
        assertEquals("Invalid saved event payload.", "{}", eventCreated.getPayload());
        assertEquals("Invalid saved event properties.", properties, eventCreated.getProperties());
        assertTrue("Invalid saved environment id.", eventCreated.getEnvironments().contains("DEFAULT"));
        assertTrue("Invalid saved organization id.", eventCreated.getOrganizations().contains("MY_ORG"));

        eventCreated.setCreatedAt(new Date());
        eventCreated.setUpdatedAt(event.getCreatedAt());
        Event eventUpdated = eventLatestRepository.createOrUpdate(eventCreated);
        assertEquals("Invalid updated event type.", EventType.PUBLISH_API, eventUpdated.getType());
        assertEquals("Invalid updated event payload.", "{}", eventUpdated.getPayload());
        assertEquals("Invalid updated event properties.", properties, eventUpdated.getProperties());
        assertTrue("Invalid updated environment id.", eventUpdated.getEnvironments().contains("DEFAULT"));
        assertTrue("Invalid saved organization id.", eventUpdated.getOrganizations().contains("MY_ORG"));

        List<Event> eventFounds = eventLatestRepository.search(EventCriteria.builder().property("key", "value").build(), null, null, null);
        assertEquals("Invalid found event", eventUpdated, eventFounds.get(0));
    }

    @Test
    public void shouldReturnAllApiEventsWhenSearchingWithoutPagingAndSize() {
        List<Event> events = eventLatestRepository.search(EventCriteria.builder().build(), Event.EventProperties.API_ID, null, null);

        assertEquals(8L, events.size());
        assertThat(events.stream().map(Event::getId))
            .containsExactly("api-1", "api-2", "api-3", "api-4", "api-5", "api-6", "api-7", "api-8");
    }

    @Test
    public void shouldReturnApiEventsForApi2WhenSearchingForPage3Size1() {
        List<Event> events = eventLatestRepository.search(EventCriteria.builder().build(), Event.EventProperties.API_ID, 3L, 1L);

        assertEquals(1L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("api-4", iterator.next().getId());
    }

    @Test
    public void shouldReturnApiEventsInSameOrderWhenSearchWithPagination() {
        //The order of events should be always the same, whatever the page size is
        final EventCriteria eventCriteria = EventCriteria.builder().build();

        // Test 1 by 1
        List<Event> events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 0L, 1L);
        Iterator<Event> iterator = events.iterator();
        assertEquals(1L, events.size());
        assertEquals("api-1", iterator.next().getId());

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 1L, 1L);
        iterator = events.iterator();
        assertEquals(1L, events.size());
        assertEquals("api-2", iterator.next().getId());

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 2L, 1L);
        iterator = events.iterator();
        assertEquals(1L, events.size());
        assertEquals("api-3", iterator.next().getId());

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 3L, 1L);
        iterator = events.iterator();
        assertEquals(1L, events.size());
        assertEquals("api-4", iterator.next().getId());

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 4L, 1L);
        iterator = events.iterator();
        assertEquals(1L, events.size());
        assertEquals("api-5", iterator.next().getId());

        // Test 2 by 2
        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 0L, 2L);
        iterator = events.iterator();
        assertEquals(2L, events.size());
        assertEquals("api-1", iterator.next().getId());
        assertEquals("api-2", iterator.next().getId());

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 1L, 2L);
        assertEquals(2L, events.size());
        iterator = events.iterator();
        assertEquals("api-3", iterator.next().getId());
        assertEquals("api-4", iterator.next().getId());

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 2L, 2L);
        iterator = events.iterator();
        assertEquals(2L, events.size());
        assertEquals("api-5", iterator.next().getId());
        assertEquals("api-6", iterator.next().getId());

        // Test 3 by 3
        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 0L, 3L);
        iterator = events.iterator();
        assertEquals(3L, events.size());
        assertEquals("api-1", iterator.next().getId());
        assertEquals("api-2", iterator.next().getId());
        assertEquals("api-3", iterator.next().getId());

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 1L, 3L);
        iterator = events.iterator();
        assertEquals(3L, events.size());
        assertEquals("api-4", iterator.next().getId());
        assertEquals("api-5", iterator.next().getId());
        assertEquals("api-6", iterator.next().getId());

        // Test 4 by 4
        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 0L, 4L);
        iterator = events.iterator();
        assertEquals(4L, events.size());
        assertEquals("api-1", iterator.next().getId());
        assertEquals("api-2", iterator.next().getId());
        assertEquals("api-3", iterator.next().getId());
        assertEquals("api-4", iterator.next().getId());

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 1L, 4L);
        iterator = events.iterator();
        assertEquals(4L, events.size());
        assertEquals("api-5", iterator.next().getId());
        assertEquals("api-6", iterator.next().getId());
        assertEquals("api-7", iterator.next().getId());
        assertEquals("api-8", iterator.next().getId());

        // Test 5 by 5
        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 0L, 5L);
        iterator = events.iterator();
        assertEquals(5L, events.size());
        assertEquals("api-1", iterator.next().getId());
        assertEquals("api-2", iterator.next().getId());
        assertEquals("api-3", iterator.next().getId());
        assertEquals("api-4", iterator.next().getId());
        assertEquals("api-5", iterator.next().getId());
    }

    @Test
    public void shouldReturnApiEventsWhenSearchingWithTime() {
        List<Event> events = eventLatestRepository.search(
            EventCriteria
                .builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), "api-3")
                .types(Set.of(EventType.START_API, EventType.STOP_API))
                .build(),
            Event.EventProperties.API_ID,
            0L,
            10L
        );

        assertEquals(1L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("api-3", iterator.next().getId());
    }

    @Test
    public void shouldReturnApiEventsWhenSearching() {
        List<Event> events = eventLatestRepository.search(
            EventCriteria
                .builder()
                .property(Event.EventProperties.API_ID.getValue(), "api-1")
                .types(Set.of(EventType.START_API, EventType.PUBLISH_API))
                .build(),
            Event.EventProperties.API_ID,
            0L,
            10L
        );

        assertEquals(1, events.size());
    }

    @Test
    public void shouldReturnApiEventsWhenSearchingWithDefaultEnvironment() {
        List<Event> events = eventLatestRepository.search(
            EventCriteria.builder().types(Set.of(EventType.START_API, EventType.PUBLISH_API)).environments(singleton("DEFAULT")).build(),
            Event.EventProperties.API_ID,
            0L,
            10L
        );

        assertEquals(5, events.size());
        assertThat(events.stream().map(Event::getId)).containsExactly("api-1", "api-4", "api-5", "api-6", "api-8");
    }

    @Test
    public void shouldReturnApiEventsWhenSearchingWithOtherOrganization() {
        eventLatestRepository.search(EventCriteria.builder().build(), Event.EventProperties.API_ID, 0L, 10L);
        List<Event> events = eventLatestRepository.search(
            EventCriteria.builder().types(Set.of(EventType.START_API, EventType.PUBLISH_API)).organizations(singleton("OTHER")).build(),
            Event.EventProperties.API_ID,
            0L,
            10L
        );

        assertEquals(2, events.size());
        assertThat(events.stream().map(Event::getId)).containsExactly("api-6", "api-8");
    }

    @Test
    public void shouldReturnEventsWhenSearchingByEnvironmentDefaultAndOrganizationDefault() {
        List<Event> events = eventLatestRepository.search(
            EventCriteria.builder().environments(singletonList("DEFAULT")).organizations(singletonList("DEFAULT")).build(),
            null,
            0L,
            100L
        );

        assertEquals(8L, events.size());
        assertThat(events.stream().map(Event::getId))
            .containsExactly("api-1", "api-2", "api-3", "api-4", "api-5", "dictionary-1", "api-6", "api-8");
    }

    @Test
    public void shouldReturnDictionaryEventsWhenSearchingBetweenTime() {
        List<Event> events = eventLatestRepository.search(
            EventCriteria.builder().from(1475200000000L).to(1475381000000L).type(EventType.PUBLISH_DICTIONARY).build(),
            Event.EventProperties.DICTIONARY_ID,
            0L,
            10L
        );

        assertEquals(1L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("dictionary-1", iterator.next().getId());
    }

    @Test
    public void shouldReturnAllDictionaryEventsWhenSearchingWithoutPagination() {
        List<Event> events = eventLatestRepository.search(EventCriteria.builder().build(), Event.EventProperties.DICTIONARY_ID, null, null);

        assertEquals(2L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("dictionary-1", iterator.next().getId());
        assertEquals("dictionary-2", iterator.next().getId());
    }

    @Test
    public void shouldDeleteEventFromExistingId() throws Exception {
        EventCriteria eventCriteria = EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api-5").build();
        List<Event> events = eventLatestRepository.search(eventCriteria, null, null, null);
        assertEquals("Invalid found event", 1, events.size());
        eventLatestRepository.delete("api-5");

        List<Event> eventsDelete = eventLatestRepository.search(eventCriteria, null, null, null);
        assertEquals("Invalid found event", 0, eventsDelete.size());
    }

    @Test
    public void should_find_by_environment_id() {
        List<Event> events = eventLatestRepository.findByEnvironmentId("DEFAULT");

        assertEquals(7L, events.size());
        assertThat(events.stream().map(Event::getId)).containsOnly("api-1", "api-2", "api-3", "dictionary-1", "api-4", "api-5", "api-8");
    }

    @Test
    public void should_find_by_organization_id() {
        List<Event> events = eventLatestRepository.findByOrganizationId("DEFAULT");

        assertEquals(8L, events.size());
        assertThat(events.stream().map(Event::getId))
            .containsOnly("api-1", "api-2", "api-3", "dictionary-1", "api-4", "api-5", "api-7", "api-8");
    }
}
