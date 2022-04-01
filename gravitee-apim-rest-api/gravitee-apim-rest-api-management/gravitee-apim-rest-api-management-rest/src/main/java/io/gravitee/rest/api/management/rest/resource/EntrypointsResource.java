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

import static java.util.stream.Collectors.toList;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.EntrypointEntity;
import io.gravitee.rest.api.model.NewEntryPointEntity;
import io.gravitee.rest.api.model.UpdateEntryPointEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EntrypointService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Entrypoints")
public class EntrypointsResource extends AbstractResource {

    @Inject
    private EntrypointService entrypointService;

    @GET
    @Path("{entrypoint}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get a platform entrypoints",
        description = "User must have the MANAGEMENT_ENTRYPOINT[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "A platform entrypoint",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EntrypointEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "Entrypoint not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.READ),
            @Permission(value = RolePermission.ORGANIZATION_ENTRYPOINT, acls = RolePermissionAction.READ),
        }
    )
    public EntrypointEntity getEntrypoint(final @PathParam("entrypoint") String entrypointId) {
        return entrypointService.findById(GraviteeContext.getExecutionContext(), entrypointId);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List the platform entrypoints",
        description = "User must have the MANAGEMENT_ENTRYPOINT[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of platform entrypoints",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = EntrypointEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.READ),
            @Permission(value = RolePermission.ORGANIZATION_ENTRYPOINT, acls = RolePermissionAction.READ),
        }
    )
    public List<EntrypointEntity> getEntrypoints() {
        return entrypointService
            .findAll(GraviteeContext.getExecutionContext())
            .stream()
            .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getValue(), o2.getValue()))
            .collect(toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create a platform entrypoint",
        description = "User must have the MANAGEMENT_ENTRYPOINT[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Entrypoint successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EntrypointEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.ORGANIZATION_ENTRYPOINT, acls = RolePermissionAction.CREATE),
        }
    )
    public EntrypointEntity createEntrypoint(@Valid @NotNull final NewEntryPointEntity entrypoint) {
        return entrypointService.create(GraviteeContext.getExecutionContext(), entrypoint);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update a platform entrypoint",
        description = "User must have the MANAGEMENT_ENTRYPOINT[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Updated entrypoint",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EntrypointEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.UPDATE),
            @Permission(value = RolePermission.ORGANIZATION_ENTRYPOINT, acls = RolePermissionAction.UPDATE),
        }
    )
    public EntrypointEntity updateEntrypoint(@Valid @NotNull final UpdateEntryPointEntity entrypoint) {
        return entrypointService.update(GraviteeContext.getExecutionContext(), entrypoint);
    }

    @Path("{entrypoint}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete a platform entrypoint",
        description = "User must have the MANAGEMENT_ENTRYPOINT[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Entrrypoint successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.DELETE),
            @Permission(value = RolePermission.ORGANIZATION_ENTRYPOINT, acls = RolePermissionAction.DELETE),
        }
    )
    public void deleteEntrypoint(@PathParam("entrypoint") String entrypoint) {
        entrypointService.delete(GraviteeContext.getExecutionContext(), entrypoint);
    }
}
