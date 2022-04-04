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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.service.converter.AlertTriggerConverter;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertService_FindByReferenceTest {

    private static final AlertReferenceType REFERENCE_TYPE = AlertReferenceType.APPLICATION;
    private static final String REFERENCE_ID = "reference-id";

    @InjectMocks
    private AlertServiceImpl alertService = new AlertServiceImpl();

    @Mock
    private AlertTriggerRepository alertTriggerRepository;

    @Mock
    private AlertTriggerConverter alertTriggerConverter;

    @Test
    public void findByReference_should_find() throws TechnicalException {
        List<AlertTrigger> alertTriggers = List.of(mock(AlertTrigger.class), mock(AlertTrigger.class), mock(AlertTrigger.class));
        List<AlertTriggerEntity> alertTriggerEntities = List.of(
            mock(AlertTriggerEntity.class),
            mock(AlertTriggerEntity.class),
            mock(AlertTriggerEntity.class)
        );
        when(alertTriggerConverter.toAlertTriggerEntities(alertTriggers)).thenReturn(alertTriggerEntities);

        when(alertTriggerRepository.findByReferenceAndReferenceId(REFERENCE_TYPE.name(), REFERENCE_ID)).thenReturn(alertTriggers);

        List<AlertTriggerEntity> results = alertService.findByReference(REFERENCE_TYPE, REFERENCE_ID);

        assertEquals(alertTriggerEntities, results);
    }

    @Test(expected = TechnicalManagementException.class)
    public void findByReference_should_throw_TechnicalManagementException_when_technicalException() throws TechnicalException {
        when(alertTriggerRepository.findByReferenceAndReferenceId(REFERENCE_TYPE.name(), REFERENCE_ID)).thenThrow(TechnicalException.class);

        alertService.findByReference(REFERENCE_TYPE, REFERENCE_ID);
    }
}
