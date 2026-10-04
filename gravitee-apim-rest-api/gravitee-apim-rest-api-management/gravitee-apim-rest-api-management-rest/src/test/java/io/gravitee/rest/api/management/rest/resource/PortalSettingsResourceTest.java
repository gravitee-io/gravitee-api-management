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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.rest.api.model.settings.BrandedSenderConfig;
import io.gravitee.rest.api.model.settings.Email;
import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
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

    @Test
    public void shouldLetTheServiceDecideOnSenderValuesWhenSaving() {
        // The console sends the whole settings object back, including sender values locked by gravitee.yml or stored
        // before the sender checks got stricter. The request-level validation must not reject them, or no setting on
        // the page can be saved; the service validates only the sender values that are actually changed.
        PortalSettingsEntity settings = new PortalSettingsEntity();
        settings.getEmail().setFrom("\"user@my.domain\"");
        settings
            .getEmail()
            .setBrandedSenders(
                List.of(BrandedSenderConfig.builder().domains(List.of("example.com")).from("Example, Inc <noreply@example.com>").build())
            );

        final Response response = envTarget().request().post(Entity.json(settings));

        assertEquals(OK_200, response.getStatus(), response.readEntity(String.class));
        verify(configService).save(any(ExecutionContext.class), any(PortalSettingsEntity.class));
    }

    @Test
    public void shouldReturn400WhenTheServiceRejectsAnEditedSender() {
        var violations = Validation.buildDefaultValidatorFactory().getValidator().validateValue(Email.class, "from", "not-an-email");
        doThrow(new ConstraintViolationException(violations))
            .when(configService)
            .save(any(ExecutionContext.class), any(PortalSettingsEntity.class));

        final Response response = envTarget().request().post(Entity.json(new PortalSettingsEntity()));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        assertEquals(
            "must be a valid email address, optionally with a display name (e.g. \"Name <user@example.com>\")",
            response.readEntity(JsonNode.class).get("message").asText()
        );
    }
}
