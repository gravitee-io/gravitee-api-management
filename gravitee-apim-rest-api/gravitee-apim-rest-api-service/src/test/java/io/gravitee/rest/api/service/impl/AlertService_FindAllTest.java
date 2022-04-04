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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.service.converter.AlertTriggerConverter;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertService_FindAllTest {

    @InjectMocks
    private AlertServiceImpl alertService = new AlertServiceImpl();

    @Mock
    private AlertTriggerRepository alertTriggerRepository;

    @Mock
    private AlertTriggerConverter alertTriggerConverter;

    @Test
    public void findAll_should_find() throws TechnicalException {
        Set<AlertTrigger> alertsTriggers = new HashSet<>();
        List<AlertTriggerEntity> alertTriggerEntities = new ArrayList<>();
        when(alertTriggerRepository.findAll()).thenReturn(alertsTriggers);
        when(alertTriggerConverter.toAlertTriggerEntities(eq(new ArrayList<>(alertsTriggers)))).thenReturn(alertTriggerEntities);

        List<AlertTriggerEntity> result = alertService.findAll();

        assertSame(alertTriggerEntities, result);
    }

    @Test(expected = TechnicalManagementException.class)
    public void findAll_should_throw_TechnicalManagementException_when_technicalException() throws TechnicalException {
        when(alertTriggerRepository.findAll()).thenThrow(TechnicalException.class);

        alertService.findAll();
    }
}
