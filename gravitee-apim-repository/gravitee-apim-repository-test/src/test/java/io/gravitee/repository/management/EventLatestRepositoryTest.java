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

import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.Date;
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
        assertThat(eventCreated).isEqualTo(event);
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
        event.setCreatedAt(new Date(1564739200000L));
        event.setUpdatedAt(new Date(1564739200000L));
        Map<String, String> properties = Map.of("key", "value", "deployment_number", "1");
        event.setProperties(properties);

        Event eventCreated = eventLatestRepository.createOrUpdate(event);
        assertThat(eventCreated).isEqualTo(event);

        Event eventUpdated = eventLatestRepository.createOrUpdate(eventCreated.toBuilder().parentId("updated").build());
        assertThat(eventUpdated).extracting(Event::getParentId).isEqualTo("updated");

        List<Event> eventFounds = eventLatestRepository.search(EventCriteria.builder().property("key", "value").build(), null, null, null);
        assertThat(eventFounds).contains(
            Event.builder()
                .id(id)
                .organizations(Set.of("MY_ORG"))
                .environments(Set.of("DEFAULT"))
                .type(EventType.PUBLISH_API)
                .payload("{}")
                .parentId("updated")
                .properties(Map.ofEntries(Map.entry("key", "value"), Map.entry("deployment_number", "1")))
                .createdAt(new Date(1564739200000L))
                .updatedAt(new Date(1564739200000L))
                .build()
        );
    }

    @Test
    public void shouldReturnAllApiEventsWhenSearchingWithoutPagingAndSize() {
        List<Event> events = eventLatestRepository.search(EventCriteria.builder().build(), Event.EventProperties.API_ID, null, null);

        assertThat(events)
            .hasSize(8)
            .extracting(Event::getId)
            .containsExactly("api-1", "api-2", "api-3", "api-4", "api-5", "api-6", "api-7", "api-8");
    }

    @Test
    public void shouldReturnApiEventsForApi2WhenSearchingForPage3Size1() {
        List<Event> events = eventLatestRepository.search(EventCriteria.builder().build(), Event.EventProperties.API_ID, 3L, 1L);

        assertThat(events)
            .hasSize(1)
            .contains(
                Event.builder()
                    .id("api-4")
                    .organizations(Set.of("DEFAULT"))
                    .environments(Set.of("DEFAULT"))
                    .type(EventType.START_API)
                    .payload("{}")
                    .properties(Map.ofEntries(Map.entry("api_id", "api-4"), Map.entry("deployment_number", "1")))
                    .createdAt(new Date(1464739200000L))
                    .updatedAt(new Date(1464739200000L))
                    .build()
            );
    }

    @Test
    public void shouldReturnApiEvents() {
        //The order of events should be always the same, whatever the page size is
        final EventCriteria eventCriteria = EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api-3").build();

        List<Event> events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 0L, 1L);
        assertThat(events).hasSize(1).extracting(Event::getId).containsExactly("api-3");
    }

    @Test
    public void shouldReturnApiEventsInSameOrderWhenSearchWithPagination() {
        //The order of events should be always the same, whatever the page size is
        final EventCriteria eventCriteria = EventCriteria.builder().build();

        // Test 1 by 1
        List<Event> events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 0L, 1L);
        assertThat(events).hasSize(1).extracting(Event::getId).containsExactly("api-1");

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 1L, 1L);
        assertThat(events).hasSize(1).extracting(Event::getId).containsExactly("api-2");

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 2L, 1L);
        assertThat(events).hasSize(1).extracting(Event::getId).containsExactly("api-3");

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 3L, 1L);
        assertThat(events).hasSize(1).extracting(Event::getId).containsExactly("api-4");

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 4L, 1L);
        assertThat(events).hasSize(1).extracting(Event::getId).containsExactly("api-5");

        // Test 2 by 2
        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 0L, 2L);
        assertThat(events).hasSize(2).extracting(Event::getId).containsExactly("api-1", "api-2");

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 1L, 2L);
        assertThat(events).hasSize(2).extracting(Event::getId).containsExactly("api-3", "api-4");

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 2L, 2L);
        assertThat(events).hasSize(2).extracting(Event::getId).containsExactly("api-5", "api-6");

        // Test 3 by 3
        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 0L, 3L);
        assertThat(events).hasSize(3).extracting(Event::getId).containsExactly("api-1", "api-2", "api-3");

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 1L, 3L);
        assertThat(events).hasSize(3).extracting(Event::getId).containsExactly("api-4", "api-5", "api-6");

        // Test 4 by 4
        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 0L, 4L);
        assertThat(events).hasSize(4).extracting(Event::getId).containsExactly("api-1", "api-2", "api-3", "api-4");

        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 1L, 4L);
        assertThat(events).hasSize(4).extracting(Event::getId).containsExactly("api-5", "api-6", "api-7", "api-8");

        // Test 5 by 5
        events = eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 0L, 5L);
        assertThat(events).hasSize(5).extracting(Event::getId).containsExactly("api-1", "api-2", "api-3", "api-4", "api-5");
    }

    @Test
    public void shouldReturnApiEventsWhenSearchingWithTime() {
        List<Event> events = eventLatestRepository.search(
            EventCriteria.builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), "api-3")
                .types(Set.of(EventType.START_API, EventType.STOP_API))
                .build(),
            Event.EventProperties.API_ID,
            0L,
            10L
        );

        assertThat(events).hasSize(1).extracting(Event::getId).containsExactly("api-3");
    }

    @Test
    public void shouldReturnApiEventsWhenSearching() {
        List<Event> events = eventLatestRepository.search(
            EventCriteria.builder()
                .property(Event.EventProperties.API_ID.getValue(), "api-1")
                .types(Set.of(EventType.START_API, EventType.PUBLISH_API))
                .build(),
            Event.EventProperties.API_ID,
            0L,
            10L
        );

        assertThat(events).hasSize(1).extracting(Event::getId).containsExactly("api-1");
    }

    @Test
    public void shouldReturnApiEventsWhenSearchingWithDefaultEnvironment() {
        List<Event> events = eventLatestRepository.search(
            EventCriteria.builder().types(Set.of(EventType.START_API, EventType.PUBLISH_API)).environments(singleton("DEFAULT")).build(),
            Event.EventProperties.API_ID,
            0L,
            10L
        );

        assertThat(events).extracting(Event::getId).containsExactly("api-1", "api-4", "api-5", "api-6", "api-8");
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

        assertThat(events).extracting(Event::getId).containsExactly("api-6", "api-8");
    }

    @Test
    public void shouldReturnEventsWhenSearchingByEnvironmentDefaultAndOrganizationDefault() {
        List<Event> events = eventLatestRepository.search(
            EventCriteria.builder().environments(singletonList("DEFAULT")).organizations(singletonList("DEFAULT")).build(),
            null,
            0L,
            100L
        );

        assertThat(events)
            .extracting(Event::getId)
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

        assertThat(events).hasSize(1).extracting(Event::getId).containsExactly("dictionary-1");
    }

    @Test
    public void shouldReturnAllDictionaryEventsWhenSearchingWithoutPagination() {
        List<Event> events = eventLatestRepository.search(EventCriteria.builder().build(), Event.EventProperties.DICTIONARY_ID, null, null);

        assertThat(events).hasSize(2).extracting(Event::getId).containsExactly("dictionary-1", "dictionary-2");
    }

    @Test
    public void shouldDeleteEventFromExistingId() throws Exception {
        EventCriteria eventCriteria = EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api-5").build();
        List<Event> events = eventLatestRepository.search(eventCriteria, null, null, null);
        assertThat(events).as("Invalid found event").hasSize(1);
        eventLatestRepository.delete("api-5");

        List<Event> eventsDelete = eventLatestRepository.search(eventCriteria, null, null, null);
        assertThat(eventsDelete).as("Invalid found event").isEmpty();
    }

    @Test
    public void should_find_by_environment_id() {
        List<Event> events = eventLatestRepository.findByEnvironmentId("DEFAULT");

        assertThat(events)
            .hasSize(7)
            .extracting(Event::getId)
            .containsOnly("api-1", "api-2", "api-3", "dictionary-1", "api-4", "api-5", "api-8");
    }

    @Test
    public void should_find_by_organization_id() {
        List<Event> events = eventLatestRepository.findByOrganizationId("DEFAULT");

        assertThat(events)
            .hasSize(8)
            .extracting(Event::getId)
            .containsOnly("api-1", "api-2", "api-3", "dictionary-1", "api-4", "api-5", "api-7", "api-8");
    }
}
