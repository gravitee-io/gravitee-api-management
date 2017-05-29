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
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.UpdateRoleEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.RoleService;
import io.gravitee.repository.management.model.RoleScope;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

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
            @Permission(value = RolePermission.MANAGEMENT_ROLE, acls = RolePermissionAction.READ)
    })
    public RoleEntity get(@PathParam("scope")RoleScope scope,
                          @PathParam("role") String role) {

        return roleService.findById(scope, role);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_ROLE, acls = RolePermissionAction.UPDATE)
    })
    public RoleEntity update(@PathParam("scope")RoleScope scope,
                             @PathParam("role") String role,
                             @Valid @NotNull final UpdateRoleEntity entity) {

        return roleService.update(entity);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_ROLE, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("scope")RoleScope scope,
                       @PathParam("role") String role) {
        roleService.delete(scope, role);
    }

    @Path("/users")
    public RoleUsersResource getRoleUsersResource() {
        return resourceContext.getResource(RoleUsersResource.class);
    }
}
