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
package io.gravitee.rest.api.service.converter;

import static java.util.Comparator.*;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.common.utils.UUID;
import io.gravitee.repository.management.model.AlertEventRule;
import io.gravitee.repository.management.model.AlertEventType;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.AlertEventRuleEntity;
import io.gravitee.rest.api.model.alert.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class AlertTriggerConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertTriggerConverter.class);

    private ObjectMapper objectMapper;

    @Autowired
    public AlertTriggerConverter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AlertTrigger toAlertTrigger(final AlertTriggerEntity alertEntity) {
        final AlertTrigger alert = new AlertTrigger();
        alert.setId(alertEntity.getId());
        alert.setName(alertEntity.getName());
        alert.setDescription(alertEntity.getDescription());
        alert.setReferenceId(alertEntity.getReferenceId());
        alert.setReferenceType(alertEntity.getReferenceType().name());
        alert.setEnabled(alertEntity.isTemplate() || alertEntity.isEnabled());
        alert.setType(alertEntity.getType());
        alert.setSeverity(alertEntity.getSeverity().name());
        alert.setTemplate(alertEntity.isTemplate());
        alert.setParentId(alertEntity.getParentId());
        alert.setCreatedAt(alertEntity.getCreatedAt());
        alert.setEnvironmentId(alertEntity.getEnvironmentId());

        buildAlertTriggerEventRulesFromEntities(alert, alertEntity.getEventRules());

        buildAlertTriggerDefinitionFromEntity(alert, alertEntity);

        return alert;
    }

    public AlertTrigger toAlertTrigger(ExecutionContext executionContext, final NewAlertTriggerEntity alertEntity) {
        final AlertTrigger alert = new AlertTrigger();
        alert.setId(UUID.toString(UUID.random()));
        alertEntity.setId(alert.getId());
        alert.setName(alertEntity.getName());
        alert.setDescription(alertEntity.getDescription());
        alert.setReferenceId(alertEntity.getReferenceId());
        alert.setReferenceType(alertEntity.getReferenceType().name());
        alert.setEnabled(alertEntity.isEnabled());
        alert.setType(alertEntity.getType());
        alert.setSeverity(alertEntity.getSeverity().name());
        alert.setTemplate(alertEntity.isTemplate());
        alert.setEnvironmentId(executionContext.getEnvironmentId());

        buildAlertTriggerEventRulesFromEntities(alert, alertEntity.getEventRules());

        buildAlertTriggerDefinitionFromEntity(alert, alertEntity);

        return alert;
    }

    private void buildAlertTriggerEventRulesFromEntities(AlertTrigger alert, List<AlertEventRuleEntity> eventRules) {
        if (eventRules != null && !eventRules.isEmpty()) {
            alert.setEventRules(
                eventRules
                    .stream()
                    .map(alertEventRuleEntity -> new AlertEventRule(AlertEventType.valueOf(alertEventRuleEntity.getEvent().toUpperCase())))
                    .collect(toList())
            );
        }
    }

    public AlertTrigger toAlertTrigger(ExecutionContext executionContext, final UpdateAlertTriggerEntity alertEntity) {
        final AlertTrigger alert = new AlertTrigger();
        alert.setId(alertEntity.getId());
        alert.setName(alertEntity.getName());
        alert.setDescription(alertEntity.getDescription());
        alert.setEnabled(alertEntity.isEnabled());
        alert.setSeverity(alertEntity.getSeverity().name());
        alert.setEnvironmentId(executionContext.getEnvironmentId());

        alert.setEventRules(null);
        buildAlertTriggerEventRulesFromEntities(alert, alertEntity.getEventRules());

        buildAlertTriggerDefinitionFromEntity(alert, alertEntity);

        return alert;
    }

    public AlertTriggerEntity toAlertTriggerEntity(final AlertTrigger alert) {
        try {
            Trigger trigger = objectMapper.readValue(alert.getDefinition(), Trigger.class);

            final AlertTriggerEntity alertTriggerEntity = new AlertTriggerEntityWrapper(trigger);
            alertTriggerEntity.setDescription(alert.getDescription());
            alertTriggerEntity.setReferenceId(alert.getReferenceId());
            alertTriggerEntity.setReferenceType(AlertReferenceType.valueOf(alert.getReferenceType()));
            alertTriggerEntity.setCreatedAt(alert.getCreatedAt());
            alertTriggerEntity.setUpdatedAt(alert.getUpdatedAt());
            alertTriggerEntity.setType(alert.getType());
            if (alert.getSeverity() != null) {
                alertTriggerEntity.setSeverity(Trigger.Severity.valueOf(alert.getSeverity()));
            } else {
                alertTriggerEntity.setSeverity(Trigger.Severity.INFO);
            }

            alertTriggerEntity.setEnabled(alert.isEnabled());
            alertTriggerEntity.setTemplate(alert.isTemplate());
            alertTriggerEntity.setParentId(alert.getParentId());
            alertTriggerEntity.setEnvironmentId(alert.getEnvironmentId());

            if (alert.getEventRules() != null) {
                alertTriggerEntity.setEventRules(
                    alert
                        .getEventRules()
                        .stream()
                        .map(alertEventRule -> new AlertEventRuleEntity(alertEventRule.getEvent().name()))
                        .collect(Collectors.toList())
                );
            }

            return alertTriggerEntity;
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurs while transforming the alert trigger from its definition", ex);
        }

        return null;
    }

    public List<AlertTriggerEntity> toAlertTriggerEntities(final List<AlertTrigger> alerts) {
        return alerts
            .stream()
            .map(this::toAlertTriggerEntity)
            .sorted(
                comparing(alertTriggerEntity -> alertTriggerEntity != null ? alertTriggerEntity.getName() : null, nullsLast(naturalOrder()))
            )
            .collect(toList());
    }

    private void buildAlertTriggerDefinitionFromEntity(AlertTrigger alertTrigger, Object alertEntity) {
        try {
            alertTrigger.setDefinition(objectMapper.writeValueAsString(alertEntity));
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurred while transforming the alert trigger definition into string", ex);
        }
    }
}
