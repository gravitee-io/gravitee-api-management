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

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.service.exceptions.AlertNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertService_FindByIdTest extends AlertServiceTest {

    @Test
    public void must_find_alert_trigger_entity_by_id() throws TechnicalException, JsonProcessingException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromUpdate(alert);

        when(alertTriggerRepository.findById(any())).thenReturn(Optional.of(alertTrigger));

        assertNotNull(alertService.findById(alert.getId()));
    }

    @Test(expected = AlertNotFoundException.class)
    public void must_throw_AlertNotFoundException_when_find_alert_trigger_entity_by_id() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(any())).thenReturn(Optional.empty());

        alertService.findById(alert.getId());
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_TechnicalManagementException_when_find_alert_trigger_entity_by_id() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        alertService.findById(alert.getId());
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_throw_TechnicalManagementException_when_find_alert_trigger_entity_by_id() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        alertService.findById(alert.getId());
    }
}
