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
import io.gravitee.alert.api.trigger.TriggerProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AlertService_CreateDefaultsTest extends AlertServiceTest {

    @Test(expected = TechnicalManagementException.class)
    public void must_throw_TechnicalManagementException_when_create_defaults_alerts_for_an_API() throws TechnicalException {
        when(parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(alertTriggerRepository.findAll()).thenThrow(new TechnicalException("An unexpected error has occurred"));

        alertService.createDefaults(executionContext, AlertReferenceType.API, "apiId");
    }

    @Test
    public void must_create_nothing_if_alerts_disabled() throws TechnicalException {
        when(parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(false);

        alertService.createDefaults(executionContext, AlertReferenceType.API, "apiId");
        verify(alertTriggerRepository, times(0)).create(any());
    }

    @Test
    public void must_create_defaults_API_alerts_linked_to_the_default_environment() throws JsonProcessingException, TechnicalException {
        NewAlertTriggerEntity newAlertTriggerEntity1 = getNewAlertTriggerEntity(true);
        NewAlertTriggerEntity newAlertTriggerEntity2 = getNewAlertTriggerEntity(true);
        NewAlertTriggerEntity newAlertTriggerEntity3 = getNewAlertTriggerEntity(true);

        Set<AlertTrigger> alertTriggers = Set.of(
            getAlertTriggerFromNew(newAlertTriggerEntity1, executionContext.getEnvironmentId()),
            getAlertTriggerFromNew(newAlertTriggerEntity2, executionContext.getEnvironmentId()),
            getAlertTriggerFromNew(newAlertTriggerEntity3, executionContext.getEnvironmentId())
        );

        when(alertTriggerRepository.findAll()).thenReturn(alertTriggers);
        when(alertTriggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);
        when(alertTriggerRepository.create(any()))
            .thenReturn(getAlertTriggerFromNew(getNewAlertTriggerEntity(), executionContext.getEnvironmentId()));

        alertService.createDefaults(executionContext, AlertReferenceType.API, "apiId");

        verify(alertTriggerRepository, times(3)).create(any());
    }

    @Test
    public void must_not_create_defaults_API_alerts() throws JsonProcessingException, TechnicalException {
        NewAlertTriggerEntity newAlertTriggerEntity1 = getNewAlertTriggerEntity(true);
        NewAlertTriggerEntity newAlertTriggerEntity2 = getNewAlertTriggerEntity(true);
        NewAlertTriggerEntity newAlertTriggerEntity3 = getNewAlertTriggerEntity(true);

        List<AlertTrigger> alertTriggers = List.of(
            getAlertTriggerFromNew(newAlertTriggerEntity1, "ENV_1"),
            getAlertTriggerFromNew(newAlertTriggerEntity2, "ENV_2"),
            getAlertTriggerFromNew(newAlertTriggerEntity3, "ENV_3")
        );

        when(alertTriggerRepository.findAll()).thenReturn(new HashSet<>(alertTriggers));
        when(alertTriggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class)));
        when(parameterService.findAsBoolean(executionContext, Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION)).thenReturn(true);

        alertService.createDefaults(executionContext, AlertReferenceType.API, "apiId");

        verify(alertTriggerRepository, never()).create(any());
    }
}
