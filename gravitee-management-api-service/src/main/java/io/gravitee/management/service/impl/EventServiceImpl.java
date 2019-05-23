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
package io.gravitee.management.service.impl;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.management.model.*;
import io.gravitee.management.service.EventService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.common.GraviteeContext;
import io.gravitee.management.service.exceptions.EventNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Event.EventProperties.API_ID;
import static io.gravitee.repository.management.model.Event.EventProperties.ID;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * @author Titouan COMPIEGNE
 */
@Component
public class EventServiceImpl extends TransactionalService implements EventService {

    private final Logger LOGGER = LoggerFactory.getLogger(EventServiceImpl.class);

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserService userService;

    @Override
    public EventEntity findById(String id) {
        try {
            LOGGER.debug("Find event by ID: {}", id);

            Optional<Event> event = eventRepository.findById(id);

            if (event.isPresent()) {
                return convert(event.get());
            }

            throw new EventNotFoundException(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an event using its ID {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an event using its ID " + id, ex);
        }
    }

    @Override
    public EventEntity create(NewEventEntity newEventEntity) {
        String hostAddress = "";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
            LOGGER.debug("Create {} for server {}", newEventEntity, hostAddress);

            Event event = convert(newEventEntity);
            event.setId(UUID.toString(UUID.random()));
            event.setEnvironment(GraviteeContext.getCurrentEnvironment());
            
            // Set origin
            event.getProperties().put(Event.EventProperties.ORIGIN.getValue(), hostAddress);
            // Set date fields
            event.setCreatedAt(new Date());
            event.setUpdatedAt(event.getCreatedAt());

            Event createdEvent = eventRepository.create(event);

            return convert(createdEvent);
        } catch (UnknownHostException e) {
            LOGGER.error("An error occurs while getting the server IP address", e);
            throw new TechnicalManagementException("An error occurs while getting the server IP address", e);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for server {}", newEventEntity, hostAddress, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + newEventEntity + " for server " + hostAddress, ex);
        }
    }

    @Override
    public EventEntity create(EventType type, String payload, Map<String, String> properties) {
        NewEventEntity event = new NewEventEntity();
        event.setType(type);
        event.setPayload(payload);
        event.setProperties(properties);
        return create(event);
    }
    
    @Override
    public void delete(String eventId) {
        try {
            LOGGER.debug("Delete Event {}", eventId);
            eventRepository.delete(eventId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete Event {}", eventId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete Event " + eventId, ex);
        }
    }

    private Set<EventEntity> findByProperty(String property, String value) {
        return convert(eventRepository.search(new EventCriteria.Builder().environment(GraviteeContext.getCurrentEnvironment()).property(property, value).build()));
    }

    @Override
    public Page<EventEntity> search(List<EventType> eventTypes,
                                    Map<String, Object> properties, long from, long to, int page, int size) {
        EventCriteria.Builder builder = new EventCriteria.Builder().from(from).to(to);

        if (eventTypes != null) {
            io.gravitee.repository.management.model.EventType[] eventTypesArr = eventTypes.stream()
                    .map(eventType -> io.gravitee.repository.management.model.EventType.valueOf(eventType.toString()))
                    .toArray(io.gravitee.repository.management.model.EventType[]::new);

            builder.types(eventTypesArr);
        }

        if (properties != null) {
            properties.forEach(builder::property);
        }

        builder.environment(GraviteeContext.getCurrentEnvironment());
        
        Page<Event> pageEvent = eventRepository.search(
                builder.build(),
                new PageableBuilder().pageNumber(page).pageSize(size).build());

        List<EventEntity> content = pageEvent.getContent().stream().map(this::convert).collect(Collectors.toList());

        return new Page<>(content, page, size, pageEvent.getTotalElements());
    }

    @Override
    public Collection<EventEntity> search(final EventQuery query) {
        LOGGER.debug("Search APIs by {}", query);
        return convert(eventRepository.search(queryToCriteria(query).build()));
    }

    private EventCriteria.Builder queryToCriteria(EventQuery query) {
        final EventCriteria.Builder builder = new EventCriteria.Builder().environment(GraviteeContext.getCurrentEnvironment());
        if (query == null) {
            return builder;
        }
        builder
                .from(query.getFrom())
                .to(query.getTo());

        if (!isEmpty(query.getTypes())) {
            query.getTypes().forEach(eventType ->
                    builder.types(io.gravitee.repository.management.model.EventType.valueOf(eventType.name())));
        }

        if (!isEmpty(query.getProperties())) {
            query.getProperties().forEach(builder::property);
        }

        if (!isBlank(query.getApi())) {
            builder.property(API_ID.getValue(), query.getApi());
        }

        if (!isBlank(query.getId())) {
            builder.property(ID.getValue(), query.getId());
        }
        
        return builder;
    }

    private Set<EventEntity> convert(List<Event> events) {
        return events.stream().map(this::convert).collect(Collectors.toSet());
    }

    private EventEntity convert(Event event) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setId(event.getId());
        eventEntity.setType(io.gravitee.management.model.EventType.valueOf(event.getType().toString()));
        eventEntity.setPayload(event.getPayload());
        eventEntity.setParentId(event.getParentId());
        eventEntity.setProperties(event.getProperties());
        eventEntity.setCreatedAt(event.getCreatedAt());
        eventEntity.setUpdatedAt(event.getUpdatedAt());

        if (event.getProperties() != null) {
            final String userId = event.getProperties().get(Event.EventProperties.USER.getValue());
            if (userId != null && !userId.isEmpty()) {
                try {
                    eventEntity.setUser(userService.findById(userId));
                } catch (UserNotFoundException unfe) {
                    UserEntity user = new UserEntity();
                    user.setSource("system");
                    user.setId(userId);
                    eventEntity.setUser(user);
                }
            }
        }

        return eventEntity;
    }

    private Event convert(NewEventEntity newEventEntity) {
        Event event = new Event();
        event.setType(io.gravitee.repository.management.model.EventType.valueOf(newEventEntity.getType().toString()));
        event.setPayload(newEventEntity.getPayload());
        event.setParentId(newEventEntity.getParentId());
        event.setProperties(newEventEntity.getProperties());

        return event;
    }
}
