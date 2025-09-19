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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertService_FindByReferencesTest extends AlertServiceTest {

    private static final AlertReferenceType REFERENCE_TYPE = AlertReferenceType.APPLICATION;
    private static final List<String> REFERENCES_ID = List.of("reference-id1", "reference-id2");

    @Test
    public void findByReferences_should_find() throws TechnicalException, JsonProcessingException {
        NewAlertTriggerEntity newAlertTriggerEntity1 = getNewAlertTriggerEntity();
        NewAlertTriggerEntity newAlertTriggerEntity2 = getNewAlertTriggerEntity();
        NewAlertTriggerEntity newAlertTriggerEntity3 = getNewAlertTriggerEntity();

        List<AlertTrigger> alertTriggers = List.of(
            getAlertTriggerFromNew(newAlertTriggerEntity1),
            getAlertTriggerFromNew(newAlertTriggerEntity2),
            getAlertTriggerFromNew(newAlertTriggerEntity3)
        );

        when(alertTriggerRepository.findByReferenceAndReferenceIds(REFERENCE_TYPE.name(), REFERENCES_ID)).thenReturn(alertTriggers);

        List<AlertTriggerEntity> results = alertService.findByReferences(REFERENCE_TYPE, REFERENCES_ID);

        assertEquals(newAlertTriggerEntity1.getId(), results.get(0).getId());
        assertEquals(newAlertTriggerEntity2.getId(), results.get(1).getId());
        assertEquals(newAlertTriggerEntity3.getId(), results.get(2).getId());
    }

    @Test(expected = TechnicalManagementException.class)
    public void findByReferences_should_throw_TechnicalManagementException_when_technicalException() throws TechnicalException {
        when(alertTriggerRepository.findByReferenceAndReferenceIds(REFERENCE_TYPE.name(), REFERENCES_ID)).thenThrow(
            TechnicalException.class
        );

        alertService.findByReferences(REFERENCE_TYPE, REFERENCES_ID);
    }

    @Test
    public void must_find_alert_trigger_entity_by_reference_and_ids() throws TechnicalException, JsonProcessingException {
        final NewAlertTriggerEntity alert = getNewAlertTriggerEntity();
        final AlertTrigger alertTrigger = getAlertTriggerFromNew(alert);

        when(
            alertTriggerRepository.findByReferenceAndReferenceIds(alert.getReferenceType().name(), List.of(alertTrigger.getReferenceId()))
        ).thenReturn(List.of(alertTrigger));

        assertEquals(1, alertService.findByReferences(alert.getReferenceType(), List.of(alert.getReferenceId())).size());
    }

    @Test
    public void must_throw_AlertNotFoundException_when_find_alert_trigger_entity_by_reference_and_ids() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findByReferenceAndReferenceIds(any(), any())).thenReturn(List.of());

        assertEquals(0, alertService.findByReferences(alert.getReferenceType(), List.of(alert.getReferenceId())).size());
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_TechnicalManagementException_when_find_alert_trigger_entity_by_reference_and_ids() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findByReferenceAndReferenceIds(any(), any())).thenThrow(
            new TechnicalException("An unexpected error has occurred")
        );

        assertEquals(0, alertService.findByReferences(alert.getReferenceType(), List.of(alert.getReferenceId())).size());
    }
}
