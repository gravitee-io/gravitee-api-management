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

import io.gravitee.management.model.EventEntity;
import io.gravitee.management.model.EventType;
import io.gravitee.management.model.NewEventEntity;
import io.gravitee.management.service.EventService;
import io.gravitee.management.service.exceptions.EventNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Titouan COMPIEGNE
 */
@Component
public class EventServiceImpl extends TransactionalService implements EventService {

	private final Logger LOGGER = LoggerFactory.getLogger(EventServiceImpl.class);

	@Autowired
	private EventRepository eventRepository;

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
			// Set origin
			event.setOrigin(hostAddress);
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
	public EventEntity create(EventType type, String payload, String username) {
		NewEventEntity event = new NewEventEntity();
		event.setType(type);
		event.setPayload(payload);
		event.setUsername(username);
		return create(event);
	}

	@Override
	public Set<EventEntity> findByType(List<EventType> eventTypes) {
		Set<Event> events = eventRepository.findByType(convert(eventTypes));

		return convert(events);
	}
	
	private List<io.gravitee.repository.management.model.EventType> convert(List<EventType> eventTypes) {
		List<io.gravitee.repository.management.model.EventType> convertedEvents = new ArrayList<io.gravitee.repository.management.model.EventType>();
		for (EventType eventType : eventTypes) {
			convertedEvents.add(convert(eventType));
		}
		return convertedEvents;
	}

	private io.gravitee.repository.management.model.EventType convert(EventType eventType) {
		return io.gravitee.repository.management.model.EventType.valueOf(eventType.toString());
	}
	
	private Set<EventEntity> convert(Set<Event> events) {
		return events.stream().map(this::convert).collect(Collectors.toSet());
	}
	
	private EventEntity convert(Event event) {
		EventEntity eventEntity = new EventEntity();
		eventEntity.setId(event.getId());
		eventEntity.setType(io.gravitee.management.model.EventType.valueOf(event.getType().toString()));
		eventEntity.setPayload(event.getPayload());
		eventEntity.setParentId(event.getParentId());
		eventEntity.setOrigin(event.getOrigin());
		eventEntity.setUsername(event.getUsername());
		eventEntity.setCreatedAt(event.getCreatedAt());
		eventEntity.setUpdatedAt(event.getUpdatedAt());

		return eventEntity;
	}

	private Event convert(NewEventEntity newEventEntity) {
		Event event = new Event();
		event.setType(io.gravitee.repository.management.model.EventType.valueOf(newEventEntity.getType().toString()));
		event.setPayload(newEventEntity.getPayload());
		event.setParentId(newEventEntity.getParentId());
		event.setUsername(newEventEntity.getUsername());

		return event;
	}
}
