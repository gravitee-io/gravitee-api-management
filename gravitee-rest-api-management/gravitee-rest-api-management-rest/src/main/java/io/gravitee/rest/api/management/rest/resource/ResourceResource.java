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
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.platform.plugin.PluginEntity;
import io.gravitee.rest.api.service.ResourceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Plugins"})
public class ResourceResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ResourceService resourceService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a resource",
            notes = "User must have the MANAGEMENT_API[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Resource plugin", response = PluginEntity.class),
            @ApiResponse(code = 404, message = "Resource not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ)
    })
    public PluginEntity getResource(
            @PathParam("resource") String resource) {
        return resourceService.findById(resource);
    }

    @GET
    @Path("schema")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a resource's schema",
            notes = "User must have the MANAGEMENT_API[READ] permission to use this service")
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ)
    })
    public String getResourceSchema(
            @PathParam("resource") String resource) {
        // Check that the resource exists
        resourceService.findById(resource);

        return resourceService.getSchema(resource);
    }

    @GET
    @Path("documentation")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Get a resource's documentation",
        notes = "User must have the MANAGEMENT_API[READ] permission to use this service")
    @Permissions({
        @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ)
    })
    public String getResourceDoc(
        @PathParam("resource") String resource) {
        // Check that the policy exists
        resourceService.findById(resource);

        return resourceService.getDocumentation(resource);
    }

}
