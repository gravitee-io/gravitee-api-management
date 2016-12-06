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

import io.gravitee.management.model.GroupEntity;
import io.gravitee.management.model.GroupEntityType;
import io.gravitee.management.model.NewGroupEntity;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.GroupService;
import io.swagger.annotations.*;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Api(tags = {"Group"})
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
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of groups", response = GroupEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response findByOptionnalType(
            @ApiParam(name = "type") @QueryParam("type") @Nullable GroupEntityType type) {
        if( isAdmin() || (type != null && type.equals(GroupEntityType.API))) {
            if (type == null) {
                return Response
                        .ok(groupService.findAll())
                        .build();
            } else {
                return Response
                        .ok(groupService.findByType(type))
                        .build();
            }
        }
        return Response
                .status(Response.Status.FORBIDDEN)
                .entity("Only administrators are allowed to do get groups.")
                .build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Create group",
            notes = "Create a new group."
    )
    @ApiResponses({
            @ApiResponse(code = 201, message = "Group successfully created"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public Response createGroup(
            @ApiParam(name = "group", required = true)
            @Valid @NotNull final NewGroupEntity newGroupEntity) {
        GroupEntity groupEntity = groupService.create(newGroupEntity);
        if (groupEntity != null) {
            return Response
                    .created(URI.create("/groups/" + groupEntity.getId()))
                    .entity(groupEntity)
                    .build();
        }

        return Response.serverError().build();
    }

    @Path("{group}")
    public GroupResource groupResource() {
        return resourceContext.getResource(GroupResource.class);
    }
}
