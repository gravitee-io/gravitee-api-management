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
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.InvitationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.GroupInvitationForbiddenException;
import io.gravitee.rest.api.service.exceptions.GroupMembersLimitationExceededException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.*;
import java.util.List;

import static io.gravitee.rest.api.model.InvitationReferenceType.GROUP;
import static io.gravitee.rest.api.model.permissions.RolePermission.GROUP_INVITATION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static io.gravitee.rest.api.service.exceptions.GroupInvitationForbiddenException.Type.EMAIL;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Group", "Invitations"})
public class GroupInvitationsResource extends AbstractResource {

    @Autowired
    private InvitationService invitationService;
    @Autowired
    private GroupService groupService;

    @GET
    @ApiOperation(value = "List configured invitations of a given group")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = GROUP_INVITATION, acls = READ)
    })
    public List<InvitationEntity> list(@PathParam("group") String group) {
        return invitationService.findByReference(GROUP, group);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.GROUP_INVITATION, acls = RolePermissionAction.CREATE)
    })
    public InvitationEntity create(@PathParam("group") String group, @Valid @NotNull final NewInvitationEntity invitationEntity) {
        // Check that group exists
        final GroupEntity groupEntity = groupService.findById(group);
        // check if user is a 'simple group admin' or a platform admin
        final boolean hasPermission = permissionService.hasPermission(RolePermission.ENVIRONMENT_GROUP, GraviteeContext.getCurrentEnvironment(), CREATE, UPDATE, DELETE);
        if (!hasPermission) {
            
            if (groupEntity.getMaxInvitation() != null &&
                    groupService.getNumberOfMembers(group) >= groupEntity.getMaxInvitation()) {
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.GROUP_INVITATION, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("group") String group, @PathParam("invitation") String invitation) {
        invitationService.delete(invitation, group);
    }
}
