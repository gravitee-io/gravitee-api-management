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
import io.gravitee.rest.api.management.rest.model.GroupMembership;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.GroupInvitationForbiddenException;
import io.gravitee.rest.api.service.exceptions.GroupMembersLimitationExceededException;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_GROUP;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static io.gravitee.rest.api.service.exceptions.GroupInvitationForbiddenException.Type.SYSTEM;
import static java.util.stream.Collectors.toList;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Api(tags = {"Group Memberships"})
public class GroupMembersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;
    @Inject
    private GroupService groupService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("group")
    @ApiParam(name = "group", hidden = true)
    private String group;

    @GET
    @Produces(io.gravitee.common.http.MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List group members")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of group's members", response = MemberEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = ENVIRONMENT_GROUP, acls = RolePermissionAction.READ),
            @Permission(value = RolePermission.GROUP_MEMBER, acls = RolePermissionAction.READ)
    })
    public List<GroupMemberEntity> getGroupMembers() {
        //check that group exists
        groupService.findById(group);

        Map<String, List<MemberEntity>> members = membershipService.
                getMembersByReference(MembershipReferenceType.GROUP, group).
                stream().
                filter(Objects::nonNull).
                collect(Collectors.groupingBy(MemberEntity::getId));

        return members.keySet().stream().
                map(id -> {
                    Map<String, String> roles = members.get(id).stream()
                        .flatMap(m-> m.getRoles().stream())
                        .collect(Collectors.toMap(role -> role.getScope().name(), RoleEntity::getName));

                    GroupMemberEntity groupMemberEntity = new GroupMemberEntity(members.get(id).get(0));
                    groupMemberEntity.setRoles(roles);
                    return groupMemberEntity;
                }).
                sorted(Comparator.comparing(GroupMemberEntity::getId)).
                collect(toList());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add or update a group member")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Member has been added"),
            @ApiResponse(code = 200, message = "Member has been updated"),
            @ApiResponse(code = 400, message = "Membership is not valid"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Permissions({
            @Permission(value = ENVIRONMENT_GROUP, acls = RolePermissionAction.CREATE),
            @Permission(value = ENVIRONMENT_GROUP, acls = RolePermissionAction.UPDATE),
            @Permission(value = RolePermission.GROUP_MEMBER, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.GROUP_MEMBER, acls = RolePermissionAction.UPDATE),
    })
    public Response addOrUpdateGroupMember(
            @Valid @NotNull final List<GroupMembership> memberships
    ) {
        // Check that group exists
        final GroupEntity groupEntity = groupService.findById(group);

        // check if user is a 'simple group admin' or a platform admin
        final boolean hasPermission = permissionService.hasPermission(ENVIRONMENT_GROUP, GraviteeContext.getCurrentEnvironment(), CREATE, UPDATE, DELETE);
        if (!hasPermission) {
            if (groupEntity.getMaxInvitation() != null) {
                final Set<MemberEntity> members = membershipService.getMembersByReference(MembershipReferenceType.GROUP, group);
                final long membershipsToAddSize = memberships.stream().map(GroupMembership::getId).filter(s -> {
                    final List<String> membershipIdsToSave = members.stream().map(MemberEntity::getId).collect(toList());
                    return !membershipIdsToSave.contains(s);
                }).count();
                if ((groupService.getNumberOfMembers(group) + membershipsToAddSize) > groupEntity.getMaxInvitation()) {
                    throw new GroupMembersLimitationExceededException(groupEntity.getMaxInvitation());
                }
            }
            if (!groupEntity.isSystemInvitation()) {
                throw new GroupInvitationForbiddenException(SYSTEM, group);
            }
        }

        for (GroupMembership membership : memberships) {
            RoleEntity previousApiRole = null;
            RoleEntity previousApplicationRole = null;
            RoleEntity previousGroupRole = null;

            if (membership.getId() != null) {
                Set<RoleEntity> userRoles = membershipService.getRoles(MembershipReferenceType.GROUP, group, MembershipMemberType.USER, membership.getId());
                for(RoleEntity role: userRoles) {
                    switch (role.getScope()) {
                        case API:
                            previousApiRole = role;
                            break;
                        case APPLICATION:
                            previousApplicationRole = role;
                            break;
                        case GROUP:
                            previousGroupRole = role;
                            break;
                        default:
                            break;
                    }
                }
            }

            // Process add / update before delete to avoid having a user without role
            if (membership.getRoles() != null && !membership.getRoles().isEmpty()) {
                Map<RoleScope, RoleEntity> roleEntities = new HashMap<>();
                for(MemberRoleEntity item : membership.getRoles()) {
                    roleService.findByScopeAndName(item.getRoleScope(), item.getRoleName()).ifPresent(roleEntity -> roleEntities.put(item.getRoleScope(), roleEntity));
                }
                
                MemberEntity updatedMembership = null;

                // Replace if new role to add
                RoleEntity apiRoleEntity = roleEntities.get(RoleScope.API);
                if (apiRoleEntity != null && !apiRoleEntity.equals(previousApiRole)) {
                    String roleName = apiRoleEntity.getName();
                    if (!hasPermission && groupEntity.isLockApiRole()) {
                        final List<RoleEntity> defaultRoles = roleService.findDefaultRoleByScopes(RoleScope.API);
                        if (defaultRoles != null && !defaultRoles.isEmpty()) {
                            roleName = defaultRoles.get(0).getName();
                        }
                    }
                    updatedMembership = membershipService.addRoleToMemberOnReference(
                            new MembershipService.MembershipReference(MembershipReferenceType.GROUP, group),
                            new MembershipService.MembershipMember(membership.getId(), membership.getReference(), MembershipMemberType.USER),
                            new MembershipService.MembershipRole(RoleScope.API, roleName));
                    if (previousApiRole != null) {
                        membershipService.removeRole(
                                MembershipReferenceType.GROUP,
                                group,
                                MembershipMemberType.USER,
                                updatedMembership.getId(),
                                previousApiRole.getId());
                    }

                }
                
                RoleEntity applicationRoleEntity = roleEntities.get(RoleScope.APPLICATION);
                if (applicationRoleEntity != null && !applicationRoleEntity.equals(previousApplicationRole)) {
                    String roleName = applicationRoleEntity.getName();
                    if (!hasPermission && groupEntity.isLockApplicationRole()) {
                        final List<RoleEntity> defaultRoles = roleService.findDefaultRoleByScopes(RoleScope.APPLICATION);
                        if (defaultRoles != null && !defaultRoles.isEmpty()) {
                            roleName = defaultRoles.get(0).getName();
                        }
                    }
                    updatedMembership = membershipService.addRoleToMemberOnReference(
                            new MembershipService.MembershipReference(MembershipReferenceType.GROUP, group),
                            new MembershipService.MembershipMember(membership.getId(), membership.getReference(), MembershipMemberType.USER),
                            new MembershipService.MembershipRole(RoleScope.APPLICATION, roleName));
                    if (previousApplicationRole != null) {
                        membershipService.removeRole(
                                MembershipReferenceType.GROUP,
                                group,
                                MembershipMemberType.USER,
                                updatedMembership.getId(),
                                previousApplicationRole.getId());
                    }

                }
                RoleEntity groupRoleEntity = roleEntities.get(RoleScope.GROUP);
                if (groupRoleEntity != null && !groupRoleEntity.equals(previousGroupRole)) {
                    updatedMembership = membershipService.addRoleToMemberOnReference(
                            new MembershipService.MembershipReference(MembershipReferenceType.GROUP, group),
                            new MembershipService.MembershipMember(membership.getId(), membership.getReference(), MembershipMemberType.USER),
                            new MembershipService.MembershipRole(RoleScope.GROUP, groupRoleEntity.getName()));
                    if (previousGroupRole != null) {
                        membershipService.removeRole(
                                MembershipReferenceType.GROUP,
                                group,
                                MembershipMemberType.USER,
                                updatedMembership.getId(),
                                previousGroupRole.getId());
                    }
                }

                // Delete if existing and new role is empty
                if (apiRoleEntity == null && previousApiRole != null) {
                    membershipService.removeRole(
                            MembershipReferenceType.GROUP,
                            group,
                            MembershipMemberType.USER,
                            membership.getId(),
                            previousApiRole.getId());
                }
                if (applicationRoleEntity == null && previousApplicationRole != null) {
                    membershipService.removeRole(
                            MembershipReferenceType.GROUP,
                            group,
                            MembershipMemberType.USER,
                            membership.getId(),
                            previousApplicationRole.getId());
                }
                if (groupRoleEntity == null && previousGroupRole != null) {
                    membershipService.removeRole(
                            MembershipReferenceType.GROUP,
                            group,
                            MembershipMemberType.USER,
                            membership.getId(),
                            previousGroupRole.getId());
                }
            }
        }

        return Response.ok().build();
    }

    @Path("{member}")
    public GroupMemberResource groupMemberResource() {
        return resourceContext.getResource(GroupMemberResource.class);
    }
}
