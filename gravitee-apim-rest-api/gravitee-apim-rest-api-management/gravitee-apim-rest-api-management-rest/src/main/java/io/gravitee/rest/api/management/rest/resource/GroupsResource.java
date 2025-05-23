/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.rest.model.Pageable;
import io.gravitee.rest.api.management.rest.model.PagedResult;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.NewGroupEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Groups")
public class GroupsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GroupService groupService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Find groups",
        description = "Find all groups, or a specific type of groups." +
        "Only administrators could see all groups." +
        "Only users with MANAGE_API permissions could see API groups."
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of groups",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = GroupEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.READ) })
    public Response getGroups() {
        return Response.ok(groupService.findAll(GraviteeContext.getExecutionContext())).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Find paginated groups",
        description = "Find paginated groups based on a size and a page query params. Results can be filtered based on a searchTerm query param." +
        "Only administrators could see all groups." +
        "Only users with MANAGE_API permissions could see API groups."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Page containing the list of groups",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.READ) })
    @Path("/_paged")
    public Response getGroupsPaged(@Valid @BeanParam Pageable pageable, @QueryParam("query") String query) {
        Page<GroupEntity> page = groupService.search(GraviteeContext.getExecutionContext(), pageable.toPageable(), query);
        PagedResult<GroupEntity> pagedResult = new PagedResult<>(page, pageable.getSize());
        return Response.ok(pagedResult).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create group", description = "Create a new group.")
    @ApiResponse(
        responseCode = "201",
        description = "Group successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GroupEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.CREATE) })
    public Response createGroup(@Parameter(name = "group", required = true) @Valid @NotNull final NewGroupEntity newGroupEntity) {
        GroupEntity groupEntity = groupService.create(GraviteeContext.getExecutionContext(), newGroupEntity);
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
