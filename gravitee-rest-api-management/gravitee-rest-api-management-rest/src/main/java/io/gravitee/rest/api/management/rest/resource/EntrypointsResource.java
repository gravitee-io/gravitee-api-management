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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.EntrypointEntity;
import io.gravitee.rest.api.model.NewEntryPointEntity;
import io.gravitee.rest.api.model.UpdateEntryPointEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EntrypointService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Entrypoints"})
public class EntrypointsResource extends AbstractResource  {

    @Inject
    private EntrypointService entrypointService;

    @GET
    @Path("{entrypoint}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a platform entrypoints",
            notes = "User must have the MANAGEMENT_ENTRYPOINT[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "A platform entrypoint", response = EntrypointEntity.class),
            @ApiResponse(code = 404, message = "Entrypoint not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.READ)
    })
    public EntrypointEntity getEntrypoint(final @PathParam("entrypoint") String entrypointId)  {
        return entrypointService.findById(entrypointId);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List the platform entrypoints",
            notes = "User must have the MANAGEMENT_ENTRYPOINT[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of platform entrypoints", response = EntrypointEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.READ)
    })
    public List<EntrypointEntity> getEntrypoints()  {
        return entrypointService.findAll()
                .stream()
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getValue(), o2.getValue()))
                .collect(toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a platform entrypoint",
            notes = "User must have the MANAGEMENT_ENTRYPOINT[CREATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Entrypoint successfully created", response = EntrypointEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.CREATE)
    })
    public EntrypointEntity createEntrypoint(@Valid @NotNull final NewEntryPointEntity entrypoint) {
        return entrypointService.create(entrypoint);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a platform entrypoint",
            notes = "User must have the MANAGEMENT_ENTRYPOINT[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Updated entrypoint", response = EntrypointEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.UPDATE)
    })
    public EntrypointEntity updateEntrypoint(@Valid @NotNull final UpdateEntryPointEntity entrypoint) {
        return entrypointService.update(entrypoint);
    }

    @Path("{entrypoint}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a platform entrypoint",
            notes = "User must have the MANAGEMENT_ENTRYPOINT[DELETE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Entrrypoint successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ENTRYPOINT, acls = RolePermissionAction.DELETE)
    })
    public void deleteEntrypoint(@PathParam("entrypoint") String entrypoint) {
        entrypointService.delete(entrypoint);
    }
}
