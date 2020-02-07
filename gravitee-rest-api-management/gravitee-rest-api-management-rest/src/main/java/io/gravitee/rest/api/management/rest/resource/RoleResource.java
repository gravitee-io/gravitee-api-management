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
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.swagger.annotations.Api;

import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

import java.util.Optional;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Roles"})
public class RoleResource extends AbstractResource  {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private RoleService roleService;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.READ)
    })
    public RoleEntity get(@PathParam("scope")RoleScope scope,
                          @PathParam("role") String role) {

        Optional<RoleEntity> optRole = roleService.findByScopeAndName(scope, role);
        if(optRole.isPresent()) {
            return optRole.get();
        } else {
            throw new RoleNotFoundException(scope.name() + "_" + role);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.UPDATE)
    })
    public RoleEntity update(@PathParam("scope")RoleScope scope,
                             @PathParam("role") String role,
                             @Valid @NotNull final UpdateRoleEntity entity) {

        return roleService.update(entity);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ORGANIZATION_ROLE, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("scope")RoleScope scope,
                       @PathParam("role") String role) {
        roleService.findByScopeAndName(scope, role).ifPresent(roleToDelete -> roleService.delete(roleToDelete.getId()));
    }

    @Path("/users")
    public RoleUsersResource getRoleUsersResource() {
        return resourceContext.getResource(RoleUsersResource.class);
    }
}
