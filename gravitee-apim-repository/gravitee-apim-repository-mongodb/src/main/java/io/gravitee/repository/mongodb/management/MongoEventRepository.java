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
package io.gravitee.repository.mongodb.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.mongodb.management.internal.event.EventMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.EventMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoEventRepository implements EventRepository {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private EventMongoRepository internalEventRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Event> findById(String id) throws TechnicalException {
        logger.debug("Find event by ID [{}]", id);

        EventMongo event = internalEventRepo.findById(id).orElse(null);
        Event res = mapper.map(event);

        logger.debug("Find event by ID [{}] - Done", id);
        return Optional.ofNullable(res);
    }

    @Override
    public Event create(Event event) throws TechnicalException {
        logger.debug("Create event [{}]", event.getId());

        EventMongo eventMongo = mapper.map(event);
        EventMongo createdEventMongo = internalEventRepo.insert(eventMongo);

        Event res = mapper.map(createdEventMongo);

        logger.debug("Create event [{}] - Done", event.getId());

        return res;
    }

    @Override
    public Event update(Event event) throws TechnicalException {
        if (event == null || event.getId() == null) {
            throw new IllegalStateException("Event to update must have an id");
        }

        final EventMongo eventMongo = internalEventRepo.findById(event.getId()).orElse(null);
        if (eventMongo == null) {
            throw new IllegalStateException(String.format("No event found with id [%s]", event.getId()));
        }

        try {
            eventMongo.setProperties(event.getProperties());
            eventMongo.setType(event.getType().toString());
            eventMongo.setPayload(event.getPayload());
            eventMongo.setParentId(event.getParentId());
            eventMongo.setCreatedAt(event.getUpdatedAt());
            eventMongo.setUpdatedAt(event.getUpdatedAt());
            eventMongo.setEnvironments(event.getEnvironments());
            eventMongo.setOrganizations(event.getOrganizations());
            EventMongo eventMongoUpdated = internalEventRepo.save(eventMongo);
            return mapper.map(eventMongoUpdated);
        } catch (Exception e) {
            logger.error("An error occurred when updating event", e);
            throw new TechnicalException("An error occurred when updating event");
        }
    }

    @Override
    public Event createOrPatch(Event event) throws TechnicalException {
        if (event == null || event.getId() == null || event.getType() == null) {
            throw new IllegalStateException("Event to create or update must have an id and a type");
        }

        if (internalEventRepo.existsById(event.getId())) {
            return internalEventRepo.patch(event);
        }
        return create(event);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalEventRepo.deleteById(id);
        } catch (Exception e) {
            logger.error("An error occurred when deleting event [{}]", id, e);
            throw new TechnicalException("An error occurred when deleting event");
        }
    }

    @Override
    public long deleteApiEvents(String apiId) throws TechnicalException {
        try {
            return internalEventRepo.deleteAllByApi(apiId);
        } catch (Exception e) {
            String error = String.format("An error occurred when deleting all events of API %s", apiId);
            logger.error(error, apiId, e);
            throw new TechnicalException(error);
        }
    }

    @Override
    public Page<Event> search(EventCriteria filter, Pageable pageable) {
        return internalEventRepo.search(filter, pageable).map(mapper::map);
    }

    @Override
    public List<Event> search(EventCriteria filter) {
        Page<EventMongo> eventsMongo = internalEventRepo.search(filter, null);

        return mapper.mapEvents(eventsMongo.getContent());
    }

    @Override
    public Set<Event> findAll() throws TechnicalException {
        throw new IllegalStateException("not implemented cause of high amount of data. Use pageable search instead");
    }
}
