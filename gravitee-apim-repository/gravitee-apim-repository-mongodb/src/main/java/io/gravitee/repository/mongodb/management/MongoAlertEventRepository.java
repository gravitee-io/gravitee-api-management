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
import io.gravitee.repository.management.api.AlertEventRepository;
import io.gravitee.repository.management.api.search.AlertEventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.AlertEvent;
import io.gravitee.repository.mongodb.management.internal.api.AlertEventMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.AlertEventMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoAlertEventRepository implements AlertEventRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoAlertEventRepository.class);

    @Autowired
    private AlertEventMongoRepository internalAlertEventRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<AlertEvent> findById(String eventId) throws TechnicalException {
        LOGGER.debug("Find an alert event by ID [{}]", eventId);

        final AlertEventMongo alertEvent = internalAlertEventRepo.findById(eventId).orElse(null);

        LOGGER.debug("Find an alert event by ID [{}] - Done", eventId);
        return Optional.ofNullable(mapper.map(alertEvent));
    }

    @Override
    public AlertEvent create(AlertEvent event) throws TechnicalException {
        LOGGER.debug("Create alert event [{}]", event.getId());

        AlertEventMongo alertEventMongo = mapper.map(event);
        AlertEventMongo createdAlertEventMongo = internalAlertEventRepo.insert(alertEventMongo);

        AlertEvent res = mapper.map(createdAlertEventMongo);

        LOGGER.debug("Create alert event [{}] - Done", event.getId());

        return res;
    }

    @Override
    public AlertEvent update(AlertEvent event) throws TechnicalException {
        if (event == null || event.getId() == null) {
            throw new IllegalStateException("Alert event to update must have an ID");
        }

        final AlertEventMongo alertEventMongo = internalAlertEventRepo.findById(event.getId()).orElse(null);

        if (alertEventMongo == null) {
            throw new IllegalStateException(String.format("No alert event found with id [%s]", event.getId()));
        }

        try {
            //Update
            alertEventMongo.setMessage(event.getMessage());
            alertEventMongo.setCreatedAt(event.getCreatedAt());
            alertEventMongo.setUpdatedAt(event.getUpdatedAt());

            AlertEventMongo alertEventMongoUpdated = internalAlertEventRepo.save(alertEventMongo);
            return mapper.map(alertEventMongoUpdated);
        } catch (Exception e) {
            LOGGER.error("An error occurs when updating alert event", e);
            throw new TechnicalException("An error occurs when updating alert event");
        }
    }

    @Override
    public void delete(String eventId) throws TechnicalException {
        try {
            internalAlertEventRepo.deleteById(eventId);
        } catch (Exception e) {
            LOGGER.error("An error occurs when deleting alert event [{}]", eventId, e);
            throw new TechnicalException("An error occurs when deleting alert event");
        }
    }

    @Override
    public void deleteAll(String alertId) {
        internalAlertEventRepo.deleteAll(alertId);
    }

    @Override
    public Page<AlertEvent> search(AlertEventCriteria criteria, Pageable pageable) {
        Page<AlertEventMongo> alertEventsMongo = internalAlertEventRepo.search(criteria, pageable);

        return alertEventsMongo.map(mapper::map);
    }

    @Override
    public long count(AlertEventCriteria criteria) {
        return internalAlertEventRepo.count(criteria);
    }

    @Override
    public Set<AlertEvent> findAll() throws TechnicalException {
        return internalAlertEventRepo.findAll().stream().map(alertEventMongo -> mapper.map(alertEventMongo)).collect(Collectors.toSet());
    }
}
