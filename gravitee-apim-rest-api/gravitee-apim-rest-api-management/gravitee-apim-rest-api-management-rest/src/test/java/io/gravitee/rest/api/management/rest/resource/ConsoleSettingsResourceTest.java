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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.common.http.HttpStatusCode.SERVICE_UNAVAILABLE_503;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.settings.ConsoleSettingsEntity;
import io.gravitee.rest.api.model.settings.Maintenance;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConsoleSettingsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "settings";
    }

    @Before
    public void init() {
        reset(parameterService, configService);
    }

    @Test
    public void shouldCallSaveMethod() {
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getCurrentOrganization(), null);
        ConsoleSettingsEntity config = new ConsoleSettingsEntity();
        config.setMaintenance(new Maintenance());

        when(
            parameterService.findAsBoolean(
                executionContext,
                Key.MAINTENANCE_MODE_ENABLED,
                GraviteeContext.getCurrentOrganization(),
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(false);

        final Response response = orgTarget().request().post(Entity.json(config));

        assertEquals(response.readEntity(String.class), OK_200, response.getStatus());
        verify(configService).save(eq(executionContext), any(ConsoleSettingsEntity.class));
    }

    @Test
    public void shouldCallSaveMethod_EnabledMaintenance() {
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getCurrentOrganization(), null);
        ConsoleSettingsEntity config = new ConsoleSettingsEntity();
        Maintenance maintenance = new Maintenance();
        maintenance.setEnabled(true);
        config.setMaintenance(maintenance);

        when(
            parameterService.findAsBoolean(
                executionContext,
                Key.MAINTENANCE_MODE_ENABLED,
                GraviteeContext.getCurrentOrganization(),
                ParameterReferenceType.ORGANIZATION
            )
        )
            .thenReturn(false);

        final Response response = orgTarget().request().post(Entity.json(config));

        assertEquals(response.readEntity(String.class), OK_200, response.getStatus());
        verify(configService).save(eq(executionContext), any(ConsoleSettingsEntity.class));
    }

    @Test
    public void shouldNotCallSaveMethod_MaintenanceAlreadyEnabled() {
        ConsoleSettingsEntity config = new ConsoleSettingsEntity();
        Maintenance maintenance = new Maintenance();
        maintenance.setEnabled(true);
        config.setMaintenance(maintenance);

        when(
            parameterService.findAsBoolean(
                any(),
                eq(Key.MAINTENANCE_MODE_ENABLED),
                eq(GraviteeContext.getCurrentOrganization()),
                eq(ParameterReferenceType.ORGANIZATION)
            )
        )
            .thenReturn(true);

        final Response response = orgTarget().request().post(Entity.json(config));

        assertEquals(response.readEntity(String.class), SERVICE_UNAVAILABLE_503, response.getStatus());
        verify(configService, never()).save(eq(GraviteeContext.getExecutionContext()), any(ConsoleSettingsEntity.class));
    }

    @Test
    public void shouldNotCallSaveMethod_MaintenanceEnabled() {
        ConsoleSettingsEntity config = new ConsoleSettingsEntity();

        when(
            parameterService.findAsBoolean(
                any(),
                eq(Key.MAINTENANCE_MODE_ENABLED),
                eq(GraviteeContext.getCurrentOrganization()),
                eq(ParameterReferenceType.ORGANIZATION)
            )
        )
            .thenReturn(true);

        final Response response = orgTarget().request().post(Entity.json(config));

        assertEquals(response.readEntity(String.class), SERVICE_UNAVAILABLE_503, response.getStatus());
        verify(configService, never()).save(eq(GraviteeContext.getExecutionContext()), any(ConsoleSettingsEntity.class));
    }
}
