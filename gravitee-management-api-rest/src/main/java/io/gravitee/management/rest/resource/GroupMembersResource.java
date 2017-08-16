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

import io.gravitee.management.model.GroupMemberEntity;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
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
                collect(Collectors.groupingBy(MemberEntity::getUsername));

        Map<String, List<MemberEntity>> membersWithApiRole = membershipService.
                getMembers(MembershipReferenceType.GROUP, group, RoleScope.API).
                stream().
                filter(Objects::nonNull).
                collect(Collectors.groupingBy(MemberEntity::getUsername));

        Set<String> usernames = new HashSet<>();
        usernames.addAll(membersWithApiRole.keySet());
        usernames.addAll(membersWithApplicationRole.keySet());

        return usernames.stream().
                map(username -> {
                    MemberEntity memberWithApiRole = Objects.isNull(membersWithApiRole.get(username)) ? null : membersWithApiRole.get(username).get(0);
                    MemberEntity memberWithApplicationRole = Objects.isNull(membersWithApplicationRole.get(username)) ? null : membersWithApplicationRole.get(username).get(0);
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
                sorted(Comparator.comparing(GroupMemberEntity::getUsername)).
                collect(Collectors.toList());
    }

    @Path("{member}")
    public GroupMemberResource groupMemberResource() {
        return resourceContext.getResource(GroupMemberResource.class);
    }
}
