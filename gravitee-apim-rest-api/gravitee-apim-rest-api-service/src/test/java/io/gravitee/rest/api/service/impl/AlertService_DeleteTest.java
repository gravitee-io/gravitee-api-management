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

import static org.mockito.Mockito.*;

import io.gravitee.alert.api.trigger.TriggerProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertEventRepository;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.service.converter.AlertTriggerConverter;
import io.gravitee.rest.api.service.exceptions.AlertNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertService_DeleteTest {

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

    @Test(expected = AlertNotFoundException.class)
    public void delete_should_throw_AlertNotFoundException_cause_alert_not_found() throws TechnicalException {
        when(alertTriggerRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

        alertService.delete(ALERT_ID, REFERENCE_ID);
    }

    @Test(expected = AlertNotFoundException.class)
    public void delete_should_throw_AlertNotFoundException_cause_alert_found_with_different_referenceId() throws TechnicalException {
        AlertTrigger alertTrigger = new AlertTrigger();
        alertTrigger.setId(ALERT_ID);
        alertTrigger.setReferenceId("another-ref-id");
        when(alertTriggerRepository.findById(ALERT_ID)).thenReturn(Optional.of(alertTrigger));

        alertService.delete(ALERT_ID, REFERENCE_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void delete_should_throw_TechnicalManagementException_cause_technicalException_thrown() throws TechnicalException {
        when(alertTriggerRepository.findById(ALERT_ID)).thenThrow(TechnicalException.class);

        alertService.delete(ALERT_ID, REFERENCE_ID);
    }

    @Test
    public void delete_should_delete_alert_triggers_and_events() throws TechnicalException {
        AlertTrigger alertTrigger = new AlertTrigger();
        alertTrigger.setId(ALERT_ID);
        alertTrigger.setReferenceId(REFERENCE_ID);
        when(alertTriggerRepository.findById(ALERT_ID)).thenReturn(Optional.of(alertTrigger));

        AlertTriggerEntity alertTriggerEntity = mock(AlertTriggerEntity.class);
        when(alertTriggerConverter.toAlertTriggerEntity(alertTrigger)).thenReturn(alertTriggerEntity);

        alertService.delete(ALERT_ID, REFERENCE_ID);

        verify(alertTriggerRepository, times(1)).delete(ALERT_ID);
        verify(alertEventRepository, times(1)).deleteAll(ALERT_ID);
        verify(triggerProvider, times(1)).unregister(alertTriggerEntity);
    }
}
