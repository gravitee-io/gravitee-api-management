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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
public class PortalSettingsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "settings";
    }

    @BeforeEach
    public void init() {
        reset(configService, environmentService);
    }

    @Test
    public void shouldResetBrandedSendersAndReturnRefreshedInheritedSettings() {
        PortalSettingsEntity refreshed = new PortalSettingsEntity();
        refreshed.getEmail().setBrandedSendersInherited(true);
        when(configService.resetPortalBrandedSenders(any(ExecutionContext.class))).thenReturn(refreshed);

        final Response response = envTarget("/email/branded-senders/reset").request().post(Entity.json(""));
        response.bufferEntity();

        assertEquals(OK_200, response.getStatus(), response.readEntity(String.class));
        assertTrue(response.readEntity(PortalSettingsEntity.class).getEmail().isBrandedSendersInherited());
        verify(configService).resetPortalBrandedSenders(any(ExecutionContext.class));
    }

    @Test
    public void shouldReturn404WhenEnvironmentDoesNotExist() {
        when(environmentService.findById(any())).thenThrow(new EnvironmentNotFoundException("unknown-env"));

        final Response response = envTarget("/email/branded-senders/reset").request().post(Entity.json(""));

        assertEquals(NOT_FOUND_404, response.getStatus(), response.readEntity(String.class));
        verify(configService, never()).resetPortalBrandedSenders(any(ExecutionContext.class));
    }

    @Test
    public void shouldReturn403WhenMissingUpdatePermission() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

        final Response response = envTarget("/email/branded-senders/reset").request().post(Entity.json(""));

        assertEquals(FORBIDDEN_403, response.getStatus(), response.readEntity(String.class));
        verify(configService, never()).resetPortalBrandedSenders(any(ExecutionContext.class));
    }
}
