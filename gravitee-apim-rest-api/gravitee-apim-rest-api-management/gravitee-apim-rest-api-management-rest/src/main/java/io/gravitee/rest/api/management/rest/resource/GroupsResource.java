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

import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.NewGroupEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.GroupService;
import io.swagger.annotations.*;
import java.net.URI;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Groups" })
public class GroupsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GroupService groupService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Find groups",
        notes = "Find all groups, or a specific type of groups." +
        "Only administrators could see all groups." +
        "Only users with MANAGE_API permissions could see API groups."
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "List of groups", response = GroupEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.READ) })
    public Response getGroups() {
        return Response.ok(groupService.findAll()).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create group", notes = "Create a new group.")
    @ApiResponses(
        { @ApiResponse(code = 201, message = "Group successfully created"), @ApiResponse(code = 500, message = "Internal Server Error") }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.CREATE) })
    public Response createGroup(@ApiParam(name = "group", required = true) @Valid @NotNull final NewGroupEntity newGroupEntity) {
        GroupEntity groupEntity = groupService.create(newGroupEntity);
        if (groupEntity != null) {
            return Response.created(this.getLocationHeader(groupEntity.getId())).entity(groupEntity).build();
        }

        return Response.serverError().build();
    }

    @Path("{group}")
    public GroupResource groupResource() {
        return resourceContext.getResource(GroupResource.class);
    }
}
