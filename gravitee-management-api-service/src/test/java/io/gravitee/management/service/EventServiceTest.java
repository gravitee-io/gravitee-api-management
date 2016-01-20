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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.gravitee.management.model.EventEntity;
import io.gravitee.management.model.NewEventEntity;
import io.gravitee.management.service.exceptions.EventNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.EventServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Titouan COMPIEGNE
 */
@RunWith(MockitoJUnitRunner.class)
public class EventServiceTest {

    private static final String EVENT_PUBLISH_API_TYPE = "PUBLISH_API";
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

    @Test
    public void shouldCreateEventWithPublishApiEventType() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.valueOf(EVENT_PUBLISH_API_TYPE));
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());
        when(eventRepository.create(any())).thenReturn(event);

        when(newEvent.getType()).thenReturn(io.gravitee.management.model.EventType.valueOf(EVENT_PUBLISH_API_TYPE));
        when(newEvent.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(newEvent.getProperties()).thenReturn(EVENT_PROPERTIES);

        final EventEntity eventEntity = eventService.create(newEvent);

        assertNotNull(eventEntity);
        assertEquals(EVENT_PUBLISH_API_TYPE, eventEntity.getType().toString());
        assertEquals(EVENT_PAYLOAD, eventEntity.getPayload());
        assertEquals(EVENT_USERNAME, eventEntity.getProperties().get(Event.EventProperties.USERNAME.getValue()));
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.valueOf(EVENT_PUBLISH_API_TYPE));
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        final EventEntity eventEntity = eventService.findById(EVENT_ID);

        assertNotNull(eventEntity);
    }

    @Test
    public void shouldFindByType() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.valueOf(EVENT_PUBLISH_API_TYPE));
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        List<EventType> eventTypes = new ArrayList<EventType>();
        eventTypes.add(EventType.valueOf(EVENT_PUBLISH_API_TYPE));
        when(eventRepository.findByType(eventTypes)).thenReturn(new HashSet<>(Arrays.asList(event)));

        List<io.gravitee.management.model.EventType> _eventTypes = new ArrayList<io.gravitee.management.model.EventType>();
        _eventTypes.add(io.gravitee.management.model.EventType.valueOf(EVENT_PUBLISH_API_TYPE));
        Set<EventEntity> eventEntities = eventService.findByType(_eventTypes);

        assertNotNull(eventEntities);
        assertEquals(1, eventEntities.size());
    }

    @Test
    public void shouldFindByApi() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.valueOf(EVENT_PUBLISH_API_TYPE));
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventRepository.findByProperty(Event.EventProperties.API_ID.getValue(), API_ID)).thenReturn(new HashSet<>(Arrays.asList(event)));

        Set<EventEntity> eventEntities = eventService.findByApi(API_ID);

        assertNotNull(eventEntities);
        assertEquals(1, eventEntities.size());
        EventEntity eventEntity = eventEntities.stream().findFirst().get();
        assertEquals(API_ID, eventEntity.getProperties().get(Event.EventProperties.API_ID.getValue()));
    }

    @Test
    public void shouldFindByUsername() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.valueOf(EVENT_PUBLISH_API_TYPE));
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventRepository.findByProperty(Event.EventProperties.USERNAME.getValue(), EVENT_USERNAME)).thenReturn(new HashSet<>(Arrays.asList(event)));

        Set<EventEntity> eventEntities = eventService.findByUser(EVENT_USERNAME);

        assertNotNull(eventEntities);
        assertEquals(1, eventEntities.size());
        EventEntity eventEntity = eventEntities.stream().findFirst().get();
        assertEquals(EVENT_USERNAME, eventEntity.getProperties().get(Event.EventProperties.USERNAME.getValue()));
    }

    @Test
    public void shouldFindByOrigin() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.valueOf(EVENT_PUBLISH_API_TYPE));
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventRepository.findByProperty(Event.EventProperties.ORIGIN.getValue(), EVENT_ORIGIN)).thenReturn(new HashSet<>(Arrays.asList(event)));

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

}
