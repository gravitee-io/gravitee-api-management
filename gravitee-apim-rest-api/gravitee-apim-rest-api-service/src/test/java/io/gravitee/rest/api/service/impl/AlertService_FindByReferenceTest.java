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
import static org.mockito.Mockito.mock;
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
public class AlertService_FindByReferenceTest extends AlertServiceTest {

    private static final AlertReferenceType REFERENCE_TYPE = AlertReferenceType.APPLICATION;
    private static final String REFERENCE_ID = "reference-id";

    @Test
    public void findByReference_should_find() throws TechnicalException, JsonProcessingException {
        NewAlertTriggerEntity newAlertTriggerEntity1 = getNewAlertTriggerEntity();
        NewAlertTriggerEntity newAlertTriggerEntity2 = getNewAlertTriggerEntity();
        NewAlertTriggerEntity newAlertTriggerEntity3 = getNewAlertTriggerEntity();

        List<AlertTrigger> alertTriggers = List.of(
            getAlertTriggerFromNew(newAlertTriggerEntity1),
            getAlertTriggerFromNew(newAlertTriggerEntity2),
            getAlertTriggerFromNew(newAlertTriggerEntity3)
        );

        when(alertTriggerRepository.findByReferenceAndReferenceId(REFERENCE_TYPE.name(), REFERENCE_ID)).thenReturn(alertTriggers);

        List<AlertTriggerEntity> results = alertService.findByReference(REFERENCE_TYPE, REFERENCE_ID);

        assertEquals(newAlertTriggerEntity1.getId(), results.get(0).getId());
        assertEquals(newAlertTriggerEntity2.getId(), results.get(1).getId());
        assertEquals(newAlertTriggerEntity3.getId(), results.get(2).getId());
    }

    @Test(expected = TechnicalManagementException.class)
    public void findByReference_should_throw_TechnicalManagementException_when_technicalException() throws TechnicalException {
        when(alertTriggerRepository.findByReferenceAndReferenceId(REFERENCE_TYPE.name(), REFERENCE_ID)).thenThrow(TechnicalException.class);

        alertService.findByReference(REFERENCE_TYPE, REFERENCE_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void must_TechnicalManagementException_when_find_alert_trigger_entity_by_reference() throws TechnicalException {
        final UpdateAlertTriggerEntity alert = getUpdateAlertTriggerEntity();

        when(alertTriggerRepository.findByReferenceAndReferenceId(any(), any())).thenThrow(
            new TechnicalException("An unexpected error has occurred")
        );

        alertService.findByReference(alert.getReferenceType(), alert.getReferenceId());
    }
}
