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
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.MemberRoleEntity;
import io.gravitee.management.model.GroupMemberEntity;
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.GroupService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Api(tags = {"Group"})
public class GroupMemberResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;

    @Inject
    private GroupService groupService;

    @Inject
    private MembershipService membershipService;

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add or update an Group member")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Member has been added"),
            @ApiResponse(code = 200, message = "Member has been updated"),
            @ApiResponse(code = 400, message = "Membership parameter is not valid"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_GROUP, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.MANAGEMENT_GROUP, acls = RolePermissionAction.UPDATE)
    })
    public Response addOrUpdateMember(
            @PathParam("group") String group,

            @PathParam("member") String member,

            @ApiParam(name = "roles")
            List<MemberRoleEntity> roles
    ) {
        //check that group exists
        groupService.findById(group);

        RoleEntity previousApiRole = membershipService.getRole(
                MembershipReferenceType.GROUP,
                group,
                member,
                RoleScope.API);

        RoleEntity previousApplicationRole = membershipService.getRole(
                MembershipReferenceType.GROUP,
                group,
                member,
                RoleScope.APPLICATION);


        // process add/update before delete
        // to avoid to have a user without roles

        if (roles != null && !roles.isEmpty()) {
            MemberRoleEntity apiRole = roles.
                    stream().
                    filter(r -> r.getRoleScope().equals(io.gravitee.management.model.permissions.RoleScope.API)
                            && !r.getRoleName().isEmpty()).
                    findFirst().
                    orElse(null);

            MemberRoleEntity applicationRole = roles.
                    stream().
                    filter(r -> r.getRoleScope().equals(io.gravitee.management.model.permissions.RoleScope.APPLICATION)
                            && !r.getRoleName().isEmpty()).
                    findFirst().
                    orElse(null);

            //Add/Update
            if (apiRole != null) {
                membershipService.addOrUpdateMember(
                        MembershipReferenceType.GROUP,
                        group,
                        member,
                        RoleScope.API,
                        apiRole.getRoleName());
            }
            if (applicationRole != null) {
                membershipService.addOrUpdateMember(
                        MembershipReferenceType.GROUP,
                        group,
                        member,
                        RoleScope.APPLICATION,
                        applicationRole.getRoleName());
            }

            //delete
            if (apiRole == null && previousApiRole != null) {
                membershipService.removeRole(
                        MembershipReferenceType.GROUP,
                        group,
                        member,
                        RoleScope.API);
            }
            if (applicationRole == null && previousApplicationRole != null) {
                membershipService.removeRole(
                        MembershipReferenceType.GROUP,
                        group,
                        member,
                        RoleScope.APPLICATION);
            }
        }

        return Response.ok().build();
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
    public Response deleteMember( @PathParam("group") String group, @PathParam("member") String member) {
        // check group exists
        groupService.findById(group);
        try {
            userService.findByName(member, false);
        } catch (UserNotFoundException unfe) {
            return Response.status(Response.Status.BAD_REQUEST).entity(unfe.getMessage()).build();
        }

        membershipService.deleteMember(MembershipReferenceType.GROUP, group, member);
        return Response.ok().build();
    }

    @Path("{user}")
    public GroupResource groupResource() {
        return resourceContext.getResource(GroupResource.class);
    }

    private GroupMemberEntity convert(MemberEntity memberEntity, Map<String, String> roles) {
        GroupMemberEntity entity = new GroupMemberEntity(memberEntity);
        entity.setRoles(roles);
        return entity;
    }
}
