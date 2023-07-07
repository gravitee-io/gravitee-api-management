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

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.alert.api.trigger.TriggerProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.exceptions.AlertNotFoundException;
import io.gravitee.rest.api.service.exceptions.AlertUnavailableException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertService_UpdateTest extends AlertServiceTest {

    @Test(expected = AlertUnavailableException.class)
    public void update_should_throw_AlertUnavailableException_cause_setting_off() {
        mockAlertSetting(false);

        final UpdateAlertTriggerEntity updateAlertTriggerEntity = getUpdateAlertTriggerEntity();

        alertService.update(executionContext, updateAlertTriggerEntity);
    }

    @Test(expected = AlertNotFoundException.class)
    public void update_should_throw_AlertNotFoundException_cause_alert_not_found() throws TechnicalException {
        mockAlertSetting(true);

        final UpdateAlertTriggerEntity updateAlertTriggerEntity = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(updateAlertTriggerEntity.getId())).thenReturn(Optional.empty());

        alertService.update(executionContext, updateAlertTriggerEntity);
    }

    @Test(expected = AlertNotFoundException.class)
    public void update_should_throw_AlertNotFoundException_cause_alert_found_with_different_referenceId() throws TechnicalException {
        mockAlertSetting(true);

        final UpdateAlertTriggerEntity updateAlertTriggerEntity = getUpdateAlertTriggerEntity();

        AlertTrigger foundAlert = new AlertTrigger();
        foundAlert.setReferenceId("another-ref-id");
        when(alertTriggerRepository.findById(updateAlertTriggerEntity.getId())).thenReturn(Optional.of(foundAlert));

        alertService.update(executionContext, updateAlertTriggerEntity);
    }

    @Test(expected = TechnicalManagementException.class)
    public void update_should_throw_TechnicalManagementException_cause_technicalException_thrown() throws TechnicalException {
        mockAlertSetting(true);

        final UpdateAlertTriggerEntity updateAlertTriggerEntity = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(updateAlertTriggerEntity.getId())).thenThrow(TechnicalException.class);

        alertService.update(executionContext, updateAlertTriggerEntity);
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_throw_TechnicalManagementException_when_update_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException, JsonProcessingException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromUpdate(alert);

        when(parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(alertTriggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.findById(any())).thenReturn(Optional.of(alertTrigger));
        when(alertTriggerRepository.update(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        alertService.update(executionContext, alert);
    }

    @Test
    public void update_should_update() throws TechnicalException, JsonProcessingException {
        mockAlertSetting(true);

        final UpdateAlertTriggerEntity updateAlertTriggerEntity = getUpdateAlertTriggerEntity();
        final AlertTrigger currentAlertTrigger = getAlertTriggerFromUpdate(updateAlertTriggerEntity);
        currentAlertTrigger.setName("Old-Name");

        AlertTrigger newAlertTrigger = getAlertTriggerFromUpdate(updateAlertTriggerEntity);

        when(alertTriggerRepository.findById(updateAlertTriggerEntity.getId())).thenReturn(Optional.of(currentAlertTrigger));
        when(alertTriggerRepository.update(any(AlertTrigger.class))).thenReturn(newAlertTrigger);

        alertService.update(executionContext, updateAlertTriggerEntity);

        verify(alertTriggerRepository, times(1)).update(argThat(alert -> alert.getId().equals(updateAlertTriggerEntity.getId())));
    }

    @Test
    public void must_update_new_alert_trigger_entity_with_email_notifier_definition() throws TechnicalException, JsonProcessingException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromUpdate(alert);

        when(parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(alertTriggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.findById(any())).thenReturn(Optional.of(alertTrigger));
        when(alertTriggerRepository.update(any())).thenReturn(alertTrigger);

        alertService.update(executionContext, alert);

        verify(triggerProvider, times(1)).register(any(Trigger.class));
    }

    private void mockAlertSetting(boolean enabled) {
        when(parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(enabled);
        when(alertTriggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class), mock(TriggerProvider.class)));
    }
}
