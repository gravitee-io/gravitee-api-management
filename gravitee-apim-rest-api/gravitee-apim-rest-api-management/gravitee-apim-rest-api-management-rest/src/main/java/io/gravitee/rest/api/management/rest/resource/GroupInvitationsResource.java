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

import static io.gravitee.rest.api.model.InvitationReferenceType.GROUP;
import static io.gravitee.rest.api.model.permissions.RolePermission.GROUP_INVITATION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static io.gravitee.rest.api.service.exceptions.GroupInvitationForbiddenException.Type.EMAIL;

import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.InvitationEntity;
import io.gravitee.rest.api.model.NewInvitationEntity;
import io.gravitee.rest.api.model.UpdateInvitationEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.InvitationService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.GroupInvitationForbiddenException;
import io.gravitee.rest.api.service.exceptions.GroupMembersLimitationExceededException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Group Invitations")
public class GroupInvitationsResource extends AbstractResource {

    @Inject
    private InvitationService invitationService;

    @Inject
    private GroupService groupService;

    @Inject
    private UserService userService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("group")
    @Parameter(name = "group", hidden = true)
    private String group;

    @GET
    @Operation(
        summary = "List existing invitations of a group",
        description = "User must have the GROUP_INVITATION[READ] permission to use this service"
    )
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = { READ, CREATE, UPDATE, DELETE }),
            @Permission(value = GROUP_INVITATION, acls = READ),
        }
    )
    public List<InvitationEntity> getGroupInvitations() {
        return invitationService.findByReference(GROUP, group);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create an invitation to join a group",
        description = "User must have the GROUP_INVITATION[CREATE] permission to use this service"
    )
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = { UPDATE, CREATE }),
            @Permission(value = RolePermission.GROUP_INVITATION, acls = RolePermissionAction.CREATE),
        }
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Invitation sent successfully",
                content = @Content(schema = @Schema(implementation = InvitationEntity.class))
            ),
            @ApiResponse(
                responseCode = "202",
                description = "Request is accepted and list of users to select and invite sent successfully",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    array = @ArraySchema(schema = @Schema(implementation = UserEntity.class))
                )
            ),
        }
    )
    public Response createGroupInvitation(@Valid @NotNull final NewInvitationEntity invitationEntity) {
        // Check that group exists
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GroupEntity groupEntity = groupService.findById(executionContext, group);
        // check if user is a 'simple group admin' or a platform admin
        final boolean hasPermission = permissionService.hasPermission(
            executionContext,
            RolePermission.ENVIRONMENT_GROUP,
            GraviteeContext.getCurrentEnvironment(),
            CREATE,
            UPDATE,
            DELETE
        );
        if (!hasPermission) {
            if (
                groupEntity.getMaxInvitation() != null &&
                groupService.getNumberOfMembers(executionContext, group) >= groupEntity.getMaxInvitation()
            ) {
                throw new GroupMembersLimitationExceededException(groupEntity.getMaxInvitation());
            }
            if (!groupEntity.isEmailInvitation()) {
                throw new GroupInvitationForbiddenException(EMAIL, group);
            }
        }

        // If there are multiple users holding the same email id, we send back the list of users to select one and send an invitation
        List<UserEntity> userEntities = userService.findByEmail(executionContext, invitationEntity.getEmail());

        if (CollectionUtils.isNotEmpty(userEntities) && userEntities.size() > 1) {
            return Response.accepted(userEntities).build();
        }

        invitationEntity.setReferenceType(GROUP);
        invitationEntity.setReferenceId(group);
        return Response.ok(invitationService.create(executionContext, invitationEntity)).build();
    }

    @Path("{invitation}")
    @PUT
    @Operation(
        summary = "Update an invitation to join a group",
        description = "User must have the GROUP_INVITATION[UPDATE] permission to use this service"
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = { UPDATE, CREATE }),
            @Permission(value = RolePermission.GROUP_INVITATION, acls = RolePermissionAction.UPDATE),
        }
    )
    public InvitationEntity updateGroupInvitation(
        @PathParam("invitation") String invitation,
        @Valid @NotNull final UpdateInvitationEntity invitationEntity
    ) {
        invitationEntity.setId(invitation);
        invitationEntity.setReferenceType(GROUP);
        invitationEntity.setReferenceId(group);
        return invitationService.update(invitationEntity);
    }

    @Path("{invitation}")
    @DELETE
    @Operation(
        summary = "Delete an invitation to join a group",
        description = "User must have the GROUP_INVITATION[DELETE] permission to use this service"
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions(
        {
            @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = { UPDATE, CREATE }),
            @Permission(value = RolePermission.GROUP_INVITATION, acls = RolePermissionAction.DELETE),
        }
    )
    public void deleteGroupInvitation(@PathParam("invitation") String invitation) {
        invitationService.delete(invitation, group);
    }
}
