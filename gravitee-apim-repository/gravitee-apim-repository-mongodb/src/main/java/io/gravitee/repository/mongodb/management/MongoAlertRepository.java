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
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.model.AlertEventRule;
import io.gravitee.repository.management.model.AlertEventType;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.repository.mongodb.management.internal.api.AlertMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.AlertEventRuleMongo;
import io.gravitee.repository.mongodb.management.internal.model.AlertTriggerMongo;
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
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoAlertRepository implements AlertTriggerRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoAlertRepository.class);

    @Autowired
    private AlertMongoRepository internalAlertRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<AlertTrigger> findById(String triggerId) throws TechnicalException {
        LOGGER.debug("Find an alert trigger by ID [{}]", triggerId);

        Optional<AlertTriggerMongo> alert = internalAlertRepo.findById(triggerId);

        LOGGER.debug("Find an alert trigger by ID [{}] - Done", triggerId);
        return alert.map(a -> mapper.map(a, AlertTrigger.class));
    }

    @Override
    public Optional<AlertTrigger> findByIdAndEnvironment(String id, String environmentId) throws TechnicalException {
        LOGGER.debug("Find an alert trigger by ID [{}] and environment [{}]", id, environmentId);

        Optional<AlertTriggerMongo> alert = internalAlertRepo.findByIdAndEnvironment(id, environmentId);

        LOGGER.debug("Find an alert trigger by ID [{}] and environment [{}] - Done", id, environmentId);
        return alert.map(a -> mapper.map(a, AlertTrigger.class));
    }

    @Override
    public AlertTrigger create(AlertTrigger trigger) throws TechnicalException {
        LOGGER.debug("Create alert trigger [{}]", trigger.getName());

        AlertTriggerMongo alertTriggerMongo = mapper.map(trigger, AlertTriggerMongo.class);
        AlertTriggerMongo createdAlertTriggerMongo = internalAlertRepo.insert(alertTriggerMongo);

        AlertTrigger res = mapper.map(createdAlertTriggerMongo, AlertTrigger.class);

        LOGGER.debug("Create alert trigger [{}] - Done", trigger.getName());

        return res;
    }

    @Override
    public AlertTrigger update(AlertTrigger trigger) throws TechnicalException {
        if (trigger == null || trigger.getName() == null) {
            throw new IllegalStateException("Alert trigger to update must have a name");
        }

        final AlertTriggerMongo alertTriggerMongo = internalAlertRepo.findById(trigger.getId()).orElse(null);

        if (alertTriggerMongo == null) {
            throw new IllegalStateException(String.format("No alert trigger found with id [%s]", trigger.getId()));
        }

        try {
            //Update
            alertTriggerMongo.setName(trigger.getName());
            alertTriggerMongo.setType(trigger.getType());
            alertTriggerMongo.setDescription(trigger.getDescription());
            alertTriggerMongo.setReferenceType(trigger.getReferenceType());
            alertTriggerMongo.setReferenceId(trigger.getReferenceId());
            alertTriggerMongo.setEnabled(trigger.isEnabled());
            alertTriggerMongo.setSeverity(trigger.getSeverity());
            alertTriggerMongo.setDefinition(trigger.getDefinition());
            alertTriggerMongo.setCreatedAt(trigger.getCreatedAt());
            alertTriggerMongo.setUpdatedAt(trigger.getUpdatedAt());
            alertTriggerMongo.setEnvironmentId(trigger.getEnvironmentId());

            if (trigger.getEventRules() != null && !trigger.getEventRules().isEmpty()) {
                alertTriggerMongo.setEventRules(
                    trigger
                        .getEventRules()
                        .stream()
                        .map(alertEventRule -> new AlertEventRuleMongo(alertEventRule.getEvent().name()))
                        .collect(Collectors.toList())
                );
            } else {
                alertTriggerMongo.setEventRules(null);
            }

            AlertTriggerMongo alertTriggerMongoUpdated = internalAlertRepo.save(alertTriggerMongo);
            return mapper.map(alertTriggerMongoUpdated, AlertTrigger.class);
        } catch (Exception e) {
            LOGGER.error("An error occurs when updating alert trigger", e);
            throw new TechnicalException("An error occurs when updating alert trigger");
        }
    }

    @Override
    public void delete(String triggerId) throws TechnicalException {
        try {
            internalAlertRepo.deleteById(triggerId);
        } catch (Exception e) {
            LOGGER.error("An error occurs when deleting alert trigger [{}]", triggerId, e);
            throw new TechnicalException("An error occurs when deleting alert trigger");
        }
    }

    @Override
    public Set<AlertTrigger> findAll() {
        final List<AlertTriggerMongo> alerts = internalAlertRepo.findAll();
        return alerts.stream().map(this::map).collect(Collectors.toSet());
    }

    @Override
    public List<AlertTrigger> findByReferenceAndReferenceIds(String referenceType, List<String> referenceIds, String environmentId) {
        LOGGER.debug("Find alert trigger by reference '{}' and referencesIds '{}' and environment_id '{}'", referenceType, referenceIds, environmentId);

        final List<AlertTriggerMongo> triggers = internalAlertRepo.findByReferenceTypeAndReferenceIds(referenceType, referenceIds, environmentId);

        LOGGER.debug("Find alert trigger by reference '{}' and referencesIds '{}' and environment_id '{}' done", referenceType, referenceIds, environmentId);
        return triggers.stream().map(this::map).collect(Collectors.toList());
    }

    private AlertTrigger map(final AlertTriggerMongo alertTriggerMongo) {
        if (alertTriggerMongo == null) {
            return null;
        }
        final AlertTrigger trigger = new AlertTrigger();
        trigger.setId(alertTriggerMongo.getId());
        trigger.setReferenceType(alertTriggerMongo.getReferenceType());
        trigger.setReferenceId(alertTriggerMongo.getReferenceId());
        trigger.setName(alertTriggerMongo.getName());
        trigger.setSeverity(alertTriggerMongo.getSeverity());
        trigger.setType(alertTriggerMongo.getType());
        trigger.setDescription(alertTriggerMongo.getDescription());
        trigger.setDefinition(alertTriggerMongo.getDefinition());
        trigger.setEnabled(alertTriggerMongo.isEnabled());
        trigger.setParentId(alertTriggerMongo.getParentId());
        trigger.setTemplate(alertTriggerMongo.isTemplate());
        trigger.setEnvironmentId(alertTriggerMongo.getEnvironmentId());

        if (alertTriggerMongo.getEventRules() != null && !alertTriggerMongo.getEventRules().isEmpty()) {
            trigger.setEventRules(
                alertTriggerMongo
                    .getEventRules()
                    .stream()
                    .map(alertEventRuleMongo -> new AlertEventRule(AlertEventType.valueOf(alertEventRuleMongo.getEvent().toUpperCase())))
                    .collect(Collectors.toList())
            );
        }

        trigger.setCreatedAt(alertTriggerMongo.getCreatedAt());
        trigger.setUpdatedAt(alertTriggerMongo.getUpdatedAt());
        return trigger;
    }
}
