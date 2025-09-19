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
package io.gravitee.rest.api.management.rest.resource.organization;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.settings.ConsoleSettingsEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.MaintenanceModeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * Defines the REST resources to manage Portal.
 *
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Settings")
public class ConsoleSettingsResource {

    @Inject
    private ConfigService configService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private ParameterService parameterService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the console settings")
    @ApiResponse(
        responseCode = "200",
        description = "Console configuration",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ConsoleSettingsEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SETTINGS, acls = READ) })
    public ConsoleSettingsEntity getConsoleSettings() {
        return configService.getConsoleSettings(GraviteeContext.getExecutionContext());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Save the console settings")
    @ApiResponse(
        responseCode = "200",
        description = "Updated console settings",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ConsoleSettingsEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SETTINGS, acls = { CREATE, UPDATE, DELETE }) })
    public Response saveConsoleSettings(
        @Parameter(name = "config", required = true) @NotNull @Valid ConsoleSettingsEntity consoleSettingsEntity
    ) {
        // reject settings update if maintenanceMode isn't disabled into the payload
        checkMaintenanceMode(consoleSettingsEntity);

        // check that organization exists
        organizationService.findById(GraviteeContext.getCurrentOrganization());

        configService.save(GraviteeContext.getExecutionContext(), consoleSettingsEntity);
        return Response.ok().entity(consoleSettingsEntity).build();
    }

    private void checkMaintenanceMode(ConsoleSettingsEntity consoleSettingsEntity) {
        boolean maintenanceMode = parameterService.findAsBoolean(
            GraviteeContext.getExecutionContext(),
            Key.MAINTENANCE_MODE_ENABLED,
            GraviteeContext.getCurrentOrganization(),
            ParameterReferenceType.ORGANIZATION
        );

        if (
            maintenanceMode &&
            (consoleSettingsEntity.getMaintenance() == null ||
                consoleSettingsEntity.getMaintenance().getEnabled() == null ||
                consoleSettingsEntity.getMaintenance().getEnabled())
        ) {
            throw new MaintenanceModeException();
        }
    }
}
