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
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.InstanceEntity;
import io.gravitee.rest.api.model.monitoring.MonitoringData;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class MonitoringResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "instances/123/monitoring/456";
    }

    @Before
    public void init() {
        reset(instanceService, monitoringService, parameterService);
    }

    @Test
    public void shouldThrowCloudEnabledException_whenCloudIsEnabled() {
        ExecutionContext executionContext = new ExecutionContext(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );
        when(
            parameterService.findAsBoolean(
                executionContext,
                Key.CLOUD_ENABLED,
                GraviteeContext.getCurrentOrganization(),
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(true);

        final Response response = envTarget().request().get();
        assertEquals(HttpStatusCode.SERVICE_UNAVAILABLE_503, response.getStatus());
    }

    @Test
    public void shouldReturnMonitoringData_whenCloudNotEnabled() {
        ExecutionContext executionContext = new ExecutionContext(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );
        when(
            parameterService.findAsBoolean(
                executionContext,
                Key.CLOUD_ENABLED,
                GraviteeContext.getCurrentOrganization(),
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(false);

        InstanceEntity mockInstance = new InstanceEntity();
        mockInstance.setEnvironments(Set.of("DEFAULT"));
        MonitoringData mockMonitoringData = new MonitoringData();
        when(instanceService.findByEvent(any(), any())).thenReturn(mockInstance);
        when(monitoringService.findMonitoring(any(), any())).thenReturn(mockMonitoringData);
        when(environmentService.findOrganizationIdsByEnvironments(any())).thenReturn(new HashSet<>(Collections.singleton("DEFAULT")));

        Response response = envTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertEquals(mockMonitoringData, response.readEntity(MonitoringData.class));
    }
}
