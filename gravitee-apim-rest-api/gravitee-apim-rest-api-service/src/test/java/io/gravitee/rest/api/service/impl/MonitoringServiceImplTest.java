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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.monitoring.MonitoringRepository;
import io.gravitee.repository.monitoring.model.MonitoringResponse;
import io.gravitee.rest.api.model.monitoring.MonitoringData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MonitoringServiceImplTest {

    @InjectMocks
    private MonitoringServiceImpl cut;

    @Mock
    private MonitoringRepository monitoringRepository;

    @Test
    public void shouldFindNoMonitoring() {
        when(monitoringRepository.query(anyString())).thenReturn(null);
        MonitoringData result = cut.findMonitoring("a_gateway_id");

        assertNull(result);
        verify(monitoringRepository, times(1)).query(any());
    }

    @Test
    public void shouldFindMonitoring() {
        when(monitoringRepository.query(anyString())).thenReturn(new MonitoringResponse());
        MonitoringData result = cut.findMonitoring("a_gateway_id");

        assertNotNull(result);
        verify(monitoringRepository, times(1)).query(any());
    }
}
