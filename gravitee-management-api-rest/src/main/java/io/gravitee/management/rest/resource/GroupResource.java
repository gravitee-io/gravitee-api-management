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
import io.gravitee.management.model.UpdateGroupEntity;
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
import java.util.List;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */

@Api(tags = {"GROUP"})
public class GroupResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GroupService groupService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a group of Apis or Applications")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Group definition", response = GroupEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public GroupEntity get(@PathParam("group") String group) {
        return groupService.findById(group);
    }

    @DELETE
    @ApiOperation(value = "Delete the Group")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Group successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response delete(@PathParam("group") String group) {
        groupService.delete(group);
        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a group")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Group successfully updated", response = GroupEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public GroupEntity update(
            @PathParam("group")String group,
            @ApiParam(name = "group", required = true)@Valid @NotNull final UpdateGroupEntity updateGroupEntity) {
        return groupService.update(group, updateGroupEntity);
    }

    @Path("members")
    public GroupMembersResource groupMembersResource() {
        return resourceContext.getResource(GroupMembersResource.class);
    }

}
