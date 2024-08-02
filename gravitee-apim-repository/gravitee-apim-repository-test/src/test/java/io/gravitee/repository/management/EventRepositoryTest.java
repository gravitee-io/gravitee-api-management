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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public class EventRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/event-tests/";
    }

    @Test
    public void createEventTest() throws Exception {
        Event event = new Event();
        event.setId(UUID.toString(UUID.random()));
        event.setEnvironments(singleton("DEFAULT"));
        event.setOrganizations(singleton("DEFAULT"));
        event.setType(EventType.PUBLISH_API);
        event.setPayload("{}");
        event.setParentId(null);
        event.setCreatedAt(new Date());
        event.setUpdatedAt(event.getCreatedAt());

        Event eventCreated = eventRepository.create(event);

        assertEquals("Invalid saved event type.", EventType.PUBLISH_API, eventCreated.getType());
        assertEquals("Invalid saved event payload.", "{}", eventCreated.getPayload());
        assertTrue("Invalid saved environment id.", eventCreated.getEnvironments().contains("DEFAULT"));
        assertTrue("Invalid saved organization id.", eventCreated.getOrganizations().contains("DEFAULT"));
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
            EventCriteria.builder().from(1420070400000L).to(1422748800000L).type(EventType.START_API).build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertEquals(0, eventPage.getTotalElements());
    }

    @Test
    public void searchBySingleEventType() throws Exception {
        Page<Event> eventPage = eventRepository.search(
            EventCriteria.builder().from(1451606400000L).to(1470157767000L).type(EventType.START_API).build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );
        assertEquals(5L, eventPage.getTotalElements());
        Event event = eventPage.getContent().iterator().next();
        assertEquals("event12", event.getId());
    }

    @Test
    public void searchByMultipleEventType() throws Exception {
        final EventCriteria eventCriteria = EventCriteria
            .builder()
            .from(1451606400000L)
            .to(1470157767000L)
            .types(Set.of(EventType.START_API, EventType.STOP_API))
            .build();

        Page<Event> eventPage = eventRepository.search(eventCriteria, new PageableBuilder().pageNumber(0).pageSize(2).build());

        assertEquals(7, eventPage.getTotalElements());
        assertEquals(2, eventPage.getPageElements());
        Event event = eventPage.getContent().iterator().next();
        assertEquals("event14", event.getId());

        eventPage = eventRepository.search(eventCriteria, new PageableBuilder().pageNumber(2).pageSize(2).build());

        assertEquals(7, eventPage.getTotalElements());
        assertEquals(2, eventPage.getPageElements());
        event = eventPage.getContent().iterator().next();
        assertEquals("event15", event.getId());
    }

    @Test
    public void searchOnlyByType() throws Exception {
        List<Event> events = eventRepository.search(EventCriteria.builder().type(EventType.GATEWAY_STOPPED).build());

        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEquals("event07", event.getId());
    }

    @Test
    public void searchByMissingType() throws Exception {
        Page<Event> eventPage = eventRepository.search(
            EventCriteria.builder().type(EventType.DEBUG_API).build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertEquals(0, eventPage.getTotalElements());
        assertTrue(eventPage.getContent().isEmpty());
    }

    @Test
    public void searchByAPIId() throws Exception {
        Page<Event> eventPage = eventRepository.search(
            EventCriteria
                .builder()
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
            EventCriteria
                .builder()
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
            EventCriteria
                .builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), "api-3")
                .types(Set.of(EventType.START_API, EventType.STOP_API))
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
            EventCriteria
                .builder()
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
            EventCriteria
                .builder()
                .from(1452606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3"))
                .build()
        );

        assertEquals(2L, events.size());
        Event event = events.iterator().next();
        assertEquals("event04", event.getId());
    }

    @Test
    public void searchByCollectionPropertyWithoutPagingAndBoundary() throws Exception {
        List<Event> events = eventRepository.search(
            EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3")).build()
        );

        assertEquals(3L, events.size());
        final Iterator<Event> iterator = events.iterator();
        assertEquals("event04", iterator.next().getId());
        assertEquals("event02", iterator.next().getId());
        assertEquals("event01", iterator.next().getId());
    }

    @Test
    public void searchByEnvironmentDefault() {
        List<Event> events = eventRepository.search(EventCriteria.builder().environments(singletonList("DEFAULT")).build());

        assertEquals(12L, events.size());
        assertThat(events.stream().map(Event::getId))
            .containsExactly(
                "event09",
                "event08",
                "event06",
                "event20",
                "event19",
                "event17",
                "event16",
                "event05",
                "event04",
                "event03",
                "event02",
                "event01"
            );
    }

    @Test
    public void searchByEnvironmentDefaultAndOrganizationDefault() {
        List<Event> events = eventRepository.search(
            EventCriteria.builder().environments(singletonList("DEFAULT")).organizations(singletonList("DEFAULT")).build()
        );

        assertEquals(12L, events.size());
        assertThat(events.stream().map(Event::getId))
            .containsExactly(
                "event09",
                "event08",
                "event06",
                "event20",
                "event19",
                "event17",
                "event16",
                "event05",
                "event04",
                "event03",
                "event02",
                "event01"
            );
    }

    @Test
    public void searchByEnvironmentsDefaultAndOther() {
        List<Event> events = eventRepository.search(EventCriteria.builder().environments(Arrays.asList("DEFAULT", "OTHER_ENV")).build());

        assertEquals(14L, events.size());
        assertThat(events.stream().map(Event::getId))
            .containsExactly(
                "event09",
                "event08",
                "event07",
                "event06",
                "event20",
                "event19",
                "event18",
                "event17",
                "event16",
                "event05",
                "event04",
                "event03",
                "event02",
                "event01"
            );
    }

    @Test
    public void searchAll() {
        List<Event> events = eventRepository.search(EventCriteria.builder().build());

        // All events.
        assertEquals(20L, events.size());
    }

    @Test
    public void searchByOrganizationsDefault() {
        List<Event> events = eventRepository.search(EventCriteria.builder().organizations(List.of("DEFAULT")).build());

        assertEquals(14L, events.size());
        assertThat(events.stream().map(Event::getId))
            .containsExactly(
                "event09",
                "event08",
                "event07",
                "event06",
                "event20",
                "event19",
                "event18",
                "event17",
                "event16",
                "event05",
                "event04",
                "event03",
                "event02",
                "event01"
            );
    }

    @Test
    public void searchByOrganizationsOther() {
        List<Event> events = eventRepository.search(EventCriteria.builder().organizations(List.of("OTHER_ORG")).build());

        assertEquals(6L, events.size());
        assertThat(events.stream().map(Event::getId)).containsExactly("event13", "event10", "event14", "event12", "event11", "event16");
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue(eventRepository.findById("event05").isPresent());

        eventRepository.delete("event05");

        assertFalse(eventRepository.findById("event05").isPresent());
    }

    @Test
    public void shouldDeleteByApi() throws Exception {
        assertFalse(
            eventRepository.search(EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api-2").build()).isEmpty()
        );

        long deleteApiEvents = eventRepository.deleteApiEvents("api-2");
        assertEquals(3, deleteApiEvents);

        assertTrue(
            eventRepository.search(EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api-2").build()).isEmpty()
        );
    }

    @Test
    public void shouldNotDeleteByApiWhenNoEvent() throws Exception {
        assertTrue(
            eventRepository
                .search(EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api-with-no-event").build())
                .isEmpty()
        );

        long deleteApiEvents = eventRepository.deleteApiEvents("api-with-no-event");
        assertEquals(0, deleteApiEvents);

        assertTrue(
            eventRepository
                .search(EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api-with-no-event").build())
                .isEmpty()
        );
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

    @Test(expected = IllegalStateException.class)
    public void createOrUpdateShouldThrowIllegalStateException() throws TechnicalException {
        eventRepository.createOrPatch(null);
    }

    @Test
    public void createOrUpdateShouldCreateHeartbeatEvent() throws TechnicalException {
        Event event = new Event();
        String uuid = UUID.toString(UUID.random());
        event.setId(uuid);
        event.setEnvironments(singleton("DEFAULT"));
        event.setOrganizations(singleton("DEFAULT"));
        // Here we use a PUBLISH_API event to ease the writing of this test and differentiate the cases
        event.setType(EventType.PUBLISH_API);
        event.setPayload("{}");
        event.setParentId(null);
        event.setCreatedAt(new Date());
        event.setUpdatedAt(event.getCreatedAt());

        var createdEvent = eventRepository.createOrPatch(event);

        assertEquals("Invalid saved event type.", EventType.PUBLISH_API, createdEvent.getType());
        assertEquals("Invalid saved event payload.", "{}", createdEvent.getPayload());
        assertTrue("Invalid saved environment id.", createdEvent.getEnvironments().contains("DEFAULT"));
        assertTrue("Invalid saved organization id.", createdEvent.getOrganizations().contains("DEFAULT"));
    }

    @Test
    public void createOrUpdateShouldUpdateHeartbeatEvent() throws TechnicalException {
        Event event = new Event();
        String uuid = UUID.toString(UUID.random());
        LocalDateTime localDate = LocalDateTime.now().minusMinutes(1);
        Date createdDate = Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
        event.setId(uuid);
        event.setEnvironments(singleton("DEFAULT"));
        event.setOrganizations(singleton("DEFAULT"));
        event.setType(EventType.PUBLISH_API);
        event.setPayload("{}");
        event.setParentId(null);
        event.setCreatedAt(createdDate);
        event.setUpdatedAt(createdDate);
        event.setProperties(new HashMap<>());
        event.getProperties().put("last_heartbeat_at", new Timestamp(createdDate.getTime()).toString());
        event.getProperties().put("to_update", "property_to_update");
        event.getProperties().put("to_update_with_null", "property_to_update_with_null");
        event.getProperties().put("not_updated", "will_not_change");

        // Should create the event
        Event createdEvent = eventRepository.createOrPatch(event);

        assertEquals("Invalid saved event type.", EventType.PUBLISH_API, createdEvent.getType());
        assertEquals("Invalid saved event payload.", "{}", createdEvent.getPayload());
        assertTrue("Invalid saved environment id.", createdEvent.getEnvironments().contains("DEFAULT"));
        assertTrue("last_heartbeat_at property is absent", createdEvent.getProperties().containsKey("last_heartbeat_at"));
        assertTrue("to_update property is absent", createdEvent.getProperties().containsKey("to_update"));
        assertEquals("property_to_update", createdEvent.getProperties().get("to_update"));
        assertTrue("to_update_with_null property is absent", createdEvent.getProperties().containsKey("to_update_with_null"));
        assertEquals("property_to_update_with_null", createdEvent.getProperties().get("to_update_with_null"));
        assertEquals("will_not_change", createdEvent.getProperties().get("not_updated"));

        event.setType(EventType.UNPUBLISH_API);
        Date updateDate = new Date();
        event.setUpdatedAt(updateDate);
        var properties = new HashMap<String, String>();
        properties.put("last_heartbeat_at", new Timestamp(updateDate.getTime()).toString());
        properties.put("to_update", "updated_property");
        properties.put("to_update_with_null", null);
        event.setProperties(properties);

        // Should update the event with the new type and properties.
        Event updatedEvent = eventRepository.createOrPatch(event);

        assertTrue(updatedEvent.getUpdatedAt().after(createdEvent.getUpdatedAt()));
        assertNotNull(updatedEvent.getProperties());
        assertTrue(
            Timestamp
                .valueOf(updatedEvent.getProperties().get("last_heartbeat_at"))
                .after(Timestamp.valueOf(createdEvent.getProperties().get("last_heartbeat_at")))
        );
        assertEquals(updatedEvent.getProperties().get("to_update"), "updated_property");
        assertEquals(updatedEvent.getType(), EventType.UNPUBLISH_API);
        assertTrue(updatedEvent.getProperties().containsKey("to_update_with_null"));
        assertNull(updatedEvent.getProperties().get("to_update_with_null"));
    }

    @Test
    public void should_find_by_environment_id() {
        List<Event> events = eventRepository.findByEnvironmentId("DEFAULT");

        assertEquals(11L, events.size());
        assertThat(events.stream().map(Event::getId))
            .containsOnly(
                "event09",
                "event08",
                "event06",
                "event20",
                "event19",
                "event17",
                "event05",
                "event04",
                "event03",
                "event02",
                "event01"
            );
    }

    @Test
    public void should_find_by_organization_id() {
        List<Event> events = eventRepository.findByOrganizationId("DEFAULT");

        assertEquals(13L, events.size());
        assertThat(events.stream().map(Event::getId))
            .containsOnly(
                "event09",
                "event08",
                "event07",
                "event06",
                "event20",
                "event19",
                "event18",
                "event17",
                "event05",
                "event04",
                "event03",
                "event02",
                "event01"
            );
    }
}
