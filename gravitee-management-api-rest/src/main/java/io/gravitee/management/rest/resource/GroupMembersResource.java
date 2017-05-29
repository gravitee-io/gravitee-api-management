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
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.management.service.GroupService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class GroupMembersResource extends AbstractResource {

    @Inject
    private UserService userService;

    @Inject
    private GroupService groupService;

    @Inject
    private MembershipService membershipService;

    @GET
    @Produces(io.gravitee.common.http.MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List Group members")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of Group's members", response = MemberEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_GROUP, acls = RolePermissionAction.READ)
    })
    public List<MemberEntity> getMembers(@PathParam("group") String group) {
        GroupEntity groupEntity = groupService.findById(group);
        return membershipService.getMembers(
                groupEntity.getType().equals(GroupEntityType.API) ?
                        MembershipReferenceType.API_GROUP :
                        MembershipReferenceType.APPLICATION_GROUP,
                group).stream()
                .sorted(Comparator.comparing(MemberEntity::getUsername))
                .collect(Collectors.toList());
    }

    @POST
    @ApiOperation(value = "Add or update an Group member")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Member has been added or updated successfully"),
            @ApiResponse(code = 400, message = "Membership parameter is not valid"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_GROUP, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.MANAGEMENT_GROUP, acls = RolePermissionAction.UPDATE)
    })
    public Response addOrUpdateMember(
            @PathParam("group") String group,

            @ApiParam(name = "user", required = true)
            @NotNull @QueryParam("user") String username,

            @ApiParam(name = "rolename", required = true)
            @QueryParam("rolename") String roleName
    ) {

        GroupEntity groupEntity = groupService.findById(group);
        RoleScope roleScope = groupEntity.getType().equals(GroupEntityType.API) ? RoleScope.API : RoleScope.APPLICATION;

        if (roleName == null) {
            roleName = roleService.
                    findDefaultRoleByScopes(groupEntity.getType().equals(GroupEntityType.API) ? RoleScope.API : RoleScope.APPLICATION).
                    get(0).
                    getName();
        }

        MemberEntity memberEntity = membershipService.addOrUpdateMember(
                groupEntity.getType().equals(GroupEntityType.API) ?
                        MembershipReferenceType.API_GROUP :
                        MembershipReferenceType.APPLICATION_GROUP,
                group,
                username,
                roleScope,
                roleName.toUpperCase());
        return Response.created(URI.create("/groups/" + group + "/members/" + username)).entity(memberEntity).build();
    }

    @DELETE
    @ApiOperation(value = "Remove a group member")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Member has been removed successfully"),
            @ApiResponse(code = 400, message = "User does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_GROUP, acls = RolePermissionAction.DELETE)
    })
    public Response deleteMember(
            @PathParam("group") String group,
            @ApiParam(name = "user", required = true) @NotNull @QueryParam("user") String username) {
        GroupEntity groupEntity = groupService.findById(group);
        try {
            userService.findByName(username, false);
        } catch (UserNotFoundException unfe) {
            return Response.status(Response.Status.BAD_REQUEST).entity(unfe.getMessage()).build();
        }

        membershipService.deleteMember(
                groupEntity.getType().equals(GroupEntityType.API) ?
                        MembershipReferenceType.API_GROUP :
                        MembershipReferenceType.APPLICATION_GROUP,
                group,
                username);
        return Response.ok().build();
    }

}
