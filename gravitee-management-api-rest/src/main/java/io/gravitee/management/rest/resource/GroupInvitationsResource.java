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
import io.gravitee.management.model.GroupEntity;
import io.gravitee.management.model.InvitationEntity;
import io.gravitee.management.model.NewInvitationEntity;
import io.gravitee.management.model.UpdateInvitationEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.GroupService;
import io.gravitee.management.service.InvitationService;
import io.gravitee.management.service.exceptions.GroupInvitationForbiddenException;
import io.gravitee.management.service.exceptions.GroupMembersLimitationExceededException;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.*;
import java.util.List;

import static io.gravitee.management.model.InvitationReferenceType.GROUP;
import static io.gravitee.management.model.permissions.RolePermission.GROUP_INVITATION;
import static io.gravitee.management.model.permissions.RolePermissionAction.*;
import static io.gravitee.management.service.exceptions.GroupInvitationForbiddenException.Type.EMAIL;
import static io.gravitee.repository.management.model.RoleScope.API;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Group Invitations"})
public class GroupInvitationsResource extends AbstractResource {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private GroupService groupService;

    @GET
    @ApiOperation(value = "List existing invitations of a group",
            notes = "User must have the GROUP_INVITATION[READ] permission to use this service")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_GROUP, acls = {READ, CREATE, UPDATE, DELETE}),
            @Permission(value = GROUP_INVITATION, acls = READ)
    })
    public List<InvitationEntity> list(@PathParam("group") String group) {
        return invitationService.findByReference(GROUP, group);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create an invitation to join a group",
            notes = "User must have the GROUP_INVITATION[CREATE] permission to use this service")
    @Permissions({
            @Permission(value = RolePermission.GROUP_INVITATION, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.MANAGEMENT_GROUP, acls = {UPDATE, CREATE})
    })
    public InvitationEntity create(@PathParam("group") String group, @Valid @NotNull final NewInvitationEntity invitationEntity) {
        // Check that group exists
        final GroupEntity groupEntity = groupService.findById(group);
        // check if user is a 'simple group admin' or a platform admin
        final boolean hasPermission = permissionService.hasPermission(RolePermission.MANAGEMENT_GROUP, null, CREATE, UPDATE, DELETE);
        if (!hasPermission) {
            if (groupEntity.getMaxInvitation() != null &&
                membershipService.getNumberOfMembers(MembershipReferenceType.GROUP, group, API) >= groupEntity.getMaxInvitation()) {
                    throw new GroupMembersLimitationExceededException(groupEntity.getMaxInvitation());
            }
            if (!groupEntity.isEmailInvitation()) {
                throw new GroupInvitationForbiddenException(EMAIL, group);
            }
        }

        invitationEntity.setReferenceType(GROUP);
        invitationEntity.setReferenceId(group);
        return invitationService.create(invitationEntity);
    }

    @Path("{invitation}")
    @PUT
    @ApiOperation(value = "Update an invitation to join a group",
            notes = "User must have the GROUP_INVITATION[UPDATE] permission to use this service")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_GROUP, acls = {UPDATE, CREATE}),
            @Permission(value = RolePermission.GROUP_INVITATION, acls = RolePermissionAction.UPDATE)
    })
    public InvitationEntity update(@PathParam("group") String group, @PathParam("invitation") String invitation,
                                   @Valid @NotNull final UpdateInvitationEntity invitationEntity) {
        invitationEntity.setId(invitation);
        invitationEntity.setReferenceType(GROUP);
        invitationEntity.setReferenceId(group);
        return invitationService.update(invitationEntity);
    }

    @Path("{invitation}")
    @DELETE
    @ApiOperation(value = "Delete an invitation to join a group",
            notes = "User must have the GROUP_INVITATION[DELETE] permission to use this service")
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_GROUP, acls = {UPDATE, CREATE}),
            @Permission(value = RolePermission.GROUP_INVITATION, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("group") String group, @PathParam("invitation") String invitation) {
        invitationService.delete(invitation, group);
    }
}
