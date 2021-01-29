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

import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.EnvironmentPermissionsEntity;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.service.EnvironmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api
public class EnvironmentsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EnvironmentService environmentService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List available environments for current user organization")
    public Collection<EnvironmentEntity> getEnvironments() {
        return this.environmentService.findByUser(getAuthenticatedUserOrNull());
    }

    @Path("{envId}")
    public EnvironmentResource getEnvironmentResource(
            @PathParam("envId") @ApiParam(name = "envId", required = true, defaultValue = "DEFAULT", value = "The ID of the environment") String envId
    ) {
        return resourceContext.getResource(EnvironmentResource.class);
    }

    @GET
    @Produces(io.gravitee.common.http.MediaType.APPLICATION_JSON)
    @Path("/permissions")
    @ApiOperation(value = "List available environments with their permissions for current user organization")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Current user permissions on its environments", response = char[].class, responseContainer = "Map"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Collection<EnvironmentPermissionsEntity> getEnvironmentsPermissions(
            @ApiParam("To filter on environment id or hrid") @QueryParam("idOrHrid") String id
    ) {
        List<EnvironmentEntity> environments = this.environmentService.findByUserAndIdOrHrid(getAuthenticatedUserOrNull(), id);

        return environments.stream().map(environment -> {
            Map<String, char[]> permissions = new HashMap<>();
            if (isAuthenticated()) {
                final String username = getAuthenticatedUser();
                if (isAdmin()) {
                    final char[] rights = new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()};
                    for (EnvironmentPermission perm : EnvironmentPermission.values()) {
                        permissions.put(perm.getName(), rights);
                    }
                } else {
                    permissions = membershipService.getUserMemberPermissions(environment, username);
                }
            }

            EnvironmentPermissionsEntity environmentPermissions = new EnvironmentPermissionsEntity();
            environmentPermissions.setId(environment.getId());
            environmentPermissions.setName(environment.getName());
            environmentPermissions.setHrids(environment.getHrids());
            environmentPermissions.setPermissions(permissions);

            return environmentPermissions;
        })
                .collect(Collectors.toList());
    }
}
