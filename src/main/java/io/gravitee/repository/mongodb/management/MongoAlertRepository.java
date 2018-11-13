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
import io.gravitee.repository.management.api.AlertRepository;
import io.gravitee.repository.management.model.Alert;
import io.gravitee.repository.mongodb.management.internal.api.AlertMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.AlertMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoAlertRepository implements AlertRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoAlertRepository.class);

    @Autowired
    private AlertMongoRepository internalAlertRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Alert> findById(String alertId) throws TechnicalException {
        LOGGER.debug("Find alert by ID [{}]", alertId);

        final AlertMongo alert = internalAlertRepo.findById(alertId).orElse(null);

        LOGGER.debug("Find alert by ID [{}] - Done", alertId);
        return Optional.ofNullable(mapper.map(alert, Alert.class));
    }

    @Override
    public Alert create(Alert alert) throws TechnicalException {
        LOGGER.debug("Create alert [{}]", alert.getName());

        AlertMongo alertMongo = mapper.map(alert, AlertMongo.class);
        AlertMongo createdAlertMongo = internalAlertRepo.insert(alertMongo);

        Alert res = mapper.map(createdAlertMongo, Alert.class);

        LOGGER.debug("Create alert [{}] - Done", alert.getName());

        return res;
    }

    @Override
    public Alert update(Alert alert) throws TechnicalException {
        if (alert == null || alert.getName() == null) {
            throw new IllegalStateException("Alert to update must have a name");
        }

        final AlertMongo alertMongo = internalAlertRepo.findById(alert.getId()).orElse(null);

        if (alertMongo == null) {
            throw new IllegalStateException(String.format("No alert found with name [%s]", alert.getId()));
        }

        try {
            //Update
            alertMongo.setName(alert.getName());
            alertMongo.setDescription(alert.getDescription());

            AlertMongo alertMongoUpdated = internalAlertRepo.save(alertMongo);
            return mapper.map(alertMongoUpdated, Alert.class);

        } catch (Exception e) {

            LOGGER.error("An error occured when updating alert", e);
            throw new TechnicalException("An error occured when updating alert");
        }
    }

    @Override
    public void delete(String alertId) throws TechnicalException {
        try {
            internalAlertRepo.deleteById(alertId);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting alert [{}]", alertId, e);
            throw new TechnicalException("An error occured when deleting alert");
        }
    }

    @Override
    public Set<Alert> findAll() {
        final List<AlertMongo> alerts = internalAlertRepo.findAll();
        return alerts.stream()
                .map(alertMongo -> {
                    final Alert alert = new Alert();
                    alert.setId(alertMongo.getId());
                    alert.setName(alertMongo.getName());
                    alert.setDescription(alertMongo.getDescription());
                    return alert;
                })
                .collect(Collectors.toSet());
    }

    @Override
    public List<Alert> findByReference(String referenceType, String referenceId) {
        LOGGER.debug("Find alert by reference '{}' / '{}'", referenceType, referenceId);

        final List<AlertMongo> alerts = internalAlertRepo.findByReferenceTypeAndReferenceId(referenceType, referenceId);

        LOGGER.debug("Find alert by reference '{}' / '{}' done", referenceType, referenceId);
        return alerts.stream().map(this::map).collect(Collectors.toList());
    }

    private Alert map(final AlertMongo alertMongo) {
        if (alertMongo == null) {
            return null;
        }
        final Alert alert = new Alert();
        alert.setId(alertMongo.getId());
        alert.setReferenceType(alertMongo.getReferenceType());
        alert.setReferenceId(alertMongo.getReferenceId());
        alert.setName(alertMongo.getName());
        alert.setDescription(alertMongo.getDescription());
        alert.setMetricType(alertMongo.getMetricType());
        alert.setMetric(alertMongo.getMetric());
        alert.setType(alertMongo.getType());
        alert.setPlan(alertMongo.getPlan());
        alert.setThresholdType(alertMongo.getThresholdType());
        alert.setThreshold(alertMongo.getThreshold());
        alert.setEnabled(alertMongo.isEnabled());
        alert.setCreatedAt(alertMongo.getCreatedAt());
        alert.setUpdatedAt(alertMongo.getUpdatedAt());
        return alert;
    }
}
