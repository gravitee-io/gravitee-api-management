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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.alert.api.trigger.TriggerProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.exceptions.AlertUnavailableException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AlertService_CreateTest extends AlertServiceTest {

    @Test(expected = AlertUnavailableException.class)
    public void must_throw_AlertUnavailableException_when_create_new_alert_trigger_entity() {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();

        when(parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(false);

        alertService.create(executionContext, alert);
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_throw_TechnicalManagementException_when_create_new_alert_trigger_entity_with_email_notifier_definition()
        throws TechnicalException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();

        when(parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(alertTriggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.create(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        alertService.create(executionContext, alert);
    }

    @Test
    public void must_create_new_alert_trigger_entity_with_email_notifier_definition() throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        when(parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(alertTriggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.create(any())).thenReturn(alertTrigger);

        alertService.create(executionContext, alert);

        verify(triggerProvider, times(1)).unregister(any(Trigger.class));
    }

    @Test
    public void must_create_new_alert_trigger_entity_with_email_notifier_definition_and_null_host() throws Exception {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        when(parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(alertTriggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(alertTriggerRepository.create(any())).thenReturn(alertTrigger);

        alertService = getAlertService();
        alertService.afterPropertiesSet();
        alertService.create(executionContext, alert);

        verify(triggerProvider, times(1)).unregister(any(Trigger.class));
    }
}
