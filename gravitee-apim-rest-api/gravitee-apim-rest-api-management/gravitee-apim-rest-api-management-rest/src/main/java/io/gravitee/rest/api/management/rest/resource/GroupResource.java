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

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;

import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.UpdateGroupEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.DELETE;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Groups")
public class GroupResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GroupService groupService;

    @PathParam("group")
    @Parameter(name = "group", required = true)
    private String group;

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Get a group")
    @ApiResponse(
        responseCode = "200",
        description = "Group definition",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = GroupEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.READ) })
    public GroupEntity getGroup() {
        return groupService.findById(GraviteeContext.getExecutionContext(), group);
    }

    @DELETE
    @Operation(summary = "Delete an existing group")
    @ApiResponse(responseCode = "204", description = "Group successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.DELETE) })
    public Response deleteGroup() {
        checkRights();
        groupService.delete(GraviteeContext.getExecutionContext(), group);
        return Response.noContent().build();
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Update an existing group")
    @ApiResponse(
        responseCode = "200",
        description = "Group successfully updated",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = GroupEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.UPDATE),
            @Permission(value = RolePermission.GROUP_MEMBER, acls = RolePermissionAction.UPDATE),
        }
    )
    public GroupEntity updateGroup(@Parameter(name = "group", required = true) @Valid @NotNull final UpdateGroupEntity updateGroupEntity) {
        final GroupEntity groupEntity = checkRights();
        // check if user is a 'simple group admin' or a platform admin
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            !permissionService.hasPermission(
                executionContext,
                RolePermission.ENVIRONMENT_GROUP,
                GraviteeContext.getCurrentEnvironment(),
                CREATE,
                UPDATE,
                DELETE
            )
        ) {
            updateGroupEntity.setMaxInvitation(groupEntity.getMaxInvitation());
            updateGroupEntity.setLockApiRole(groupEntity.isLockApiRole());
            updateGroupEntity.setLockApplicationRole(groupEntity.isLockApplicationRole());
            updateGroupEntity.setSystemInvitation(groupEntity.isSystemInvitation());
            updateGroupEntity.setEmailInvitation(groupEntity.isEmailInvitation());
            if (groupEntity.isLockApiRole()) {
                updateGroupEntity.getRoles().put(RoleScope.API, groupEntity.getRoles().get(RoleScope.API));
            }
            if (groupEntity.isLockApplicationRole()) {
                updateGroupEntity.getRoles().put(RoleScope.APPLICATION, groupEntity.getRoles().get(RoleScope.APPLICATION));
            }
        }
        return groupService.update(executionContext, group, updateGroupEntity);
    }

    @GET
    @Path("/memberships")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "List APIs or applications linked to this group")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.READ) })
    public Response getGroupMemberships(@QueryParam("type") String type) {
        if ("api".equalsIgnoreCase(type)) {
            return Response.ok(groupService.getApis(GraviteeContext.getCurrentEnvironment(), group)).build();
        } else if ("application".equalsIgnoreCase(type)) {
            return Response.ok(groupService.getApplications(group)).build();
        }

        return Response.noContent().build();
    }

    @POST
    @Path("/memberships")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Associate a group to existing APIs or Applications")
    @ApiResponse(
        responseCode = "200",
        description = "Group successfully updated",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = GroupEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.UPDATE) })
    public GroupEntity addGroupMember(@QueryParam("type") String type) {
        final GroupEntity groupEntity = checkRights();

        groupService.associate(GraviteeContext.getExecutionContext(), group, type);

        return groupEntity;
    }

    private GroupEntity checkRights() {
        final GroupEntity groupEntity = getGroup();
        if (!groupEntity.isManageable()) {
            throw new ForbiddenAccessException();
        }
        return groupEntity;
    }

    @Path("members")
    public GroupMembersResource groupMembersResource() {
        return resourceContext.getResource(GroupMembersResource.class);
    }

    @Path("invitations")
    public GroupInvitationsResource groupInvitationsResource() {
        return resourceContext.getResource(GroupInvitationsResource.class);
    }
}
