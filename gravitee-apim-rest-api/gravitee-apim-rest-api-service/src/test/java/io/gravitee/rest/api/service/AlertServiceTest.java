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
package io.gravitee.rest.api.service;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.alert.api.condition.StringCondition;
import io.gravitee.alert.api.trigger.Dampening;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.alert.api.trigger.Trigger.Severity;
import io.gravitee.alert.api.trigger.TriggerProvider;
import io.gravitee.common.data.domain.Page;
import io.gravitee.notifier.api.Notification;
import io.gravitee.plugin.alert.AlertTriggerProviderManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertEventRepository;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.AlertEvent;
import io.gravitee.repository.management.model.AlertEventRule;
import io.gravitee.repository.management.model.AlertEventType;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.AlertEventQuery.Builder;
import io.gravitee.rest.api.model.AlertEventRuleEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.alert.AlertEventEntity;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.exceptions.AlertNotFoundException;
import io.gravitee.rest.api.service.exceptions.AlertTemplateInvalidException;
import io.gravitee.rest.api.service.exceptions.AlertUnavailableException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AlertServiceImpl;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertServiceTest {

    @Mock
    private ConfigurableEnvironment environment;

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private AlertTriggerRepository alertTriggerRepository;

    @Mock
    private ApiService apiService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private PlanService planService;

    @Mock
    private AlertEventRepository alertEventRepository;

    @Mock
    private TriggerProvider triggerProvider;

    @Mock
    private AlertTriggerProviderManager triggerProviderManager;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApiMetadataService apiMetadataService;

    private AlertServiceImpl cut;

    @Before
    public void setup() throws Exception {
        cut = getAlertService("my.host.io");
        cut.afterPropertiesSet();
    }

    @NotNull
    private AlertServiceImpl getAlertService(String host) {
        return new AlertServiceImpl(
            "[Gravitee.io %s]",
            host,
            "587",
            "username",
            "password",
            new String[] { "LOGIN", "PLAIN" },
            false,
            false,
            null,
            null,
            environment,
            mapper,
            alertTriggerRepository,
            apiService,
            applicationService,
            planService,
            alertEventRepository,
            triggerProvider,
            triggerProviderManager,
            parameterService,
            apiRepository,
            apiMetadataService
        );
    }

    @Test
    public void must_return_status_disable_with_no_plugins() {
        when(parameterService.findAsBoolean(Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(false);
        when(triggerProviderManager.findAll()).thenReturn(List.of());

        var alertStatus = cut.getStatus();
        assertFalse(alertStatus.isEnabled());
        assertEquals(0, alertStatus.getPlugins());
    }

    @Test
    public void must_return_status_enabled_with_two_plugins() {
        when(parameterService.findAsBoolean(Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(triggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class), mock(TriggerProvider.class)));

        var alertStatus = cut.getStatus();
        assertTrue(alertStatus.isEnabled());
        assertEquals(2, alertStatus.getPlugins());
    }

    @Test(expected = AlertUnavailableException.class)
    public void must_throw_AlertUnavailableException_when_create_new_alert_trigger_entity() {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();

        when(parameterService.findAsBoolean(Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(false);

        cut.create(alert);
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_throw_TechnicalManagementException_when_create_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();

        when(parameterService.findAsBoolean(Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(triggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.create(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        cut.create(alert);
    }

    @Test
    public void must_create_new_alert_trigger_entity_with_email_notifier_definition() throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        when(parameterService.findAsBoolean(Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(triggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.create(any())).thenReturn(alertTrigger);

        cut.create(alert);

        verify(triggerProvider, times(1)).unregister(any(Trigger.class));
    }

    @Test
    public void must_create_new_alert_trigger_entity_with_email_notifier_definition_and_null_host() throws Exception {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        when(parameterService.findAsBoolean(Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(triggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.create(any())).thenReturn(alertTrigger);

        cut = getAlertService(null);
        cut.afterPropertiesSet();
        cut.create(alert);

        verify(triggerProvider, times(1)).unregister(any(Trigger.class));
    }

    @Test
    public void must_create_defaults_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);
        alertTrigger.setTemplate(true);

        when(parameterService.findAsBoolean(Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(triggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.create(any())).thenReturn(alertTrigger);
        when(alertTriggerRepository.findAll()).thenReturn(Set.of(alertTrigger));

        cut.createDefaults(alert.getReferenceType(), alert.getReferenceId());

        verify(triggerProvider, times(0)).register(any(Trigger.class));
        verify(triggerProvider, times(0)).unregister(any(Trigger.class));
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_throw_TechnicalManagementException_when_create_defaults_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);
        alertTrigger.setTemplate(true);

        when(parameterService.findAsBoolean(Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(triggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.findAll()).thenThrow(new TechnicalException("An unexpected exception has occurred"));

        cut.createDefaults(alert.getReferenceType(), alert.getReferenceId());

        verify(triggerProvider, times(1)).unregister(any(Trigger.class));
    }

    @Test(expected = AlertNotFoundException.class)
    public void must_throw_AlertNotFoundException_when_apply_defaults_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();

        when(alertTriggerRepository.findById(alert.getId())).thenReturn(Optional.empty());

        cut.applyDefaults(alert.getId(), alert.getReferenceType());
    }

    @Test(expected = AlertTemplateInvalidException.class)
    public void must_throw_AlertTemplateInvalidException_when_apply_defaults_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        when(alertTriggerRepository.findById(alert.getId())).thenReturn(Optional.of(alertTrigger));

        cut.applyDefaults(alert.getId(), alert.getReferenceType());
    }

    @Test
    public void must_apply_defaults_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity(true);
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        alertTrigger.setReferenceType(AlertReferenceType.API.name());
        alertTrigger.setDefinition(mapper.writeValueAsString(alert));

        when(alertTriggerRepository.findById(alert.getId())).thenReturn(Optional.of(alertTrigger));
        when(alertTriggerRepository.create(any())).thenReturn(alertTrigger);

        final Api mock = mock(Api.class);
        final List<Api> apis = List.of(mock);
        when(mock.getId()).thenReturn(UUID.randomUUID().toString());
        when(apiRepository.search(any())).thenReturn(apis);

        cut.applyDefaults(alert.getId(), alert.getReferenceType());
    }

    @Test(expected = AlertNotFoundException.class)
    public void must_throw_AlertNotFoundException_when_update_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(parameterService.findAsBoolean(Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(triggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.findById(any())).thenReturn(Optional.empty());

        cut.update(alert);
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_throw_TechnicalManagementException_when_update_new_alert_trigger_findById() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(parameterService.findAsBoolean(Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(triggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.findById(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        cut.update(alert);
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_throw_TechnicalManagementException_when_update_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException, JsonProcessingException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromUpdate(alert);

        when(parameterService.findAsBoolean(Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(triggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.findById(any())).thenReturn(Optional.of(alertTrigger));
        when(alertTriggerRepository.update(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        cut.update(alert);
    }

    @Test
    public void must_update_new_alert_trigger_entity_with_email_notifier_definition() throws TechnicalException, JsonProcessingException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromUpdate(alert);

        when(parameterService.findAsBoolean(Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(triggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.findById(any())).thenReturn(Optional.of(alertTrigger));
        when(alertTriggerRepository.update(any())).thenReturn(alertTrigger);

        cut.update(alert);

        verify(triggerProvider, times(1)).register(any(Trigger.class));
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_throw_TechnicalManagementException_delete_alert_trigger_entity_with_email_notifier_definition_and_disable_trigger()
        throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        cut.delete(alert.getId(), alert.getReferenceId());

        verify(alertTriggerRepository, times(0)).delete(eq(alert.getId()));
        verify(alertEventRepository, times(0)).deleteAll(eq(alert.getId()));
        verify(triggerProvider, times(0)).unregister(any(Trigger.class));
    }

    @Test(expected = AlertNotFoundException.class)
    public void must_throw_AlertNotFoundException_delete_alert_trigger_entity_with_email_notifier_definition_and_disable_trigger()
        throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(any())).thenReturn(Optional.empty());

        cut.delete(alert.getId(), alert.getReferenceId());

        verify(alertTriggerRepository, times(0)).delete(eq(alert.getId()));
        verify(alertEventRepository, times(0)).deleteAll(eq(alert.getId()));
        verify(triggerProvider, times(0)).unregister(any(Trigger.class));
    }

    @Test
    public void must_delete_alert_trigger_entity_with_email_notifier_definition_and_disable_trigger()
        throws TechnicalException, JsonProcessingException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromUpdate(alert);

        when(alertTriggerRepository.findById(any())).thenReturn(Optional.of(alertTrigger));
        cut.delete(alert.getId(), alert.getReferenceId());

        verify(alertTriggerRepository, times(1)).delete(eq(alert.getId()));
        verify(alertEventRepository, times(1)).deleteAll(eq(alert.getId()));
        verify(triggerProvider, times(1)).unregister(any(Trigger.class));
    }

    @Test
    public void must_find_alert_trigger_entity_by_reference() throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        when(alertTriggerRepository.findByReference(alert.getReferenceType().name(), alertTrigger.getReferenceId()))
            .thenReturn(List.of(alertTrigger));

        assertEquals(1, cut.findByReference(alert.getReferenceType(), alert.getReferenceId()).size());
    }

    @Test
    public void must_throw_AlertNotFoundException_when_find_alert_trigger_entity_by_reference() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findByReference(any(), any())).thenReturn(List.of());

        assertEquals(0, cut.findByReference(alert.getReferenceType(), alert.getReferenceId()).size());
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_TechnicalManagementException_when_find_alert_trigger_entity_by_reference() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findByReference(any(), any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        cut.findByReference(alert.getReferenceType(), alert.getReferenceId());
    }

    @Test
    public void must_find_alert_trigger_entity_by_reference_and_ids() throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        when(alertTriggerRepository.findByReferenceAndReferenceIds(alert.getReferenceType().name(), List.of(alertTrigger.getReferenceId())))
            .thenReturn(List.of(alertTrigger));

        assertEquals(1, cut.findByReferenceAndReferenceIds(alert.getReferenceType(), List.of(alert.getReferenceId())).size());
    }

    @Test
    public void must_throw_AlertNotFoundException_when_find_alert_trigger_entity_by_reference_and_ids() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findByReferenceAndReferenceIds(any(), any())).thenReturn(List.of());

        assertEquals(0, cut.findByReferenceAndReferenceIds(alert.getReferenceType(), List.of(alert.getReferenceId())).size());
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_TechnicalManagementException_when_find_alert_trigger_entity_by_reference_and_ids() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findByReferenceAndReferenceIds(any(), any()))
            .thenThrow(new TechnicalException("An unexpected error has occurred"));

        assertEquals(0, cut.findByReferenceAndReferenceIds(alert.getReferenceType(), List.of(alert.getReferenceId())).size());
    }

    @Test
    public void must_find_alert_trigger_entity_by_id() throws TechnicalException, JsonProcessingException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromUpdate(alert);

        when(alertTriggerRepository.findById(any())).thenReturn(Optional.of(alertTrigger));

        assertNotNull(cut.findById(alert.getId()));
    }

    @Test(expected = AlertNotFoundException.class)
    public void must_throw_AlertNotFoundException_when_find_alert_trigger_entity_by_id() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(any())).thenReturn(Optional.empty());

        cut.findById(alert.getId());
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_TechnicalManagementException_when_find_alert_trigger_entity_by_id() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        cut.findById(alert.getId());
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_throw_TechnicalManagementException_when_find_alert_trigger_entity_by_id() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        cut.findById(alert.getId());
    }

    @Test
    public void must_search_alert_event_entity_by_event_types() {
        AlertEvent entity = newAlertEvent();

        when(alertEventRepository.search(any(), any())).thenReturn(new Page<>(List.of(entity), 0, 1, 1));

        final Page<AlertEventEntity> page = cut.findEvents(
            UUID.randomUUID().toString(),
            new Builder().pageNumber(0).pageSize(1).type(EventType.ALERT_NOTIFICATION).build()
        );

        assertEquals(0, page.getPageNumber());
        assertEquals(1, page.getPageElements());
        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getContent().size());
    }

    @NotNull
    private AlertEvent newAlertEvent() {
        AlertEvent entity = new AlertEvent();
        entity.setId(UUID.randomUUID().toString());
        entity.setAlert(EventType.ALERT_NOTIFICATION.name());
        entity.setMessage("Alert ! Alert !");
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        return entity;
    }

    @Test
    public void must_search_alert_event_entity_by_event_type_and_return_empty() {
        when(alertEventRepository.search(any(), any())).thenReturn(new Page<>(List.of(), 0, 0, 0));

        final Page<AlertEventEntity> page = cut.findEvents(
            UUID.randomUUID().toString(),
            new Builder().pageNumber(0).pageSize(1).type(EventType.ALERT_NOTIFICATION).build()
        );

        assertEquals(1, page.getPageNumber());
        assertEquals(0, page.getPageElements());
        assertEquals(0, page.getTotalElements());
        assertEquals(0, page.getContent().size());
    }

    @NotNull
    private NewAlertTriggerEntity getNewAlertTriggerEntity() {
        return getNewAlertTriggerEntity(false);
    }

    @NotNull
    private NewAlertTriggerEntity getNewAlertTriggerEntity(boolean isTemplate) {
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
    private UpdateAlertTriggerEntity getUpdateAlertTriggerEntity() {
        final UpdateAlertTriggerEntity alert = new UpdateAlertTriggerEntity();
        alert.setId(UUID.randomUUID().toString());
        alert.setName("New alert");
        alert.setDescription("New alert description");
        alert.setReferenceId(UUID.randomUUID().toString());
        alert.setReferenceType(AlertReferenceType.PLATFORM);
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
    private AlertTrigger getAlertTriggerFromNew(NewAlertTriggerEntity alertEntity) throws JsonProcessingException {
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
        alert.setDefinition(mapper.writeValueAsString(alertEntity));
        return alert;
    }

    @NotNull
    private AlertTrigger getAlertTriggerFromUpdate(UpdateAlertTriggerEntity alertEntity) throws JsonProcessingException {
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
        alert.setDefinition(mapper.writeValueAsString(alertEntity));
        return alert;
    }
}
