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
package io.gravitee.rest.api.service.impl;

import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.alert.api.condition.StringCondition;
import io.gravitee.alert.api.trigger.Dampening;
import io.gravitee.alert.api.trigger.Trigger.Severity;
import io.gravitee.alert.api.trigger.TriggerProvider;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.notifier.api.Notification;
import io.gravitee.plugin.alert.AlertTriggerProviderManager;
import io.gravitee.repository.management.api.AlertEventRepository;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.AlertEvent;
import io.gravitee.repository.management.model.AlertEventRule;
import io.gravitee.repository.management.model.AlertEventType;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.AlertEventRuleEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.AlertTriggerConverter;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.mockito.Mock;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertServiceTest {

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected final AlertTriggerConverter alertTriggerConverter = new AlertTriggerConverter(objectMapper);

    @Mock
    protected AlertTriggerRepository alertTriggerRepository;

    @Mock
    protected ApiService apiService;

    @Mock
    protected ApplicationService applicationService;

    @Mock
    protected PlanService planService;

    @Mock
    protected AlertEventRepository alertEventRepository;

    @Mock
    protected TriggerProvider triggerProvider;

    @Mock
    protected AlertTriggerProviderManager alertTriggerProviderManager;

    @Mock
    protected ParameterService parameterService;

    @Mock
    protected ApiRepository apiRepository;

    @Mock
    protected ApiMetadataService apiMetadataService;

    @Mock
    protected EnvironmentService environmentService;

    @Mock
    protected Configuration configuration;

    protected AlertServiceImpl alertService;

    protected ExecutionContext executionContext = new ExecutionContext("DEFAULT", "DEFAULT");

    @Before
    public void setup() throws Exception {
        alertService = getAlertService();
        alertService.afterPropertiesSet();

        lenient().when(configuration.getProperty(eq("notifiers.email.subject"), anyString())).thenReturn("[Gravitee.io %s]");
        lenient().when(configuration.getProperty("notifiers.email.host")).thenReturn("my.host.io");
        lenient().when(configuration.getProperty(eq("notifiers.email.port"), eq(Integer.class))).thenReturn(587);
        lenient().when(configuration.getProperty("notifiers.email.username")).thenReturn("username");
        lenient().when(configuration.getProperty("notifiers.email.password")).thenReturn("password");
        lenient()
            .when(configuration.getProperty("notifiers.email.authMethods", String[].class))
            .thenReturn(new String[] { "LOGIN", "PLAIN" });
        lenient().when(configuration.getProperty("notifiers.email.starttls.enabled", Boolean.class, false)).thenReturn(false);
        lenient().when(configuration.getProperty("notifiers.email.ssl.trustAll", Boolean.class, false)).thenReturn(false);
    }

    @NotNull
    protected AlertServiceImpl getAlertService() {
        return new AlertServiceImpl(
            configuration,
            objectMapper,
            alertTriggerRepository,
            apiService,
            applicationService,
            planService,
            alertEventRepository,
            triggerProvider,
            alertTriggerProviderManager,
            parameterService,
            apiRepository,
            alertTriggerConverter,
            environmentService
        );
    }

    @NotNull
    protected AlertEvent newAlertEvent() {
        AlertEvent entity = new AlertEvent();
        entity.setId(UUID.randomUUID().toString());
        entity.setAlert(EventType.ALERT_NOTIFICATION.name());
        entity.setMessage("Alert ! Alert !");
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        return entity;
    }

    @NotNull
    protected NewAlertTriggerEntity getNewAlertTriggerEntity() {
        return getNewAlertTriggerEntity(false);
    }

    @NotNull
    protected NewAlertTriggerEntity getNewAlertTriggerEntity(boolean isTemplate) {
        final NewAlertTriggerEntity alert = new NewAlertTriggerEntity();
        alert.setId(UUID.randomUUID().toString());
        alert.setName("New alert");
        alert.setDescription("New alert description");
        alert.setReferenceId(UUID.randomUUID().toString());
        alert.setReferenceType(AlertReferenceType.API);
        alert.setSeverity(Severity.CRITICAL);
        alert.setSource("REQUEST");
        alert.setType("API");

        final Notification emailNotification = new Notification();
        emailNotification.setType("default-email");
        emailNotification.setConfiguration(
            "{\"from\":\"from@example.com\", \"to\":\"from@example.com\", \"subject\":\"[New Alert !]\", \"body\":\"Alert ! Alert !\"}"
        );

        alert.setNotifications(List.of(emailNotification));
        alert.setConditions(List.of(StringCondition.equals("property", "value").build()));
        alert.setEnabled(true);
        alert.setTemplate(isTemplate);
        final AlertEventRuleEntity alertEventRuleEntity = new AlertEventRuleEntity();
        alertEventRuleEntity.setEvent(AlertEventType.API_CREATE.name());
        alert.setEventRules(List.of(alertEventRuleEntity));
        alert.setFilters(List.of(StringCondition.equals("apiId", UUID.randomUUID().toString()).build()));
        alert.setDampening(Dampening.strictCount(1));
        return alert;
    }

    @NotNull
    protected UpdateAlertTriggerEntity getUpdateAlertTriggerEntity() {
        final UpdateAlertTriggerEntity alert = new UpdateAlertTriggerEntity();
        alert.setId(UUID.randomUUID().toString());
        alert.setName("New alert");
        alert.setDescription("New alert description");
        alert.setReferenceId(UUID.randomUUID().toString());
        alert.setReferenceType(AlertReferenceType.APPLICATION);
        alert.setSeverity(Severity.CRITICAL);
        alert.setSource("REQUEST");

        final Notification emailNotification = new Notification();
        emailNotification.setType("default-email");
        emailNotification.setConfiguration(
            "{\"from\":\"from@example.com\", \"to\":\"from@example.com\", \"subject\":\"[New Alert !]\", \"body\":\"Alert ! Alert !\"}"
        );

        alert.setNotifications(List.of(emailNotification));
        alert.setConditions(List.of(StringCondition.equals("property", "value").build()));
        alert.setEnabled(true);

        final AlertEventRuleEntity alertEventRuleEntity = new AlertEventRuleEntity();
        alertEventRuleEntity.setEvent(AlertEventType.API_CREATE.name());
        alert.setEventRules(List.of(alertEventRuleEntity));
        alert.setFilters(List.of(StringCondition.equals("apiId", UUID.randomUUID().toString()).build()));
        alert.setDampening(Dampening.strictCount(1));
        return alert;
    }

    @NotNull
    protected AlertTrigger getAlertTriggerFromNew(NewAlertTriggerEntity alertEntity) throws JsonProcessingException {
        return getAlertTriggerFromNew(alertEntity, executionContext.getEnvironmentId());
    }

    @NotNull
    protected AlertTrigger getAlertTriggerFromNew(NewAlertTriggerEntity alertEntity, String environmentId) throws JsonProcessingException {
        var alert = new AlertTrigger();
        alert.setId(alertEntity.getId());
        alert.setName(alertEntity.getName());
        alert.setDescription(alertEntity.getDescription());
        alert.setReferenceId(alertEntity.getReferenceId());
        alert.setReferenceType(alertEntity.getReferenceType().name());
        alert.setEnabled(alertEntity.isTemplate() || alertEntity.isEnabled());
        alert.setType(alertEntity.getType());
        alert.setSeverity(alertEntity.getSeverity().name());
        alert.setTemplate(alertEntity.isTemplate());
        if (alertEntity.getEventRules() != null && !alertEntity.getEventRules().isEmpty()) {
            alert.setEventRules(
                alertEntity
                    .getEventRules()
                    .stream()
                    .map(alertEventRuleEntity -> new AlertEventRule(AlertEventType.valueOf(alertEventRuleEntity.getEvent().toUpperCase())))
                    .collect(toList())
            );
        }
        alert.setEnabled(false);
        alert.setDefinition(objectMapper.writeValueAsString(alertEntity));
        alert.setEnvironmentId(environmentId);
        return alert;
    }

    @NotNull
    protected AlertTrigger getAlertTriggerFromUpdate(UpdateAlertTriggerEntity alertEntity) throws JsonProcessingException {
        var alert = new AlertTrigger();
        alert.setId(alertEntity.getId());
        alert.setName(alertEntity.getName());
        alert.setDescription(alertEntity.getDescription());
        alert.setReferenceId(alertEntity.getReferenceId());
        alert.setReferenceType(alertEntity.getReferenceType().name());
        alert.setEnabled(true);
        alert.setType(EventType.ALERT_NOTIFICATION.name());
        alert.setSeverity(alertEntity.getSeverity().name());
        alert.setTemplate(false);
        if (alertEntity.getEventRules() != null && !alertEntity.getEventRules().isEmpty()) {
            alert.setEventRules(
                alertEntity
                    .getEventRules()
                    .stream()
                    .map(alertEventRuleEntity -> new AlertEventRule(AlertEventType.valueOf(alertEventRuleEntity.getEvent().toUpperCase())))
                    .collect(toList())
            );
        }
        alert.setEnabled(true);
        alert.setDefinition(objectMapper.writeValueAsString(alertEntity));
        return alert;
    }
}
