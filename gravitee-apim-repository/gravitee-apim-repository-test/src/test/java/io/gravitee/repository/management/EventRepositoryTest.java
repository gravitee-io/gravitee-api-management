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
import java.util.Map;
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
        event.setCreatedAt(new Date(1564739200000L));
        event.setUpdatedAt(event.getCreatedAt());
        event.setProperties(Map.of("key", "value", "deployment_number", "1"));

        Event eventCreated = eventRepository.create(event);

        assertThat(eventCreated).isEqualTo(
            Event.builder()
                .id(event.getId())
                .organizations(Set.of("DEFAULT"))
                .environments(Set.of("DEFAULT"))
                .type(EventType.PUBLISH_API)
                .payload("{}")
                .properties(Map.ofEntries(Map.entry("key", "value"), Map.entry("deployment_number", "1")))
                .createdAt(new Date(1564739200000L))
                .updatedAt(new Date(1564739200000L))
                .build()
        );
    }

    @Test
    public void findByIdTest() throws Exception {
        Optional<Event> event = eventRepository.findById("event01");

        assertThat(event).contains(
            Event.builder()
                .id("event01")
                .organizations(Set.of("DEFAULT"))
                .environments(Set.of("DEFAULT"))
                .type(EventType.PUBLISH_API)
                .payload("{}")
                .properties(Map.ofEntries(Map.entry("api_id", "api-1"), Map.entry("deployment_number", "1")))
                .createdAt(new Date(1451606400000L))
                .updatedAt(new Date(1451606400000L))
                .build()
        );

        // ensure we can update properties
        event.get().getProperties().put("key", "value");
    }

    @Test
    public void searchNoResults() {
        Page<Event> eventPage = eventRepository.search(
            EventCriteria.builder().from(1420070400000L).to(1422748800000L).type(EventType.START_API).build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertThat(eventPage.getTotalElements()).isZero();
    }

    @Test
    public void searchBySingleEventType() {
        Page<Event> eventPage = eventRepository.search(
            EventCriteria.builder().from(1451606400000L).to(1470157767000L).type(EventType.START_API).build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );
        assertThat(eventPage.getTotalElements()).isEqualTo(5L);
        assertThat(eventPage.getContent()).extracting(Event::getId).contains("event12");
    }

    @Test
    public void searchByMultipleEventType() {
        final EventCriteria eventCriteria = EventCriteria.builder()
            .from(1451606400000L)
            .to(1470157767000L)
            .types(Set.of(EventType.START_API, EventType.STOP_API))
            .build();

        Page<Event> eventPage = eventRepository.search(eventCriteria, new PageableBuilder().pageNumber(0).pageSize(2).build());
        assertThat(eventPage).extracting(Page::getTotalElements, Page::getPageElements).contains(7L, 2L);
        assertThat(eventPage.getContent()).extracting(Event::getId).contains("event14");

        eventPage = eventRepository.search(eventCriteria, new PageableBuilder().pageNumber(2).pageSize(2).build());

        assertThat(eventPage).extracting(Page::getTotalElements, Page::getPageElements).contains(7L, 2L);
        assertThat(eventPage.getContent()).extracting(Event::getId).contains("event15");
    }

    @Test
    public void searchOnlyByType() {
        List<Event> events = eventRepository.search(EventCriteria.builder().type(EventType.GATEWAY_STOPPED).build());

        assertThat(events).hasSize(1).extracting(Event::getId).contains("event07");
    }

    @Test
    public void searchByMissingType() {
        Page<Event> eventPage = eventRepository.search(
            EventCriteria.builder().type(EventType.DEBUG_API).build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertThat(eventPage).extracting(Page::getTotalElements, Page::getPageElements).contains(0L, 0L);
        assertThat(eventPage.getContent()).isEmpty();
    }

    @Test
    public void searchByAPIId() {
        Page<Event> eventPage = eventRepository.search(
            EventCriteria.builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), "api-1")
                .build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertThat(eventPage.getTotalElements()).isEqualTo(2L);
        assertThat(eventPage.getContent()).extracting(Event::getId).contains("event02");
    }

    @Test
    public void searchByAPI_EmptyPageable() {
        Page<Event> eventPage = eventRepository.search(
            EventCriteria.builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), "api-1")
                .build(),
            null
        );

        assertThat(eventPage.getTotalElements()).isEqualTo(2L);
        assertThat(eventPage.getContent()).extracting(Event::getId).contains("event02");
    }

    @Test
    public void searchByMixProperties() {
        Page<Event> eventPage = eventRepository.search(
            EventCriteria.builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), "api-3")
                .types(Set.of(EventType.START_API, EventType.STOP_API))
                .build(),
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertThat(eventPage.getTotalElements()).isOne();
        assertThat(eventPage.getContent()).contains(
            Event.builder()
                .id("event04")
                .organizations(Set.of("DEFAULT"))
                .environments(Set.of("DEFAULT"))
                .type(EventType.STOP_API)
                .payload("{}")
                .properties(Map.ofEntries(Map.entry("api_id", "api-3"), Map.entry("deployment_number", "1")))
                .createdAt(new Date(1459468800000L))
                .updatedAt(new Date(1459468800000L))
                .build()
        );
    }

    @Test
    public void searchByCollectionProperty() {
        Page<Event> eventPage = eventRepository.search(
            EventCriteria.builder()
                .from(1451606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3"))
                .build(),
            null
        );

        assertThat(eventPage.getTotalElements()).isEqualTo(3L);
        assertThat(eventPage.getContent()).extracting(Event::getId).contains("event04");
    }

    @Test
    public void searchByCollectionPropertyWithoutPaging() {
        List<Event> events = eventRepository.search(
            EventCriteria.builder()
                .from(1452606400000L)
                .to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3"))
                .build()
        );

        assertThat(events).hasSize(2).extracting(Event::getId).contains("event04");
    }

    @Test
    public void searchByCollectionPropertyWithoutPagingAndBoundary() {
        List<Event> events = eventRepository.search(
            EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3")).build()
        );

        assertThat(events).hasSize(3).extracting(Event::getId).containsExactly("event04", "event02", "event01");
    }

    @Test
    public void searchByEnvironmentDefault() {
        List<Event> events = eventRepository.search(EventCriteria.builder().environments(singletonList("DEFAULT")).build());

        assertThat(events)
            .hasSize(12)
            .extracting(Event::getId)
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

        assertThat(events)
            .hasSize(12)
            .extracting(Event::getId)
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

        assertThat(events)
            .hasSize(14)
            .extracting(Event::getId)
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
        assertThat(events).hasSize(20);
    }

    @Test
    public void searchByOrganizationsDefault() {
        List<Event> events = eventRepository.search(EventCriteria.builder().organizations(List.of("DEFAULT")).build());

        assertThat(events)
            .hasSize(14)
            .extracting(Event::getId)
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

        assertThat(events)
            .hasSize(6)
            .extracting(Event::getId)
            .containsExactly("event13", "event10", "event14", "event12", "event11", "event16");
    }

    @Test
    public void shouldDelete() throws Exception {
        assertThat(eventRepository.findById("event05")).isPresent();

        eventRepository.delete("event05");

        assertThat(eventRepository.findById("event05")).isEmpty();
    }

    @Test
    public void shouldDeleteByApi() throws Exception {
        assertThat(
            eventRepository.search(EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api-2").build())
        ).isNotEmpty();

        long deleteApiEvents = eventRepository.deleteApiEvents("api-2");
        assertThat(deleteApiEvents).isEqualTo(3);

        assertThat(
            eventRepository.search(EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api-2").build())
        ).isEmpty();
    }

    @Test
    public void shouldNotDeleteByApiWhenNoEvent() throws Exception {
        assertThat(
            eventRepository.search(EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api-with-no-event").build())
        ).isEmpty();

        long deleteApiEvents = eventRepository.deleteApiEvents("api-with-no-event");
        assertThat(deleteApiEvents).isZero();

        assertThat(
            eventRepository.search(EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api-with-no-event").build())
        ).isEmpty();
    }

    @Test
    public void shouldFindEventByTypeByApiAndEnvironment() {
        EventCriteria.EventCriteriaBuilder criteria = EventCriteria.builder()
            .types(List.of(EventType.PUBLISH_API, EventType.UNPUBLISH_API))
            .property(Event.EventProperties.API_ID.getValue(), "api-1")
            .from(0)
            .to(0)
            .environment("DEFAULT");
        List<Event> events = eventRepository.search(criteria.build(), new PageableBuilder().pageSize(1).pageNumber(0).build()).getContent();

        assertThat(events).hasSize(1).extracting(Event::getId).contains("event02");
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

        assertThat(createdEvent).isEqualTo(
            Event.builder()
                .id(event.getId())
                .organizations(Set.of("DEFAULT"))
                .environments(Set.of("DEFAULT"))
                .type(EventType.PUBLISH_API)
                .payload("{}")
                .properties(
                    Map.ofEntries(
                        Map.entry("last_heartbeat_at", new Timestamp(createdDate.getTime()).toString()),
                        Map.entry("to_update", "property_to_update"),
                        Map.entry("to_update_with_null", "property_to_update_with_null"),
                        Map.entry("not_updated", "will_not_change")
                    )
                )
                .createdAt(createdDate)
                .updatedAt(createdDate)
                .build()
        );

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

        assertThat(updatedEvent).isEqualTo(
            Event.builder()
                .id(event.getId())
                .organizations(Set.of("DEFAULT"))
                .environments(Set.of("DEFAULT"))
                .type(EventType.UNPUBLISH_API)
                .payload("{}")
                .properties(properties)
                .createdAt(createdDate)
                .updatedAt(updateDate)
                .build()
        );
        assertThat(Timestamp.valueOf(updatedEvent.getProperties().get("last_heartbeat_at"))).isAfter(
            Timestamp.valueOf(createdEvent.getProperties().get("last_heartbeat_at"))
        );
    }

    @Test
    public void updateShouldUpdateEvent() throws TechnicalException {
        LocalDateTime localDate = LocalDateTime.now().minusMinutes(1);
        var createdDate = Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
        Event event = Event.builder()
            .id(UUID.toString(UUID.random()))
            .createdAt(createdDate)
            .updatedAt(createdDate)
            .environments(singleton("DEFAULT"))
            .organizations(singleton("DEFAULT"))
            .type(EventType.DEBUG_API)
            .payload("{}")
            .properties(
                Map.ofEntries(
                    Map.entry("to_update", "property_to_update"),
                    Map.entry("to_update_with_null", "property_to_update_with_null"),
                    Map.entry("not_updated", "will_not_change")
                )
            )
            .build();

        // Should create the event
        Event createdEvent = eventRepository.create(event);

        assertThat(createdEvent).isEqualTo(
            Event.builder()
                .id(event.getId())
                .organizations(Set.of("DEFAULT"))
                .environments(Set.of("DEFAULT"))
                .type(EventType.DEBUG_API)
                .payload("{}")
                .properties(
                    Map.ofEntries(
                        Map.entry("to_update", "property_to_update"),
                        Map.entry("to_update_with_null", "property_to_update_with_null"),
                        Map.entry("not_updated", "will_not_change")
                    )
                )
                .createdAt(createdDate)
                .updatedAt(createdDate)
                .build()
        );

        Date updateDate = new Date();
        var updatedProperties = new HashMap<String, String>();
        updatedProperties.put("to_update", "updated_property");
        updatedProperties.put("to_update_with_null", null);

        // Should update the event with the new type and properties.
        var toUpdate = event.toBuilder().updatedAt(updateDate).properties(updatedProperties).build();
        Event updatedEvent = eventRepository.update(toUpdate);

        assertThat(updatedEvent).isEqualTo(
            Event.builder()
                .id(event.getId())
                .organizations(Set.of("DEFAULT"))
                .environments(Set.of("DEFAULT"))
                .type(EventType.DEBUG_API)
                .payload("{}")
                .properties(updatedProperties)
                .createdAt(createdDate)
                .updatedAt(updateDate)
                .build()
        );
    }

    @Test
    public void should_find_by_environment_id() {
        List<Event> events = eventRepository.findByEnvironmentId("DEFAULT");

        assertThat(events)
            .hasSize(11)
            .extracting(Event::getId)
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

        assertThat(events)
            .hasSize(13)
            .extracting(Event::getId)
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
