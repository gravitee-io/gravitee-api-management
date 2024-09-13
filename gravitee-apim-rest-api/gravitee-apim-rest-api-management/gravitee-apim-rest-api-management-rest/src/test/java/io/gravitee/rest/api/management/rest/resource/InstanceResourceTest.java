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
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.InstanceEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class InstanceResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "instances/123";
    }

    @Before
    public void init() {
        reset(instanceService, parameterService);
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
    public void shouldReturnInstance_whenCloudNotEnabled() {
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
        mockInstance.setEnvironments(Set.of(GraviteeContext.getCurrentEnvironment()));
        when(instanceService.findByEvent(any(), any())).thenReturn(mockInstance);

        final Response response = envTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertEquals(mockInstance, response.readEntity(InstanceEntity.class));
    }
}
