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

import io.gravitee.common.utils.UUID;
import io.gravitee.management.model.alert.*;
import io.gravitee.management.service.AlertService;
import io.gravitee.management.service.alert.AlertTriggerService;
import io.gravitee.management.service.exceptions.AlertInvalidException;
import io.gravitee.management.service.exceptions.AlertNotFoundException;
import io.gravitee.management.service.exceptions.AlertUnavailableException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertRepository;
import io.gravitee.repository.management.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.gravitee.management.model.alert.AlertReferenceType.API;
import static io.gravitee.management.model.alert.AlertType.HEALTH_CHECK;
import static io.gravitee.management.model.alert.AlertType.REQUEST;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AlertServiceImpl extends TransactionalService implements AlertService {

    private final Logger LOGGER = LoggerFactory.getLogger(AlertServiceImpl.class);

    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private AlertTriggerService alertTriggerService;

    @Value("${alerts.enabled:false}")
    private boolean enabled;

    @Override
    public AlertEntity create(final NewAlertEntity alert) {
        checkAlert(alert.getType(), alert.getReferenceType());
        try {
            if (HEALTH_CHECK.equals(alert.getType())) {
                // disallow to create HC conf twice
                final List<Alert> alerts = alertRepository.findByReference(alert.getReferenceType().name(), alert.getReferenceId());
                if (alerts.stream().anyMatch(a -> HEALTH_CHECK.name().equals(a.getType()))) {
                    throw new IllegalStateException("A HC configuration already exists");
                }
            }

            final Alert createdAlert = alertRepository.create(convert(alert));
            final AlertEntity alertEntity = convert(createdAlert);
            triggerOrCancelAlert(alertEntity);
            return alertEntity;
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to create alert of type " + alert.getType();
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public AlertEntity update(final UpdateAlertEntity alert) {
        checkAlert(alert.getType(), alert.getReferenceType());
        try {
            final Optional<Alert> alertOptional = alertRepository.findById(alert.getId());
            if (alertOptional.isPresent()) {
                final Alert alertToUpdate = alertOptional.get();
                if (!alertToUpdate.getReferenceId().equals(alert.getReferenceId())) {
                    throw new AlertNotFoundException(alert.getId());
                }

                alertToUpdate.setName(alert.getName());
                alertToUpdate.setDescription(alert.getDescription());
                alertToUpdate.setEnabled(alert.isEnabled());
                if (alert.getMetricType() != null) {
                    alertToUpdate.setMetricType(alert.getMetricType().name());
                }
                if (alert.getMetric() != null) {
                    alertToUpdate.setMetric(alert.getMetric().name());
                }
                if (alert.getThresholdType() != null) {
                    alertToUpdate.setThresholdType(alert.getThresholdType().name());
                }
                if (alert.getThreshold() != null) {
                    alertToUpdate.setThreshold(alert.getThreshold());
                }
                alertToUpdate.setPlan(alert.getPlan());
                alertToUpdate.setUpdatedAt(new Date());

                final AlertEntity alertEntity = convert(alertRepository.update(alertToUpdate));
                triggerOrCancelAlert(alertEntity);
                return alertEntity;

            } else {
                throw new AlertNotFoundException(alert.getId());
            }
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to update alert of type " + alert.getType();
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public List<AlertEntity> findAll() {
        try {
            final Set<Alert> alerts = alertRepository.findAll();
            return alerts.stream().map(this::convert).collect(toList());
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to list all alerts";
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public List<AlertEntity> findByReference(final AlertReferenceType referenceType, final String referenceId) {
        if (!enabled) {
            return emptyList();
        }
        try {
            final List<Alert> alerts = alertRepository.findByReference(referenceType.name(), referenceId);
            createDefaultAlert(referenceType, referenceId, alerts);
            return alerts.stream().map(this::convert).sorted(comparing(AlertEntity::getName)).collect(toList());
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to list alerts by reference " + referenceType
                    + '/' + referenceId;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    private void createDefaultAlert(AlertReferenceType referenceType, String referenceId, List<Alert> alerts) {
        if (API.equals(referenceType)) {
            if (alerts.stream().noneMatch(alert -> HEALTH_CHECK.name().equals(alert.getType()))) {
                final Alert alert = new Alert();
                alert.setName("Health-check");
                alert.setDescription(referenceType + " health-check status changed");
                alert.setType(HEALTH_CHECK.name());
                alert.setReferenceType(referenceType.name());
                alert.setReferenceId(referenceId);
                alerts.add(alert);
            }
        } else {
            if (alerts.isEmpty()) {
                final Alert alert = new Alert();
                alert.setName("Request");
                alert.setType(REQUEST.name());
                alert.setReferenceType(referenceType.name());
                alert.setReferenceId(referenceId);
                alerts.add(alert);
            }
        }
    }

    @Override
    public void delete(final String alertId, final String referenceId) {
        try {
            final Optional<Alert> optionalAlert = alertRepository.findById(alertId);
            if (!optionalAlert.isPresent() || !optionalAlert.get().getReferenceId().equals(referenceId)) {
                throw new AlertNotFoundException(alertId);
            }
            final AlertEntity alert = convert(optionalAlert.get());
            checkAlert(alert.getType(), alert.getReferenceType());
            alertRepository.delete(alertId);
            alertTriggerService.disable(alert);
        } catch (TechnicalException te) {
            final String msg = "An error occurs while trying to delete the alert " + alertId;
            LOGGER.error(msg, te);
            throw new TechnicalManagementException(msg, te);
        }
    }

    private void checkAlert(final AlertType type, final AlertReferenceType referenceType) {
        if (!enabled) {
            throw new AlertUnavailableException();
        }
        if (!type.getReferenceTypes().contains(referenceType)) {
            throw new AlertInvalidException(type.name(), referenceType.name());
        }
    }

    private void triggerOrCancelAlert(final AlertEntity alert) {
        if (alert.isEnabled()) {
            alertTriggerService.trigger(alert);
        } else {
            alertTriggerService.disable(alert);
        }
    }

    private Alert convert(final NewAlertEntity alertEntity) {
        final Alert alert = new Alert();
        alert.setId(UUID.toString(UUID.random()));
        alert.setName(alertEntity.getName());
        alert.setDescription(alertEntity.getDescription());
        alert.setReferenceId(alertEntity.getReferenceId());
        alert.setReferenceType(alertEntity.getReferenceType().name());
        alert.setType(alertEntity.getType().name());
        alert.setEnabled(alertEntity.isEnabled());
        if (alertEntity.getMetricType() != null) {
            alert.setMetricType(alertEntity.getMetricType().name());
        }
        if (alertEntity.getMetric() != null) {
            alert.setMetric(alertEntity.getMetric().name());
        }
        if (alertEntity.getThresholdType() != null) {
            alert.setThresholdType(alertEntity.getThresholdType().name());
        }
        if (alertEntity.getThreshold() != null) {
            alert.setThreshold(alertEntity.getThreshold());
        }
        alert.setPlan(alertEntity.getPlan());
        final Date now = new Date();
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);
        return alert;
    }

    private AlertEntity convert(final Alert alert) {
        final AlertEntity alertEntity = new AlertEntity();
        alertEntity.setId(alert.getId());
        alertEntity.setName(alert.getName());
        alertEntity.setDescription(alert.getDescription());
        alertEntity.setReferenceId(alert.getReferenceId());
        alertEntity.setReferenceType(AlertReferenceType.valueOf(alert.getReferenceType()));
        alertEntity.setType(AlertType.valueOf(alert.getType()));
        alertEntity.setEnabled(alert.isEnabled());
        if (alert.getMetricType() != null) {
            alertEntity.setMetricType(MetricType.valueOf(alert.getMetricType()));
        }
        if (alert.getMetric() != null) {
            alertEntity.setMetric(Metric.valueOf(alert.getMetric()));
        }
        if (alert.getThresholdType() != null) {
            alertEntity.setThresholdType(ThresholdType.valueOf(alert.getThresholdType()));
        }
        if (alert.getThreshold() != null) {
            alertEntity.setThreshold(alert.getThreshold());
        }
        alertEntity.setPlan(alert.getPlan());
        alertEntity.setCreatedAt(alert.getCreatedAt());
        alertEntity.setUpdatedAt(alert.getUpdatedAt());
        return alertEntity;
    }
}
