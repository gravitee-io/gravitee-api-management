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
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
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
public class AlertService_DeleteTest extends AlertServiceTest {

    @Test(expected = AlertNotFoundException.class)
    public void delete_should_throw_AlertNotFoundException_cause_alert_not_found() throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();

        when(alertTriggerRepository.findById(alert.getId())).thenReturn(Optional.empty());

        alertService.delete(alert.getId(), alert.getReferenceId());
    }

    @Test(expected = AlertNotFoundException.class)
    public void delete_should_throw_AlertNotFoundException_cause_alert_found_with_different_referenceId()
        throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);
        alertTrigger.setReferenceId("another-ref-id");
        when(alertTriggerRepository.findById(alert.getId())).thenReturn(Optional.of(alertTrigger));

        alertService.delete(alert.getId(), alert.getReferenceId());
    }

    @Test(expected = TechnicalManagementException.class)
    public void delete_should_throw_TechnicalManagementException_cause_technicalException_thrown()
        throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();

        when(alertTriggerRepository.findById(alert.getId())).thenThrow(TechnicalException.class);

        alertService.delete(alert.getId(), alert.getReferenceId());
    }

    @Test
    public void delete_should_delete_alert_triggers_and_events() throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        when(alertTriggerRepository.findById(alert.getId())).thenReturn(Optional.of(alertTrigger));

        alertService.delete(alert.getId(), alert.getReferenceId());

        verify(alertTriggerRepository, times(1)).delete(alertTrigger.getId());
        verify(alertEventRepository, times(1)).deleteAll(alert.getId());
        verify(triggerProvider, times(1)).unregister(argThat(trigger -> trigger.getId().equals(alertTrigger.getId())));
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_throw_TechnicalManagementException_delete_alert_trigger_entity_with_email_notifier_definition_and_disable_trigger()
        throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(any())).thenThrow(new TechnicalException("An unexpected error has occurred"));

        alertService.delete(alert.getId(), alert.getReferenceId());

        verify(alertTriggerRepository, times(0)).delete(eq(alert.getId()));
        verify(alertEventRepository, times(0)).deleteAll(eq(alert.getId()));
        verify(triggerProvider, times(0)).unregister(any(Trigger.class));
    }

    @Test(expected = AlertNotFoundException.class)
    public void must_throw_AlertNotFoundException_delete_alert_trigger_entity_with_email_notifier_definition_and_disable_trigger()
        throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findById(any())).thenReturn(Optional.empty());

        alertService.delete(alert.getId(), alert.getReferenceId());

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
        alertService.delete(alert.getId(), alert.getReferenceId());

        verify(alertTriggerRepository, times(1)).delete(eq(alert.getId()));
        verify(alertEventRepository, times(1)).deleteAll(eq(alert.getId()));
        verify(triggerProvider, times(1)).unregister(any(Trigger.class));
    }
}
