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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.AlertTriggerConverter;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertService_ApplyDefaultsTest {

    @InjectMocks
    private AlertServiceImpl alertService = new AlertServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private AlertTriggerRepository alertTriggerRepository;

    @Mock
    private AlertTriggerConverter alertTriggerConverter;

    @Test
    public void applyDefaults_should_call_apiRepository_to_find_apis_using_environment_as_criteria() throws TechnicalException {
        AlertTrigger alert = new AlertTrigger();
        alert.setTemplate(true);
        when(alertTriggerRepository.findById("my-alert")).thenReturn(Optional.of(alert));

        AlertTriggerEntity alertTriggerEntity = mock(AlertTriggerEntity.class);
        when(alertTriggerEntity.isTemplate()).thenReturn(true);
        when(alertTriggerConverter.toAlertTriggerEntity(alert)).thenReturn(alertTriggerEntity);

        alertService.applyDefaults(GraviteeContext.getExecutionContext(), "my-alert", AlertReferenceType.API);

        verify(apiRepository, times(1))
            .search(argThat(criteria -> criteria.getEnvironmentId().equals(GraviteeContext.getCurrentEnvironment())));
    }
}
