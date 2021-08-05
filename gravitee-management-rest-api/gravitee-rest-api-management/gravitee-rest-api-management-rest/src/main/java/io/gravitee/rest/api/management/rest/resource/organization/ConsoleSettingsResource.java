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
package io.gravitee.rest.api.management.rest.resource.organization;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.settings.ConsoleSettingsEntity;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.MaintenanceModeException;
import io.swagger.annotations.*;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Defines the REST resources to manage Portal.
 *
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Settings" })
public class ConsoleSettingsResource {

    @Inject
    private ConfigService configService;

    @Inject
    private ParameterService parameterService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the console settings")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Console configuration", response = ConsoleSettingsEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SETTINGS, acls = READ) })
    public ConsoleSettingsEntity getConsoleSettings() {
        return configService.getConsoleSettings();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Save the console settings")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Updated console settings", response = ConsoleSettingsEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SETTINGS, acls = { CREATE, UPDATE, DELETE }) })
    public Response saveConsoleSettings(@ApiParam(name = "config", required = true) @NotNull ConsoleSettingsEntity consoleSettingsEntity) {
        // reject settings update if maintenanceMode isn't disabled into the payload
        checkMaintenanceMode(consoleSettingsEntity);

        configService.save(consoleSettingsEntity);
        return Response.ok().entity(consoleSettingsEntity).build();
    }

    private void checkMaintenanceMode(ConsoleSettingsEntity consoleSettingsEntity) {
        boolean maintenanceMode = parameterService.findAsBoolean(
            Key.MAINTENANCE_MODE_ENABLED,
            GraviteeContext.getCurrentOrganization(),
            ParameterReferenceType.ORGANIZATION
        );

        if (
            maintenanceMode &&
            (
                consoleSettingsEntity.getMaintenance() == null ||
                consoleSettingsEntity.getMaintenance().getEnabled() == null ||
                consoleSettingsEntity.getMaintenance().getEnabled()
            )
        ) {
            throw new MaintenanceModeException();
        }
    }
}
