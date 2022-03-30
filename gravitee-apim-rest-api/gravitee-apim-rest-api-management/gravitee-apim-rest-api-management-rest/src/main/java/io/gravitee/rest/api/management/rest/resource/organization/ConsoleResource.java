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
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.settings.ConsoleConfigEntity;
import io.gravitee.rest.api.model.settings.ConsoleSettingsEntity;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Console")
public class ConsoleResource {

    @Inject
    private ConfigService configService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the console configuration needed for runtime", description = "Every users can use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Console configuration",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ConsoleConfigEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ConsoleConfigEntity getConsoleConfig() {
        return configService.getConsoleConfig(GraviteeContext.getExecutionContext());
    }

    @Deprecated
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Save the console configuration")
    @ApiResponse(
        responseCode = "200",
        description = "Console configuration",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ConsoleSettingsEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_SETTINGS, acls = { CREATE, UPDATE, DELETE }) })
    public Response saveConsoleConfig(@Parameter(name = "config", required = true) @NotNull ConsoleSettingsEntity consoleSettingsEntity) {
        configService.save(GraviteeContext.getExecutionContext(), consoleSettingsEntity);
        return Response.ok().entity(consoleSettingsEntity).build();
    }
}
