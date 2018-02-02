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
import io.gravitee.management.model.GroupMemberEntity;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.MemberRoleEntity;
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.model.GroupMembership;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.GroupService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class GroupMembersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

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
    public List<GroupMemberEntity> getMembers(@PathParam("group") String group) {
        //check that group exists
        groupService.findById(group);

        Map<String, List<MemberEntity>> membersWithApplicationRole = membershipService.
                getMembers(MembershipReferenceType.GROUP, group, RoleScope.APPLICATION).
                stream().
                filter(Objects::nonNull).
                collect(Collectors.groupingBy(MemberEntity::getId));

        Map<String, List<MemberEntity>> membersWithApiRole = membershipService.
                getMembers(MembershipReferenceType.GROUP, group, RoleScope.API).
                stream().
                filter(Objects::nonNull).
                collect(Collectors.groupingBy(MemberEntity::getId));

        Set<String> ids = new HashSet<>();
        ids.addAll(membersWithApiRole.keySet());
        ids.addAll(membersWithApplicationRole.keySet());

        return ids.stream().
                map(id -> {
                    MemberEntity memberWithApiRole = Objects.isNull(membersWithApiRole.get(id)) ? null : membersWithApiRole.get(id).get(0);
                    MemberEntity memberWithApplicationRole = Objects.isNull(membersWithApplicationRole.get(id)) ? null : membersWithApplicationRole.get(id).get(0);
                    GroupMemberEntity groupMemberEntity = new GroupMemberEntity(Objects.nonNull(memberWithApiRole) ? memberWithApiRole : memberWithApplicationRole);
                    groupMemberEntity.setRoles(new HashMap<>());
                    if (Objects.nonNull(memberWithApiRole)) {
                        groupMemberEntity.getRoles().put(RoleScope.API.name(), memberWithApiRole.getRole());
                    }
                    if (Objects.nonNull(memberWithApplicationRole)) {
                        groupMemberEntity.getRoles().put(RoleScope.APPLICATION.name(), memberWithApplicationRole.getRole());
                    }
                    return groupMemberEntity;
                }).
                sorted(Comparator.comparing(GroupMemberEntity::getId)).
                collect(Collectors.toList());
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
            @Permission(value = RolePermission.MANAGEMENT_GROUP, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.MANAGEMENT_GROUP, acls = RolePermissionAction.UPDATE)
    })
    public Response addOrUpdateMember(
            @PathParam("group") String group,
            @Valid @NotNull final GroupMembership membership
    ) {
        // Check that group exists
        groupService.findById(group);

        RoleEntity previousApiRole = null, previousApplicationRole = null;

        if (membership.getId() != null) {
            previousApiRole = membershipService.getRole(
                    MembershipReferenceType.GROUP,
                    group,
                    membership.getId(),
                    RoleScope.API);

            previousApplicationRole = membershipService.getRole(
                    MembershipReferenceType.GROUP,
                    group,
                    membership.getId(),
                    RoleScope.APPLICATION);
        }

        // Process add / update before delete to avoid having a user without role
        if (membership.getRoles() != null && !membership.getRoles().isEmpty()) {
            MemberRoleEntity apiRole = membership.getRoles().
                    stream().
                    filter(r -> r.getRoleScope().equals(io.gravitee.management.model.permissions.RoleScope.API)
                            && !r.getRoleName().isEmpty()).
                    findFirst().
                    orElse(null);

            MemberRoleEntity applicationRole = membership.getRoles().
                    stream().
                    filter(r -> r.getRoleScope().equals(io.gravitee.management.model.permissions.RoleScope.APPLICATION)
                            && !r.getRoleName().isEmpty()).
                    findFirst().
                    orElse(null);

            MemberEntity updatedMembership = null;

            // Add / Update
            if (apiRole != null) {
                updatedMembership = membershipService.addOrUpdateMember(
                        new MembershipService.MembershipReference(MembershipReferenceType.GROUP, group),
                        new MembershipService.MembershipUser(membership.getId(), membership.getReference()),
                        new MembershipService.MembershipRole(RoleScope.API, apiRole.getRoleName()));
            }
            if (applicationRole != null) {
                updatedMembership = membershipService.addOrUpdateMember(
                        new MembershipService.MembershipReference(MembershipReferenceType.GROUP, group),
                        new MembershipService.MembershipUser(membership.getId(), membership.getReference()),
                        new MembershipService.MembershipRole(RoleScope.APPLICATION, applicationRole.getRoleName()));
            }

            // Delete
            if (apiRole == null && previousApiRole != null) {
                membershipService.removeRole(
                        MembershipReferenceType.GROUP,
                        group,
                        updatedMembership.getId(),
                        RoleScope.API);
            }
            if (applicationRole == null && previousApplicationRole != null) {
                membershipService.removeRole(
                        MembershipReferenceType.GROUP,
                        group,
                        updatedMembership.getId(),
                        RoleScope.APPLICATION);
            }
        }

        return Response.ok().build();
    }

    @Path("{member}")
    public GroupMemberResource groupMemberResource() {
        return resourceContext.getResource(GroupMemberResource.class);
    }
}
