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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Roles")
public class RoleResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private RoleService roleService;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a role", description = "User must have the MANAGEMENT_ROLE[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "A role",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RoleEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "Role not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.READ) })
    public RoleEntity getRole(@PathParam("scope") RoleScope scope, @PathParam("role") String role) {
        Optional<RoleEntity> optRole = roleService.findByScopeAndName(scope, role);
        if (optRole.isPresent()) {
            return optRole.get();
        } else {
            throw new RoleNotFoundException(scope.name() + "_" + role);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a role", description = "User must have the MANAGEMENT_ROLE[UPDATE] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Role successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RoleEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "Role not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.UPDATE) })
    public RoleEntity updateRole(
        @PathParam("scope") RoleScope scope,
        @PathParam("role") String role,
        @Valid @NotNull final UpdateRoleEntity entity
    ) {
        return roleService.update(entity);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete a role", description = "User must have the MANAGEMENT_ROLE[DELETE] permission to use this service")
    @ApiResponse(
        responseCode = "204",
        description = "Role successfully deleted",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RoleEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "Role not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.DELETE) })
    public void deleteRole(@PathParam("scope") RoleScope scope, @PathParam("role") String role) {
        roleService.findByScopeAndName(scope, role).ifPresent(roleToDelete -> roleService.delete(roleToDelete.getId()));
    }

    @Path("/users")
    public RoleUsersResource getRoleUsersResource() {
        return resourceContext.getResource(RoleUsersResource.class);
    }
}
