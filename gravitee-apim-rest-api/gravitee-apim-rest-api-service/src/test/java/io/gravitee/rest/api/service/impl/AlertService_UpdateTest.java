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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.alert.api.trigger.Trigger.Severity.INFO;
import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;
import static org.mockito.Mockito.*;

import io.gravitee.alert.api.trigger.TriggerProvider;
import io.gravitee.plugin.alert.AlertTriggerProviderManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertEventRepository;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.AlertTriggerConverter;
import io.gravitee.rest.api.service.exceptions.AlertNotFoundException;
import io.gravitee.rest.api.service.exceptions.AlertUnavailableException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertService_UpdateTest {

    private static final String ALERT_ID = "alert-id";
    private static final String REFERENCE_ID = "reference-id";

    @InjectMocks
    private AlertServiceImpl alertService = new AlertServiceImpl();

    @Mock
    private AlertTriggerRepository alertTriggerRepository;

    @Mock
    private AlertEventRepository alertEventRepository;

    @Mock
    private AlertTriggerConverter alertTriggerConverter;

    @Mock
    private TriggerProvider triggerProvider;

    @Mock
    private ParameterService parameterService;

    @Mock
    private AlertTriggerProviderManager alertTriggerProviderManager;

    @Test(expected = AlertUnavailableException.class)
    public void update_should_throw_AlertUnavailableException_cause_setting_off() {
        mockAlertSetting(false);

        UpdateAlertTriggerEntity updateAlertTriggerEntity = buildUpdateAlertTriggerEntity();

        alertService.update(getExecutionContext(), updateAlertTriggerEntity);
    }

    @Test(expected = AlertNotFoundException.class)
    public void update_should_throw_AlertNotFoundException_cause_alert_not_found() throws TechnicalException {
        mockAlertSetting(true);

        UpdateAlertTriggerEntity updateAlertTriggerEntity = buildUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

        alertService.update(getExecutionContext(), updateAlertTriggerEntity);
    }

    @Test(expected = AlertNotFoundException.class)
    public void update_should_throw_AlertNotFoundException_cause_alert_found_with_different_referenceId() throws TechnicalException {
        mockAlertSetting(true);

        UpdateAlertTriggerEntity updateAlertTriggerEntity = buildUpdateAlertTriggerEntity();

        AlertTrigger foundAlert = new AlertTrigger();
        foundAlert.setReferenceId("another-ref-id");
        when(alertTriggerRepository.findById(ALERT_ID)).thenReturn(Optional.of(foundAlert));

        alertService.update(getExecutionContext(), updateAlertTriggerEntity);
    }

    @Test(expected = TechnicalManagementException.class)
    public void update_should_throw_TechnicalManagementException_cause_technicalException_thrown() throws TechnicalException {
        mockAlertSetting(true);

        UpdateAlertTriggerEntity updateAlertTriggerEntity = buildUpdateAlertTriggerEntity();

        AlertTrigger foundAlert = new AlertTrigger();
        foundAlert.setReferenceId("another-ref-id");

        when(alertTriggerRepository.findById(ALERT_ID)).thenThrow(TechnicalException.class);

        alertService.update(getExecutionContext(), updateAlertTriggerEntity);
    }

    @Test
    public void update_should_update() throws TechnicalException {
        mockAlertSetting(true);

        UpdateAlertTriggerEntity updateAlertTriggerEntity = buildUpdateAlertTriggerEntity();

        AlertTrigger foundAlert = new AlertTrigger();
        foundAlert.setReferenceId(REFERENCE_ID);
        when(alertTriggerRepository.findById(ALERT_ID)).thenReturn(Optional.of(foundAlert));

        when(alertTriggerConverter.toAlertTriggerEntity(any(AlertTrigger.class))).thenAnswer(i -> buildAlertTriggerEntity());
        when(alertTriggerConverter.toAlertTrigger(any(ExecutionContext.class), any(UpdateAlertTriggerEntity.class)))
            .thenAnswer(i -> buildAlertTrigger());
        when(alertTriggerRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        alertService.update(getExecutionContext(), updateAlertTriggerEntity);

        verify(alertTriggerRepository, times(1)).update(argThat(alert -> alert.getId().equals(ALERT_ID)));
    }

    private AlertTriggerEntity buildAlertTriggerEntity() {
        return mock(AlertTriggerEntity.class);
    }

    private UpdateAlertTriggerEntity buildUpdateAlertTriggerEntity() {
        UpdateAlertTriggerEntity updateAlertTriggerEntity = new UpdateAlertTriggerEntity();
        updateAlertTriggerEntity.setId(ALERT_ID);
        updateAlertTriggerEntity.setReferenceId(REFERENCE_ID);
        updateAlertTriggerEntity.setSeverity(INFO);
        return updateAlertTriggerEntity;
    }

    private AlertTrigger buildAlertTrigger() {
        AlertTrigger alertTrigger = mock(AlertTrigger.class);
        when(alertTrigger.getId()).thenReturn(ALERT_ID);
        return alertTrigger;
    }

    private void mockAlertSetting(boolean enabled) {
        when(parameterService.findAsBoolean(GraviteeContext.getExecutionContext(), Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION))
            .thenReturn(enabled);
        when(alertTriggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class), mock(TriggerProvider.class)));
    }
}
