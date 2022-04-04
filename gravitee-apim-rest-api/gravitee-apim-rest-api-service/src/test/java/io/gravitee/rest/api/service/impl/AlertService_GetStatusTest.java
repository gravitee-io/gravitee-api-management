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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.alert.api.trigger.TriggerProvider;
import io.gravitee.plugin.alert.AlertTriggerProviderManager;
import io.gravitee.rest.api.model.alert.AlertStatusEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
public class AlertService_GetStatusTest {

    @InjectMocks
    private AlertServiceImpl alertService = new AlertServiceImpl();

    @Mock
    private ParameterService parameterService;

    @Mock
    private AlertTriggerProviderManager alertTriggerProviderManager;

    @Test
    public void getStatus_should_get_enabled_status() {
        when(alertTriggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class), mock(TriggerProvider.class)));
        when(parameterService.findAsBoolean(GraviteeContext.getExecutionContext(), Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION))
            .thenReturn(true);

        AlertStatusEntity alertStatus = alertService.getStatus(GraviteeContext.getExecutionContext());

        assertTrue(alertStatus.isEnabled());
        assertEquals(2, alertStatus.getPlugins());
    }

    @Test
    public void getStatus_should_get_disabled_status() {
        when(alertTriggerProviderManager.findAll()).thenReturn(List.of(mock(TriggerProvider.class), mock(TriggerProvider.class)));
        when(parameterService.findAsBoolean(GraviteeContext.getExecutionContext(), Key.ALERT_ENABLED, ParameterReferenceType.ORGANIZATION))
            .thenReturn(false);

        AlertStatusEntity alertStatus = alertService.getStatus(GraviteeContext.getExecutionContext());

        assertFalse(alertStatus.isEnabled());
        assertEquals(2, alertStatus.getPlugins());
    }
}
