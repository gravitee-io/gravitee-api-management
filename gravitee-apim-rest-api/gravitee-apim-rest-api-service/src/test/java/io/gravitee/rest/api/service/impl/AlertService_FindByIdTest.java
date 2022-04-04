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

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.service.converter.AlertTriggerConverter;
import io.gravitee.rest.api.service.exceptions.AlertNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
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
public class AlertService_FindByIdTest {

    private static final String ALERT_ID = "id-alert";

    @InjectMocks
    private AlertServiceImpl alertService = new AlertServiceImpl();

    @Mock
    private AlertTriggerRepository alertTriggerRepository;

    @Mock
    private AlertTriggerConverter alertTriggerConverter;

    @Mock
    private AlertTrigger alertTrigger;

    @Mock
    private AlertTriggerEntity alertTriggerEntity;

    @Test
    public void findById_should_find() throws TechnicalException {
        when(alertTriggerRepository.findById(ALERT_ID)).thenReturn(Optional.of(alertTrigger));
        when(alertTriggerConverter.toAlertTriggerEntity(alertTrigger)).thenReturn(alertTriggerEntity);

        AlertTriggerEntity result = alertService.findById(ALERT_ID);

        assertSame(result, alertTriggerEntity);
    }

    @Test(expected = AlertNotFoundException.class)
    public void findById_should_throw_AlertNotFoundException_when_not_found() throws TechnicalException {
        when(alertTriggerRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

        alertService.findById(ALERT_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void findById_should_throw_TechnicalManagementException_when_technicalException() throws TechnicalException {
        when(alertTriggerRepository.findById(ALERT_ID)).thenThrow(TechnicalException.class);

        alertService.findById(ALERT_ID);
    }
}
