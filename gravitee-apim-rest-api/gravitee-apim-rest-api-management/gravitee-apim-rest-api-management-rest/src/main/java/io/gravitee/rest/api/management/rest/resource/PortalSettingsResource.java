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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
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
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Settings")
public class PortalSettingsResource {

    @Inject
    private ConfigService configService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the portal settings")
    @ApiResponse(
        responseCode = "200",
        description = "Portal configuration",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PortalSettingsEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = READ) })
    public PortalSettingsEntity getPortalSettings() {
        return configService.getPortalSettings(GraviteeContext.getExecutionContext());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Save the portal settings")
    @ApiResponse(
        responseCode = "200",
        description = "Updated portal settings",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PortalSettingsEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { CREATE, UPDATE, DELETE }) })
    public Response savePortalSettings(@Parameter(name = "config", required = true) @NotNull PortalSettingsEntity portalSettingsEntity) {
        configService.save(GraviteeContext.getExecutionContext(), portalSettingsEntity);
        return Response.ok().entity(portalSettingsEntity).build();
    }
}
