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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.mongodb.management.internal.event.EventMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.EventMongo;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Titouan COMPIEGNE
 */
@Component
public class MongoEventRepository implements EventRepository {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private EventMongoRepository internalEventRepo;

	@Override
	public Optional<Event> findById(String id) throws TechnicalException {
		logger.debug("Find event by ID [{}]", id);

		EventMongo event = internalEventRepo.findOne(id);
		Event res = mapEvent(event);

		logger.debug("Find event by ID [{}] - Done", id);
		return Optional.ofNullable(res);
	}

	@Override
	public Event create(Event event) throws TechnicalException {
		logger.debug("Create event [{}]", event.getId());

		EventMongo eventMongo = mapEvent(event);
		EventMongo createdEventMongo = internalEventRepo.insert(eventMongo);

		Event res = mapEvent(createdEventMongo);

		logger.debug("Create event [{}] - Done", event.getId());

		return res;
	}

	@Override
	public Event update(Event event) throws TechnicalException {
		if (event == null || event.getId() == null) {
			throw new IllegalStateException("Event to update must have an id");
		}

		EventMongo eventMongo = internalEventRepo.findOne(event.getId());
		if (eventMongo == null) {
			throw new IllegalStateException(String.format("No event found with id [%s]", event.getId()));
		}

		try {
			eventMongo.setType(event.getType().toString());
			eventMongo.setPayload(event.getPayload());
			eventMongo.setParentId(event.getParentId());
			eventMongo.setOrigin(event.getOrigin());

			EventMongo eventMongoUpdated = internalEventRepo.save(eventMongo);
			return mapEvent(eventMongoUpdated);
		} catch (Exception e) {
			logger.error("An error occured when updating event", e);
			throw new TechnicalException("An error occured when updating event");
		}
	}

	@Override
	public void delete(String id) throws TechnicalException {
		try {
			internalEventRepo.delete(id);
		} catch (Exception e) {
			logger.error("An error occured when deleting event [{}]", id, e);
			throw new TechnicalException("An error occured when deleting event");
		}
	}

	private EventMongo mapEvent(Event event) {
		EventMongo eventMongo = new EventMongo();
		eventMongo.setId(event.getId());
		eventMongo.setType(event.getType().toString());
		eventMongo.setPayload(event.getPayload());
		eventMongo.setParentId(event.getParentId());
		eventMongo.setOrigin(event.getOrigin());
		eventMongo.setCreatedAt(event.getCreatedAt());
		eventMongo.setUpdatedAt(event.getUpdatedAt());

		return eventMongo;
	}

	private Event mapEvent(EventMongo eventMongo) {
		Event event = new Event();
		event.setId(eventMongo.getId());
		event.setType(EventType.valueOf(eventMongo.getType()));
		event.setPayload(eventMongo.getPayload());
		event.setParentId(eventMongo.getParentId());
		event.setOrigin(eventMongo.getOrigin());
		event.setCreatedAt(eventMongo.getCreatedAt());
		event.setUpdatedAt(eventMongo.getUpdatedAt());

		return event;
	}
}
