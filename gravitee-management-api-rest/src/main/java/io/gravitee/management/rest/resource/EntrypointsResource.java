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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.ApplicationMetadataEntity;
import io.gravitee.management.model.EntrypointEntity;
import io.gravitee.management.model.NewEntryPointEntity;
import io.gravitee.management.model.UpdateEntryPointEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.EntrypointService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
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
            @Permission(value = RolePermission.MANAGEMENT_ENTRYPOINT, acls = RolePermissionAction.READ)
    })
    public EntrypointEntity get(final @PathParam("entrypoint") String entrypointId)  {
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
            @Permission(value = RolePermission.MANAGEMENT_ENTRYPOINT, acls = RolePermissionAction.READ)
    })
    public List<EntrypointEntity> list()  {
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
            @Permission(value = RolePermission.MANAGEMENT_ENTRYPOINT, acls = RolePermissionAction.CREATE)
    })
    public EntrypointEntity create(@Valid @NotNull final NewEntryPointEntity entrypoint) {
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
            @Permission(value = RolePermission.MANAGEMENT_ENTRYPOINT, acls = RolePermissionAction.UPDATE)
    })
    public EntrypointEntity update(@Valid @NotNull final UpdateEntryPointEntity entrypoint) {
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
            @Permission(value = RolePermission.MANAGEMENT_ENTRYPOINT, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("entrypoint") String entrypoint) {
        entrypointService.delete(entrypoint);
    }
}
