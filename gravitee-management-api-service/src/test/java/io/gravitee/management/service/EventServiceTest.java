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
package io.gravitee.management.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.management.model.EventEntity;
import io.gravitee.management.model.NewEventEntity;
import io.gravitee.management.service.exceptions.EventNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.EventServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EventServiceTest {

    private static final String EVENT_ID = "id-event";
    private static final String EVENT_PAYLOAD = "{}";
    private static final String EVENT_USERNAME = "admin";
    private static final String EVENT_ORIGIN = "localhost";
    private static final String API_ID = "id-api";
    private static final Map<String, String> EVENT_PROPERTIES = new HashMap<String, String>() {
        {
            put(Event.EventProperties.API_ID.getValue(), API_ID);
            put(Event.EventProperties.USERNAME.getValue(), EVENT_USERNAME);
            put(Event.EventProperties.ORIGIN.getValue(), EVENT_ORIGIN);
        }
    };

    @InjectMocks
    private EventServiceImpl eventService = new EventServiceImpl();

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NewEventEntity newEvent;

    @Mock
    private Event event;

    @Mock
    private Event event2;

    @Mock
    private Page<Event> eventPage;

    @Test
    public void shouldCreateEventWithPublishApiEventType() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.PUBLISH_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());
        when(eventRepository.create(any())).thenReturn(event);

        when(newEvent.getType()).thenReturn(io.gravitee.management.model.EventType.PUBLISH_API);
        when(newEvent.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(newEvent.getProperties()).thenReturn(EVENT_PROPERTIES);

        final EventEntity eventEntity = eventService.create(newEvent);

        assertNotNull(eventEntity);
        assertEquals(EventType.PUBLISH_API.toString(), eventEntity.getType().toString());
        assertEquals(EVENT_PAYLOAD, eventEntity.getPayload());
        assertEquals(EVENT_USERNAME, eventEntity.getProperties().get(Event.EventProperties.USERNAME.getValue()));
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.PUBLISH_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        final EventEntity eventEntity = eventService.findById(EVENT_ID);

        assertNotNull(eventEntity);
    }

    @Test
    public void shouldFindByType() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.PUBLISH_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventRepository.search(
                new EventCriteria.Builder().types(EventType.PUBLISH_API).build()
        )).thenReturn(Collections.singletonList(event));

        List<io.gravitee.management.model.EventType> _eventTypes = new ArrayList<io.gravitee.management.model.EventType>();
        _eventTypes.add(io.gravitee.management.model.EventType.PUBLISH_API);
        Set<EventEntity> eventEntities = eventService.findByType(_eventTypes);

        assertNotNull(eventEntities);
        assertEquals(1, eventEntities.size());
    }

    @Test
    public void shouldFindByApi() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.PUBLISH_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventRepository.search(
                new EventCriteria.Builder().property(Event.EventProperties.API_ID.getValue(), API_ID).build()
        )).thenReturn(Collections.singletonList(event));

        Set<EventEntity> eventEntities = eventService.findByApi(API_ID);

        assertNotNull(eventEntities);
        assertEquals(1, eventEntities.size());
        EventEntity eventEntity = eventEntities.stream().findFirst().get();
        assertEquals(API_ID, eventEntity.getProperties().get(Event.EventProperties.API_ID.getValue()));
    }

    @Test
    public void shouldFindByUsername() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.PUBLISH_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventRepository.search(
                new EventCriteria.Builder().property(Event.EventProperties.USERNAME.getValue(), EVENT_USERNAME).build()
        )).thenReturn(Collections.singletonList(event));

        Set<EventEntity> eventEntities = eventService.findByUser(EVENT_USERNAME);

        assertNotNull(eventEntities);
        assertEquals(1, eventEntities.size());
        EventEntity eventEntity = eventEntities.stream().findFirst().get();
        assertEquals(EVENT_USERNAME, eventEntity.getProperties().get(Event.EventProperties.USERNAME.getValue()));
    }

    @Test
    public void shouldFindByOrigin() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.PUBLISH_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventPage.getTotalElements()).thenReturn(1L);
        when(eventPage.getContent()).thenReturn(Collections.singletonList(event));

        when(eventRepository.search(
                new EventCriteria.Builder().property(Event.EventProperties.ORIGIN.getValue(), EVENT_ORIGIN).build()
        )).thenReturn(Collections.singletonList(event));

        Set<EventEntity> eventEntities = eventService.findByOrigin(EVENT_ORIGIN);

        assertNotNull(eventEntities);
        assertEquals(1, eventEntities.size());
        EventEntity eventEntity = eventEntities.stream().findFirst().get();
        assertEquals(EVENT_ORIGIN, eventEntity.getProperties().get(Event.EventProperties.ORIGIN.getValue()));
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
        when(eventRepository.findById(any(String.class))).thenThrow(TechnicalException.class);

        eventService.findById(EVENT_ID);
    }

    @Test(expected = EventNotFoundException.class)
    public void shouldNotFindByNameBecauseNotExists() throws TechnicalException {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        eventService.findById(EVENT_ID);
    }
    
    @Test
    public void shouldDelete() throws TechnicalException {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        eventService.delete(EVENT_ID);

        verify(eventRepository).delete(EVENT_ID);
    }

    @Test
    public void shouldSearchNoResults() {
        when(eventPage.getTotalElements()).thenReturn(0L);
        when(eventPage.getContent()).thenReturn(Collections.emptyList());

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .from(1420070400000L).to(1422748800000L)
                        .types(EventType.START_API)
                        .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
        )).thenReturn(eventPage);

        Page<EventEntity> eventPageEntity = eventService.search(
                Collections.singletonList(io.gravitee.management.model.EventType.START_API),
                null, 1420070400000L, 1422748800000L, 0, 10);
        assertTrue(0L == eventPageEntity.getTotalElements());
    }

    @Test
    public void shouldSearchBySingleEventType() throws Exception {
        Map<String, Object> values = new HashMap<>();
        values.put("type", EventType.START_API.toString());

        when(event.getId()).thenReturn("event1");
        when(event.getType()).thenReturn(EventType.START_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.START_API);
        when(event2.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event2.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventPage.getTotalElements()).thenReturn(2L);
        when(eventPage.getContent()).thenReturn(Arrays.asList(event, event2));

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .from(1420070400000L).to(1422748800000L)
                        .types(EventType.START_API)
                        .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
        )).thenReturn(eventPage);

        Page<EventEntity> eventPageEntity = eventService.search(
                Collections.singletonList(io.gravitee.management.model.EventType.START_API),
                null, 1420070400000L, 1422748800000L, 0, 10);

        assertTrue(2L == eventPageEntity.getTotalElements());
        assertTrue("event1".equals(eventPageEntity.getContent().get(0).getId()));
    }

    @Test
    public void shouldSearchByMultipleEventType() throws Exception {
        when(event.getId()).thenReturn("event1");
        when(event.getType()).thenReturn(EventType.START_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.STOP_API);
        when(event2.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event2.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventPage.getTotalElements()).thenReturn(2l);
        when(eventPage.getContent()).thenReturn(Arrays.asList(event, event2));

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .from(1420070400000L).to(1422748800000L)
                        .types(EventType.START_API, EventType.STOP_API)
                        .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
        )).thenReturn(eventPage);

        Page<EventEntity> eventPageEntity = eventService.search(
                Arrays.asList(
                        io.gravitee.management.model.EventType.START_API,
                        io.gravitee.management.model.EventType.STOP_API),
                null, 1420070400000L, 1422748800000L, 0, 10);

        assertTrue(2L == eventPageEntity.getTotalElements());
        assertTrue("event1".equals(eventPageEntity.getContent().get(0).getId()));
    }

    @Test
    public void shouldSearchByAPIId() throws Exception {
        Map<String, Object> values = new HashMap<>();
        values.put(Event.EventProperties.API_ID.getValue(), "id-api");

        when(event.getId()).thenReturn("event1");
        when(event.getType()).thenReturn(EventType.START_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.STOP_API);
        when(event2.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event2.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventPage.getTotalElements()).thenReturn(2L);
        when(eventPage.getContent()).thenReturn(Arrays.asList(event, event2));

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .from(1420070400000L).to(1422748800000L)
                        .property(Event.EventProperties.API_ID.getValue(), "id-api")
                        .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
        )).thenReturn(eventPage);

        Page<EventEntity> eventPageEntity = eventService.search(
                null, values, 1420070400000L, 1422748800000L, 0, 10);

        assertTrue(2L == eventPageEntity.getTotalElements());
        assertTrue("event1".equals(eventPageEntity.getContent().get(0).getId()));
    }

    @Test
    public void shouldSearchByMixProperties() throws Exception {
        Map<String, Object> values = new HashMap<>();
        values.put(Event.EventProperties.API_ID.getValue(), "id-api");

        when(event.getId()).thenReturn("event1");
        when(event.getType()).thenReturn(EventType.START_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.STOP_API);
        when(event2.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event2.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventPage.getTotalElements()).thenReturn(2L);
        when(eventPage.getContent()).thenReturn(Arrays.asList(event, event2));

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .from(1420070400000L).to(1422748800000L)
                        .property(Event.EventProperties.API_ID.getValue(), "id-api")
                        .types(EventType.START_API, EventType.STOP_API)
                        .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
        )).thenReturn(eventPage);

        Page<EventEntity> eventPageEntity = eventService.search(
                Arrays.asList(
                        io.gravitee.management.model.EventType.START_API,
                        io.gravitee.management.model.EventType.STOP_API),
                values, 1420070400000L, 1422748800000L, 0, 10);

        assertTrue(2L == eventPageEntity.getTotalElements());
        assertTrue("event1".equals(eventPageEntity.getContent().get(0).getId()));
    }

}
